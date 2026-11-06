package com.repz.app.core.data.template

import androidx.room3.Room
import com.repz.app.core.data.TestClock
import com.repz.app.core.data.testUuid
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.buildRepzDatabase
import com.repz.app.core.database.sync.OutboxOperation
import com.repz.app.core.database.sync.SyncEntityType
import com.repz.app.core.database.template.TemplateExerciseEntity
import com.repz.app.core.database.template.WorkoutTemplateEntity
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.TemplateExercise
import com.repz.app.core.domain.model.WorkoutTemplate
import com.repz.app.core.domain.model.WorkoutTemplateId
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
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.Uuid

class OfflineFirstWorkoutTemplateRepositoryTest {
    // Room needs a real dispatcher for its query/invalidation work, not the runTest scheduler.
    private val database = Room.inMemoryDatabaseBuilder<RepzDatabase>().buildRepzDatabase(Dispatchers.IO)
    private val templateDao = database.templateDao()
    private val outboxDao = database.outboxDao()
    private val clock = TestClock()
    private val syncScheduler = FakeSyncScheduler()
    private val repository = OfflineFirstWorkoutTemplateRepository(database, templateDao, outboxDao, syncScheduler, clock)

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun givenATemplate_whenSaved_thenTheEntityAndExactlyOneUpsertOutboxRowExist() =
        runTest {
            val template = workoutTemplate()

            val result = repository.saveTemplate(template)

            assertIs<DomainResult.Success<Unit>>(result)
            assertEquals(template, repository.observeTemplate(template.id).first())
            val entries = outboxDao.findReadyToPush(clock.now(), limit = 100)
            assertEquals(1, entries.size)
            assertEquals(SyncEntityType.TEMPLATE, entries.single().entityType)
            assertEquals(OutboxOperation.UPSERT, entries.single().operation)
        }

    @Test
    fun givenTheSameTemplateSavedTenTimes_whenEnqueued_thenOneOutboxRowRemainsWithRevisionTen() =
        runTest {
            val template = workoutTemplate()

            repeat(10) { repository.saveTemplate(template) }

            val entries = outboxDao.findReadyToPush(clock.now(), limit = 100)
            assertEquals(1, entries.size)
            assertEquals(10, entries.single().revision)
        }

    @Test
    fun givenAnInvalidTemplate_whenSaved_thenFailsAndWritesNothingAndNeverRequestsSync() =
        runTest {
            val template = workoutTemplate(name = "")

            val result = repository.saveTemplate(template)

            assertEquals(DomainResult.Failure(ValidationError.NameBlank), result)
            assertNull(repository.observeTemplate(template.id).first())
            assertEquals(0, outboxDao.countPending())
            assertEquals(0, syncScheduler.requestSyncCallCount)
        }

    @Test
    fun givenASavedTemplate_whenDeleted_thenHiddenFromObserversAndHeldUntilTheUndoWindowElapses() =
        runTest {
            val template = workoutTemplate()
            repository.saveTemplate(template)

            repository.deleteTemplate(template.id)

            assertNull(repository.observeTemplate(template.id).first())
            assertTrue(repository.observeTemplates().first().isEmpty())
            assertTrue(outboxDao.findReadyToPush(clock.now(), limit = 100).isEmpty())
            val entries = outboxDao.findReadyToPush(clock.now() + DELETE_UNDO_WINDOW, limit = 100)
            assertEquals(1, entries.size)
            assertEquals(OutboxOperation.DELETE, entries.single().operation)
        }

    @Test
    fun givenADeletedTemplate_whenUndone_thenVisibleAgainWithAnUpsertAndTheHoldCleared() =
        runTest {
            val template = workoutTemplate()
            repository.saveTemplate(template)
            repository.deleteTemplate(template.id)

            repository.undoTemplateDeletion(template.id)

            assertEquals(template, repository.observeTemplate(template.id).first())
            val entries = outboxDao.findReadyToPush(clock.now(), limit = 100)
            assertEquals(1, entries.size)
            assertEquals(OutboxOperation.UPSERT, entries.single().operation)
            assertEquals(2, syncScheduler.requestSyncCallCount)
        }

    @Test
    fun givenADeletedTemplate_whenConfirmed_thenTheHoldIsReleasedAndSyncIsRequested() =
        runTest {
            val template = workoutTemplate()
            repository.saveTemplate(template)
            repository.deleteTemplate(template.id)

            repository.confirmTemplateDeletion(template.id)

            val entries = outboxDao.findReadyToPush(clock.now(), limit = 100)
            assertEquals(1, entries.size)
            assertEquals(2, syncScheduler.requestSyncCallCount)
        }

    @Test
    fun givenATemplateWithOpenAndRangedTargetRepsAndRest_whenSavedAndRead_thenEveryFieldRoundTrips() =
        runTest {
            val openEnded = templateExercise(exerciseId = testUuid("10"), targetReps = null, rest = 90.seconds)
            val ranged = templateExercise(exerciseId = testUuid("11"), targetReps = 8..12, rest = 150.seconds)
            val template = workoutTemplate(exercises = listOf(openEnded, ranged))

            repository.saveTemplate(template)

            assertEquals(template, repository.observeTemplate(template.id).first())
        }

    @Test
    fun givenATemplateWithAHalfNullStoredTargetRepsPair_whenObserved_thenDroppedFromTheListAndNullForASingleRead() =
        runTest {
            val templateId = testUuid("30")
            templateDao.saveSyncedTemplate(
                WorkoutTemplateEntity(
                    id = templateId,
                    name = "Corrupt",
                    notes = "",
                    updatedAt = Instant.parse("2024-01-01T11:00:00Z"),
                    serverVersion = null,
                    isDeleted = false,
                ),
                listOf(
                    TemplateExerciseEntity(
                        templateId = templateId,
                        position = 0,
                        exerciseId = testUuid("31"),
                        targetSets = 3,
                        targetRepsMin = 8,
                        targetRepsMax = null,
                        restSeconds = 90,
                    ),
                ),
            )

            assertTrue(repository.observeTemplates().first().isEmpty())
            assertNull(repository.observeTemplate(WorkoutTemplateId(templateId)).first())
        }

    private fun workoutTemplate(
        id: Uuid = testUuid("1"),
        name: String = "Push day",
        exercises: List<TemplateExercise> = listOf(templateExercise()),
    ) = WorkoutTemplate(id = WorkoutTemplateId(id), name = name, notes = "", exercises = exercises)

    private fun templateExercise(
        exerciseId: Uuid = testUuid("2"),
        targetSets: Int = 3,
        targetReps: IntRange? = 5..10,
        rest: Duration = 90.seconds,
    ) = TemplateExercise(exerciseId = ExerciseId(exerciseId), targetSets = targetSets, targetReps = targetReps, rest = rest)
}
