package com.repz.app.feature.workout.impl.presentation.live

import com.repz.app.core.domain.model.WorkoutExercise
import com.repz.app.core.domain.model.WorkoutExerciseId
import com.repz.app.core.domain.model.WorkoutSet
import com.repz.app.core.domain.model.WorkoutSetId

/**
 * In-memory-only UI state the ViewModel keeps alongside the Room-backed workout (ADR-0029 T3):
 * none of this is a Room column, so a process death loses it (a prefilled value then just shows
 * as a normal one).
 */
internal data class LocalState(
    val touchedSetIds: Set<WorkoutSetId> = emptySet(),
    val editingField: SetFieldRef? = null,
    val nameDraft: String? = null,
    val editingNotesExerciseId: WorkoutExerciseId? = null,
    val notesDraft: String = "",
    val isReordering: Boolean = false,
    val pendingUndo: PendingUndo? = null,
    val discardConfirmation: DiscardConfirmationUi? = null,
    val isFinishing: Boolean = false,
) {
    fun touched(setId: WorkoutSetId) = copy(touchedSetIds = touchedSetIds + setId)
}

/** The single pending undo (ADR-0029): a newer delete of either kind replaces whatever was pending. */
internal sealed interface PendingUndo {
    data class DeletedSet(
        val exerciseId: WorkoutExerciseId,
        val index: Int,
        val set: WorkoutSet,
    ) : PendingUndo

    data class RemovedExercise(
        val index: Int,
        val exercise: WorkoutExercise,
    ) : PendingUndo
}
