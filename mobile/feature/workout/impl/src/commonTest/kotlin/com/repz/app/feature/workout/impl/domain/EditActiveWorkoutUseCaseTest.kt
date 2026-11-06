package com.repz.app.feature.workout.impl.domain

import com.repz.app.core.domain.workout.renamed
import com.repz.app.core.domain.workout.withExercisesAppended
import com.repz.app.core.testing.workout.FakeWorkoutRepository
import com.repz.app.feature.workout.impl.testActiveWorkout
import com.repz.app.feature.workout.impl.testWorkoutExercise
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditActiveWorkoutUseCaseTest {
    @Test
    fun givenNoActiveWorkout_whenInvoked_thenItReturnsFalseAndSavesNothing() =
        runTest {
            val workouts = FakeWorkoutRepository()
            val useCase = EditActiveWorkoutUseCase(workouts, ActiveWorkoutWriteLock())

            val result = useCase { it.renamed("New name") }

            assertFalse(result)
            assertNull(workouts.observeActiveWorkout().first())
        }

    @Test
    fun givenAnActiveWorkout_whenInvoked_thenTheEditIsSavedAndTrueIsReturned() =
        runTest {
            val workout = testActiveWorkout(name = "Old name")
            val workouts = FakeWorkoutRepository(listOf(workout))
            val useCase = EditActiveWorkoutUseCase(workouts, ActiveWorkoutWriteLock())

            val result = useCase { it.renamed("New name") }

            assertTrue(result)
            assertEquals("New name", workouts.findWorkoutIncludingDeleted(workout.id)?.name)
        }

    /** The lock (ADR-0029 T4) must serialize two concurrent edits so the second reads the first's write instead of a stale copy. */
    @Test
    fun givenTwoConcurrentEdits_whenBothRun_thenBothChangesArePreserved() =
        runTest {
            val workout = testActiveWorkout(exercises = emptyList())
            val workouts = FakeWorkoutRepository(listOf(workout))
            val useCase = EditActiveWorkoutUseCase(workouts, ActiveWorkoutWriteLock())
            val exerciseA = testWorkoutExercise(seed = 10)
            val exerciseB = testWorkoutExercise(seed = 20)

            val jobA = launch { useCase { it.withExercisesAppended(listOf(exerciseA)) } }
            val jobB = launch { useCase { it.withExercisesAppended(listOf(exerciseB)) } }
            jobA.join()
            jobB.join()

            val savedExerciseIds =
                workouts
                    .findWorkoutIncludingDeleted(workout.id)
                    ?.exercises
                    ?.map { it.id }
                    ?.toSet()
            assertEquals(setOf(exerciseA.id, exerciseB.id), savedExerciseIds)
        }
}
