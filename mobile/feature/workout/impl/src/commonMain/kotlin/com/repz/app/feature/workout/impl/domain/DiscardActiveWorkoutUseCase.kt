package com.repz.app.feature.workout.impl.domain

import com.repz.app.core.domain.workout.WorkoutRepository
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

/**
 * Discards the active workout: a hard delete, since it never entered the outbox. No-op if there is
 * none. Runs under [ActiveWorkoutWriteLock] (ADR-0029), so a queued edit can never read the draft
 * before Discard and save it back as active afterwards.
 */
@Factory
class DiscardActiveWorkoutUseCase(
    private val workouts: WorkoutRepository,
    private val lock: ActiveWorkoutWriteLock,
) {
    suspend operator fun invoke() {
        lock.withLock {
            val current = workouts.observeActiveWorkout().first() ?: return@withLock
            workouts.discardActiveWorkout(current.id)
        }
    }
}
