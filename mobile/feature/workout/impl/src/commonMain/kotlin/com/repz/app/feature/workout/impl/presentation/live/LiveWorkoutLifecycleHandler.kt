package com.repz.app.feature.workout.impl.presentation.live

import co.touchlab.kermit.Logger
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.workout.FinishActiveWorkoutUseCase
import com.repz.app.core.domain.workout.completedSetCount
import com.repz.app.core.domain.workout.renamed
import com.repz.app.feature.workout.impl.domain.DiscardActiveWorkoutUseCase
import com.repz.app.feature.workout.impl.domain.EditActiveWorkoutUseCase
import com.repz.app.feature.workout.impl.domain.LiveWorkoutSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Minimize, workout name editing, Finish and Discard. */
internal class LiveWorkoutLifecycleHandler(
    private val editActiveWorkout: EditActiveWorkoutUseCase,
    private val discardActiveWorkout: DiscardActiveWorkoutUseCase,
    private val finishActiveWorkout: FinishActiveWorkoutUseCase,
    private val logger: Logger,
    private val scope: CoroutineScope,
    private val localState: MutableStateFlow<LocalState>,
    private val snapshotFlow: StateFlow<LiveWorkoutSnapshot?>,
    private val sendEffect: (LiveWorkoutEffect) -> Unit,
) {
    fun onMinimizeClicked() {
        closeAndClearTransientState()
    }

    fun onNameClicked() {
        val currentName = snapshotFlow.value?.workout?.name ?: return
        localState.update { it.copy(nameDraft = currentName) }
    }

    fun onNameChanged(name: String) {
        if (localState.value.nameDraft == null) return
        localState.update { it.copy(nameDraft = name) }
    }

    fun onNameEditingFinished() {
        val draft = localState.value.nameDraft ?: return
        localState.update { it.copy(nameDraft = null) }
        if (draft.isNotBlank()) {
            scope.launch { editActiveWorkout { it.renamed(draft) } }
        }
    }

    fun onFinishClicked() {
        if (localState.value.isFinishing) return
        localState.update { it.copy(isFinishing = true) }
        scope.launch {
            when (val result = finishActiveWorkout()) {
                is DomainResult.Success -> {
                    closeAndClearTransientState()
                }

                is DomainResult.Failure -> {
                    logger.e { "Finish failed: ${result.error}" }
                    localState.update { it.copy(isFinishing = false) }
                    sendEffect(LiveWorkoutEffect.ShowFinishFailed)
                }
            }
        }
    }

    fun onDiscardClicked() {
        val workout = snapshotFlow.value?.workout ?: return
        if (workout.completedSetCount == 0) {
            scope.launch {
                discardActiveWorkout()
                closeAndClearTransientState()
            }
        } else {
            localState.update { it.copy(discardConfirmation = DiscardConfirmationUi(workout.name, workout.completedSetCount)) }
        }
    }

    fun onDiscardConfirmed() {
        scope.launch {
            discardActiveWorkout()
            closeAndClearTransientState()
        }
    }

    fun onDiscardDismissed() {
        localState.update { it.copy(discardConfirmation = null) }
    }

    private fun closeAndClearTransientState() {
        localState.update { it.copy(editingField = null, pendingUndo = null, discardConfirmation = null) }
        sendEffect(LiveWorkoutEffect.Close)
    }
}
