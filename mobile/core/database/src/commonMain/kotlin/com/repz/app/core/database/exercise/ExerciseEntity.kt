package com.repz.app.core.database.exercise

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * [name] is the catalog's English name, or the user's own name for a custom exercise. Attribution
 * ([license], [author], [sourceUrl], [imageUrl]) is nullable here; `core:data` requires it for a
 * catalog row ([isCustom] false) and treats a missing value as a mapping error.
 */
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: Uuid,
    val name: String,
    @ColumnInfo(name = "primary_muscle") val primaryMuscle: String,
    @ColumnInfo(name = "secondary_muscles") val secondaryMuscles: List<String>,
    val equipment: String,
    @ColumnInfo(name = "is_custom") val isCustom: Boolean,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    val license: String?,
    val author: String?,
    @ColumnInfo(name = "source_url") val sourceUrl: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
    @ColumnInfo(name = "server_version") val serverVersion: Long?,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean,
)
