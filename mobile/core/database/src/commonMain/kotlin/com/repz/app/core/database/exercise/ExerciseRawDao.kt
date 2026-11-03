package com.repz.app.core.database.exercise

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlin.uuid.Uuid

/**
 * The single-statement primitives [ExerciseDao] composes into its multi-step transactions. Split
 * out so [ExerciseDao] itself stays within the function-count limit; `RepzDatabase` exposes this
 * only internally; `core:data`/`core:sync` never see it.
 *
 * No `@Upsert`: see [com.repz.app.core.database.workout.WorkoutRawDao].
 */
@Dao
internal abstract class ExerciseRawDao {
    @Query("SELECT server_version FROM exercises WHERE id = :id")
    abstract suspend fun findServerVersion(id: Uuid): Long?

    @Update
    abstract suspend fun updateExercise(exercise: ExerciseEntity): Int

    @Insert
    abstract suspend fun insertExercise(exercise: ExerciseEntity)

    @Query("DELETE FROM exercise_translations WHERE exercise_id = :exerciseId")
    abstract suspend fun deleteTranslations(exerciseId: Uuid)

    @Insert
    abstract suspend fun insertTranslations(translations: List<ExerciseTranslationEntity>)
}
