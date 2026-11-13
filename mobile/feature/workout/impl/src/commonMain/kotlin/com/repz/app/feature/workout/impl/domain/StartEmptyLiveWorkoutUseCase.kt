package com.repz.app.feature.workout.impl.domain

import com.repz.app.core.domain.model.WorkoutId
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.workout.StartEmptyWorkoutUseCase
import com.repz.app.core.domain.workout.StartWorkoutError
import org.koin.core.annotation.Factory

/**
 * Runs [StartEmptyWorkoutUseCase] under [ActiveWorkoutWriteLock] (ADR-0029): without this, two
 * rapid taps on Start could both read "no active workout" and both save one.
 */
@Factory
class StartEmptyLiveWorkoutUseCase(
    private val startEmptyWorkout: StartEmptyWorkoutUseCase,
    private val lock: ActiveWorkoutWriteLock,
) {
    suspend operator fun invoke(name: String): DomainResult<WorkoutId, StartWorkoutError> = lock.withLock { startEmptyWorkout(name) }
}
