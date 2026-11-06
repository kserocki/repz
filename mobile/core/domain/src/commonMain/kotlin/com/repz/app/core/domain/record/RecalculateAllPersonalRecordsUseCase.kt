package com.repz.app.core.domain.record

import com.repz.app.core.domain.transaction.TransactionRunner
import com.repz.app.core.domain.workout.WorkoutRepository

/**
 * Recomputes every exercise's personal records from all finished workouts and replaces the whole
 * table, inside one write transaction. Used once, after the initial sync restore.
 */
class RecalculateAllPersonalRecordsUseCase(
    private val workouts: WorkoutRepository,
    private val records: PersonalRecordRepository,
    private val transactions: TransactionRunner,
) {
    suspend operator fun invoke() {
        transactions.inWriteTransaction {
            records.replaceAll(calculatePersonalRecords(workouts.findFinishedWorkouts()))
        }
    }
}
