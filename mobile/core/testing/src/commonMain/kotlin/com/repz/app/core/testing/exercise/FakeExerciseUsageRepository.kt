package com.repz.app.core.testing.exercise

import com.repz.app.core.domain.exercise.ExerciseUsageRepository
import com.repz.app.core.domain.exercise.RecentExerciseUse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * A hand-written fake (N7): a single [MutableStateFlow] holds every recent use, newest first (the
 * caller sets this ordering directly, since the fake doesn't derive it from a
 * [com.repz.app.core.testing.workout.FakeWorkoutRepository]).
 */
class FakeExerciseUsageRepository(
    initial: List<RecentExerciseUse> = emptyList(),
) : ExerciseUsageRepository {
    private val recentExerciseUses = MutableStateFlow(initial)

    override fun observeRecentlyUsedExercises(limit: Int): Flow<List<RecentExerciseUse>> = recentExerciseUses.map { it.take(limit) }
}
