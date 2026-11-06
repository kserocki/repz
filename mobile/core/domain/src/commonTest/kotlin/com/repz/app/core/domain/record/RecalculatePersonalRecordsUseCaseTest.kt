package com.repz.app.core.domain.record

import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.workout.testFinishedWorkout
import com.repz.app.core.domain.workout.testUuid
import com.repz.app.core.domain.workout.testWorkoutExercise
import com.repz.app.core.testing.record.FakePersonalRecordRepository
import com.repz.app.core.testing.transaction.FakeTransactionRunner
import com.repz.app.core.testing.workout.FakeWorkoutRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecalculatePersonalRecordsUseCaseTest {
    @Test
    fun givenAnEmptyExerciseIdSet_whenInvoked_thenNothingHappens() =
        runTest {
            val workouts = FakeWorkoutRepository(listOf(testFinishedWorkout()))
            val records = FakePersonalRecordRepository()
            val transactions = FakeTransactionRunner()
            val useCase = RecalculatePersonalRecordsUseCase(workouts, records, transactions)

            useCase(emptySet())

            assertEquals(0, transactions.callCount)
            assertTrue(records.replaceForExercisesCalls.isEmpty())
        }

    @Test
    fun givenFinishedWorkoutsContainingTheRequestedExercise_whenInvoked_thenTheirRecordsAreReplaced() =
        runTest {
            val exerciseA = ExerciseId(testUuid(1))
            val workout = testFinishedWorkout(seed = 1, exercises = listOf(testWorkoutExercise(seed = 1, exerciseId = exerciseA)))
            val workouts = FakeWorkoutRepository(listOf(workout))
            val records = FakePersonalRecordRepository()
            val useCase = RecalculatePersonalRecordsUseCase(workouts, records, FakeTransactionRunner())

            useCase(setOf(exerciseA))

            val (requestedIds, replacedRecords) = records.replaceForExercisesCalls.single()
            assertEquals(setOf(exerciseA), requestedIds)
            assertTrue(replacedRecords.all { it.exerciseId == exerciseA })
            assertTrue(replacedRecords.isNotEmpty())
        }

    @Test
    fun givenAWorkoutContainingBothTheRequestedAndAnotherExercise_whenInvoked_thenOnlyTheRequestedExercisesRecordsAreReplaced() =
        runTest {
            val exerciseA = ExerciseId(testUuid(1))
            val exerciseB = ExerciseId(testUuid(2))
            val workout =
                testFinishedWorkout(
                    seed = 1,
                    exercises =
                        listOf(
                            testWorkoutExercise(seed = 1, exerciseId = exerciseA),
                            testWorkoutExercise(seed = 2, exerciseId = exerciseB),
                        ),
                )
            val workouts = FakeWorkoutRepository(listOf(workout))
            val records = FakePersonalRecordRepository()
            val useCase = RecalculatePersonalRecordsUseCase(workouts, records, FakeTransactionRunner())

            useCase(setOf(exerciseA))

            val (_, replacedRecords) = records.replaceForExercisesCalls.single()
            assertTrue(replacedRecords.all { it.exerciseId == exerciseA })
        }

    @Test
    fun givenInvoked_whenRecomputing_thenItRunsInsideOneWriteTransaction() =
        runTest {
            val exerciseA = ExerciseId(testUuid(1))
            val workout = testFinishedWorkout(seed = 1, exercises = listOf(testWorkoutExercise(seed = 1, exerciseId = exerciseA)))
            val transactions = FakeTransactionRunner()
            val useCase =
                RecalculatePersonalRecordsUseCase(FakeWorkoutRepository(listOf(workout)), FakePersonalRecordRepository(), transactions)

            useCase(setOf(exerciseA))

            assertEquals(1, transactions.callCount)
        }
}
