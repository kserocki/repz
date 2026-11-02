package com.repz.app.core.database.template

import androidx.room3.Embedded
import androidx.room3.Relation

/** `@Relation` returns children in Room's own order; callers sort by [TemplateExerciseEntity.position]. */
data class TemplateAggregate(
    @Embedded val template: WorkoutTemplateEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["template_id"])
    val exercises: List<TemplateExerciseEntity>,
)
