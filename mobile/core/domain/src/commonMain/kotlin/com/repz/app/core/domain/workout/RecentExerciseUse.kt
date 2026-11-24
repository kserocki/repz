package com.repz.app.core.domain.workout

import com.repz.app.core.domain.model.ExerciseId
import kotlin.time.Instant

/** One exercise's most recent use across finished workouts (its latest [com.repz.app.core.domain.model.Workout.startedAt]). */
data class RecentExerciseUse(
    val exerciseId: ExerciseId,
    val lastUsedAt: Instant,
)
