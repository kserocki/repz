package com.repz.app.core.domain.workout

import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.record.RecalculatePersonalRecordsUseCase
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import com.repz.app.core.testing.record.FakePersonalRecordRepository
import com.repz.app.core.testing.transaction.FakeTransactionRunner
import com.repz.app.core.testing.workout.FakeWorkoutRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SaveFinishedWorkoutUseCaseTest {
    @Test
    fun givenANewFinishedWorkout_whenSaved_thenItSucceedsAndRecomputesItsExercises() =
        runTest {
            val exerciseA = ExerciseId(testUuid(1))
            val workout = testFinishedWorkout(seed = 1, exercises = listOf(testWorkoutExercise(seed = 1, exerciseId = exerciseA)))
            val workouts = FakeWorkoutRepository()
            val records = FakePersonalRecordRepository()
            val useCase =
                SaveFinishedWorkoutUseCase(workouts, RecalculatePersonalRecordsUseCase(workouts, records, FakeTransactionRunner()))

            val result = useCase(workout)

            assertEquals(DomainResult.Success(Unit), result)
            assertEquals(setOf(exerciseA), records.replaceForExercisesCalls.single().first)
        }

    @Test
    fun givenAnEditRemovingAnExercise_whenSaved_thenRecomputesThePreviousAndNewExerciseIdsUnion() =
        runTest {
            val exerciseA = ExerciseId(testUuid(1))
            val exerciseB = ExerciseId(testUuid(2))
            val original =
                testFinishedWorkout(
                    seed = 1,
                    exercises =
                        listOf(
                            testWorkoutExercise(seed = 1, exerciseId = exerciseA),
                            testWorkoutExercise(seed = 2, exerciseId = exerciseB),
                        ),
                )
            val edited = original.copy(exercises = listOf(testWorkoutExercise(seed = 2, exerciseId = exerciseB)))
            val workouts = FakeWorkoutRepository(listOf(original))
            val records = FakePersonalRecordRepository()
            val useCase =
                SaveFinishedWorkoutUseCase(workouts, RecalculatePersonalRecordsUseCase(workouts, records, FakeTransactionRunner()))

            useCase(edited)

            assertEquals(setOf(exerciseA, exerciseB), records.replaceForExercisesCalls.single().first)
        }

    @Test
    fun givenValidationFails_whenSaved_thenNoRecomputeHappens() =
        runTest {
            val workout = testFinishedWorkout()
            val workouts = FakeWorkoutRepository(saveFinishedWorkoutResult = DomainResult.Failure(ValidationError.NameBlank))
            val records = FakePersonalRecordRepository()
            val transactions = FakeTransactionRunner()
            val useCase = SaveFinishedWorkoutUseCase(workouts, RecalculatePersonalRecordsUseCase(workouts, records, transactions))

            val result = useCase(workout)

            assertEquals(DomainResult.Failure(ValidationError.NameBlank), result)
            assertTrue(records.replaceForExercisesCalls.isEmpty())
            assertEquals(0, transactions.callCount)
        }
}
