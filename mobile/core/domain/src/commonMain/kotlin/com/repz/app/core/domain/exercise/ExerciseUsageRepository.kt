package com.repz.app.core.domain.exercise

import kotlinx.coroutines.flow.Flow

/**
 * Where each exercise was last used, across finished workouts (rule 6: the UI reads only from
 * Room). Kept apart from [ExerciseRepository] and
 * [com.repz.app.core.domain.workout.WorkoutRepository] (ADR-0029): the exercise picker's Recent
 * section is its only consumer, and the join it needs spans both aggregates.
 */
interface ExerciseUsageRepository {
    /**
     * Exercises in finished, non-deleted workouts whose exercise isn't deleted, most recently used
     * first (by the workout's `startedAt`), at most [limit].
     */
    fun observeRecentlyUsedExercises(limit: Int): Flow<List<RecentExerciseUse>>
}
