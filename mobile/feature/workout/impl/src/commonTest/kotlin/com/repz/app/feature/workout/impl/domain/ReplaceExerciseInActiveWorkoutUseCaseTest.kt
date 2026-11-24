package com.repz.app.feature.workout.impl.domain

import com.repz.app.core.domain.model.ExerciseId
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
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class ReplaceExerciseInActiveWorkoutUseCaseTest {
    // Starts well past every fixture seed below, so a freshly generated id never collides with one.
    private fun useCase(workouts: FakeWorkoutRepository) =
        ReplaceExerciseInActiveWorkoutUseCase(
            workouts,
            FindLastPerformanceUseCase(workouts),
            SequentialIdGenerator(next = 1_000),
            ActiveWorkoutWriteLock(),
        )

    @Test
    fun givenNoActiveWorkout_whenInvoked_thenItReturnsFalseAndWritesNothing() =
        runTest {
            val result = useCase(FakeWorkoutRepository())(unknownWorkoutExerciseId(seed = 1), ExerciseId(testUuid(2)))

            assertFalse(result)
        }

    @Test
    fun givenNoCardWithTheGivenId_whenInvoked_thenItReturnsFalseAndWritesNothing() =
        runTest {
            val kept = testWorkoutExercise(seed = 1)
            val workouts = FakeWorkoutRepository(listOf(testActiveWorkout(exercises = listOf(kept))))

            val result = useCase(workouts)(unknownWorkoutExerciseId(seed = 99), ExerciseId(testUuid(2)))

            assertFalse(result)
            assertEquals(listOf(kept), workouts.observeActiveWorkout().first()?.exercises)
        }

    @Test
    fun givenACardAmongOthers_whenReplaced_thenItKeepsItsPositionWithANewId() =
        runTest {
            val before = testWorkoutExercise(seed = 1)
            val target = testWorkoutExercise(seed = 2)
            val after = testWorkoutExercise(seed = 3)
            val workouts = FakeWorkoutRepository(listOf(testActiveWorkout(exercises = listOf(before, target, after))))
            val newExerciseId = ExerciseId(testUuid(10))

            val result = useCase(workouts)(target.id, newExerciseId)

            assertTrue(result)
            val exercises =
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    .orEmpty()
            assertEquals(3, exercises.size)
            assertEquals(before.id, exercises[0].id)
            assertEquals(after.id, exercises[2].id)
            assertNotEquals(target.id, exercises[1].id)
            assertEquals(newExerciseId, exercises[1].exerciseId)
        }

    @Test
    fun givenAReplacedCard_whenInspected_thenItHasANewWorkoutExerciseIdAndEmptyNotes() =
        runTest {
            val target = testWorkoutExercise(seed = 1, notes = "Pause reps")
            val workouts = FakeWorkoutRepository(listOf(testActiveWorkout(exercises = listOf(target))))

            useCase(workouts)(target.id, ExerciseId(testUuid(2)))

            val replaced =
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.single()
            assertNotEquals(target.id, replaced?.id)
            assertEquals("", replaced?.notes)
        }

    @Test
    fun givenTheNewExerciseHasALastPerformance_whenReplaced_thenItsSetsArePrefilledFromThatHistory() =
        runTest {
            val target = testWorkoutExercise(seed = 1)
            val newExerciseId = ExerciseId(testUuid(10))
            val history =
                testFinishedWorkout(
                    seed = 20,
                    exercises =
                        listOf(
                            testWorkoutExercise(seed = 20, exerciseId = newExerciseId, sets = listOf(testWorkoutSet(seed = 21, reps = 9))),
                        ),
                )
            val workouts = FakeWorkoutRepository(listOf(history, testActiveWorkout(seed = 30, exercises = listOf(target))))

            useCase(workouts)(target.id, newExerciseId)

            val replacedSet =
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.single()
                    ?.sets
                    ?.single()
            assertEquals(9, replacedSet?.reps)
        }

    @Test
    fun givenTheTargetHadCompletedSets_whenReplaced_thenTheNewCardHasNoCompletedSets() =
        runTest {
            val completedSet = testWorkoutSet(seed = 1, completedAt = Instant.parse("2024-01-01T10:00:00Z"))
            val target = testWorkoutExercise(seed = 1, sets = listOf(completedSet))
            val workouts = FakeWorkoutRepository(listOf(testActiveWorkout(exercises = listOf(target))))

            useCase(workouts)(target.id, ExerciseId(testUuid(2)))

            val replaced =
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.single()
            assertEquals(0, replaced?.sets?.count { it.isCompleted })
        }

    private fun unknownWorkoutExerciseId(seed: Int) = testWorkoutExercise(seed = seed).id
}
