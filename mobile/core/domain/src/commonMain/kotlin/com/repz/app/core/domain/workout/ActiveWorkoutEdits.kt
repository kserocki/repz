package com.repz.app.core.domain.workout

import com.repz.app.core.domain.model.Workout
import com.repz.app.core.domain.model.WorkoutExercise
import com.repz.app.core.domain.model.WorkoutExerciseId

/**
 * Pure, total edits on the active workout draft's name and exercise list (ADR-0029): each returns a
 * whole new [Workout] built from the current one, ready for
 * [WorkoutRepository.saveActiveWorkout]. An unknown id leaves the workout unchanged instead of
 * failing: by the time an edit from the UI reaches here, its own state can only be a step behind
 * the latest Room emission, never pointing at something that was never there. Set-level edits are
 * in [ActiveWorkoutSetEdits.kt].
 */
fun Workout.renamed(name: String): Workout = copy(name = name)

fun Workout.withExercisesAppended(exercises: List<WorkoutExercise>): Workout = copy(exercises = this.exercises + exercises)

/** Puts [replacement] where [id] is; unchanged if [id] is unknown. */
fun Workout.withExerciseReplaced(
    id: WorkoutExerciseId,
    replacement: WorkoutExercise,
): Workout = copy(exercises = exercises.map { exercise -> if (exercise.id == id) replacement else exercise })

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
