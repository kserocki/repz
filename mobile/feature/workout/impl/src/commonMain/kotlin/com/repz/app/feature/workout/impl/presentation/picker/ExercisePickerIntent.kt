package com.repz.app.feature.workout.impl.presentation.picker

import com.repz.app.core.domain.model.Equipment
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.Muscle

sealed interface ExercisePickerIntent {
    data class QueryChanged(
        val query: String,
    ) : ExercisePickerIntent

    data object QueryCleared : ExercisePickerIntent

    data class MuscleFilterToggled(
        val muscle: Muscle,
    ) : ExercisePickerIntent

    data class EquipmentFilterToggled(
        val equipment: Equipment,
    ) : ExercisePickerIntent

    data object FiltersCleared : ExercisePickerIntent

    data class ExerciseClicked(
        val id: ExerciseId,
    ) : ExercisePickerIntent

    data object AddClicked : ExercisePickerIntent

    data object ReplaceConfirmed : ExercisePickerIntent

    /** Cancel, scrim, and system Back all send this. */
    data object ReplaceDismissed : ExercisePickerIntent

    data object CloseClicked : ExercisePickerIntent
}
