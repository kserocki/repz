package com.repz.app.feature.workout.impl.domain

import com.repz.app.core.domain.exercise.ExerciseRepository
import com.repz.app.core.domain.exercise.ExerciseUsageRepository
import com.repz.app.core.domain.exercise.RecentExerciseUse
import com.repz.app.core.domain.model.Exercise
import com.repz.app.core.domain.model.Workout
import com.repz.app.core.domain.workout.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.koin.core.annotation.Factory

/** How many recently used exercises the picker's Recent section shows at most. */
internal const val RECENT_EXERCISES_LIMIT = 5

/** Everything the exercise picker needs to render its list, search and Recent section (ADR-0029). */
data class ExercisePickerData(
    val exercises: List<Exercise>,
    val recent: List<RecentExerciseUse>,
    val activeWorkout: Workout?,
)

@Factory
class ObserveExercisePickerUseCase(
    private val exercises: ExerciseRepository,
    private val exerciseUsage: ExerciseUsageRepository,
    private val workouts: WorkoutRepository,
) {
    operator fun invoke(): Flow<ExercisePickerData> =
        combine(
            exercises.observeExercises(),
            exerciseUsage.observeRecentlyUsedExercises(RECENT_EXERCISES_LIMIT),
            workouts.observeActiveWorkout(),
        ) { exerciseList, recent, activeWorkout -> ExercisePickerData(exerciseList, recent, activeWorkout) }
}
