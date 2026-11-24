package com.repz.app.feature.workout.impl.domain

import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.validation.ValidationLimits
import com.repz.app.core.domain.workout.FindLastPerformanceUseCase
import com.repz.app.core.testing.id.SequentialIdGenerator
import com.repz.app.core.testing.workout.FakeWorkoutRepository
import com.repz.app.feature.workout.impl.testActiveWorkout
import com.repz.app.feature.workout.impl.testFinishedWorkout
import com.repz.app.feature.workout.impl.testUuid
import com.repz.app.feature.workout.impl.testWorkoutExercise
import com.repz.app.feature.workout.impl.testWorkoutSet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AddExercisesToActiveWorkoutUseCaseTest {
    // Starts well past every fixture seed below, so a freshly generated id never collides with one.
    private fun useCase(workouts: FakeWorkoutRepository) =
        AddExercisesToActiveWorkoutUseCase(
            workouts,
            FindLastPerformanceUseCase(workouts),
            SequentialIdGenerator(next = 1_000),
            ActiveWorkoutWriteLock(),
        )

    @Test
    fun givenNoActiveWorkout_whenInvoked_thenItReturnsFalse() =
        runTest {
            val result = useCase(FakeWorkoutRepository())(listOf(ExerciseId(testUuid(1))))

            assertFalse(result)
        }

    @Test
    fun givenAnEmptyList_whenInvoked_thenItReturnsTrueAndWritesNothing() =
        runTest {
            val workouts = FakeWorkoutRepository(listOf(testActiveWorkout(exercises = emptyList())))

            val result = useCase(workouts)(emptyList())

            assertTrue(result)
            assertTrue(
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    .orEmpty()
                    .isEmpty(),
            )
        }

    @Test
    fun givenExerciseIdsInAnOrder_whenInvoked_thenTheyAreAppendedInThatOrder() =
        runTest {
            val first = ExerciseId(testUuid(1))
            val second = ExerciseId(testUuid(2))
            val third = ExerciseId(testUuid(3))
            val workouts = FakeWorkoutRepository(listOf(testActiveWorkout(exercises = emptyList())))

            useCase(workouts)(listOf(third, first, second))

            assertEquals(
                listOf(third, first, second),
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.map { it.exerciseId },
            )
        }

    @Test
    fun givenTheSameExerciseIdTwice_whenInvoked_thenEachOccurrenceBecomesItsOwnCard() =
        runTest {
            val exerciseId = ExerciseId(testUuid(1))
            val workouts = FakeWorkoutRepository(listOf(testActiveWorkout(exercises = emptyList())))

            useCase(workouts)(listOf(exerciseId, exerciseId))

            val exercises =
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    .orEmpty()
            assertEquals(2, exercises.size)
            assertEquals(setOf(exerciseId), exercises.map { it.exerciseId }.toSet())
            assertEquals(2, exercises.map { it.id }.toSet().size)
        }

    @Test
    fun givenNoHistoryForAnExercise_whenInvoked_thenOneEmptyNormalSetIsAppended() =
        runTest {
            val exerciseId = ExerciseId(testUuid(1))
            val workouts = FakeWorkoutRepository(listOf(testActiveWorkout(exercises = emptyList())))

            useCase(workouts)(listOf(exerciseId))

            val set =
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.single()
                    ?.sets
                    ?.single()
            assertEquals(Weight.ZERO, set?.weight)
            assertEquals(0, set?.reps)
            assertNull(set?.completedAt)
        }

    @Test
    fun givenEachExerciseHasItsOwnLastPerformance_whenInvoked_thenEachIsPrefilledFromItsOwnHistory() =
        runTest {
            val firstId = ExerciseId(testUuid(1))
            val secondId = ExerciseId(testUuid(2))
            val firstHistory =
                testFinishedWorkout(
                    seed = 10,
                    exercises =
                        listOf(
                            testWorkoutExercise(seed = 10, exerciseId = firstId, sets = listOf(testWorkoutSet(seed = 11, reps = 8))),
                        ),
                )
            val secondHistory =
                testFinishedWorkout(
                    seed = 20,
                    exercises =
                        listOf(
                            testWorkoutExercise(seed = 20, exerciseId = secondId, sets = listOf(testWorkoutSet(seed = 21, reps = 12))),
                        ),
                )
            val workouts =
                FakeWorkoutRepository(listOf(firstHistory, secondHistory, testActiveWorkout(seed = 30, exercises = emptyList())))

            useCase(workouts)(listOf(firstId, secondId))

            val exercises =
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    .orEmpty()
            assertEquals(
                8,
                exercises
                    .first { it.exerciseId == firstId }
                    .sets
                    .single()
                    .reps,
            )
            assertEquals(
                12,
                exercises
                    .first { it.exerciseId == secondId }
                    .sets
                    .single()
                    .reps,
            )
        }

    @Test
    fun givenMoreExercisesThanTheWorkoutHasRoomFor_whenInvoked_thenOnlyTheFirstThatFitAreAppended() =
        runTest {
            val existingExercises = (1..ValidationLimits.EXERCISES_PER_WORKOUT_MAX - 1).map { testWorkoutExercise(seed = it) }
            val workouts = FakeWorkoutRepository(listOf(testActiveWorkout(exercises = existingExercises)))
            val first = ExerciseId(testUuid(100))
            val second = ExerciseId(testUuid(101))

            useCase(workouts)(listOf(first, second))

            val exercises =
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    .orEmpty()
            assertEquals(ValidationLimits.EXERCISES_PER_WORKOUT_MAX, exercises.size)
            assertEquals(first, exercises.last().exerciseId)
        }
}
