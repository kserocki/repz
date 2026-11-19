package com.repz.app.feature.workout.impl.domain

import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.Weight
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

class AddExerciseToActiveWorkoutUseCaseTest {
    private fun useCase(workouts: FakeWorkoutRepository) =
        AddExerciseToActiveWorkoutUseCase(workouts, FindLastPerformanceUseCase(workouts), SequentialIdGenerator(), ActiveWorkoutWriteLock())

    @Test
    fun givenNoActiveWorkout_whenInvoked_thenItReturnsFalse() =
        runTest {
            val result = useCase(FakeWorkoutRepository())(ExerciseId(testUuid(1)))

            assertFalse(result)
        }

    @Test
    fun givenNoHistoryForTheExercise_whenInvoked_thenOneEmptyNormalSetIsAppended() =
        runTest {
            val exerciseId = ExerciseId(testUuid(1))
            val workout = testActiveWorkout(exercises = emptyList())
            val workouts = FakeWorkoutRepository(listOf(workout))

            val result = useCase(workouts)(exerciseId)

            assertTrue(result)
            val savedExercise =
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.single()
            assertEquals(exerciseId, savedExercise?.exerciseId)
            val set = savedExercise?.sets?.single()
            assertEquals(Weight.ZERO, set?.weight)
            assertEquals(0, set?.reps)
            assertNull(set?.completedAt)
        }

    @Test
    fun givenLastPerformanceForTheExercise_whenInvoked_thenItsSetsAreCopiedUncompleted() =
        runTest {
            val exerciseId = ExerciseId(testUuid(1))
            val pastWorkout =
                testFinishedWorkout(
                    seed = 2,
                    exercises =
                        listOf(
                            testWorkoutExercise(
                                seed = 2,
                                exerciseId = exerciseId,
                                sets = listOf(testWorkoutSet(seed = 3, weight = Weight.fromKilograms(80.0), reps = 8)),
                            ),
                        ),
                )
            val activeWorkout = testActiveWorkout(seed = 1, exercises = emptyList())
            val workouts = FakeWorkoutRepository(listOf(pastWorkout, activeWorkout))

            useCase(workouts)(exerciseId)

            val set =
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.single()
                    ?.sets
                    ?.single()
            assertEquals(Weight.fromKilograms(80.0), set?.weight)
            assertEquals(8, set?.reps)
            assertNull(set?.completedAt)
        }
}
