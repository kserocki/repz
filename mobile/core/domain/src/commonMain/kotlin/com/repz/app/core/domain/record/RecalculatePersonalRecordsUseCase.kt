package com.repz.app.core.domain.record

import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.transaction.TransactionRunner
import com.repz.app.core.domain.workout.WorkoutRepository

/**
 * Replaces exactly [exerciseIds]'s personal records, recomputed from their current finished
 * history, inside one write transaction, so a concurrent recompute (a local finish and a sync pull)
 * can't interleave its read and write. An empty set is a no-op.
 */
class RecalculatePersonalRecordsUseCase(
    private val workouts: WorkoutRepository,
    private val records: PersonalRecordRepository,
    private val transactions: TransactionRunner,
) {
    suspend operator fun invoke(exerciseIds: Set<ExerciseId>) {
        if (exerciseIds.isEmpty()) return
        transactions.inWriteTransaction {
            val finishedWorkouts = workouts.findFinishedWorkoutsContaining(exerciseIds)
            // A workout containing a requested exercise can also contain others; only the
            // requested exercises' records are replaced.
            val recalculated = calculatePersonalRecords(finishedWorkouts).filter { it.exerciseId in exerciseIds }
            records.replaceForExercises(exerciseIds, recalculated)
        }
    }
}
