package com.repz.app.feature.workout.impl.presentation.live

import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.repz.app.core.common.id.IdGenerator
import com.repz.app.core.domain.workout.WorkoutRepository
import com.repz.app.feature.workout.impl.domain.DiscardActiveWorkoutUseCase
import com.repz.app.feature.workout.impl.domain.EditActiveWorkoutUseCase
import com.repz.app.feature.workout.impl.domain.FinishLiveWorkoutUseCase
import com.repz.app.feature.workout.impl.domain.LiveWorkoutSnapshot
import com.repz.app.feature.workout.impl.domain.ObserveLiveWorkoutUseCase
import com.repz.app.feature.workout.impl.domain.SetWeightStepUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * [languageTag] is a defaulted constructor parameter rather than injected, so tests pass a
 * fixed one. [LocalState] (touched sets, editing field, reorder mode, name/notes drafts, the
 * single pending undo) is kept in memory only (ADR-0029). [snapshotFlow] stays eagerly shared
 * for the lifetime of the ViewModel so every intent handler can read the latest workout
 * synchronously; only the elapsed-time ticker is tied to [state]'s own subscribers
 * (`WhileSubscribed`), so it stops ticking once nothing is observing the screen. Intent handling
 * is split by area into [LiveWorkoutLifecycleHandler], [LiveWorkoutSetEditingHandler] and
 * [LiveWorkoutExerciseHandler] (Detekt's per-class function-count limit). [applicationScope]
 * outlives [viewModelScope] (which is already cancelled by the time [onCleared] runs), so it is
 * the one used to flush a pending name/notes draft on cleanup.
 */
@KoinViewModel
class LiveWorkoutViewModel(
    observeLiveWorkout: ObserveLiveWorkoutUseCase,
    private val workouts: WorkoutRepository,
    editActiveWorkout: EditActiveWorkoutUseCase,
    discardActiveWorkout: DiscardActiveWorkoutUseCase,
    finishActiveWorkout: FinishLiveWorkoutUseCase,
    setWeightStep: SetWeightStepUseCase,
    idGenerator: IdGenerator,
    clock: Clock,
    private val applicationScope: CoroutineScope,
    languageTag: () -> String = { Locale.current.toLanguageTag() },
    logger: Logger = Logger.withTag("LiveWorkout"),
) : ViewModel() {
    private val localState = MutableStateFlow(LocalState())

    private val snapshotFlow: StateFlow<LiveWorkoutSnapshot?> =
        observeLiveWorkout().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val mutableEffects = Channel<LiveWorkoutEffect>(Channel.BUFFERED)
    val effects: Flow<LiveWorkoutEffect> = mutableEffects.receiveAsFlow()

    private val exerciseHandler =
        LiveWorkoutExerciseHandler(editActiveWorkout, viewModelScope, localState, snapshotFlow, ::sendEffect)
    private val lifecycleHandler =
        LiveWorkoutLifecycleHandler(
            editActiveWorkout,
            discardActiveWorkout,
            finishActiveWorkout,
            exerciseHandler::flushNotesDraft,
            logger,
            viewModelScope,
            localState,
            snapshotFlow,
            ::sendEffect,
        )
    private val setEditingHandler =
        LiveWorkoutSetEditingHandler(
            editActiveWorkout,
            setWeightStep,
            idGenerator,
            clock,
            viewModelScope,
            localState,
            snapshotFlow,
            ::sendEffect,
        )
    private val uiStateMapper = LiveWorkoutUiStateMapper(languageTag, clock)

    val state: StateFlow<LiveWorkoutUiState> =
        combine(snapshotFlow, localState, ticker()) { snapshot, local, _ -> uiStateMapper.toUiState(snapshot, local) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LiveWorkoutUiState())

    init {
        // Opened with no active workout at all (discarded elsewhere, a restored stack); a plain
        // repository read is enough here, so this doesn't pay for observeLiveWorkout()'s own
        // exercise/last-performance lookup a second time. Later null emissions (Finish/Discard)
        // close through their own intent handlers instead.
        viewModelScope.launch {
            if (workouts.observeActiveWorkout().first() == null) sendEffect(LiveWorkoutEffect.Close)
        }
    }

    /** viewModelScope is already cancelled by the time this runs, so the flush uses [applicationScope] instead. */
    override fun onCleared() {
        applicationScope.launch { lifecycleHandler.flushDrafts() }
    }

    fun onIntent(intent: LiveWorkoutIntent) {
        when (intent) {
            LiveWorkoutIntent.MinimizeClicked -> lifecycleHandler.onMinimizeClicked()
            LiveWorkoutIntent.NameClicked -> lifecycleHandler.onNameClicked()
            is LiveWorkoutIntent.NameChanged -> lifecycleHandler.onNameChanged(intent.name)
            LiveWorkoutIntent.NameEditingFinished -> lifecycleHandler.onNameEditingFinished()
            LiveWorkoutIntent.FinishClicked -> lifecycleHandler.onFinishClicked()
            LiveWorkoutIntent.DiscardClicked -> lifecycleHandler.onDiscardClicked()
            LiveWorkoutIntent.DiscardConfirmed -> lifecycleHandler.onDiscardConfirmed()
            LiveWorkoutIntent.DiscardDismissed -> lifecycleHandler.onDiscardDismissed()
            LiveWorkoutIntent.AddExerciseClicked -> sendEffect(LiveWorkoutEffect.OpenExercisePicker)
            is LiveWorkoutIntent.AddSetClicked -> setEditingHandler.onAddSetClicked(intent.exerciseId)
            is LiveWorkoutIntent.SetCompletionToggled -> setEditingHandler.onSetCompletionToggled(intent.setId)
            is LiveWorkoutIntent.SetTypeSelected -> setEditingHandler.onSetTypeSelected(intent.setId, intent.type)
            is LiveWorkoutIntent.SetDeleted -> setEditingHandler.onSetDeleted(intent.setId)
            LiveWorkoutIntent.SetDeletionUndone -> setEditingHandler.onSetDeletionUndone()
            is LiveWorkoutIntent.SetFieldClicked -> setEditingHandler.onSetFieldClicked(intent.setId, intent.field)
            is LiveWorkoutIntent.SetWeightEntered -> setEditingHandler.onSetWeightEntered(intent.setId, intent.weight)
            is LiveWorkoutIntent.SetRepsEntered -> setEditingHandler.onSetRepsEntered(intent.setId, intent.reps)
            is LiveWorkoutIntent.SetRpeEntered -> setEditingHandler.onSetRpeEntered(intent.setId, intent.rpe)
            is LiveWorkoutIntent.KeypadFieldSelected -> setEditingHandler.onKeypadFieldSelected(intent.field)
            is LiveWorkoutIntent.WeightStepChanged -> setEditingHandler.onWeightStepChanged(intent.step)
            LiveWorkoutIntent.NextFieldRequested -> setEditingHandler.onNextFieldRequested()
            LiveWorkoutIntent.KeypadDismissed -> localState.update { it.copy(editingField = null) }
            is LiveWorkoutIntent.NotesClicked -> exerciseHandler.onNotesClicked(intent.exerciseId)
            is LiveWorkoutIntent.NotesChanged -> exerciseHandler.onNotesChanged(intent.exerciseId, intent.notes)
            is LiveWorkoutIntent.NotesEditingFinished -> exerciseHandler.onNotesEditingFinished(intent.exerciseId)
            is LiveWorkoutIntent.ExerciseRemoved -> exerciseHandler.onExerciseRemoved(intent.exerciseId)
            LiveWorkoutIntent.ExerciseRemovalUndone -> exerciseHandler.onExerciseRemovalUndone()
            LiveWorkoutIntent.ReorderStarted -> localState.update { it.copy(isReordering = true) }
            is LiveWorkoutIntent.ExerciseMoved -> exerciseHandler.onExerciseMoved(intent.fromIndex, intent.toIndex)
            LiveWorkoutIntent.ReorderFinished -> localState.update { it.copy(isReordering = false) }
        }
    }

    private fun sendEffect(effect: LiveWorkoutEffect) {
        viewModelScope.launch { mutableEffects.send(effect) }
    }
}

/** A 1 s ticker, running only while collected (used to drive the elapsed timer, ADR-0029). */
private fun ticker(): Flow<Unit> =
    flow {
        while (true) {
            emit(Unit)
            delay(1.seconds)
        }
    }
