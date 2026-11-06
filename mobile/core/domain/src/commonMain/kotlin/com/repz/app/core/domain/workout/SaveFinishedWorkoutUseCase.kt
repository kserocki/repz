package com.repz.app.core.domain.workout

import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.Workout
import com.repz.app.core.domain.record.RecalculatePersonalRecordsUseCase
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError

/**
 * Finishes or edits a finished workout, then keeps its exercises' personal records in sync: on a
 * successful save, [recalculate] runs for the union of the exercise ids the workout had before the
 * save and the ones it has after, so an exercise an edit removed also loses its stale record.
 */
class SaveFinishedWorkoutUseCase(
    private val workouts: WorkoutRepository,
    private val recalculate: RecalculatePersonalRecordsUseCase,
) {
    suspend operator fun invoke(workout: Workout): DomainResult<Unit, ValidationError> {
        val previousExerciseIds = workouts.findWorkoutIncludingDeleted(workout.id)?.exerciseIds() ?: emptySet()

        val result = workouts.saveFinishedWorkout(workout)
        if (result is DomainResult.Success) {
            recalculate(previousExerciseIds + workout.exerciseIds())
        }
        return result
    }
}

private fun Workout.exerciseIds(): Set<ExerciseId> = exercises.map { it.exerciseId }.toSet()
