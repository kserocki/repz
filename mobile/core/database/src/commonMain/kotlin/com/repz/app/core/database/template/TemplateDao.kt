package com.repz.app.core.database.template

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import com.repz.app.core.database.RepzDatabase
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Dao
abstract class TemplateDao(
    private val database: RepzDatabase,
) {
    /** Not deleted, ordered by id: UUID v7 ids sort chronologically by creation. */
    @Transaction
    @Query("SELECT * FROM workout_templates WHERE is_deleted = 0 ORDER BY id")
    abstract fun observeTemplates(): Flow<List<TemplateAggregate>>

    @Transaction
    @Query("SELECT * FROM workout_templates WHERE id = :id AND is_deleted = 0")
    abstract fun observeTemplate(id: Uuid): Flow<TemplateAggregate?>

    @Transaction
    @Query("SELECT * FROM workout_templates WHERE id = :id")
    abstract suspend fun findTemplateIncludingDeleted(id: Uuid): TemplateAggregate?

    /** Keeps the stored [WorkoutTemplateEntity.serverVersion] and forces [WorkoutTemplateEntity.isDeleted] to false. */
    @Transaction
    open suspend fun saveLocalTemplate(
        template: WorkoutTemplateEntity,
        exercises: List<TemplateExerciseEntity>,
    ) {
        val storedServerVersion = database.templateRawDao().findServerVersion(template.id)
        saveSyncedTemplate(template.copy(serverVersion = storedServerVersion, isDeleted = false), exercises)
    }

    /** Takes [WorkoutTemplateEntity.serverVersion] and [WorkoutTemplateEntity.isDeleted] as given; replaces all children. */
    @Transaction
    open suspend fun saveSyncedTemplate(
        template: WorkoutTemplateEntity,
        exercises: List<TemplateExerciseEntity>,
    ) {
        database.templateRawDao().upsertTemplate(template)
        deleteChildren(template.id)
        database.templateRawDao().insertExercises(exercises)
    }

    @Query("UPDATE workout_templates SET server_version = :serverVersion WHERE id = :id")
    abstract suspend fun setServerVersion(
        id: Uuid,
        serverVersion: Long?,
    )

    @Query("UPDATE workout_templates SET is_deleted = :isDeleted, updated_at = :updatedAt WHERE id = :id")
    abstract suspend fun setDeleted(
        id: Uuid,
        isDeleted: Boolean,
        updatedAt: Instant,
    )

    /** The tombstoned root row is kept; only its exercises are removed. */
    @Query("DELETE FROM template_exercises WHERE template_id = :id")
    abstract suspend fun deleteChildren(id: Uuid)

    @Query("DELETE FROM workout_templates WHERE id = :id")
    abstract suspend fun purge(id: Uuid)
}
