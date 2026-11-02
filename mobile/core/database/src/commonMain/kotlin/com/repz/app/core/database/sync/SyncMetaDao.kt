package com.repz.app.core.database.sync

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
interface SyncMetaDao {
    /** [SyncMetaEntity.INITIAL] when the row is missing, i.e. before the first sync ever ran. */
    fun observe(): Flow<SyncMetaEntity> = observeNullable().map { it ?: SyncMetaEntity.INITIAL }

    suspend fun get(): SyncMetaEntity = findNullable() ?: SyncMetaEntity.INITIAL

    @Upsert
    suspend fun save(meta: SyncMetaEntity)

    @Query("SELECT * FROM sync_meta WHERE id = ${SyncMetaEntity.SINGLETON_ID}")
    fun observeNullable(): Flow<SyncMetaEntity?>

    @Query("SELECT * FROM sync_meta WHERE id = ${SyncMetaEntity.SINGLETON_ID}")
    suspend fun findNullable(): SyncMetaEntity?
}
