package com.repz.app.feature.workout.impl.domain

import com.repz.app.core.domain.model.WorkoutId
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.workout.FinishActiveWorkoutUseCase
import com.repz.app.core.domain.workout.FinishWorkoutError
import org.koin.core.annotation.Factory

/**
 * Runs [FinishActiveWorkoutUseCase] under [ActiveWorkoutWriteLock] (ADR-0029): without this, a
 * queued edit could read the draft before Finish and save it back as active afterwards.
 * `core:domain` stays Koin-free, so this feature-local wrapper - not the shared use case directly -
 * is what the live workout calls.
 */
@Factory
class FinishLiveWorkoutUseCase(
    private val finishActiveWorkout: FinishActiveWorkoutUseCase,
    private val lock: ActiveWorkoutWriteLock,
) {
    suspend operator fun invoke(): DomainResult<WorkoutId, FinishWorkoutError> = lock.withLock { finishActiveWorkout() }
}
