package com.repz.app.core.domain.workout

import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.testing.workout.FakeWorkoutRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class FindLastPerformanceUseCaseTest {
    @Test
    fun givenNoFinishedWorkout_whenInvoked_thenTheExerciseIsMissingFromTheResult() =
        runTest {
            val exerciseA = ExerciseId(testUuid(1))
            val useCase = FindLastPerformanceUseCase(FakeWorkoutRepository())

            val result = useCase(setOf(exerciseA))

            assertTrue(result.isEmpty())
        }

    @Test
    fun givenAnEmptyExerciseIdSet_whenInvoked_thenNoLookupIsNeededAndTheResultIsEmpty() =
        runTest {
            val useCase = FindLastPerformanceUseCase(FakeWorkoutRepository())

            val result = useCase(emptySet())

            assertTrue(result.isEmpty())
        }

    @Test
    fun givenTwoFinishedWorkoutsContainingTheExercise_whenInvoked_thenTheMostRecentOnesSetsAreReturned() =
        runTest {
            val exerciseA = ExerciseId(testUuid(1))
            val olderSets = listOf(testWorkoutSet(seed = 1, reps = 5))
            val newerSets = listOf(testWorkoutSet(seed = 2, reps = 8))
            val older =
                testFinishedWorkout(
                    seed = 1,
                    startedAt = Instant.fromEpochSeconds(0),
                    exercises = listOf(testWorkoutExercise(seed = 1, exerciseId = exerciseA, sets = olderSets)),
                )
            val newer =
                testFinishedWorkout(
                    seed = 2,
                    startedAt = Instant.fromEpochSeconds(100),
                    exercises = listOf(testWorkoutExercise(seed = 2, exerciseId = exerciseA, sets = newerSets)),
                )
            val useCase = FindLastPerformanceUseCase(FakeWorkoutRepository(listOf(older, newer)))

            val result = useCase(setOf(exerciseA))

            assertEquals(newerSets, result[exerciseA])
        }

    @Test
    fun givenAWorkoutWithTheExerciseTwice_whenInvoked_thenTheFirstOccurrencesSetsAreReturned() =
        runTest {
            val exerciseA = ExerciseId(testUuid(1))
            val firstOccurrenceSets = listOf(testWorkoutSet(seed = 1, reps = 5))
            val secondOccurrenceSets = listOf(testWorkoutSet(seed = 2, reps = 9))
            val workout =
                testFinishedWorkout(
                    seed = 1,
                    exercises =
                        listOf(
                            testWorkoutExercise(seed = 1, exerciseId = exerciseA, sets = firstOccurrenceSets),
                            testWorkoutExercise(seed = 2, exerciseId = exerciseA, sets = secondOccurrenceSets),
                        ),
                )
            val useCase = FindLastPerformanceUseCase(FakeWorkoutRepository(listOf(workout)))

            val result = useCase(setOf(exerciseA))

            assertEquals(firstOccurrenceSets, result[exerciseA])
        }
}
