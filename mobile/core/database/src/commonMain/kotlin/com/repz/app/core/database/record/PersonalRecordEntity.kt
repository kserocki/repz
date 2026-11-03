package com.repz.app.core.database.record

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** Derived data, recomputed locally and never synced: no sync columns, no outbox use, no FKs. */
@Entity(tableName = "personal_records", primaryKeys = ["exercise_id", "type"])
data class PersonalRecordEntity(
    @ColumnInfo(name = "exercise_id") val exerciseId: Uuid,
    val type: String,
    @ColumnInfo(name = "weight_grams") val weightGrams: Long,
    val reps: Int,
    @ColumnInfo(name = "workout_id") val workoutId: Uuid,
    @ColumnInfo(name = "set_id") val setId: Uuid,
    @ColumnInfo(name = "achieved_at") val achievedAt: Instant,
)
