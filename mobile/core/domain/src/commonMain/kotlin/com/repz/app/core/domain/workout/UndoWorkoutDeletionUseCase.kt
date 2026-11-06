package com.repz.app.core.domain.workout

import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.Workout
import com.repz.app.core.domain.model.WorkoutId
import com.repz.app.core.domain.record.RecalculatePersonalRecordsUseCase

/** Undoes a held workout deletion, then recomputes its exercises' personal records (ADR-0026), restoring any record it had held. */
class UndoWorkoutDeletionUseCase(
    private val workouts: WorkoutRepository,
    private val recalculate: RecalculatePersonalRecordsUseCase,
) {
    suspend operator fun invoke(id: WorkoutId) {
        workouts.undoWorkoutDeletion(id)
        val exerciseIds = workouts.findWorkoutIncludingDeleted(id)?.exerciseIds() ?: emptySet()
        recalculate(exerciseIds)
    }
}

private fun Workout.exerciseIds(): Set<ExerciseId> = exercises.map { it.exerciseId }.toSet()
