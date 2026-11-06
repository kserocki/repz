package com.repz.app.feature.workout.impl.presentation.picker

import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.Muscle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** v0: a plain list sorted by localized name (TODO(REP-37): search, filters, multi-select, create custom). */
data class ExercisePickerUiState(
    val isLoading: Boolean = true,
    val exercises: ImmutableList<PickerExerciseUi> = persistentListOf(),
)

data class PickerExerciseUi(
    val id: ExerciseId,
    val name: String,
    val primaryMuscle: Muscle,
)
