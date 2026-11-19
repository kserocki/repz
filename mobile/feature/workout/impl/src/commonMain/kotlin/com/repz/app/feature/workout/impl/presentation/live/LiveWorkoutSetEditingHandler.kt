package com.repz.app.feature.workout.impl.presentation.live

import com.repz.app.core.common.id.IdGenerator
import com.repz.app.core.domain.model.Rpe
import com.repz.app.core.domain.model.SetType
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.domain.model.WorkoutExerciseId
import com.repz.app.core.domain.model.WorkoutSetId
import com.repz.app.core.domain.preferences.WeightStep
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.validation.ValidationLimits
import com.repz.app.core.domain.workout.findSet
import com.repz.app.core.domain.workout.prefilledSetToAppend
import com.repz.app.core.domain.workout.withSetAppended
import com.repz.app.core.domain.workout.withSetInserted
import com.repz.app.core.domain.workout.withSetUpdated
import com.repz.app.core.domain.workout.withoutSet
import com.repz.app.feature.workout.impl.domain.EditActiveWorkoutUseCase
import com.repz.app.feature.workout.impl.domain.LiveWorkoutSnapshot
import com.repz.app.feature.workout.impl.domain.SetWeightStepUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** Adding/editing/completing/deleting a set and the keypad's own field navigation. */
internal class LiveWorkoutSetEditingHandler(
    private val editActiveWorkout: EditActiveWorkoutUseCase,
    private val setWeightStep: SetWeightStepUseCase,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
    private val scope: CoroutineScope,
    private val localState: MutableStateFlow<LocalState>,
    private val snapshotFlow: StateFlow<LiveWorkoutSnapshot?>,
    private val sendEffect: (LiveWorkoutEffect) -> Unit,
) {
    fun onAddSetClicked(exerciseId: WorkoutExerciseId) {
        val snapshot = snapshotFlow.value
        val exercise = snapshot?.workout?.exercises?.firstOrNull { it.id == exerciseId }
        if (exercise == null || exercise.sets.size >= ValidationLimits.SETS_PER_EXERCISE_MAX) return

        val lastPerformanceSets = snapshot.lastPerformanceByExercise[exercise.exerciseId]
        val newSet = prefilledSetToAppend(exercise, lastPerformanceSets, WorkoutSetId(idGenerator.newId()))
        scope.launch { editActiveWorkout { it.withSetAppended(exerciseId, newSet) } }
    }

    fun onSetCompletionToggled(setId: WorkoutSetId) {
        val workout = snapshotFlow.value?.workout ?: return
        val (exercise, index) = workout.findSet(setId) ?: return
        val set = exercise.sets[index]

        when {
            set.isCompleted -> {
                scope.launch { editActiveWorkout { it.withSetUpdated(setId) { s -> s.copy(completedAt = null) } } }
            }

            set.reps == 0 -> {
                // An untouched set (reps still 0) can't be completed: nothing is saved, and the
                // reps field of this set becomes the editing field instead.
                localState.update { it.copy(editingField = SetFieldRef(setId, SetField.Reps)) }
                sendEffect(LiveWorkoutEffect.PlayRejectHaptic)
            }

            else -> {
                localState.update { it.touched(setId) }
                scope.launch { editActiveWorkout { it.withSetUpdated(setId) { s -> s.copy(completedAt = clock.now()) } } }
                sendEffect(LiveWorkoutEffect.PlayConfirmHaptic)
            }
        }
    }

    fun onSetTypeSelected(
        setId: WorkoutSetId,
        type: SetType,
    ) {
        scope.launch { editActiveWorkout { it.withSetUpdated(setId) { s -> s.copy(type = type) } } }
    }

    fun onSetDeleted(setId: WorkoutSetId) {
        val workout = snapshotFlow.value?.workout ?: return
        val (exercise, index) = workout.findSet(setId) ?: return
        val set = exercise.sets[index]

        localState.update { current ->
            current.copy(
                pendingUndo = PendingUndo.DeletedSet(exercise.id, index, set),
                // The keypad's own target must not survive the set it points to, or Undo would reopen it.
                editingField = current.editingField?.takeUnless { it.setId == setId },
            )
        }
        scope.launch { editActiveWorkout { it.withoutSet(setId) } }
        sendEffect(LiveWorkoutEffect.PlayRejectHaptic)
        sendEffect(LiveWorkoutEffect.ShowSetDeleted)
    }

    fun onSetDeletionUndone() {
        val pending = localState.value.pendingUndo as? PendingUndo.DeletedSet ?: return
        localState.update { it.copy(pendingUndo = null) }
        scope.launch { editActiveWorkout { it.withSetInserted(pending.exerciseId, pending.index, pending.set) } }
    }

    fun onSetFieldClicked(
        setId: WorkoutSetId,
        field: SetField,
    ) {
        localState.update { it.copy(editingField = SetFieldRef(setId, field)) }
    }

    fun onSetWeightEntered(
        setId: WorkoutSetId,
        weight: Weight?,
    ) {
        if (weight != null && ValidationLimits.validateSetWeight(weight) is DomainResult.Failure) return
        localState.update { it.touched(setId) }
        scope.launch { editActiveWorkout { it.withSetUpdated(setId) { s -> s.copy(weight = weight ?: Weight.ZERO) } } }
    }

    fun onSetRepsEntered(
        setId: WorkoutSetId,
        reps: Int?,
    ) {
        if (reps != null && ValidationLimits.validateReps(reps) is DomainResult.Failure) return
        localState.update { it.touched(setId) }
        scope.launch { editActiveWorkout { it.withSetUpdated(setId) { s -> s.copy(reps = reps ?: 0) } } }
    }

    fun onSetRpeEntered(
        setId: WorkoutSetId,
        rpe: Rpe?,
    ) {
        scope.launch { editActiveWorkout { it.withSetUpdated(setId) { s -> s.copy(rpe = rpe) } } }
    }

    fun onKeypadFieldSelected(field: SetField) {
        val current = localState.value.editingField ?: return
        localState.update { it.copy(editingField = current.copy(field = field)) }
    }

    /** The keypad's Step chip; persisted device-wide, for `step.unitSystem` only. */
    fun onWeightStepChanged(step: WeightStep) {
        scope.launch { setWeightStep(step) }
    }

    fun onNextFieldRequested() {
        val current = localState.value.editingField ?: return
        val workout = snapshotFlow.value?.workout ?: return
        localState.update { it.copy(editingField = nextFieldAfter(workout, current)) }
    }
}
