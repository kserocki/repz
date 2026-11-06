package com.repz.app.core.domain.workout

import com.repz.app.core.domain.model.WorkoutExerciseId
import com.repz.app.core.domain.model.WorkoutSetId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ActiveWorkoutEditsTest {
    @Test
    fun givenAWorkout_whenRenamed_thenOnlyTheNameChanges() {
        val workout = testActiveWorkout(name = "Old name")

        val renamed = workout.renamed("New name")

        assertEquals("New name", renamed.name)
        assertEquals(workout.exercises, renamed.exercises)
    }

    @Test
    fun givenAWorkout_whenExercisesAppended_thenTheyAreAddedAtTheEnd() {
        val first = testWorkoutExercise(seed = 1)
        val second = testWorkoutExercise(seed = 2)
        val workout = testActiveWorkout(exercises = listOf(first))

        val result = workout.withExercisesAppended(listOf(second))

        assertEquals(listOf(first, second), result.exercises)
    }

    @Test
    fun givenAnOutOfRangeIndex_whenExerciseInserted_thenItIsClampedToTheEnd() {
        val first = testWorkoutExercise(seed = 1)
        val reinserted = testWorkoutExercise(seed = 2)
        val workout = testActiveWorkout(exercises = listOf(first))

        val result = workout.withExerciseInserted(index = 99, exercise = reinserted)

        assertEquals(listOf(first, reinserted), result.exercises)
    }

    @Test
    fun givenAWorkout_whenExerciseRemoved_thenItIsGone() {
        val first = testWorkoutExercise(seed = 1)
        val second = testWorkoutExercise(seed = 2)
        val workout = testActiveWorkout(exercises = listOf(first, second))

        val result = workout.withoutExercise(first.id)

        assertEquals(listOf(second), result.exercises)
    }

    @Test
    fun givenAnUnknownExerciseId_whenRemoved_thenTheWorkoutIsUnchanged() {
        val workout = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1)))

        val result = workout.withoutExercise(WorkoutExerciseId(testUuid(99)))

        assertEquals(workout, result)
    }

    @Test
    fun givenAWorkout_whenExerciseMoved_thenItIsReorderedToTheTargetIndex() {
        val first = testWorkoutExercise(seed = 1)
        val second = testWorkoutExercise(seed = 2)
        val third = testWorkoutExercise(seed = 3)
        val workout = testActiveWorkout(exercises = listOf(first, second, third))

        val result = workout.withExerciseMoved(fromIndex = 0, toIndex = 2)

        assertEquals(listOf(second, third, first), result.exercises)
    }

    @Test
    fun givenAnOutOfRangeTargetIndex_whenExerciseMoved_thenItIsClampedToTheLastPosition() {
        val first = testWorkoutExercise(seed = 1)
        val second = testWorkoutExercise(seed = 2)
        val workout = testActiveWorkout(exercises = listOf(first, second))

        val result = workout.withExerciseMoved(fromIndex = 0, toIndex = 99)

        assertEquals(listOf(second, first), result.exercises)
    }

    @Test
    fun givenAnOutOfRangeSourceIndex_whenExerciseMoved_thenTheWorkoutIsUnchanged() {
        val workout = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1)))

        val result = workout.withExerciseMoved(fromIndex = 5, toIndex = 0)

        assertEquals(workout, result)
    }

    @Test
    fun givenAWorkout_whenExerciseNotesChanged_thenOnlyThatExercisesNotesChange() {
        val exercise = testWorkoutExercise(seed = 1, notes = "")
        val workout = testActiveWorkout(exercises = listOf(exercise))

        val result = workout.withExerciseNotes(exercise.id, "Pause on chest")

        assertEquals("Pause on chest", result.exercises.single().notes)
    }

    @Test
    fun givenAWorkout_whenSetAppended_thenItIsAddedToThatExercise() {
        val existingSet = testWorkoutSet(seed = 1)
        val exercise = testWorkoutExercise(seed = 1, sets = listOf(existingSet))
        val newSet = testWorkoutSet(seed = 2)
        val workout = testActiveWorkout(exercises = listOf(exercise))

        val result = workout.withSetAppended(exercise.id, newSet)

        assertEquals(listOf(existingSet, newSet), result.exercises.single().sets)
    }

    @Test
    fun givenAnOutOfRangeIndex_whenSetReinserted_thenItIsClampedToTheEnd() {
        val remainingSet = testWorkoutSet(seed = 1)
        val exercise = testWorkoutExercise(seed = 1, sets = listOf(remainingSet))
        val deletedSet = testWorkoutSet(seed = 2)
        val workout = testActiveWorkout(exercises = listOf(exercise))

        val result = workout.withSetInserted(exercise.id, index = 99, set = deletedSet)

        assertEquals(listOf(remainingSet, deletedSet), result.exercises.single().sets)
    }

    @Test
    fun givenAWorkout_whenSetRemoved_thenItIsGoneFromItsExercise() {
        val keptSet = testWorkoutSet(seed = 1)
        val removedSet = testWorkoutSet(seed = 2)
        val exercise = testWorkoutExercise(seed = 1, sets = listOf(keptSet, removedSet))
        val workout = testActiveWorkout(exercises = listOf(exercise))

        val result = workout.withoutSet(removedSet.id)

        assertEquals(listOf(keptSet), result.exercises.single().sets)
    }

    @Test
    fun givenAWorkout_whenSetUpdated_thenOnlyThatSetChanges() {
        val set = testWorkoutSet(seed = 1, reps = 5)
        val exercise = testWorkoutExercise(seed = 1, sets = listOf(set))
        val workout = testActiveWorkout(exercises = listOf(exercise))

        val result = workout.withSetUpdated(set.id) { it.copy(reps = 8) }

        assertEquals(
            8,
            result.exercises
                .single()
                .sets
                .single()
                .reps,
        )
    }

    @Test
    fun givenAnUnknownSetId_whenUpdated_thenTheWorkoutIsUnchanged() {
        val workout = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1)))

        val result = workout.withSetUpdated(WorkoutSetId(testUuid(99))) { it.copy(reps = 1) }

        assertEquals(workout, result)
    }

    @Test
    fun givenAKnownSet_whenFound_thenItsOwningExerciseAndIndexAreReturned() {
        val firstSet = testWorkoutSet(seed = 1)
        val secondSet = testWorkoutSet(seed = 2)
        val exercise = testWorkoutExercise(seed = 1, sets = listOf(firstSet, secondSet))
        val workout = testActiveWorkout(exercises = listOf(exercise))

        val found = workout.findSet(secondSet.id)

        assertEquals(exercise to 1, found)
    }

    @Test
    fun givenAnUnknownSetId_whenFound_thenNullIsReturned() {
        val workout = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1)))

        val found = workout.findSet(WorkoutSetId(testUuid(99)))

        assertNull(found)
    }

    @Test
    fun givenAMixOfCompletedAndUncompletedSets_whenCounted_thenOnlyCompletedOnesCount() {
        val completed = testWorkoutSet(seed = 1)
        val uncompletedInFirstExercise = testWorkoutSet(seed = 2, completedAt = null)
        val uncompletedInSecondExercise = testWorkoutSet(seed = 3, completedAt = null)
        val workout =
            testActiveWorkout(
                exercises =
                    listOf(
                        testWorkoutExercise(seed = 1, sets = listOf(completed, uncompletedInFirstExercise)),
                        testWorkoutExercise(seed = 2, sets = listOf(uncompletedInSecondExercise)),
                    ),
            )

        assertEquals(1, workout.completedSetCount)
    }
}
