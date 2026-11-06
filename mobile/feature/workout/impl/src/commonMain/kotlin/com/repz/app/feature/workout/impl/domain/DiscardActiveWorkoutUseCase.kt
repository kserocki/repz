package com.repz.app.feature.workout.impl.domain

import com.repz.app.core.domain.workout.WorkoutRepository
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

/** Discards the active workout (Q2): a hard delete, since it never entered the outbox. No-op if there is none. */
@Factory
class DiscardActiveWorkoutUseCase(
    private val workouts: WorkoutRepository,
) {
    suspend operator fun invoke() {
        val current = workouts.observeActiveWorkout().first() ?: return
        workouts.discardActiveWorkout(current.id)
    }
}
