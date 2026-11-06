package com.repz.app.feature.workout.impl.domain

import com.repz.app.core.domain.exercise.ExerciseRepository
import com.repz.app.core.domain.model.Exercise
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.model.Workout
import com.repz.app.core.domain.model.WorkoutSet
import com.repz.app.core.domain.preferences.UserPreferencesRepository
import com.repz.app.core.domain.preferences.WeightStep
import com.repz.app.core.domain.workout.FindLastPerformanceUseCase
import com.repz.app.core.domain.workout.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Factory

/** The active workout plus everything the live screen needs to render it (ADR-0029). */
data class LiveWorkoutSnapshot(
    val workout: Workout,
    val exercisesById: Map<ExerciseId, Exercise>,
    val lastPerformanceByExercise: Map<ExerciseId, List<WorkoutSet>>,
    val unit: UnitSystem,
    val weightStep: WeightStep,
)

/**
 * Observes the active workout together with its exercises - including tombstones, so a soft-deleted
 * custom exercise still resolves a name (T9) - and each exercise's last performance. The exercise
 * lookup and the last-performance read only re-run when the workout's set of exercise ids changes,
 * not on every set edit.
 */
@Factory
class ObserveLiveWorkoutUseCase(
    private val workouts: WorkoutRepository,
    private val exercises: ExerciseRepository,
    private val findLastPerformance: FindLastPerformanceUseCase,
    private val preferences: UserPreferencesRepository,
) {
    operator fun invoke(): Flow<LiveWorkoutSnapshot?> {
        val exerciseContext =
            workouts
                .observeActiveWorkout()
                .map { workout -> workout?.exercises?.map { it.exerciseId }?.toSet() }
                .distinctUntilChanged()
                .map { exerciseIds -> if (exerciseIds == null) null else loadExerciseContext(exerciseIds) }

        return combine(workouts.observeActiveWorkout(), exerciseContext, preferences.observePreferences()) { workout, context, prefs ->
            if (workout == null || context == null) {
                null
            } else {
                LiveWorkoutSnapshot(
                    workout = workout,
                    exercisesById = context.exercisesById,
                    lastPerformanceByExercise = context.lastPerformanceByExercise,
                    unit = prefs.unitSystem,
                    weightStep = prefs.weightStepFor(prefs.unitSystem),
                )
            }
        }
    }

    private suspend fun loadExerciseContext(exerciseIds: Set<ExerciseId>): ExerciseContext {
        val exercisesById = exercises.findExercisesIncludingDeleted(exerciseIds).associateBy { it.id }
        val lastPerformanceByExercise = findLastPerformance(exerciseIds)
        return ExerciseContext(exercisesById, lastPerformanceByExercise)
    }

    private data class ExerciseContext(
        val exercisesById: Map<ExerciseId, Exercise>,
        val lastPerformanceByExercise: Map<ExerciseId, List<WorkoutSet>>,
    )
}
