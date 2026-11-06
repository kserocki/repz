package com.repz.app.feature.workout.impl.presentation.train

import com.repz.app.core.domain.model.WorkoutTemplateId

sealed interface TrainIntent {
    /** [defaultName] is the localized weekday name `TrainContent` resolved from [TrainUiState.today] (T13). */
    data class StartEmptyWorkoutClicked(
        val defaultName: String,
    ) : TrainIntent

    data class StartTemplateClicked(
        val templateId: WorkoutTemplateId,
    ) : TrainIntent

    data object ResumeClicked : TrainIntent
}
