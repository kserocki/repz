package com.repz.app.feature.workout.impl.domain

import com.repz.app.core.domain.model.WorkoutId
import com.repz.app.core.domain.model.WorkoutTemplateId
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.workout.StartWorkoutError
import com.repz.app.core.domain.workout.StartWorkoutFromTemplateUseCase
import org.koin.core.annotation.Factory

/**
 * Runs [StartWorkoutFromTemplateUseCase] under [ActiveWorkoutWriteLock] (ADR-0029), for the same
 * reason as [StartEmptyLiveWorkoutUseCase].
 */
@Factory
class StartLiveWorkoutFromTemplateUseCase(
    private val startWorkoutFromTemplate: StartWorkoutFromTemplateUseCase,
    private val lock: ActiveWorkoutWriteLock,
) {
    suspend operator fun invoke(templateId: WorkoutTemplateId): DomainResult<WorkoutId, StartWorkoutError> =
        lock.withLock { startWorkoutFromTemplate(templateId) }
}
