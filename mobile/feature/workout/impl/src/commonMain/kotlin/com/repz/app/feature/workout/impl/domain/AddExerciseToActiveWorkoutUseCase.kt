package com.repz.app.feature.workout.impl.domain

import com.repz.app.core.common.id.IdGenerator
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.WorkoutExercise
import com.repz.app.core.domain.model.WorkoutExerciseId
import com.repz.app.core.domain.model.WorkoutSetId
import com.repz.app.core.domain.workout.FindLastPerformanceUseCase
import com.repz.app.core.domain.workout.WorkoutRepository
import com.repz.app.core.domain.workout.prefilledSetsForAddedExercise
import com.repz.app.core.domain.workout.withExercisesAppended
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

/**
 * Adds [ExerciseId] to the active workout, prefilled from its last performance (ADR-0029),
 * under [ActiveWorkoutWriteLock].
 */
@Factory
class AddExerciseToActiveWorkoutUseCase(
    private val workouts: WorkoutRepository,
    private val findLastPerformance: FindLastPerformanceUseCase,
    private val idGenerator: IdGenerator,
    private val lock: ActiveWorkoutWriteLock,
) {
    /** `false` when there is no active workout to add [exerciseId] to. */
    suspend operator fun invoke(exerciseId: ExerciseId): Boolean =
        lock.withLock {
            val current = workouts.observeActiveWorkout().first() ?: return@withLock false
            val lastPerformanceSets = findLastPerformance(setOf(exerciseId))[exerciseId]
            val newExercise =
                WorkoutExercise(
                    id = WorkoutExerciseId(idGenerator.newId()),
                    exerciseId = exerciseId,
                    notes = "",
                    sets = prefilledSetsForAddedExercise(lastPerformanceSets) { WorkoutSetId(idGenerator.newId()) },
                )
            workouts.saveActiveWorkout(current.withExercisesAppended(listOf(newExercise)))
            true
        }
}
