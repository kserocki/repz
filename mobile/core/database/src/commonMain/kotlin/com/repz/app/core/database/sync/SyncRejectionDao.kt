package com.repz.app.core.database.sync

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

@Dao
interface SyncRejectionDao {
    @Upsert
    suspend fun upsert(rejection: SyncRejectionEntity)

    @Query("DELETE FROM sync_rejections WHERE entity_type = :entityType AND entity_id = :entityId")
    suspend fun delete(
        entityType: SyncEntityType,
        entityId: Uuid,
    )

    @Query("SELECT * FROM sync_rejections")
    fun observeAll(): Flow<List<SyncRejectionEntity>>

    @Query("SELECT COUNT(*) FROM sync_rejections")
    fun observeCount(): Flow<Int>
}
