package com.repz.app.feature.workout.impl.presentation.live

import com.repz.app.core.domain.model.WorkoutExerciseId
import com.repz.app.core.domain.workout.withExerciseInserted
import com.repz.app.core.domain.workout.withExerciseMoved
import com.repz.app.core.domain.workout.withExerciseNotes
import com.repz.app.core.domain.workout.withoutExercise
import com.repz.app.feature.workout.impl.domain.EditActiveWorkoutUseCase
import com.repz.app.feature.workout.impl.domain.LiveWorkoutSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Exercise notes and Remove exercise with undo. */
internal class LiveWorkoutExerciseHandler(
    private val editActiveWorkout: EditActiveWorkoutUseCase,
    private val scope: CoroutineScope,
    private val localState: MutableStateFlow<LocalState>,
    private val snapshotFlow: StateFlow<LiveWorkoutSnapshot?>,
    private val sendEffect: (LiveWorkoutEffect) -> Unit,
) {
    fun onNotesClicked(exerciseId: WorkoutExerciseId) {
        val currentNotes =
            snapshotFlow.value
                ?.workout
                ?.exercises
                ?.firstOrNull { it.id == exerciseId }
                ?.notes ?: return
        localState.update { it.copy(editingNotesExerciseId = exerciseId, notesDraft = currentNotes) }
    }

    fun onNotesChanged(
        exerciseId: WorkoutExerciseId,
        notes: String,
    ) {
        if (localState.value.editingNotesExerciseId != exerciseId) return
        localState.update { it.copy(notesDraft = notes) }
    }

    fun onNotesEditingFinished(exerciseId: WorkoutExerciseId) {
        if (localState.value.editingNotesExerciseId != exerciseId) return
        val notes = localState.value.notesDraft
        localState.update { it.copy(editingNotesExerciseId = null, notesDraft = "") }
        scope.launch { editActiveWorkout { it.withExerciseNotes(exerciseId, notes) } }
    }

    fun onExerciseRemoved(exerciseId: WorkoutExerciseId) {
        val workout = snapshotFlow.value?.workout ?: return
        val index = workout.exercises.indexOfFirst { it.id == exerciseId }
        if (index < 0) return
        val exercise = workout.exercises[index]

        localState.update { it.copy(pendingUndo = PendingUndo.RemovedExercise(index, exercise)) }
        scope.launch { editActiveWorkout { it.withoutExercise(exerciseId) } }
        sendEffect(LiveWorkoutEffect.PlayRejectHaptic)
        sendEffect(LiveWorkoutEffect.ShowExerciseRemoved)
    }

    fun onExerciseRemovalUndone() {
        val pending = localState.value.pendingUndo as? PendingUndo.RemovedExercise ?: return
        localState.update { it.copy(pendingUndo = null) }
        scope.launch { editActiveWorkout { it.withExerciseInserted(pending.index, pending.exercise) } }
    }

    fun onExerciseMoved(
        fromIndex: Int,
        toIndex: Int,
    ) {
        scope.launch { editActiveWorkout { it.withExerciseMoved(fromIndex, toIndex) } }
    }
}
