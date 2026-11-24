package com.repz.app.core.database.workout

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import com.repz.app.core.database.RepzDatabase
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Dao
abstract class WorkoutDao(
    private val database: RepzDatabase,
) {
    @Transaction
    @Query("SELECT * FROM workouts WHERE finished_at IS NULL AND is_deleted = 0 ORDER BY started_at DESC LIMIT 1")
    abstract fun observeActiveWorkout(): Flow<WorkoutAggregate?>

    @Transaction
    @Query("SELECT * FROM workouts WHERE finished_at IS NOT NULL AND is_deleted = 0 ORDER BY started_at DESC")
    abstract fun observeFinishedWorkouts(): Flow<List<WorkoutAggregate>>

    /** Same filter as [observeFinishedWorkouts], a one-shot read for a full-history recompute. */
    @Transaction
    @Query("SELECT * FROM workouts WHERE finished_at IS NOT NULL AND is_deleted = 0 ORDER BY started_at DESC")
    abstract suspend fun findFinishedWorkouts(): List<WorkoutAggregate>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id AND is_deleted = 0")
    abstract fun observeWorkout(id: Uuid): Flow<WorkoutAggregate?>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id")
    abstract suspend fun findWorkoutIncludingDeleted(id: Uuid): WorkoutAggregate?

    @Transaction
    @Query(
        "SELECT * FROM workouts WHERE finished_at IS NOT NULL AND is_deleted = 0 " +
            "AND id IN (SELECT workout_id FROM workout_exercises WHERE exercise_id IN (:exerciseIds))",
    )
    abstract suspend fun findFinishedWorkoutsContainingExercises(exerciseIds: List<Uuid>): List<WorkoutAggregate>

    /** Keeps the stored [WorkoutEntity.serverVersion] and forces [WorkoutEntity.isDeleted] to false. */
    @Transaction
    open suspend fun saveLocalWorkout(
        workout: WorkoutEntity,
        exercises: List<WorkoutExerciseEntity>,
        sets: List<WorkoutSetEntity>,
    ) {
        val storedServerVersion = database.workoutRawDao().findServerVersion(workout.id)
        saveSyncedWorkout(workout.copy(serverVersion = storedServerVersion, isDeleted = false), exercises, sets)
    }

    /** Takes [WorkoutEntity.serverVersion] and [WorkoutEntity.isDeleted] as given; replaces all children. */
    @Transaction
    open suspend fun saveSyncedWorkout(
        workout: WorkoutEntity,
        exercises: List<WorkoutExerciseEntity>,
        sets: List<WorkoutSetEntity>,
    ) {
        val raw = database.workoutRawDao()
        if (raw.updateWorkout(workout) == 0) raw.insertWorkout(workout)
        deleteChildren(workout.id)
        raw.insertChildren(exercises, sets)
    }

    @Query("UPDATE workouts SET server_version = :serverVersion WHERE id = :id")
    abstract suspend fun setServerVersion(
        id: Uuid,
        serverVersion: Long?,
    )

    @Query("UPDATE workouts SET is_deleted = :isDeleted, updated_at = :updatedAt WHERE id = :id")
    abstract suspend fun setDeleted(
        id: Uuid,
        isDeleted: Boolean,
        updatedAt: Instant,
    )

    /** Removes the exercises (and, by cascade, their sets); the tombstoned root row is kept. */
    @Query("DELETE FROM workout_exercises WHERE workout_id = :id")
    abstract suspend fun deleteChildren(id: Uuid)

    /** Hard delete, used only for the discarded (never synced) active workout. */
    @Query("DELETE FROM workouts WHERE id = :id")
    abstract suspend fun purge(id: Uuid)
}
