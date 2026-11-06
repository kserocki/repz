package com.repz.app.feature.workout.impl.domain

import com.repz.app.core.domain.model.Workout
import com.repz.app.core.domain.model.WorkoutTemplate
import com.repz.app.core.domain.template.WorkoutTemplateRepository
import com.repz.app.core.domain.workout.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.koin.core.annotation.Factory

/** The Train tab v0's data: the active workout, if any, plus every template (T7). */
data class TrainOverview(
    val activeWorkout: Workout?,
    val templates: List<WorkoutTemplate>,
)

@Factory
class ObserveTrainOverviewUseCase(
    private val workouts: WorkoutRepository,
    private val templates: WorkoutTemplateRepository,
) {
    operator fun invoke(): Flow<TrainOverview> =
        combine(workouts.observeActiveWorkout(), templates.observeTemplates()) { activeWorkout, templateList ->
            TrainOverview(activeWorkout, templateList)
        }
}
