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

class RecalculateAllPersonalRecordsUseCaseTest {
    @Test
    fun givenFinishedWorkouts_whenInvoked_thenTheWholeTableIsReplaced() =
        runTest {
            val exerciseA = ExerciseId(testUuid(1))
            val exerciseB = ExerciseId(testUuid(2))
            val workouts =
                FakeWorkoutRepository(
                    listOf(
                        testFinishedWorkout(seed = 1, exercises = listOf(testWorkoutExercise(seed = 1, exerciseId = exerciseA))),
                        testFinishedWorkout(seed = 2, exercises = listOf(testWorkoutExercise(seed = 2, exerciseId = exerciseB))),
                    ),
                )
            val records = FakePersonalRecordRepository()
            val useCase = RecalculateAllPersonalRecordsUseCase(workouts, records, FakeTransactionRunner())

            useCase()

            val replaced = records.replaceAllCalls.single()
            assertEquals(setOf(exerciseA, exerciseB), replaced.map { it.exerciseId }.toSet())
        }

    @Test
    fun givenInvoked_whenRecomputing_thenItRunsInsideOneWriteTransaction() =
        runTest {
            val transactions = FakeTransactionRunner()
            val useCase = RecalculateAllPersonalRecordsUseCase(FakeWorkoutRepository(), FakePersonalRecordRepository(), transactions)

            useCase()

            assertEquals(1, transactions.callCount)
        }

    @Test
    fun givenNoFinishedWorkouts_whenInvoked_thenTheTableIsReplacedWithAnEmptyList() =
        runTest {
            val records = FakePersonalRecordRepository()
            val useCase = RecalculateAllPersonalRecordsUseCase(FakeWorkoutRepository(), records, FakeTransactionRunner())

            useCase()

            assertTrue(records.replaceAllCalls.single().isEmpty())
        }
}
