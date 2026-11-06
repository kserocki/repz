package com.repz.app.core.data.workout

import androidx.room3.Room
import com.repz.app.core.data.TestClock
import com.repz.app.core.data.testUuid
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.buildRepzDatabase
import com.repz.app.core.database.sync.OutboxOperation
import com.repz.app.core.database.sync.SyncEntityType
import com.repz.app.core.database.workout.WorkoutEntity
import com.repz.app.core.database.workout.WorkoutExerciseEntity
import com.repz.app.core.database.workout.WorkoutSetEntity
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.Rpe
import com.repz.app.core.domain.model.SetType
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.model.Workout
import com.repz.app.core.domain.model.WorkoutExercise
import com.repz.app.core.domain.model.WorkoutExerciseId
import com.repz.app.core.domain.model.WorkoutId
import com.repz.app.core.domain.model.WorkoutSet
import com.repz.app.core.domain.model.WorkoutSetId
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

class OfflineFirstWorkoutRepositoryTest {
    // Room needs a real dispatcher for its query/invalidation work, not the runTest scheduler.
    private val database = Room.inMemoryDatabaseBuilder<RepzDatabase>().buildRepzDatabase(Dispatchers.IO)
    private val workoutDao = database.workoutDao()
    private val outboxDao = database.outboxDao()
    private val clock = TestClock()
    private val syncScheduler = FakeSyncScheduler()
    private val repository = OfflineFirstWorkoutRepository(database, workoutDao, outboxDao, syncScheduler, clock)

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun givenAFinishedWorkout_whenSaved_thenTheEntityAndExactlyOneUpsertOutboxRowExist() =
        runTest {
            val workout = finishedWorkout()

            val result = repository.saveFinishedWorkout(workout)

            assertIs<DomainResult.Success<Unit>>(result)
            assertEquals(workout, repository.findWorkoutIncludingDeleted(workout.id))
            val entries = outboxDao.findReadyToPush(clock.now(), limit = 100)
            assertEquals(1, entries.size)
            assertEquals(SyncEntityType.WORKOUT, entries.single().entityType)
            assertEquals(OutboxOperation.UPSERT, entries.single().operation)
        }

    @Test
    fun givenTheSameWorkoutSavedTenTimes_whenEnqueued_thenOneOutboxRowRemainsWithRevisionTen() =
        runTest {
            val workout = finishedWorkout()

            repeat(10) { repository.saveFinishedWorkout(workout) }

            val entries = outboxDao.findReadyToPush(clock.now(), limit = 100)
            assertEquals(1, entries.size)
            assertEquals(10, entries.single().revision)
        }

    @Test
    fun givenAnInvalidWorkout_whenSaved_thenFailsAndWritesNothingAndNeverRequestsSync() =
        runTest {
            val workout = finishedWorkout(name = "   ")

            val result = repository.saveFinishedWorkout(workout)

            assertEquals(DomainResult.Failure(ValidationError.NameBlank), result)
            assertNull(repository.findWorkoutIncludingDeleted(workout.id))
            assertEquals(0, outboxDao.countPending())
            assertEquals(0, syncScheduler.requestSyncCallCount)
        }

    @Test
    fun givenASuccessfulSave_whenSaved_thenRequestSyncIsCalledExactlyOnce() =
        runTest {
            repository.saveFinishedWorkout(finishedWorkout())

            assertEquals(1, syncScheduler.requestSyncCallCount)
        }

    @Test
    fun givenASavedWorkout_whenDeleted_thenHiddenFromObserversAndHeldUntilTheUndoWindowElapses() =
        runTest {
            val workout = finishedWorkout()
            repository.saveFinishedWorkout(workout)

            repository.deleteWorkout(workout.id)

            assertNull(repository.observeWorkout(workout.id).first())
            assertTrue(repository.observeFinishedWorkouts().first().isEmpty())
            assertTrue(outboxDao.findReadyToPush(clock.now(), limit = 100).isEmpty())
            val entries = outboxDao.findReadyToPush(clock.now() + DELETE_UNDO_WINDOW, limit = 100)
            assertEquals(1, entries.size)
            assertEquals(OutboxOperation.DELETE, entries.single().operation)
            // deleteWorkout itself never requests a sync: the row stays held until confirmed or undone.
            assertEquals(1, syncScheduler.requestSyncCallCount)
        }

    @Test
    fun givenADeletedWorkout_whenUndone_thenVisibleAgainWithAnUpsertAndTheHoldCleared() =
        runTest {
            val workout = finishedWorkout()
            repository.saveFinishedWorkout(workout)
            repository.deleteWorkout(workout.id)

            repository.undoWorkoutDeletion(workout.id)

            assertEquals(workout, repository.observeWorkout(workout.id).first())
            val entries = outboxDao.findReadyToPush(clock.now(), limit = 100)
            assertEquals(1, entries.size)
            assertEquals(OutboxOperation.UPSERT, entries.single().operation)
            assertEquals(2, syncScheduler.requestSyncCallCount)
        }

    @Test
    fun givenADeletedWorkout_whenConfirmed_thenTheHoldIsReleasedAndSyncIsRequested() =
        runTest {
            val workout = finishedWorkout()
            repository.saveFinishedWorkout(workout)
            repository.deleteWorkout(workout.id)

            repository.confirmWorkoutDeletion(workout.id)

            val entries = outboxDao.findReadyToPush(clock.now(), limit = 100)
            assertEquals(1, entries.size)
            assertEquals(OutboxOperation.DELETE, entries.single().operation)
            assertEquals(2, syncScheduler.requestSyncCallCount)
        }

