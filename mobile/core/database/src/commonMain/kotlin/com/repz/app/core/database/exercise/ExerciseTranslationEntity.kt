package com.repz.app.core.database.exercise

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import kotlin.uuid.Uuid

/** No locale whitelist: a new language is new rows, never a code change (ADR-0021). */
@Entity(
    tableName = "exercise_translations",
    primaryKeys = ["exercise_id", "locale"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ExerciseTranslationEntity(
    @ColumnInfo(name = "exercise_id") val exerciseId: Uuid,
    val locale: String,
    val name: String,
    val instructions: String?,
)
