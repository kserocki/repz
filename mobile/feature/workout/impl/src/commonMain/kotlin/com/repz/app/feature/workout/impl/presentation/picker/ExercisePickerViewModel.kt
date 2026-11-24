package com.repz.app.feature.workout.impl.presentation.picker

import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repz.app.core.domain.exercise.ExerciseFilter
import com.repz.app.core.domain.model.Equipment
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.Muscle
import com.repz.app.core.domain.model.WorkoutExerciseId
import com.repz.app.core.domain.model.localizedName
import com.repz.app.core.domain.validation.ValidationLimits
import com.repz.app.feature.workout.impl.domain.AddExercisesToActiveWorkoutUseCase
import com.repz.app.feature.workout.impl.domain.ExercisePickerData
import com.repz.app.feature.workout.impl.domain.ObserveExercisePickerUseCase
import com.repz.app.feature.workout.impl.domain.ReplaceExerciseInActiveWorkoutUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Clock

/**
 * [replacingWorkoutExerciseId] is the Screen's `koinViewModel { parametersOf(...) }` argument: `null`
 * for Add mode, the card's id for Replace mode. [languageTag]/[timeZone] are defaulted constructor
 * parameters rather than injected, so tests pass fixed ones (the `TrainViewModel` pattern).
 * [LocalState] (query, filter, selection, submitting, the pending replace confirmation) is kept in
 * memory only (ADR-0029) and comes back empty after process death.
 */
