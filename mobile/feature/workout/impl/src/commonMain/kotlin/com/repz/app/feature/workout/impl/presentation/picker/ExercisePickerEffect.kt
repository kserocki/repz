package com.repz.app.feature.workout.impl.presentation.picker

sealed interface ExercisePickerEffect {
    data object Close : ExercisePickerEffect

    /** `RepzHaptics.tick()`. */
    data object PlaySelectionHaptic : ExercisePickerEffect

    /** `RepzHaptics.confirm()`. */
    data object PlayConfirmHaptic : ExercisePickerEffect

    /** `RepzHaptics.reject()`. */
    data object PlayRejectHaptic : ExercisePickerEffect

    data class ShowSelectionLimit(
        val maxExercises: Int,
    ) : ExercisePickerEffect
}
