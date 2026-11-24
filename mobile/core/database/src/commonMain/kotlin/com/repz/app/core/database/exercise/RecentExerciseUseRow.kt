package com.repz.app.core.database.exercise

import kotlin.time.Instant
import kotlin.uuid.Uuid

/** Projection for [ExerciseUsageDao.observeRecentlyUsedExercises]: one row per exercise, its latest use. */
data class RecentExerciseUseRow(
    val exerciseId: Uuid,
    val lastUsedAt: Instant,
)
