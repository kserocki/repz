package com.repz.app.core.database.sync

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Dao
abstract class OutboxDao {
    /**
     * Finds the pending row (if any), coalesces it with [operation], and persists the result:
     * inserted when nothing was pending, updated otherwise. No `@Upsert`: see
     * [com.repz.app.core.database.workout.WorkoutRawDao].
     */
    @Transaction
    open suspend fun enqueue(
        entityType: SyncEntityType,
        entityId: Uuid,
        operation: OutboxOperation,
        now: Instant,
        heldUntil: Instant? = null,
    ) {
        val existing = findEntry(entityType, entityId)
        val entry = coalesceOutboxEntry(existing, entityType, entityId, operation, now, heldUntil)
        if (existing == null) insertEntry(entry) else updateEntry(entry)
    }

    @Query(
        "SELECT * FROM outbox WHERE held_until IS NULL OR held_until <= :now " +
            "ORDER BY created_at, entity_type, entity_id LIMIT :limit",
    )
    abstract suspend fun findReadyToPush(
        now: Instant,
        limit: Int,
    ): List<OutboxEntryEntity>

    /** Returns the number of rows deleted: 0 means the row changed (a higher revision) since it was read. */
    @Query("DELETE FROM outbox WHERE entity_type = :entityType AND entity_id = :entityId AND revision = :revision")
    abstract suspend fun deleteIfUnchanged(
        entityType: SyncEntityType,
        entityId: Uuid,
        revision: Long,
    ): Int

    @Query("UPDATE outbox SET held_until = NULL WHERE entity_type = :entityType AND entity_id = :entityId")
    abstract suspend fun releaseHold(
        entityType: SyncEntityType,
        entityId: Uuid,
    )

    @Query("UPDATE outbox SET attempts = attempts + 1 WHERE entity_type = :entityType AND entity_id = :entityId")
    abstract suspend fun incrementAttempts(
        entityType: SyncEntityType,
        entityId: Uuid,
    )

    /** A held row still counts as pending, so a pull can skip it. */
    @Query("SELECT entity_id FROM outbox WHERE entity_type = :entityType AND entity_id IN (:entityIds)")
    abstract suspend fun findPendingEntityIds(
        entityType: SyncEntityType,
        entityIds: List<Uuid>,
    ): List<Uuid>

    @Query("SELECT COUNT(*) FROM outbox")
    abstract fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM outbox")
    abstract suspend fun countPending(): Int

    @Query("SELECT * FROM outbox WHERE entity_type = :entityType AND entity_id = :entityId")
    internal abstract suspend fun findEntry(
        entityType: SyncEntityType,
        entityId: Uuid,
    ): OutboxEntryEntity?

    @Insert
    internal abstract suspend fun insertEntry(entry: OutboxEntryEntity)

    @Update
    internal abstract suspend fun updateEntry(entry: OutboxEntryEntity): Int
}
