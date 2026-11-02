package com.repz.app.core.database.workout

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** [finishedAt] null marks the one local-only active workout; it enters sync only once finished. */
@Entity(
    tableName = "workouts",
    indices = [
        Index("started_at"),
        Index("finished_at"),
    ],
)
data class WorkoutEntity(
    @PrimaryKey val id: Uuid,
    @ColumnInfo(name = "template_id") val templateId: Uuid?,
    val name: String,
    val notes: String,
    @ColumnInfo(name = "started_at") val startedAt: Instant,
    @ColumnInfo(name = "finished_at") val finishedAt: Instant?,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
    @ColumnInfo(name = "server_version") val serverVersion: Long?,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean,
)
