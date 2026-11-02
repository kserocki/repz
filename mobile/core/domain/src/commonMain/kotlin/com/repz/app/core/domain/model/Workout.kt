package com.repz.app.core.domain.model

import kotlin.time.Instant

data class Workout(
    val id: WorkoutId,
    val templateId: WorkoutTemplateId?,
    val name: String,
    val notes: String,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val exercises: List<WorkoutExercise>,
) {
    val isFinished: Boolean get() = finishedAt != null
}

/** List order is the order (ADR-0020): there is no separate `order` field to drift from it. */
data class WorkoutExercise(
    val id: WorkoutExerciseId,
    val exerciseId: ExerciseId,
    val notes: String,
    val sets: List<WorkoutSet>,
)

data class WorkoutSet(
    val id: WorkoutSetId,
    val type: SetType,
    val weight: Weight,
    val reps: Int,
    val rpe: Rpe?,
    val completedAt: Instant?,
) {
    val isCompleted: Boolean get() = completedAt != null
}
