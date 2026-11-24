package com.repz.app.core.database.exercise

import androidx.room3.Dao
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/**
 * Where each exercise was last used (ADR-0029), joining the workout and exercise tables that
 * [com.repz.app.core.database.workout.WorkoutDao] and [ExerciseDao] own.
 */
@Dao
abstract class ExerciseUsageDao {
    /** The active workout is excluded (`finished_at IS NULL`); a tombstoned workout or exercise is excluded too. */
    @Query(
        "SELECT we.exercise_id AS exerciseId, MAX(w.started_at) AS lastUsedAt " +
            "FROM workout_exercises we " +
            "JOIN workouts w ON w.id = we.workout_id " +
            "JOIN exercises e ON e.id = we.exercise_id " +
            "WHERE w.finished_at IS NOT NULL AND w.is_deleted = 0 AND e.is_deleted = 0 " +
            "GROUP BY we.exercise_id " +
            "ORDER BY lastUsedAt DESC, we.exercise_id ASC " +
            "LIMIT :limit",
    )
    abstract fun observeRecentlyUsedExercises(limit: Int): Flow<List<RecentExerciseUseRow>>
}
