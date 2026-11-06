package com.repz.app.core.domain.workout

import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.WorkoutId
import com.repz.app.core.domain.record.RecalculatePersonalRecordsUseCase
import com.repz.app.core.testing.record.FakePersonalRecordRepository
import com.repz.app.core.testing.transaction.FakeTransactionRunner
import com.repz.app.core.testing.workout.FakeWorkoutRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeleteWorkoutUseCaseTest {
    @Test
    fun givenAFinishedWorkout_whenDeleted_thenItIsTombstonedAndItsExercisesRecordsAreRecomputed() =
        runTest {
            val exerciseA = ExerciseId(testUuid(1))
            val workout = testFinishedWorkout(seed = 1, exercises = listOf(testWorkoutExercise(seed = 1, exerciseId = exerciseA)))
            val workouts = FakeWorkoutRepository(listOf(workout))
            val records = FakePersonalRecordRepository()
            val useCase = DeleteWorkoutUseCase(workouts, RecalculatePersonalRecordsUseCase(workouts, records, FakeTransactionRunner()))

            useCase(workout.id)

            assertEquals(listOf(workout.id), workouts.deleteWorkoutCalls)
            assertEquals(setOf(exerciseA), records.replaceForExercisesCalls.single().first)
        }

    @Test
    fun givenAMissingWorkout_whenDeleted_thenNoRecomputeHappens() =
        runTest {
            val workouts = FakeWorkoutRepository()
            val records = FakePersonalRecordRepository()
            val transactions = FakeTransactionRunner()
            val useCase = DeleteWorkoutUseCase(workouts, RecalculatePersonalRecordsUseCase(workouts, records, transactions))

            useCase(WorkoutId(testUuid(99)))

            assertTrue(records.replaceForExercisesCalls.isEmpty())
            assertEquals(0, transactions.callCount)
        }
}
