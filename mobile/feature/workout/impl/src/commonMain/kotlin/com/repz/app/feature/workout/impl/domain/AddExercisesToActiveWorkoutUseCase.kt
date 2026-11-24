package com.repz.app.feature.workout.impl.domain

import com.repz.app.core.common.id.IdGenerator
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.WorkoutExercise
import com.repz.app.core.domain.model.WorkoutExerciseId
import com.repz.app.core.domain.model.WorkoutSet
import com.repz.app.core.domain.model.WorkoutSetId
import com.repz.app.core.domain.validation.ValidationLimits
import com.repz.app.core.domain.workout.FindLastPerformanceUseCase
import com.repz.app.core.domain.workout.WorkoutRepository
import com.repz.app.core.domain.workout.prefilledSetsForAddedExercise
import com.repz.app.core.domain.workout.withExercisesAppended
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

/**
 * Adds [exerciseIds] to the active workout in one locked write, each prefilled from its last
 * performance (ADR-0029), in pick order (duplicates allowed).
 */
@Factory
class AddExercisesToActiveWorkoutUseCase(
    private val workouts: WorkoutRepository,
    private val findLastPerformance: FindLastPerformanceUseCase,
    private val idGenerator: IdGenerator,
    private val lock: ActiveWorkoutWriteLock,
) {
    /** `false` when there is no active workout to add to. An empty list is a no-op returning `true`. */
    suspend operator fun invoke(exerciseIds: List<ExerciseId>): Boolean {
        if (exerciseIds.isEmpty()) return true

        return lock.withLock {
            val current = workouts.observeActiveWorkout().first() ?: return@withLock false
            val lastPerformance = findLastPerformance(exerciseIds.toSet())
            val newExercises = exerciseIds.map { exerciseId -> exerciseId.toNewWorkoutExercise(lastPerformance[exerciseId]) }
            // Defensive: the UI already caps the selection, so this only guards a stale read.
            val remaining = (ValidationLimits.EXERCISES_PER_WORKOUT_MAX - current.exercises.size).coerceAtLeast(0)
            workouts.saveActiveWorkout(current.withExercisesAppended(newExercises.take(remaining)))
            true
        }
    }

    private fun ExerciseId.toNewWorkoutExercise(lastPerformance: List<WorkoutSet>?): WorkoutExercise =
        WorkoutExercise(
            id = WorkoutExerciseId(idGenerator.newId()),
            exerciseId = this,
            notes = "",
            sets = prefilledSetsForAddedExercise(lastPerformance) { WorkoutSetId(idGenerator.newId()) },
        )
}
