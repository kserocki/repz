package com.repz.app.feature.workout.impl.presentation.live

sealed interface LiveWorkoutEffect {
    /** Minimize, discard, finish, or no active workout found (T10): navigates back to Train. */
    data object Close : LiveWorkoutEffect

    data object OpenExercisePicker : LiveWorkoutEffect

    /** Undo snackbar "Set deleted · Undo" (T6); its action sends [LiveWorkoutIntent.SetDeletionUndone]. */
    data object ShowSetDeleted : LiveWorkoutEffect

    /** Undo snackbar "Exercise removed · Undo" (T6); its action sends [LiveWorkoutIntent.ExerciseRemovalUndone]. */
    data object ShowExerciseRemoved : LiveWorkoutEffect

    /** A generic failure snackbar for a [com.repz.app.core.domain.workout.FinishWorkoutError]. */
    data object ShowFinishFailed : LiveWorkoutEffect

    data object PlayConfirmHaptic : LiveWorkoutEffect

    data object PlayRejectHaptic : LiveWorkoutEffect
}
