package com.repz.app.feature.workout.impl.presentation.picker

sealed interface ExercisePickerEffect {
    data object Close : ExercisePickerEffect
}
