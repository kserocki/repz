package com.repz.app.core.database.template

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import kotlin.uuid.Uuid

/** No id of its own, mirroring the server: the primary key is `(template_id, position)`. */
@Entity(
    tableName = "template_exercises",
    primaryKeys = ["template_id", "position"],
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["template_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class TemplateExerciseEntity(
    @ColumnInfo(name = "template_id") val templateId: Uuid,
    val position: Int,
    @ColumnInfo(name = "exercise_id") val exerciseId: Uuid,
    @ColumnInfo(name = "target_sets") val targetSets: Int,
    @ColumnInfo(name = "target_reps_min") val targetRepsMin: Int?,
    @ColumnInfo(name = "target_reps_max") val targetRepsMax: Int?,
    @ColumnInfo(name = "rest_seconds") val restSeconds: Int,
)
