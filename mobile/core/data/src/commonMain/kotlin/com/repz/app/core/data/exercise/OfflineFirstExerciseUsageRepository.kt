package com.repz.app.core.data.exercise

import com.repz.app.core.database.exercise.ExerciseUsageDao
import com.repz.app.core.domain.exercise.ExerciseUsageRepository
import com.repz.app.core.domain.exercise.RecentExerciseUse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

/** Rule 6: reads only from Room, through its own [ExerciseUsageDao] (ADR-0029). */
@Single
class OfflineFirstExerciseUsageRepository(
    private val exerciseUsageDao: ExerciseUsageDao,
) : ExerciseUsageRepository {
    override fun observeRecentlyUsedExercises(limit: Int): Flow<List<RecentExerciseUse>> =
        exerciseUsageDao.observeRecentlyUsedExercises(limit).map { it.toDomainList() }
}
