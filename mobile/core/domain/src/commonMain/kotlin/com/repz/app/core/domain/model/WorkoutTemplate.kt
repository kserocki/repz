package com.repz.app.core.domain.model

import kotlin.time.Duration

data class WorkoutTemplate(
    val id: WorkoutTemplateId,
    val name: String,
    val notes: String,
    val exercises: List<TemplateExercise>,
)

/** List order is the order (ADR-0020): there is no separate `order` field to drift from it. */
data class TemplateExercise(
    val exerciseId: ExerciseId,
    val targetSets: Int,
    val targetReps: IntRange?,
    val rest: Duration,
)
