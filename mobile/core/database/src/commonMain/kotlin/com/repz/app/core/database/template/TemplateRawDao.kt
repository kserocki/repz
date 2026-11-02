package com.repz.app.core.database.template

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Upsert
import kotlin.uuid.Uuid

/**
 * The single-statement primitives [TemplateDao] composes into its multi-step transactions. Split
 * out so [TemplateDao] itself stays within the function-count limit; `RepzDatabase` exposes this
 * only internally; `core:data`/`core:sync` never see it.
 */
@Dao
internal abstract class TemplateRawDao {
    @Query("SELECT server_version FROM workout_templates WHERE id = :id")
    abstract suspend fun findServerVersion(id: Uuid): Long?

    @Upsert
    abstract suspend fun upsertTemplate(template: WorkoutTemplateEntity)

    @Insert
    abstract suspend fun insertExercises(exercises: List<TemplateExerciseEntity>)
}
