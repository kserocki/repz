package com.repz.app.feature.workout.impl.presentation.live

import com.repz.app.core.domain.model.Workout
import com.repz.app.core.domain.workout.findSet

/** T12: Weight -> Reps of the same set -> Weight of the next set -> Weight of the next card's first set; `null` on the last field. */
internal fun nextFieldAfter(
    workout: Workout,
    current: SetFieldRef,
): SetFieldRef? {
    if (current.field == SetField.Weight) return SetFieldRef(current.setId, SetField.Reps)

    val owningExercise = workout.findSet(current.setId)
    return if (owningExercise == null) {
        null
    } else {
        val (exercise, index) = owningExercise
        val nextSetInSameExercise = exercise.sets.getOrNull(index + 1)
        if (nextSetInSameExercise != null) {
            SetFieldRef(nextSetInSameExercise.id, SetField.Weight)
        } else {
            val exerciseIndex = workout.exercises.indexOfFirst { it.id == exercise.id }
            workout.exercises
                .getOrNull(exerciseIndex + 1)
                ?.sets
                ?.firstOrNull()
                ?.let { SetFieldRef(it.id, SetField.Weight) }
        }
    }
}
