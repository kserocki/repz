package com.repz.app.feature.workout.impl.presentation.train

import com.repz.app.core.domain.model.WorkoutTemplateId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.DayOfWeek

/**
 * The Train tab's v0 root (TODO(REP-32): the real home screen). [today] is already resolved by the
 * ViewModel (T13: via an injected `Clock`/`TimeZone`), so `TrainContent` only looks up its string.
 * [activeWorkoutName] non-null shows "Workout in progress · Resume" instead of the Start button
 * (T7: at most one active workout).
 */
data class TrainUiState(
    val isLoading: Boolean = true,
    val today: DayOfWeek = DayOfWeek.MONDAY,
    val activeWorkoutName: String? = null,
    val templates: ImmutableList<TemplateItemUi> = persistentListOf(),
)

data class TemplateItemUi(
    val id: WorkoutTemplateId,
    val name: String,
    val exerciseCount: Int,
)
