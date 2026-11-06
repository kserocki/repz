package com.repz.app.core.domain.workout

import com.repz.app.core.domain.record.RecalculatePersonalRecordsUseCase
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import com.repz.app.core.testing.record.FakePersonalRecordRepository
import com.repz.app.core.testing.time.MutableClock
import com.repz.app.core.testing.transaction.FakeTransactionRunner
import com.repz.app.core.testing.workout.FakeWorkoutRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class FinishActiveWorkoutUseCaseTest {
    private fun saveFinished(workouts: FakeWorkoutRepository): SaveFinishedWorkoutUseCase =
        SaveFinishedWorkoutUseCase(
            workouts,
            RecalculatePersonalRecordsUseCase(workouts, FakePersonalRecordRepository(), FakeTransactionRunner()),
        )

    @Test
    fun givenNoActiveWorkout_whenFinished_thenItFails() =
        runTest {
            val workouts = FakeWorkoutRepository()
            val useCase = FinishActiveWorkoutUseCase(workouts, saveFinished(workouts), MutableClock(Instant.fromEpochSeconds(0)))

            val result = useCase()

            assertEquals(DomainResult.Failure(FinishWorkoutError.NoActiveWorkout), result)
        }

    @Test
    fun givenNoCompletedSet_whenFinished_thenItFailsWithoutSaving() =
        runTest {
            val uncompleted = testWorkoutSet(seed = 1, completedAt = null)
            val active = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(uncompleted))))
            val workouts = FakeWorkoutRepository(listOf(active))
            val useCase = FinishActiveWorkoutUseCase(workouts, saveFinished(workouts), MutableClock(Instant.fromEpochSeconds(0)))

            val result = useCase()

            assertEquals(DomainResult.Failure(FinishWorkoutError.NoCompletedSet), result)
            assertTrue(workouts.saveFinishedWorkoutCalls.isEmpty())
        }

    @Test
    fun givenCompletedAndUncompletedSetsAndAWhollyUncompletedExercise_whenFinished_thenOnlyCompletedSetsAndNonEmptyExercisesAreSaved() =
        runTest {
            val completed = testWorkoutSet(seed = 1, completedAt = Instant.fromEpochSeconds(10))
            val uncompleted = testWorkoutSet(seed = 2, completedAt = null)
            val mixedExercise = testWorkoutExercise(seed = 1, sets = listOf(completed, uncompleted))
            val emptyAfterFinishExercise = testWorkoutExercise(seed = 2, sets = listOf(testWorkoutSet(seed = 3, completedAt = null)))
            val active = testActiveWorkout(exercises = listOf(mixedExercise, emptyAfterFinishExercise))
            val workouts = FakeWorkoutRepository(listOf(active))
            val finishTime = Instant.fromEpochSeconds(999)
            val useCase = FinishActiveWorkoutUseCase(workouts, saveFinished(workouts), MutableClock(finishTime))

            val result = useCase() as DomainResult.Success

            assertEquals(active.id, result.value)
            val saved = workouts.saveFinishedWorkoutCalls.single()
            assertEquals(finishTime, saved.finishedAt)
            val savedExercise = saved.exercises.single()
            assertEquals(mixedExercise.id, savedExercise.id)
            assertEquals(listOf(completed), savedExercise.sets)
        }

    @Test
    fun givenValidationFails_whenFinished_thenTheErrorIsWrapped() =
        runTest {
            val completed = testWorkoutSet(seed = 1, completedAt = Instant.fromEpochSeconds(10))
            val active = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(completed))))
            val workouts =
                FakeWorkoutRepository(listOf(active), saveFinishedWorkoutResult = DomainResult.Failure(ValidationError.NameBlank))
            val useCase = FinishActiveWorkoutUseCase(workouts, saveFinished(workouts), MutableClock(Instant.fromEpochSeconds(0)))

            val result = useCase()

            assertEquals(DomainResult.Failure(FinishWorkoutError.Invalid(ValidationError.NameBlank)), result)
        }
}
