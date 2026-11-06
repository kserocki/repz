package com.repz.app.core.domain.workout

import com.repz.app.core.domain.model.Workout
import com.repz.app.core.domain.model.WorkoutExercise
import com.repz.app.core.domain.model.WorkoutExerciseId
import com.repz.app.core.domain.model.WorkoutSet
import com.repz.app.core.domain.model.WorkoutSetId

/**
 * Pure, total edits on the active workout draft (ADR-0029): each returns a whole new [Workout]
 * built from the current one, ready for [WorkoutRepository.saveActiveWorkout]. An unknown id
 * leaves the workout unchanged instead of failing: by the time an edit from the UI reaches here,
 * its own state can only be a step behind the latest Room emission, never pointing at something
 * that was never there.
 */
fun Workout.renamed(name: String): Workout = copy(name = name)

fun Workout.withExercisesAppended(exercises: List<WorkoutExercise>): Workout = copy(exercises = this.exercises + exercises)

/** Re-inserts a removed exercise at [index] (undo of [withoutExercise]); [index] is clamped into range. */
fun Workout.withExerciseInserted(
    index: Int,
    exercise: WorkoutExercise,
): Workout {
    val clampedIndex = index.coerceIn(0, exercises.size)
    return copy(exercises = exercises.toMutableList().apply { add(clampedIndex, exercise) })
}

fun Workout.withoutExercise(id: WorkoutExerciseId): Workout = copy(exercises = exercises.filterNot { it.id == id })

/** Moves the exercise at [fromIndex] to [toIndex] (both clamped into range); a no-op if [fromIndex] is unknown or the move is a no-op. */
fun Workout.withExerciseMoved(
    fromIndex: Int,
    toIndex: Int,
): Workout {
    if (fromIndex !in exercises.indices) return this

    val clampedToIndex = toIndex.coerceIn(0, exercises.lastIndex)
    return if (fromIndex == clampedToIndex) {
        this
    } else {
        val reordered = exercises.toMutableList()
        val moved = reordered.removeAt(fromIndex)
        reordered.add(clampedToIndex, moved)
        copy(exercises = reordered)
    }
}

fun Workout.withExerciseNotes(
    id: WorkoutExerciseId,
    notes: String,
): Workout = copy(exercises = exercises.map { exercise -> if (exercise.id == id) exercise.copy(notes = notes) else exercise })

fun Workout.withSetAppended(
    exerciseId: WorkoutExerciseId,
    set: WorkoutSet,
): Workout =
    copy(
        exercises = exercises.map { exercise -> if (exercise.id == exerciseId) exercise.copy(sets = exercise.sets + set) else exercise },
    )

/** Re-inserts a deleted set at [index] within [exerciseId]'s sets (undo of [withoutSet]); [index] is clamped into range. */
fun Workout.withSetInserted(
    exerciseId: WorkoutExerciseId,
    index: Int,
    set: WorkoutSet,
): Workout =
    copy(
        exercises =
            exercises.map { exercise ->
                if (exercise.id != exerciseId) {
                    exercise
                } else {
                    val clampedIndex = index.coerceIn(0, exercise.sets.size)
                    exercise.copy(sets = exercise.sets.toMutableList().apply { add(clampedIndex, set) })
                }
            },
    )

fun Workout.withoutSet(setId: WorkoutSetId): Workout =
    copy(exercises = exercises.map { exercise -> exercise.copy(sets = exercise.sets.filterNot { it.id == setId }) })

fun Workout.withSetUpdated(
    setId: WorkoutSetId,
    update: (WorkoutSet) -> WorkoutSet,
): Workout =
    copy(
        exercises =
            exercises.map { exercise ->
                exercise.copy(sets = exercise.sets.map { set -> if (set.id == setId) update(set) else set })
            },
    )

/** The set's owning exercise and its index within it, for undo bookkeeping; `null` if [setId] is unknown. */
fun Workout.findSet(setId: WorkoutSetId): Pair<WorkoutExercise, Int>? {
    for (exercise in exercises) {
        val index = exercise.sets.indexOfFirst { it.id == setId }
        if (index >= 0) return exercise to index
    }
    return null
}

val Workout.completedSetCount: Int
    get() = exercises.sumOf { exercise -> exercise.sets.count { it.isCompleted } }
