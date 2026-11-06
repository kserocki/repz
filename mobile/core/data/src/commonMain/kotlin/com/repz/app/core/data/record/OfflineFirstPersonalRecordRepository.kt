package com.repz.app.core.data.record

import co.touchlab.kermit.Logger
import com.repz.app.core.database.record.PersonalRecordDao
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.record.PersonalRecord
import com.repz.app.core.domain.record.PersonalRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

internal const val PERSONAL_RECORD_LOG_TAG = "PersonalRecord"

/**
 * Rule 6: derived data (ADR-0023), never synced. [replaceForExercises]/[replaceAll] are called only
 * from [com.repz.app.core.domain.record.RecalculatePersonalRecordsUseCase]/
 * [com.repz.app.core.domain.record.RecalculateAllPersonalRecordsUseCase], inside their own write
 * transaction alongside the workout read they recompute from.
 */
@Single
class OfflineFirstPersonalRecordRepository(
    private val personalRecordDao: PersonalRecordDao,
    private val logger: Logger = Logger.withTag(PERSONAL_RECORD_LOG_TAG),
) : PersonalRecordRepository {
    override fun observeAll(): Flow<List<PersonalRecord>> = personalRecordDao.observeAll().map { it.toDomainList(logger) }

    override fun observeForExercise(id: ExerciseId): Flow<List<PersonalRecord>> =
        personalRecordDao.observeForExercise(id.value).map { it.toDomainList(logger) }

    override suspend fun replaceForExercises(
        exerciseIds: Set<ExerciseId>,
        records: List<PersonalRecord>,
    ) {
        personalRecordDao.replaceForExercises(exerciseIds.map { it.value }, records.map { it.toEntity() })
    }

    override suspend fun replaceAll(records: List<PersonalRecord>) {
        personalRecordDao.replaceAll(records.map { it.toEntity() })
    }
}
