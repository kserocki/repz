package com.repz.app.core.database.workout

import kotlin.time.Instant
import kotlin.uuid.Uuid

/** Projection for [WorkoutDao.observeRecentlyUsedExercises]: one row per exercise, its latest use. */
data class RecentExerciseUseRow(
    val exerciseId: Uuid,
    val lastUsedAt: Instant,
)
