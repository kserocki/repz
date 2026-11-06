package com.repz.app.core.domain.workout

import com.repz.app.core.domain.model.Workout
import com.repz.app.core.domain.model.WorkoutId
import com.repz.app.core.domain.result.DomainError
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

sealed interface FinishWorkoutError : DomainError {
    data object NoActiveWorkout : FinishWorkoutError

    data object NoCompletedSet : FinishWorkoutError

    data class Invalid(
        val error: ValidationError,
    ) : FinishWorkoutError
}

/**
 * Finishes the active workout (ADR-0029, before the finish confirmation dialog and summary land in
 * a later issue): drops every uncompleted set, then every exercise left with no sets, stamps
 * `finishedAt = now` and saves through [saveFinished]. Requires at least one completed set.
 */
class FinishActiveWorkoutUseCase(
    private val workouts: WorkoutRepository,
    private val saveFinished: SaveFinishedWorkoutUseCase,
    private val clock: Clock,
) {
    suspend operator fun invoke(): DomainResult<WorkoutId, FinishWorkoutError> {
        val activeWorkout = workouts.observeActiveWorkout().first()

        return when {
            activeWorkout == null -> DomainResult.Failure(FinishWorkoutError.NoActiveWorkout)
            activeWorkout.completedSetCount == 0 -> DomainResult.Failure(FinishWorkoutError.NoCompletedSet)
            else -> finish(activeWorkout)
        }
    }

    private suspend fun finish(activeWorkout: Workout): DomainResult<WorkoutId, FinishWorkoutError> {
        val exercisesWithOnlyCompletedSets =
            activeWorkout.exercises
                .map { exercise -> exercise.copy(sets = exercise.sets.filter { it.isCompleted }) }
                .filter { it.sets.isNotEmpty() }

        val finishedWorkout = activeWorkout.copy(exercises = exercisesWithOnlyCompletedSets, finishedAt = clock.now())

        return when (val result = saveFinished(finishedWorkout)) {
            is DomainResult.Success -> DomainResult.Success(finishedWorkout.id)
            is DomainResult.Failure -> DomainResult.Failure(FinishWorkoutError.Invalid(result.error))
        }
    }
}
