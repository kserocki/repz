package com.repz.app.feature.workout.impl.presentation.live

import com.repz.app.core.domain.model.WorkoutExerciseId

sealed interface LiveWorkoutEffect {
    /** Minimize, discard, finish, or no active workout found: navigates back to Train. */
    data object Close : LiveWorkoutEffect

    data object OpenExercisePicker : LiveWorkoutEffect

    data class OpenExerciseReplacement(
        val exerciseId: WorkoutExerciseId,
    ) : LiveWorkoutEffect

    /** Undo snackbar "Set deleted · Undo"; its action sends [LiveWorkoutIntent.SetDeletionUndone]. */
    data object ShowSetDeleted : LiveWorkoutEffect

    /** Undo snackbar "Exercise removed · Undo"; its action sends [LiveWorkoutIntent.ExerciseRemovalUndone]. */
    data object ShowExerciseRemoved : LiveWorkoutEffect

    /** A generic failure snackbar for a [com.repz.app.core.domain.workout.FinishWorkoutError]. */
    data object ShowFinishFailed : LiveWorkoutEffect

    data object PlayConfirmHaptic : LiveWorkoutEffect

    data object PlayRejectHaptic : LiveWorkoutEffect
}
