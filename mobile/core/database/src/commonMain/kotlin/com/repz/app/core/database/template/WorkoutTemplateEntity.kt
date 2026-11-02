package com.repz.app.core.database.template

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity(tableName = "workout_templates")
data class WorkoutTemplateEntity(
    @PrimaryKey val id: Uuid,
    val name: String,
    val notes: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
    @ColumnInfo(name = "server_version") val serverVersion: Long?,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean,
)
