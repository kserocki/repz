package com.repz.app.core.testing.record

import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.record.PersonalRecord
import com.repz.app.core.domain.record.PersonalRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * A hand-written fake (N7): a single [MutableStateFlow] holds every record;
 * [replaceForExercises]/[replaceAll] calls are recorded for assertions.
 */
class FakePersonalRecordRepository(
    initial: List<PersonalRecord> = emptyList(),
) : PersonalRecordRepository {
    private val records = MutableStateFlow(initial)

    val replaceForExercisesCalls = mutableListOf<Pair<Set<ExerciseId>, List<PersonalRecord>>>()
    val replaceAllCalls = mutableListOf<List<PersonalRecord>>()

    override fun observeAll(): Flow<List<PersonalRecord>> = records

    override fun observeForExercise(id: ExerciseId): Flow<List<PersonalRecord>> =
        records.map { list -> list.filter { it.exerciseId == id } }

    override suspend fun replaceForExercises(
        exerciseIds: Set<ExerciseId>,
        records: List<PersonalRecord>,
    ) {
        replaceForExercisesCalls += exerciseIds to records
        this.records.value = this.records.value.filterNot { it.exerciseId in exerciseIds } + records
    }

    override suspend fun replaceAll(records: List<PersonalRecord>) {
        replaceAllCalls += records
        this.records.value = records
    }
}
