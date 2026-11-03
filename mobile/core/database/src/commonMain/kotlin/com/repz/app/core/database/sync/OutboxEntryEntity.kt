package com.repz.app.core.database.sync

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** At most one pending row per entity, enforced by the primary key. Carries no payload. */
@Entity(
    tableName = "outbox",
    primaryKeys = ["entity_type", "entity_id"],
    indices = [Index("created_at")],
)
data class OutboxEntryEntity(
    @ColumnInfo(name = "entity_type") val entityType: SyncEntityType,
    @ColumnInfo(name = "entity_id") val entityId: Uuid,
    val operation: OutboxOperation,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
    val revision: Long,
    val attempts: Int,
    @ColumnInfo(name = "held_until") val heldUntil: Instant?,
)
