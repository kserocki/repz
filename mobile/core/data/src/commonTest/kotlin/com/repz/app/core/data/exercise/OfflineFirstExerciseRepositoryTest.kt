package com.repz.app.core.data.exercise

import androidx.room3.Room
import com.repz.app.core.data.TestClock
import com.repz.app.core.data.testUuid
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.buildRepzDatabase
import com.repz.app.core.database.exercise.ExerciseEntity
import com.repz.app.core.database.sync.OutboxOperation
import com.repz.app.core.database.sync.SyncEntityType
import com.repz.app.core.domain.model.Equipment
import com.repz.app.core.domain.model.Exercise
import com.repz.app.core.domain.model.ExerciseAttribution
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.ExerciseOrigin
import com.repz.app.core.domain.model.ExerciseTranslation
import com.repz.app.core.domain.model.Muscle
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import com.repz.app.core.domain.sync.DELETE_UNDO_WINDOW
import com.repz.app.core.testing.sync.FakeSyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

class OfflineFirstExerciseRepositoryTest {
    // Room needs a real dispatcher for its query/invalidation work, not the runTest scheduler.
    private val database = Room.inMemoryDatabaseBuilder<RepzDatabase>().buildRepzDatabase(Dispatchers.IO)
    private val exerciseDao = database.exerciseDao()
    private val outboxDao = database.outboxDao()
    private val clock = TestClock()
    private val syncScheduler = FakeSyncScheduler()
    private val repository = OfflineFirstExerciseRepository(database, exerciseDao, outboxDao, syncScheduler, clock)

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun givenACustomExercise_whenSaved_thenTheEntityAndExactlyOneUpsertOutboxRowExist() =
        runTest {
            val exercise = customExercise()

            val result = repository.saveCustomExercise(exercise)

            assertIs<DomainResult.Success<Unit>>(result)
            assertEquals(exercise, repository.observeExercise(exercise.id).first())
            val entries = outboxDao.findReadyToPush(clock.now(), limit = 100)
            assertEquals(1, entries.size)
            assertEquals(SyncEntityType.EXERCISE, entries.single().entityType)
            assertEquals(OutboxOperation.UPSERT, entries.single().operation)
        }

    @Test
    fun givenTheSameCustomExerciseSavedTenTimes_whenEnqueued_thenOneOutboxRowRemainsWithRevisionTen() =
        runTest {
            val exercise = customExercise()

            repeat(10) { repository.saveCustomExercise(exercise) }

            val entries = outboxDao.findReadyToPush(clock.now(), limit = 100)
            assertEquals(1, entries.size)
            assertEquals(10, entries.single().revision)
        }

    @Test
    fun givenACatalogOriginExercise_whenSavedAsCustom_thenThePreconditionFails() =
        runTest {
            assertFailsWith<IllegalArgumentException> { repository.saveCustomExercise(catalogExercise()) }
        }

    @Test
    fun givenACustomExerciseWithThePrimaryMuscleRepeatedAsSecondary_whenSaved_thenFailsAndWritesNothingAndNeverRequestsSync() =
        runTest {
            val exercise = customExercise(primaryMuscle = Muscle.BICEPS, secondaryMuscles = setOf(Muscle.BICEPS))

            val result = repository.saveCustomExercise(exercise)

            assertEquals(DomainResult.Failure(ValidationError.SecondaryMusclesIncludePrimary), result)
            assertNull(repository.observeExercise(exercise.id).first())
            assertEquals(0, outboxDao.countPending())
            assertEquals(0, syncScheduler.requestSyncCallCount)
        }

    @Test
    fun givenACatalogExercise_whenDeleteCustomExerciseIsCalledOnIt_thenItIsANoOpAndStaysVisible() =
        runTest {
            val catalog = catalogExercise()
            exerciseDao.saveSyncedExercise(catalog.toEntity(clock.now()), catalog.toTranslationEntities())

            repository.deleteCustomExercise(catalog.id)

            assertEquals(catalog, repository.observeExercise(catalog.id).first())
            assertEquals(0, outboxDao.countPending())
        }

    @Test
    fun givenAnUnknownId_whenDeleteCustomExerciseIsCalled_thenItIsANoOp() =
        runTest {
            repository.deleteCustomExercise(ExerciseId(testUuid("40")))

            assertEquals(0, outboxDao.countPending())
        }

    @Test
    fun givenASavedCustomExercise_whenDeleted_thenHiddenFromObserversAndHeldUntilTheUndoWindowElapses() =
        runTest {
            val exercise = customExercise()
            repository.saveCustomExercise(exercise)

            repository.deleteCustomExercise(exercise.id)

            assertNull(repository.observeExercise(exercise.id).first())
            assertTrue(repository.observeExercises().first().isEmpty())
            assertTrue(outboxDao.findReadyToPush(clock.now(), limit = 100).isEmpty())
            val entries = outboxDao.findReadyToPush(clock.now() + DELETE_UNDO_WINDOW, limit = 100)
            assertEquals(1, entries.size)
            assertEquals(OutboxOperation.DELETE, entries.single().operation)
        }