    @Test
    fun givenAnActiveWorkout_whenSaved_thenNoOutboxRowIsEnqueued() =
        runTest {
            val activeWorkout = finishedWorkout(finishedAt = null)

            repository.saveActiveWorkout(activeWorkout)

            assertEquals(activeWorkout, repository.observeActiveWorkout().first())
            assertEquals(0, outboxDao.countPending())
            assertEquals(0, syncScheduler.requestSyncCallCount)
        }

    @Test
    fun givenAFinishedWorkout_whenSavedAsActive_thenThePreconditionFails() =
        runTest {
            assertFailsWith<IllegalArgumentException> { repository.saveActiveWorkout(finishedWorkout()) }
        }

    @Test
    fun givenAnUnfinishedActiveWorkout_whenDiscarded_thenItIsPurged() =
        runTest {
            val activeWorkout = finishedWorkout(finishedAt = null)
            repository.saveActiveWorkout(activeWorkout)

            repository.discardActiveWorkout(activeWorkout.id)

            assertNull(repository.findWorkoutIncludingDeleted(activeWorkout.id))
        }

    @Test
    fun givenAFinishedWorkout_whenDiscardIsCalledOnIt_thenItIsNotPurged() =
        runTest {
            val workout = finishedWorkout()
            repository.saveFinishedWorkout(workout)

            repository.discardActiveWorkout(workout.id)

            assertNotNull(repository.findWorkoutIncludingDeleted(workout.id))
        }

    @Test
    fun givenAWorkoutWithRpeAndASetType_whenSavedAndRead_thenEveryFieldRoundTrips() =
        runTest {
            val set =
                workoutSet(
                    type = SetType.DROP,
                    weight = Weight.fromKilograms(82.5),
                    reps = 8,
                    rpe = rpeOf(8.5),
                    completedAt = Instant.parse("2024-01-01T10:45:00Z"),
                )
            val workout = finishedWorkout(exercises = listOf(workoutExercise(sets = listOf(set))))

            repository.saveFinishedWorkout(workout)

            assertEquals(workout, repository.findWorkoutIncludingDeleted(workout.id))
        }

    @Test
    fun givenAWorkoutWithAnUnknownStoredSetType_whenObserved_thenDroppedFromTheListAndNullForASingleRead() =
        runTest {
            val workoutId = testUuid("20")
            val exerciseEntityId = testUuid("21")
            val setEntityId = testUuid("22")
            workoutDao.saveSyncedWorkout(
                WorkoutEntity(
                    id = workoutId,
                    templateId = null,
                    name = "Corrupt",
                    notes = "",
                    startedAt = Instant.parse("2024-01-01T10:00:00Z"),
                    finishedAt = Instant.parse("2024-01-01T11:00:00Z"),
                    updatedAt = Instant.parse("2024-01-01T11:00:00Z"),
                    serverVersion = null,
                    isDeleted = false,
                ),
                listOf(
                    WorkoutExerciseEntity(
                        id = exerciseEntityId,
                        workoutId = workoutId,
                        exerciseId = testUuid("23"),
                        position = 0,
                        notes = "",
                    ),
                ),
                listOf(
                    WorkoutSetEntity(
                        id = setEntityId,
                        workoutExerciseId = exerciseEntityId,
                        position = 0,
                        type = "bogus",
                        weightGrams = 1000,
                        reps = 5,
                        rpe = null,
                        completedAt = null,
                    ),
                ),
            )

            assertTrue(repository.observeFinishedWorkouts().first().isEmpty())
            assertNull(repository.observeWorkout(WorkoutId(workoutId)).first())
            assertNull(repository.findWorkoutIncludingDeleted(WorkoutId(workoutId)))
        }

    private fun finishedWorkout(
        id: Uuid = testUuid("1"),
        name: String = "Push day",
        startedAt: Instant = Instant.parse("2024-01-01T10:00:00Z"),
        finishedAt: Instant? = Instant.parse("2024-01-01T11:00:00Z"),
        exercises: List<WorkoutExercise> = listOf(workoutExercise()),
    ) = Workout(
        id = WorkoutId(id),
        templateId = null,
        name = name,
        notes = "",
        startedAt = startedAt,
        finishedAt = finishedAt,
        exercises = exercises,
    )

    private fun workoutExercise(
        id: Uuid = testUuid("2"),
        exerciseId: Uuid = testUuid("3"),
        sets: List<WorkoutSet> = listOf(workoutSet()),
    ) = WorkoutExercise(id = WorkoutExerciseId(id), exerciseId = ExerciseId(exerciseId), notes = "", sets = sets)

    private fun workoutSet(
        id: Uuid = testUuid("4"),
        type: SetType = SetType.NORMAL,
        weight: Weight = Weight.fromKilograms(50.0),
        reps: Int = 5,
        rpe: Rpe? = null,
        completedAt: Instant? = Instant.parse("2024-01-01T10:30:00Z"),
    ) = WorkoutSet(id = WorkoutSetId(id), type = type, weight = weight, reps = reps, rpe = rpe, completedAt = completedAt)

    private fun rpeOf(value: Double) = (Rpe.of(value) as DomainResult.Success).value
}
