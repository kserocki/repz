package com.repz.app.core.data.transaction

import androidx.room3.Room
import com.repz.app.core.data.testUuid
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.buildRepzDatabase
import com.repz.app.core.database.record.PersonalRecordEntity
import com.repz.app.core.database.workout.WorkoutEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

private class RollbackTestException : Exception()

class RoomTransactionRunnerTest {
    // Room needs a real dispatcher for its query/invalidation work, not the runTest scheduler.
    private val database = Room.inMemoryDatabaseBuilder<RepzDatabase>().buildRepzDatabase(Dispatchers.IO)
    private val runner = RoomTransactionRunner(database)

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun givenANonThrowingBlock_whenRunInAWriteTransaction_thenItsResultIsReturnedAndBothWritesCommit() =
        runTest {
            val workoutId = testUuid("1")
            val exerciseId = testUuid("2")

            val result =
                runner.inWriteTransaction {
                    database.workoutDao().saveLocalWorkout(workout(workoutId), emptyList(), emptyList())
                    database.personalRecordDao().replaceForExercises(listOf(exerciseId), listOf(record(exerciseId)))
                    "done"
                }

            assertEquals("done", result)
            assertEquals(
                workoutId,
                database
                    .workoutDao()
                    .findWorkoutIncludingDeleted(workoutId)
                    ?.workout
                    ?.id,
            )
            assertEquals(
                1,
                database
                    .personalRecordDao()
                    .observeForExercise(exerciseId)
                    .first()
                    .size,
            )
        }

    @Test
    fun givenAThrowingBlock_whenItSavesAWorkoutAndReplacesARecord_thenBothAreRolledBack() =
        runTest {
            val workoutId = testUuid("1")
            val exerciseId = testUuid("2")

            assertFailsWith<RollbackTestException> {
                runner.inWriteTransaction {
                    database.workoutDao().saveLocalWorkout(workout(workoutId), emptyList(), emptyList())
                    database.personalRecordDao().replaceForExercises(listOf(exerciseId), listOf(record(exerciseId)))
                    throw RollbackTestException()
                }
            }

            assertNull(database.workoutDao().findWorkoutIncludingDeleted(workoutId))
            assertTrue(
                database
                    .personalRecordDao()
                    .observeForExercise(exerciseId)
                    .first()
                    .isEmpty(),
            )
        }

    private fun workout(id: Uuid) =
        WorkoutEntity(
            id = id,
            templateId = null,
            name = "Push day",
            notes = "",
            startedAt = Instant.parse("2024-01-01T10:00:00Z"),
            finishedAt = Instant.parse("2024-01-01T11:00:00Z"),
            updatedAt = Instant.parse("2024-01-01T11:00:00Z"),
            serverVersion = null,
            isDeleted = false,
        )

    private fun record(exerciseId: Uuid) =
        PersonalRecordEntity(
            exerciseId = exerciseId,
            type = "heaviest",
            weightGrams = 100_000,
            reps = 5,
            workoutId = testUuid("90"),
            setId = testUuid("91"),
            achievedAt = Instant.parse("2024-01-01T10:00:00Z"),
        )
}
