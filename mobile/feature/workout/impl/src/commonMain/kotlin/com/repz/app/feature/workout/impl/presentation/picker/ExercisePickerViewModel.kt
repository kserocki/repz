package com.repz.app.feature.workout.impl.presentation.picker

import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repz.app.core.domain.model.Exercise
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.localizedName
import com.repz.app.feature.workout.impl.domain.AddExerciseToActiveWorkoutUseCase
import com.repz.app.feature.workout.impl.domain.ObservePickerExercisesUseCase
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * v0: a plain list sorted by localized name. TODO(REP-37): adds search, filters, multi-select,
 * create custom. [languageTag] is a defaulted constructor parameter rather than injected,
 * so tests pass a fixed one.
 */
@KoinViewModel
class ExercisePickerViewModel(
    private val observeExercises: ObservePickerExercisesUseCase,
    private val addExerciseToActiveWorkout: AddExerciseToActiveWorkoutUseCase,
    private val languageTag: () -> String = { Locale.current.toLanguageTag() },
) : ViewModel() {
    private val mutableEffects = Channel<ExercisePickerEffect>(Channel.BUFFERED)
    val effects: Flow<ExercisePickerEffect> = mutableEffects.receiveAsFlow()

    val state: StateFlow<ExercisePickerUiState> =
        observeExercises()
            .map(::toUiState)
            .stateIn(viewModelScope, SharingStarted.Eagerly, ExercisePickerUiState())

    fun onIntent(intent: ExercisePickerIntent) {
        when (intent) {
            is ExercisePickerIntent.ExerciseClicked -> onExerciseClicked(intent.id)
            ExercisePickerIntent.BackClicked -> sendEffect(ExercisePickerEffect.Close)
        }
    }

    private fun toUiState(exercises: List<Exercise>): ExercisePickerUiState {
        val tag = languageTag()
        return ExercisePickerUiState(
            isLoading = false,
            exercises =
                exercises
                    .sortedBy { it.localizedName(tag) }
                    .map { PickerExerciseUi(id = it.id, name = it.localizedName(tag), primaryMuscle = it.primaryMuscle) }
                    .toImmutableList(),
        )
    }

    private fun onExerciseClicked(id: ExerciseId) {
        viewModelScope.launch {
            addExerciseToActiveWorkout(id)
            sendEffect(ExercisePickerEffect.Close)
        }
    }

    private fun sendEffect(effect: ExercisePickerEffect) {
        viewModelScope.launch { mutableEffects.send(effect) }
    }
}
