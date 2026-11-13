package com.repz.app.feature.workout.impl.presentation.train

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repz.app.core.domain.model.WorkoutTemplateId
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.feature.workout.impl.domain.ObserveTrainOverviewUseCase
import com.repz.app.feature.workout.impl.domain.StartEmptyLiveWorkoutUseCase
import com.repz.app.feature.workout.impl.domain.StartLiveWorkoutFromTemplateUseCase
import com.repz.app.feature.workout.impl.domain.TrainOverview
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Clock

/**
 * The Train tab's v0 root (TODO(REP-32): the real home screen with the hero split button and FAB
 * menu). [today] is resolved fresh from [clock] and [timeZone] on every state emission rather than
 * cached once, so a tab left open past midnight still names a new workout after today once
 * something else (e.g. starting or finishing a workout) refreshes the overview; it does not run a
 * dedicated timer purely to flip at midnight with nothing else changing.
 */
@KoinViewModel
class TrainViewModel(
    private val observeTrainOverview: ObserveTrainOverviewUseCase,
    private val startEmptyWorkout: StartEmptyLiveWorkoutUseCase,
    private val startWorkoutFromTemplate: StartLiveWorkoutFromTemplateUseCase,
    private val clock: Clock,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) : ViewModel() {
    private val mutableEffects = Channel<TrainEffect>(Channel.BUFFERED)
    val effects: Flow<TrainEffect> = mutableEffects.receiveAsFlow()

    val state: StateFlow<TrainUiState> =
        observeTrainOverview()
            .map(::toUiState)
            .stateIn(viewModelScope, SharingStarted.Eagerly, TrainUiState(today = resolveToday()))

    fun onIntent(intent: TrainIntent) {
        when (intent) {
            is TrainIntent.StartEmptyWorkoutClicked -> onStartEmptyWorkoutClicked(intent.defaultName)
            is TrainIntent.StartTemplateClicked -> onStartTemplateClicked(intent.templateId)
            TrainIntent.ResumeClicked -> onResumeClicked()
        }
    }

    private fun resolveToday(): DayOfWeek = clock.now().toLocalDateTime(timeZone()).dayOfWeek

    private fun toUiState(overview: TrainOverview): TrainUiState =
        TrainUiState(
            isLoading = false,
            today = resolveToday(),
            activeWorkoutName = overview.activeWorkout?.name,
            templates =
                overview.templates
                    .map { template -> TemplateItemUi(id = template.id, name = template.name, exerciseCount = template.exercises.size) }
                    .toImmutableList(),
        )

    private fun onStartEmptyWorkoutClicked(defaultName: String) {
        viewModelScope.launch {
            val result = startEmptyWorkout(defaultName)
            if (result is DomainResult.Success) sendEffect(TrainEffect.OpenLiveWorkout)
        }
    }

    private fun onStartTemplateClicked(templateId: WorkoutTemplateId) {
        viewModelScope.launch {
            val result = startWorkoutFromTemplate(templateId)
            if (result is DomainResult.Success) sendEffect(TrainEffect.OpenLiveWorkout)
        }
    }

    private fun onResumeClicked() {
        sendEffect(TrainEffect.OpenLiveWorkout)
    }

    private fun sendEffect(effect: TrainEffect) {
        viewModelScope.launch { mutableEffects.send(effect) }
    }
}
