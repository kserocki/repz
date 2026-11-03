package com.repz.app.core.database.workout

import androidx.room3.Embedded
import androidx.room3.Relation

/** `@Relation` returns children in Room's own order; callers sort by [WorkoutSetEntity.position]. */
data class WorkoutExerciseWithSets(
    @Embedded val exercise: WorkoutExerciseEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["workout_exercise_id"])
    val sets: List<WorkoutSetEntity>,
)
