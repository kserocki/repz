package com.repz.app.core.database.workout

import androidx.room3.Embedded
import androidx.room3.Relation

/** `@Relation` returns children in Room's own order; callers sort by [WorkoutExerciseEntity.position]. */
data class WorkoutAggregate(
    @Embedded val workout: WorkoutEntity,
    @Relation(entity = WorkoutExerciseEntity::class, parentColumns = ["id"], entityColumns = ["workout_id"])
    val exercises: List<WorkoutExerciseWithSets>,
)
