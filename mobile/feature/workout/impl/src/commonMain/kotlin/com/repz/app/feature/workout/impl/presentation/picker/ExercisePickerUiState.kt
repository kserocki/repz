package com.repz.app.feature.workout.impl.presentation.picker

import com.repz.app.core.domain.model.Equipment
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.Muscle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

enum class ExercisePickerMode { Add, Replace }

data class ExercisePickerUiState(
    val isLoading: Boolean = true,
    val mode: ExercisePickerMode = ExercisePickerMode.Add,
    val query: String = "",
    val muscleChips: ImmutableList<MuscleChipUi> = persistentListOf(),
    val equipmentChips: ImmutableList<EquipmentChipUi> = persistentListOf(),
    val activeFilterCount: Int = 0,
    val recent: ImmutableList<PickerExerciseUi> = persistentListOf(),
    val results: ImmutableList<PickerExerciseUi> = persistentListOf(),
    val resultsHeader: PickerResultsHeader = PickerResultsHeader.AllExercises,
    val emptyState: PickerEmptyState? = null,
    val selectedCount: Int = 0,
    val isSubmitting: Boolean = false,
    val replaceConfirmation: ReplaceConfirmationUi? = null,
)

data class MuscleChipUi(
    val muscle: Muscle,
    val isSelected: Boolean,
)

data class EquipmentChipUi(
    val equipment: Equipment,
    val isSelected: Boolean,
)

/** [lastUsedDaysAgo] is set only on a `recent` row (0 = today, 1 = yesterday, n >= 2); always `null` on a `results` row. */
data class PickerExerciseUi(
    val id: ExerciseId,
    val name: String,
    val primaryMuscle: Muscle,
    val equipment: Equipment,
    val isSelected: Boolean,
    val lastUsedDaysAgo: Int?,
)

sealed interface PickerResultsHeader {
    data object AllExercises : PickerResultsHeader

    data class Results(
        val count: Int,
    ) : PickerResultsHeader
}

sealed interface PickerEmptyState {
    data object NoExercises : PickerEmptyState

    data class NoMatchForQuery(
        val query: String,
        val activeFilterCount: Int,
    ) : PickerEmptyState

    data class NoMatchForFilters(
        val activeFilterCount: Int,
    ) : PickerEmptyState
}

/** [replacedExerciseName] is `null` when the card's exercise is missing even with tombstones. */
data class ReplaceConfirmationUi(
    val replacedExerciseName: String?,
    val completedSetCount: Int,
)
