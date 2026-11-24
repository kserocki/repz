package com.repz.app.feature.workout.impl.domain

import com.repz.app.core.common.id.IdGenerator
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.WorkoutExercise
import com.repz.app.core.domain.model.WorkoutExerciseId
import com.repz.app.core.domain.model.WorkoutSetId
import com.repz.app.core.domain.workout.FindLastPerformanceUseCase
import com.repz.app.core.domain.workout.WorkoutRepository
import com.repz.app.core.domain.workout.prefilledSetsForAddedExercise
import com.repz.app.core.domain.workout.withExerciseReplaced
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

/**
 * Replaces [workoutExerciseId]'s exercise with [exerciseId], in one locked write: the card keeps
 * its position, notes are cleared, and its sets are prefilled from the new exercise's last
 * performance (ADR-0029). The replacement gets a fresh [WorkoutExerciseId] so the card's
 * `LazyColumn` key and local UI state (e.g. the keypad target) start over.
 */
@Factory
class ReplaceExerciseInActiveWorkoutUseCase(
    private val workouts: WorkoutRepository,
    private val findLastPerformance: FindLastPerformanceUseCase,
    private val idGenerator: IdGenerator,
    private val lock: ActiveWorkoutWriteLock,
) {
    /** `false` when there is no active workout, or no card with [workoutExerciseId]; nothing is written. */
    suspend operator fun invoke(
        workoutExerciseId: WorkoutExerciseId,
        exerciseId: ExerciseId,
    ): Boolean =
        lock.withLock {
            val current = workouts.observeActiveWorkout().first() ?: return@withLock false
            if (current.exercises.none { it.id == workoutExerciseId }) return@withLock false

            val lastPerformance = findLastPerformance(setOf(exerciseId))[exerciseId]
            val replacement =
                WorkoutExercise(
                    id = WorkoutExerciseId(idGenerator.newId()),
                    exerciseId = exerciseId,
                    notes = "",
                    sets = prefilledSetsForAddedExercise(lastPerformance) { WorkoutSetId(idGenerator.newId()) },
                )
            workouts.saveActiveWorkout(current.withExerciseReplaced(workoutExerciseId, replacement))
            true
        }
}
