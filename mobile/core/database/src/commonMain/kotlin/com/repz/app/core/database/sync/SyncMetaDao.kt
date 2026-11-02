package com.repz.app.core.database.sync

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
abstract class SyncMetaDao {
    @Query("SELECT * FROM sync_meta WHERE id = ${SyncMetaEntity.SINGLETON_ID}")
    abstract fun observeNullable(): Flow<SyncMetaEntity?>

    @Query("SELECT * FROM sync_meta WHERE id = ${SyncMetaEntity.SINGLETON_ID}")
    abstract suspend fun findNullable(): SyncMetaEntity?

    @Update
    abstract suspend fun updateMeta(meta: SyncMetaEntity): Int

    @Insert
    abstract suspend fun insertMeta(meta: SyncMetaEntity)

    /** [SyncMetaEntity.INITIAL] when the row is missing, i.e. before the first sync ever ran. */
    fun observe(): Flow<SyncMetaEntity> = observeNullable().map { it ?: SyncMetaEntity.INITIAL }

    suspend fun get(): SyncMetaEntity = findNullable() ?: SyncMetaEntity.INITIAL

    /** No `@Upsert`: see [WorkoutRawDao][com.repz.app.core.database.workout.WorkoutRawDao]. */
    @Transaction
    open suspend fun save(meta: SyncMetaEntity) {
        if (updateMeta(meta) == 0) insertMeta(meta)
    }
}
