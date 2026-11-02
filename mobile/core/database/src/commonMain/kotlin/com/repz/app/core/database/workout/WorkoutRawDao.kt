package com.repz.app.core.database.workout

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Upsert
import kotlin.uuid.Uuid

/**
 * The single-statement primitives [WorkoutDao] composes into its multi-step transactions. Split
 * out so [WorkoutDao] itself stays within the function-count limit; `RepzDatabase` exposes this
 * only internally; `core:data`/`core:sync` never see it.
 */
@Dao
internal abstract class WorkoutRawDao {
    @Query("SELECT server_version FROM workouts WHERE id = :id")
    abstract suspend fun findServerVersion(id: Uuid): Long?

    @Upsert
    abstract suspend fun upsertWorkout(workout: WorkoutEntity)

    @Insert
    abstract suspend fun insertChildren(
        exercises: List<WorkoutExerciseEntity>,
        sets: List<WorkoutSetEntity>,
    )
}
