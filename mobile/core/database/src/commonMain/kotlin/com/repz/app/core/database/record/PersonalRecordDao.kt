package com.repz.app.core.database.record

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

@Dao
abstract class PersonalRecordDao {
    @Query("SELECT * FROM personal_records")
    abstract fun observeAll(): Flow<List<PersonalRecordEntity>>

    @Query("SELECT * FROM personal_records WHERE exercise_id = :exerciseId")
    abstract fun observeForExercise(exerciseId: Uuid): Flow<List<PersonalRecordEntity>>

    /** Replaces the given exercises' records; every other exercise's records are untouched. */
    @Transaction
    open suspend fun replaceForExercises(
        exerciseIds: List<Uuid>,
        records: List<PersonalRecordEntity>,
    ) {
        deleteForExercises(exerciseIds)
        insertAll(records)
    }

    @Query("DELETE FROM personal_records WHERE exercise_id IN (:exerciseIds)")
    internal abstract suspend fun deleteForExercises(exerciseIds: List<Uuid>)

    @Insert
    internal abstract suspend fun insertAll(records: List<PersonalRecordEntity>)
}
