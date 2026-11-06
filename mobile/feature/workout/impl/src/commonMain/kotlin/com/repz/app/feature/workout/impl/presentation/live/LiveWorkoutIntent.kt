package com.repz.app.feature.workout.impl.presentation.live

import com.repz.app.core.domain.model.Rpe
import com.repz.app.core.domain.model.SetType
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.model.WorkoutExerciseId
import com.repz.app.core.domain.model.WorkoutSetId
import com.repz.app.core.domain.preferences.WeightStep

sealed interface LiveWorkoutIntent {
    data object MinimizeClicked : LiveWorkoutIntent

    data object NameClicked : LiveWorkoutIntent

    data class NameChanged(
        val name: String,
    ) : LiveWorkoutIntent

    /** Blank restores the previous name. */
    data object NameEditingFinished : LiveWorkoutIntent

    data object FinishClicked : LiveWorkoutIntent

    data object DiscardClicked : LiveWorkoutIntent

    data object DiscardConfirmed : LiveWorkoutIntent

    /** Cancel, system Back, and a scrim tap all send this. */
    data object DiscardDismissed : LiveWorkoutIntent

    data object AddExerciseClicked : LiveWorkoutIntent

    data class AddSetClicked(
        val exerciseId: WorkoutExerciseId,
    ) : LiveWorkoutIntent

    data class SetCompletionToggled(
        val setId: WorkoutSetId,
    ) : LiveWorkoutIntent

    data class SetTypeSelected(
        val setId: WorkoutSetId,
        val type: SetType,
    ) : LiveWorkoutIntent

    data class SetDeleted(
        val setId: WorkoutSetId,
    ) : LiveWorkoutIntent

    data object SetDeletionUndone : LiveWorkoutIntent

    data class SetFieldClicked(
        val setId: WorkoutSetId,
        val field: SetField,
    ) : LiveWorkoutIntent

    /** From `SetValueEdit.Weight`; `null` means the field was cleared, which stores 0. */
    data class SetWeightEntered(
        val setId: WorkoutSetId,
        val weight: Weight?,
    ) : LiveWorkoutIntent

    /** From `SetValueEdit.Reps`; `null` means the field was cleared, which stores 0. */
    data class SetRepsEntered(
        val setId: WorkoutSetId,
        val reps: Int?,
    ) : LiveWorkoutIntent

    /** From `SetValueEdit.Rpe`; unused while RPE mode is off. */
    data class SetRpeEntered(
        val setId: WorkoutSetId,
        val rpe: Rpe?,
    ) : LiveWorkoutIntent

    /** The keypad's own mode switch (weight/reps tab), not a field tap on the row. */
    data class KeypadFieldSelected(
        val field: SetField,
    ) : LiveWorkoutIntent

    /** The keypad's Step chip (keypad plan §8.2): persisted device-wide via `SetWeightStepUseCase`. */
    data class WeightStepChanged(
        val step: WeightStep,
    ) : LiveWorkoutIntent

    /** T12: Weight -> Reps of the same set -> Weight of the next set -> Weight of the next card's first set; Done on the last field. */
    data object NextFieldRequested : LiveWorkoutIntent

    /** Keypad hide key, Back/predictive back, or a tap outside the sheet. */
    data object KeypadDismissed : LiveWorkoutIntent

    data class NotesClicked(
        val exerciseId: WorkoutExerciseId,
    ) : LiveWorkoutIntent

    data class NotesChanged(
        val exerciseId: WorkoutExerciseId,
        val notes: String,
    ) : LiveWorkoutIntent

    data class NotesEditingFinished(
        val exerciseId: WorkoutExerciseId,
    ) : LiveWorkoutIntent

    data class ExerciseRemoved(
        val exerciseId: WorkoutExerciseId,
    ) : LiveWorkoutIntent

    data object ExerciseRemovalUndone : LiveWorkoutIntent

    /** From the card menu or a long-press on its header. */
    data object ReorderStarted : LiveWorkoutIntent

    data class ExerciseMoved(
        val fromIndex: Int,
        val toIndex: Int,
    ) : LiveWorkoutIntent

    data object ReorderFinished : LiveWorkoutIntent
}
