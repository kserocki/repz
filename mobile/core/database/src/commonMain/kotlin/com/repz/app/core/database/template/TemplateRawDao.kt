package com.repz.app.core.database.template

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlin.uuid.Uuid

/**
 * The single-statement primitives [TemplateDao] composes into its multi-step transactions. Split
 * out so [TemplateDao] itself stays within the function-count limit; `RepzDatabase` exposes this
 * only internally; `core:data`/`core:sync` never see it.
 *
 * No `@Upsert`: see [com.repz.app.core.database.workout.WorkoutRawDao].
 */
@Dao
internal abstract class TemplateRawDao {
    @Query("SELECT server_version FROM workout_templates WHERE id = :id")
    abstract suspend fun findServerVersion(id: Uuid): Long?

    @Update
    abstract suspend fun updateTemplate(template: WorkoutTemplateEntity): Int

    @Insert
    abstract suspend fun insertTemplate(template: WorkoutTemplateEntity)

    @Insert
    abstract suspend fun insertExercises(exercises: List<TemplateExerciseEntity>)

    /** Test-only: lets a test assert the `ON DELETE CASCADE` directly instead of through PK reuse. */
    @Query("SELECT COUNT(*) FROM template_exercises WHERE template_id = :templateId")
    abstract suspend fun countExercisesByTemplateId(templateId: Uuid): Int
}
