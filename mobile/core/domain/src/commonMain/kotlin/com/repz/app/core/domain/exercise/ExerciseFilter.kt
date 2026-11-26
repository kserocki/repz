package com.repz.app.core.domain.exercise

import com.repz.app.core.domain.model.Equipment
import com.repz.app.core.domain.model.Exercise
import com.repz.app.core.domain.model.Muscle

/**
 * Muscle and equipment filters for the exercise picker and library: muscles match the exercise's
 * primary or any secondary muscle, equipment matches its own. A group combines its own values with
 * OR; the two groups combine with AND. An empty group matches everything.
 */
data class ExerciseFilter(
    val muscles: Set<Muscle> = emptySet(),
    val equipment: Set<Equipment> = emptySet(),
) {
    val activeCount: Int get() = muscles.size + equipment.size
    val isActive: Boolean get() = activeCount > 0

    fun matches(exercise: Exercise): Boolean {
        val muscleMatches = muscles.isEmpty() || exercise.primaryMuscle in muscles || exercise.secondaryMuscles.any { it in muscles }
        val equipmentMatches = equipment.isEmpty() || exercise.equipment in equipment
        return muscleMatches && equipmentMatches
    }
}
