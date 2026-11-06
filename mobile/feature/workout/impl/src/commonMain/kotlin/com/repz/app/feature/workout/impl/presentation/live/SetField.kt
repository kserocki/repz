package com.repz.app.feature.workout.impl.presentation.live

import com.repz.app.core.domain.model.WorkoutSetId

/** Which value of a set is being edited; `Rpe` is unused while RPE mode is off (T12). */
enum class SetField { Weight, Reps, Rpe }

/** Identifies one field of one set, for [LiveWorkoutUiState.editingField]. */
data class SetFieldRef(
    val setId: WorkoutSetId,
    val field: SetField,
)
