package com.repz.app.core.database.sync

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** [problemType] is the Problem Details `type` URI; the UI maps it to localised copy. */
@Entity(tableName = "sync_rejections", primaryKeys = ["entity_type", "entity_id"])
data class SyncRejectionEntity(
    @ColumnInfo(name = "entity_type") val entityType: SyncEntityType,
    @ColumnInfo(name = "entity_id") val entityId: Uuid,
    @ColumnInfo(name = "problem_type") val problemType: String,
    @ColumnInfo(name = "rejected_at") val rejectedAt: Instant,
)
