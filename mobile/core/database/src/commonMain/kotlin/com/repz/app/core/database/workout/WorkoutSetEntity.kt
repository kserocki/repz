package com.repz.app.core.database.workout

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** [type] and [weightGrams]/[reps]/[rpe] mirror the wire vocabulary and units exactly (ADR-0019). */
@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["workout_exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("workout_exercise_id", "position"),
    ],
)
data class WorkoutSetEntity(
    @PrimaryKey val id: Uuid,
    @ColumnInfo(name = "workout_exercise_id") val workoutExerciseId: Uuid,
    val position: Int,
    val type: String,
    @ColumnInfo(name = "weight_grams") val weightGrams: Long,
    val reps: Int,
    val rpe: Double?,
    @ColumnInfo(name = "completed_at") val completedAt: Instant?,
)
