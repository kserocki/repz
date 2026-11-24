package com.repz.app.feature.workout.impl.presentation.picker

import com.repz.app.core.domain.exercise.ExerciseFilter
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.WorkoutExerciseId

/**
 * In-memory-only picker state (ADR-0029): none of this is a Room column, so a process death loses
 * the query, filter and selection (they come back empty). [replacingWorkoutExerciseId] is `null`
 * in Add mode, and the id of the card being replaced in Replace mode; it is fixed for the
 * ViewModel's lifetime (the Screen's `koinViewModel` parameter), not one of the fields a user
 * action changes.
 */
internal data class LocalState(
    val replacingWorkoutExerciseId: WorkoutExerciseId? = null,
    val query: String = "",
    val filter: ExerciseFilter = ExerciseFilter(),
    val selection: List<ExerciseId> = emptyList(),
    val isSubmitting: Boolean = false,
    val pendingReplace: PendingReplace? = null,
) {
    val mode: ExercisePickerMode get() = if (replacingWorkoutExerciseId != null) ExercisePickerMode.Replace else ExercisePickerMode.Add
}

/** The exercise clicked while its target card had completed sets, with the dialog text already resolved. */
internal data class PendingReplace(
    val newExerciseId: ExerciseId,
    val confirmation: ReplaceConfirmationUi,
)
