package com.repz.app.feature.workout.impl.presentation.picker

import com.repz.app.core.domain.model.ExerciseId

sealed interface ExercisePickerIntent {
    data class ExerciseClicked(
        val id: ExerciseId,
    ) : ExercisePickerIntent

    data object BackClicked : ExercisePickerIntent
}