@KoinViewModel
class ExercisePickerViewModel(
    observeExercisePicker: ObserveExercisePickerUseCase,
    private val addExercisesToActiveWorkout: AddExercisesToActiveWorkoutUseCase,
    private val replaceExerciseInActiveWorkout: ReplaceExerciseInActiveWorkoutUseCase,
    clock: Clock,
    replacingWorkoutExerciseId: WorkoutExerciseId?,
    private val languageTag: () -> String = { Locale.current.toLanguageTag() },
    timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) : ViewModel() {
    private val localState = MutableStateFlow(LocalState(replacingWorkoutExerciseId = replacingWorkoutExerciseId))

    private val dataFlow: StateFlow<ExercisePickerData?> =
        observeExercisePicker().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val mutableEffects = Channel<ExercisePickerEffect>(Channel.BUFFERED)
    val effects: Flow<ExercisePickerEffect> = mutableEffects.receiveAsFlow()

    private val uiStateMapper = ExercisePickerUiStateMapper(languageTag, timeZone, clock)

    val state: StateFlow<ExercisePickerUiState> =
        combine(dataFlow, localState, uiStateMapper::toUiState)
            .stateIn(viewModelScope, SharingStarted.Eagerly, ExercisePickerUiState())

    init {
        viewModelScope.launch {
            dataFlow
                .filterNotNull()
                .map { it.activeWorkout == null }
                .distinctUntilChanged()
                .collect { hasNoActiveWorkout -> if (hasNoActiveWorkout) sendEffect(ExercisePickerEffect.Close) }
        }
    }

    fun onIntent(intent: ExercisePickerIntent) {
        if (localState.value.isSubmitting && intent != ExercisePickerIntent.CloseClicked) return
        when (intent) {
            is ExercisePickerIntent.QueryChanged -> localState.update { it.copy(query = intent.query) }
            ExercisePickerIntent.QueryCleared -> localState.update { it.copy(query = "") }
            is ExercisePickerIntent.MuscleFilterToggled -> localState.update { it.copy(filter = it.filter.toggled(intent.muscle)) }
            is ExercisePickerIntent.EquipmentFilterToggled -> localState.update { it.copy(filter = it.filter.toggled(intent.equipment)) }
            ExercisePickerIntent.FiltersCleared -> localState.update { it.copy(filter = ExerciseFilter()) }
            is ExercisePickerIntent.ExerciseClicked -> onExerciseClicked(intent.id)
            ExercisePickerIntent.AddClicked -> onAddClicked()
            ExercisePickerIntent.ReplaceConfirmed -> onReplaceConfirmed()
            ExercisePickerIntent.ReplaceDismissed -> localState.update { it.copy(pendingReplace = null) }
            ExercisePickerIntent.CloseClicked -> sendEffect(ExercisePickerEffect.Close)
        }
    }

    private fun onExerciseClicked(id: ExerciseId) {
        val local = localState.value
        if (local.mode == ExercisePickerMode.Replace) {
            if (local.pendingReplace != null) return
            onReplaceModeClicked(id, local)
        } else {
            onAddModeClicked(id, local)
        }
    }

    private fun onAddModeClicked(
        id: ExerciseId,
        local: LocalState,
    ) {
        val remaining =
            ValidationLimits.EXERCISES_PER_WORKOUT_MAX - (
                dataFlow.value
                    ?.activeWorkout
                    ?.exercises
                    ?.size ?: 0
            )
        when {
            id in local.selection -> {
                localState.update { it.copy(selection = it.selection - id) }
                sendEffect(ExercisePickerEffect.PlaySelectionHaptic)
            }

            local.selection.size < remaining -> {
                localState.update { it.copy(selection = it.selection - id + id) }
                sendEffect(ExercisePickerEffect.PlaySelectionHaptic)
            }

            else -> {
                sendEffect(ExercisePickerEffect.PlayRejectHaptic)
                sendEffect(ExercisePickerEffect.ShowSelectionLimit(ValidationLimits.EXERCISES_PER_WORKOUT_MAX))
            }
        }
    }

    private fun onReplaceModeClicked(
        id: ExerciseId,
        local: LocalState,
    ) {
        val targetId = local.replacingWorkoutExerciseId ?: return
        val target =
            dataFlow.value
                ?.activeWorkout
                ?.exercises
                ?.firstOrNull { it.id == targetId }
        when {
            target == null || target.exerciseId == id -> {
                sendEffect(ExercisePickerEffect.Close)
            }

            target.sets.any { it.isCompleted } -> {
                val confirmation =
                    ReplaceConfirmationUi(
                        replacedExerciseName =
                            dataFlow.value
                                ?.exercises
                                ?.firstOrNull { it.id == target.exerciseId }
                                ?.localizedName(languageTag()),
                        completedSetCount = target.sets.count { it.isCompleted },
                    )
                localState.update { it.copy(pendingReplace = PendingReplace(id, confirmation)) }
            }

            else -> {
                submitReplace(targetId, id)
            }
        }
    }

    private fun onAddClicked() {
        val local = localState.value
        if (local.selection.isEmpty()) return
        localState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            if (addExercisesToActiveWorkout(local.selection)) sendEffect(ExercisePickerEffect.PlayConfirmHaptic)
            sendEffect(ExercisePickerEffect.Close)
        }
    }

    private fun onReplaceConfirmed() {
        val targetId = localState.value.replacingWorkoutExerciseId ?: return
        val pending = localState.value.pendingReplace ?: return
        submitReplace(targetId, pending.newExerciseId)
    }

    private fun submitReplace(
        targetId: WorkoutExerciseId,
        newExerciseId: ExerciseId,
    ) {
        localState.update { it.copy(isSubmitting = true, pendingReplace = null) }
        viewModelScope.launch {
            if (replaceExerciseInActiveWorkout(targetId, newExerciseId)) sendEffect(ExercisePickerEffect.PlayConfirmHaptic)
            sendEffect(ExercisePickerEffect.Close)
        }
    }

    private fun sendEffect(effect: ExercisePickerEffect) {
        viewModelScope.launch { mutableEffects.send(effect) }
    }
}

private fun ExerciseFilter.toggled(muscle: Muscle): ExerciseFilter = copy(muscles = muscles.toggled(muscle))

private fun ExerciseFilter.toggled(equipment: Equipment): ExerciseFilter = copy(equipment = equipment.let { this.equipment.toggled(it) })

private fun <T> Set<T>.toggled(value: T): Set<T> = if (value in this) this - value else this + value
