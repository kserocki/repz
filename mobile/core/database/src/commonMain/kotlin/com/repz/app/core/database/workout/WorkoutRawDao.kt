package com.repz.app.core.database.workout

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlin.uuid.Uuid

/**
 * The single-statement primitives [WorkoutDao] composes into its multi-step transactions. Split
 * out so [WorkoutDao] itself stays within the function-count limit; `RepzDatabase` exposes this
 * only internally; `core:data`/`core:sync` never see it.
 *
 * No `@Upsert`: Room 3's generated `EntityUpsertAdapter` only tells an update-worthy conflict
 * apart from any other failure by parsing the driver's exception message, and the Android host
 * test JVM's substituted `sqlite-bundled-jvm` driver doesn't set one, so the adapter rethrows
 * instead of falling back to its update path. `@Update` returning the affected row count, falling
 * back to `@Insert` on `0`, needs no exception parsing and behaves the same on every target.
 */
@Dao
internal abstract class WorkoutRawDao {
    @Query("SELECT server_version FROM workouts WHERE id = :id")
    abstract suspend fun findServerVersion(id: Uuid): Long?

    @Update
    abstract suspend fun updateWorkout(workout: WorkoutEntity): Int

    @Insert
    abstract suspend fun insertWorkout(workout: WorkoutEntity)

    @Insert
    abstract suspend fun insertChildren(
        exercises: List<WorkoutExerciseEntity>,
        sets: List<WorkoutSetEntity>,
    )
}
