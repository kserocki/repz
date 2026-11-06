package com.repz.app.core.domain.record

import com.repz.app.core.domain.model.ExerciseId
import kotlinx.coroutines.flow.Flow

/**
 * Derived personal records (rule 6: the UI reads only from Room). They are never written
 * directly; [RecalculatePersonalRecordsUseCase] and [RecalculateAllPersonalRecordsUseCase] are the
 * only callers of [replaceForExercises]/[replaceAll].
 */
interface PersonalRecordRepository {
    fun observeAll(): Flow<List<PersonalRecord>>

    fun observeForExercise(id: ExerciseId): Flow<List<PersonalRecord>>

    suspend fun replaceForExercises(
        exerciseIds: Set<ExerciseId>,
        records: List<PersonalRecord>,
    )

    suspend fun replaceAll(records: List<PersonalRecord>)
}
