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
 * No `@Upsert`: Room 3's `EntityUpsertAdapter` (a Room runtime class, not generated code) only
 * tells an update-worthy conflict apart from any other failure by parsing the driver's exception
 * message. On Android host tests that exception comes from the mockable `android.jar`, whose
 * `SQLException(String)` constructor drops the message, so the adapter can't recognise the
 * conflict and rethrows instead of falling back to its update path; production is unaffected.
 * `@Update` returning the affected row count, falling back to `@Insert` on `0`, needs no exception
 * parsing and behaves the same on every target.
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

    /** Test-only: lets a test assert the `ON DELETE CASCADE` directly instead of through PK reuse. */
    @Query("SELECT COUNT(*) FROM workout_exercises WHERE id = :id")
    abstract suspend fun countExerciseRowsById(id: Uuid): Int

    @Query("SELECT COUNT(*) FROM workout_sets WHERE id = :id")
    abstract suspend fun countSetRowsById(id: Uuid): Int
}
