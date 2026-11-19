package com.repz.app.core.domain.workout

import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.WorkoutSet

/**
 * Each requested exercise's most recent finished workout containing it (by `startedAt`,
 * descending), its first occurrence within that workout if it appears more than once: the
 * "Previous" column's and the prefill rules' source (ADR-0029). An exercise id with no finished
 * workout is missing from the result map.
 */
class FindLastPerformanceUseCase(
    private val workouts: WorkoutRepository,
) {
    suspend operator fun invoke(exerciseIds: Set<ExerciseId>): Map<ExerciseId, List<WorkoutSet>> {
        if (exerciseIds.isEmpty()) return emptyMap()

        val matchingWorkouts = workouts.findFinishedWorkoutsContaining(exerciseIds)

        return exerciseIds
            .mapNotNull { exerciseId ->
                val lastWorkout =
                    matchingWorkouts
                        .filter { workout -> workout.exercises.any { it.exerciseId == exerciseId } }
                        .maxByOrNull { it.startedAt }
                        ?: return@mapNotNull null
                val lastOccurrenceSets = lastWorkout.exercises.first { it.exerciseId == exerciseId }.sets
                exerciseId to lastOccurrenceSets
            }.toMap()
    }
}
