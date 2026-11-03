package com.repz.app.core.database.workout

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import kotlin.uuid.Uuid

/** No FK to `exercises`: a workout may reference a custom exercise created and deleted offline. */
@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workout_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("workout_id", "position"),
        Index("exercise_id"),
    ],
)
data class WorkoutExerciseEntity(
    @PrimaryKey val id: Uuid,
    @ColumnInfo(name = "workout_id") val workoutId: Uuid,
    @ColumnInfo(name = "exercise_id") val exerciseId: Uuid,
    val position: Int,
    val notes: String,
)
