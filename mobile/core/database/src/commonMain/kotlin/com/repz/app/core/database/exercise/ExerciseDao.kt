package com.repz.app.core.database.exercise

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import com.repz.app.core.database.RepzDatabase
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Dao
abstract class ExerciseDao(
    private val database: RepzDatabase,
) {
    /** Not deleted; sorting by localised name happens in `core:data`. */
    @Transaction
    @Query("SELECT * FROM exercises WHERE is_deleted = 0")
    abstract fun observeExercises(): Flow<List<ExerciseWithTranslations>>

    @Transaction
    @Query("SELECT * FROM exercises WHERE id = :id AND is_deleted = 0")
    abstract fun observeExercise(id: Uuid): Flow<ExerciseWithTranslations?>

    /** Includes tombstones, so history can still name a deleted exercise (ADR-0023). */
    @Transaction
    @Query("SELECT * FROM exercises WHERE id IN (:ids)")
    abstract suspend fun findExercisesIncludingDeleted(ids: List<Uuid>): List<ExerciseWithTranslations>

    @Query("UPDATE exercises SET server_version = :serverVersion WHERE id = :id")
    abstract suspend fun setServerVersion(
        id: Uuid,
        serverVersion: Long?,
    )

    @Query("UPDATE exercises SET is_deleted = :isDeleted, updated_at = :updatedAt WHERE id = :id")
    abstract suspend fun setDeleted(
        id: Uuid,
        isDeleted: Boolean,
        updatedAt: Instant,
    )

    /** Keeps the stored [ExerciseEntity.serverVersion] and forces [ExerciseEntity.isDeleted] to false. */
    @Transaction
    open suspend fun saveLocalExercise(
        exercise: ExerciseEntity,
        translations: List<ExerciseTranslationEntity>,
    ) {
        val storedServerVersion = database.exerciseRawDao().findServerVersion(exercise.id)
        saveSyncedExercise(exercise.copy(serverVersion = storedServerVersion, isDeleted = false), translations)
    }

    /** Takes [ExerciseEntity.serverVersion] and [ExerciseEntity.isDeleted] as given; replaces all translations. */
    @Transaction
    open suspend fun saveSyncedExercise(
        exercise: ExerciseEntity,
        translations: List<ExerciseTranslationEntity>,
    ) {
        val raw = database.exerciseRawDao()
        if (raw.updateExercise(exercise) == 0) raw.insertExercise(exercise)
        raw.deleteTranslations(exercise.id)
        raw.insertTranslations(translations)
    }
}
