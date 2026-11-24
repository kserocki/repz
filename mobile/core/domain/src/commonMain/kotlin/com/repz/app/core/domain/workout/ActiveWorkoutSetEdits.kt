package com.repz.app.core.domain.workout

import com.repz.app.core.domain.model.Workout
import com.repz.app.core.domain.model.WorkoutExercise
import com.repz.app.core.domain.model.WorkoutExerciseId
import com.repz.app.core.domain.model.WorkoutSet
import com.repz.app.core.domain.model.WorkoutSetId

/**
 * Pure, total edits on the active workout draft's sets (ADR-0029): each returns a whole new
 * [Workout] built from the current one, ready for [WorkoutRepository.saveActiveWorkout]. An
 * unknown id leaves the workout unchanged instead of failing, as in [ActiveWorkoutEdits.kt].
 */
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
