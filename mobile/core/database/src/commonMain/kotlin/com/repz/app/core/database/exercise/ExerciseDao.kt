package com.repz.app.core.database.exercise

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Dao
abstract class ExerciseDao {
    /** Not deleted; sorting by localised name happens in `core:data`. */
    @Transaction
    @Query("SELECT * FROM exercises WHERE is_deleted = 0")
    abstract fun observeExercises(): Flow<List<ExerciseWithTranslations>>

    @Transaction
    @Query("SELECT * FROM exercises WHERE id = :id AND is_deleted = 0")
    abstract fun observeExercise(id: Uuid): Flow<ExerciseWithTranslations?>

    /** History/summary names (Q1): includes tombstones for ids the device has ever seen. */
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

    @Query("SELECT server_version FROM exercises WHERE id = :id")
    abstract suspend fun findServerVersion(id: Uuid): Long?

    @Upsert
    abstract suspend fun upsertExercise(exercise: ExerciseEntity)

    @Query("DELETE FROM exercise_translations WHERE exercise_id = :exerciseId")
    abstract suspend fun deleteTranslations(exerciseId: Uuid)

    @Insert
    abstract suspend fun insertTranslations(translations: List<ExerciseTranslationEntity>)

    /** Keeps the stored [ExerciseEntity.serverVersion] and forces [ExerciseEntity.isDeleted] to false. */
    @Transaction
    open suspend fun saveLocalExercise(
        exercise: ExerciseEntity,
        translations: List<ExerciseTranslationEntity>,
    ) {
        val storedServerVersion = findServerVersion(exercise.id)
        saveSyncedExercise(exercise.copy(serverVersion = storedServerVersion, isDeleted = false), translations)
    }

    /** Takes [ExerciseEntity.serverVersion] and [ExerciseEntity.isDeleted] as given; replaces all translations. */
    @Transaction
    open suspend fun saveSyncedExercise(
        exercise: ExerciseEntity,
        translations: List<ExerciseTranslationEntity>,
    ) {
        upsertExercise(exercise)
        deleteTranslations(exercise.id)
        insertTranslations(translations)
    }
}
