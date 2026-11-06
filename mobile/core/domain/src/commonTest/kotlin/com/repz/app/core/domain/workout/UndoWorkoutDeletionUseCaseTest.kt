package com.repz.app.core.domain.workout

import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.record.RecalculatePersonalRecordsUseCase
import com.repz.app.core.testing.record.FakePersonalRecordRepository
import com.repz.app.core.testing.transaction.FakeTransactionRunner
import com.repz.app.core.testing.workout.FakeWorkoutRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UndoWorkoutDeletionUseCaseTest {
    @Test
    fun givenATombstonedWorkout_whenUndone_thenItIsRestoredAndItsExercisesRecordsAreRecomputed() =
        runTest {
            val exerciseA = ExerciseId(testUuid(1))
            val workout = testFinishedWorkout(seed = 1, exercises = listOf(testWorkoutExercise(seed = 1, exerciseId = exerciseA)))
            val workouts = FakeWorkoutRepository(listOf(workout))
            workouts.deleteWorkout(workout.id)
            val records = FakePersonalRecordRepository()
            val useCase =
                UndoWorkoutDeletionUseCase(workouts, RecalculatePersonalRecordsUseCase(workouts, records, FakeTransactionRunner()))

            useCase(workout.id)

            assertEquals(listOf(workout.id), workouts.undoWorkoutDeletionCalls)
            assertEquals(setOf(exerciseA), records.replaceForExercisesCalls.single().first)
        }
}
