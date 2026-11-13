package com.repz.app.feature.workout.impl.domain

import com.repz.app.core.domain.model.Workout
import com.repz.app.core.domain.workout.WorkoutRepository
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

/**
 * Applies [edit] to the latest active workout draft and saves it whole, under
 * [ActiveWorkoutWriteLock] (ADR-0029); every other edit in this feature goes through this use
 * case rather than [WorkoutRepository.saveActiveWorkout] directly.
 */
@Factory
class EditActiveWorkoutUseCase(
    private val workouts: WorkoutRepository,
    private val lock: ActiveWorkoutWriteLock,
) {
    /** `false` when there is no active workout to edit. */
    suspend operator fun invoke(edit: (Workout) -> Workout): Boolean =
        lock.withLock {
            val current = workouts.observeActiveWorkout().first() ?: return@withLock false
            workouts.saveActiveWorkout(edit(current))
            true
        }
}
