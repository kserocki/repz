package com.repz.app.core.database.sync

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

@Dao
abstract class SyncRejectionDao {
    @Query("DELETE FROM sync_rejections WHERE entity_type = :entityType AND entity_id = :entityId")
    abstract suspend fun delete(
        entityType: SyncEntityType,
        entityId: Uuid,
    )

    @Query("SELECT * FROM sync_rejections")
    abstract fun observeAll(): Flow<List<SyncRejectionEntity>>

    @Query("SELECT COUNT(*) FROM sync_rejections")
    abstract fun observeCount(): Flow<Int>

    @Update
    abstract suspend fun updateRejection(rejection: SyncRejectionEntity): Int

    @Insert
    abstract suspend fun insertRejection(rejection: SyncRejectionEntity)

    /**
     * Re-upsert replaces the stored problem/timestamp. No `@Upsert`: see
     * [WorkoutRawDao][com.repz.app.core.database.workout.WorkoutRawDao].
     */
    @Transaction
    open suspend fun upsert(rejection: SyncRejectionEntity) {
        if (updateRejection(rejection) == 0) insertRejection(rejection)
    }
}