    @Test
    fun givenADeletedCustomExercise_whenUndone_thenVisibleAgainWithAnUpsertAndTheHoldCleared() =
        runTest {
            val exercise = customExercise()
            repository.saveCustomExercise(exercise)
            repository.deleteCustomExercise(exercise.id)

            repository.undoCustomExerciseDeletion(exercise.id)

            assertEquals(exercise, repository.observeExercise(exercise.id).first())
            val entries = outboxDao.findReadyToPush(clock.now(), limit = 100)
            assertEquals(1, entries.size)
            assertEquals(OutboxOperation.UPSERT, entries.single().operation)
            assertEquals(2, syncScheduler.requestSyncCallCount)
        }

    @Test
    fun givenADeletedCustomExercise_whenConfirmed_thenTheHoldIsReleasedAndSyncIsRequested() =
        runTest {
            val exercise = customExercise()
            repository.saveCustomExercise(exercise)
            repository.deleteCustomExercise(exercise.id)

            repository.confirmCustomExerciseDeletion(exercise.id)

            val entries = outboxDao.findReadyToPush(clock.now(), limit = 100)
            assertEquals(1, entries.size)
            assertEquals(2, syncScheduler.requestSyncCallCount)
        }

    @Test
    fun givenACatalogExerciseWithAttributionAndTranslations_whenSavedAndRead_thenEveryFieldRoundTrips() =
        runTest {
            val catalog = catalogExercise()

            exerciseDao.saveSyncedExercise(catalog.toEntity(clock.now()), catalog.toTranslationEntities())

            assertEquals(catalog, repository.observeExercise(catalog.id).first())
            assertEquals(listOf(catalog), repository.findExercisesIncludingDeleted(setOf(catalog.id)))
        }

    @Test
    fun givenACatalogExerciseMissingAttribution_whenObserved_thenDroppedFromTheListAndNullForASingleRead() =
        runTest {
            val id = testUuid("41")
            exerciseDao.saveSyncedExercise(
                ExerciseEntity(
                    id = id,
                    name = "Bench press",
                    primaryMuscle = "chest",
                    secondaryMuscles = emptyList(),
                    equipment = "barbell",
                    isCustom = false,
                    imageUrl = null,
                    license = null,
                    author = null,
                    sourceUrl = null,
                    updatedAt = Instant.parse("2024-01-01T11:00:00Z"),
                    serverVersion = null,
                    isDeleted = false,
                ),
                emptyList(),
            )

            assertTrue(repository.observeExercises().first().isEmpty())
            assertNull(repository.observeExercise(ExerciseId(id)).first())
        }

    @Test
    fun givenAnExerciseWithAnUnknownStoredPrimaryMuscle_whenObserved_thenDroppedFromTheListAndNullForASingleRead() =
        runTest {
            val id = testUuid("41")
            exerciseDao.saveSyncedExercise(
                ExerciseEntity(
                    id = id,
                    name = "Mystery move",
                    primaryMuscle = "wings",
                    secondaryMuscles = emptyList(),
                    equipment = "bodyweight",
                    isCustom = true,
                    imageUrl = null,
                    license = null,
                    author = null,
                    sourceUrl = null,
                    updatedAt = Instant.parse("2024-01-01T11:00:00Z"),
                    serverVersion = null,
                    isDeleted = false,
                ),
                emptyList(),
            )

            assertTrue(repository.observeExercises().first().isEmpty())
            assertNull(repository.observeExercise(ExerciseId(id)).first())
        }

    private fun customExercise(
        id: Uuid = testUuid("1"),
        name: String = "My curl",
        primaryMuscle: Muscle = Muscle.BICEPS,
        secondaryMuscles: Set<Muscle> = emptySet(),
        equipment: Equipment = Equipment.DUMBBELL,
    ) = Exercise(
        id = ExerciseId(id),
        name = name,
        primaryMuscle = primaryMuscle,
        secondaryMuscles = secondaryMuscles,
        equipment = equipment,
        origin = ExerciseOrigin.Custom,
        translations = emptyList(),
    )

    private fun catalogExercise(
        id: Uuid = testUuid("2"),
        name: String = "Bench press",
    ) = Exercise(
        id = ExerciseId(id),
        name = name,
        primaryMuscle = Muscle.CHEST,
        secondaryMuscles = setOf(Muscle.TRICEPS, Muscle.SHOULDERS),
        equipment = Equipment.BARBELL,
        origin =
            ExerciseOrigin.Catalog(
                imageUrl = "https://example.com/bench.webp",
                attribution = ExerciseAttribution(license = "CC BY-SA 4.0", author = "wger", sourceUrl = "https://wger.de/exercise/1"),
            ),
        translations = listOf(ExerciseTranslation(languageTag = "en", name = "Bench press", instructions = "Lie on the bench.")),
    )
}
