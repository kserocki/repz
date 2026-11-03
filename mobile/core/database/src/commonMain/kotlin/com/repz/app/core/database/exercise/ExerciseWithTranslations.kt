package com.repz.app.core.database.exercise

import androidx.room3.Embedded
import androidx.room3.Relation

data class ExerciseWithTranslations(
    @Embedded val exercise: ExerciseEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["exercise_id"])
    val translations: List<ExerciseTranslationEntity>,
)
