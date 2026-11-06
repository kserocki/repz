package com.repz.app.core.domain.workout

import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.Workout
import com.repz.app.core.domain.model.WorkoutId
import com.repz.app.core.domain.record.RecalculatePersonalRecordsUseCase

/** Deletes a finished workout, then recomputes its exercises' personal records (ADR-0026), so a deleted workout stops holding one. */
class DeleteWorkoutUseCase(
    private val workouts: WorkoutRepository,
    private val recalculate: RecalculatePersonalRecordsUseCase,
) {
    suspend operator fun invoke(id: WorkoutId) {
        val exerciseIds = workouts.findWorkoutIncludingDeleted(id)?.exerciseIds() ?: emptySet()
        workouts.deleteWorkout(id)
        recalculate(exerciseIds)
    }
}

private fun Workout.exerciseIds(): Set<ExerciseId> = exercises.map { it.exerciseId }.toSet()
