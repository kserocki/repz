package com.repz.app.shared.di

import com.repz.app.core.domain.record.PersonalRecordRepository
import com.repz.app.core.domain.record.RecalculateAllPersonalRecordsUseCase
import com.repz.app.core.domain.record.RecalculatePersonalRecordsUseCase
import com.repz.app.core.domain.transaction.TransactionRunner
import com.repz.app.core.domain.workout.DeleteWorkoutUseCase
import com.repz.app.core.domain.workout.SaveFinishedWorkoutUseCase
import com.repz.app.core.domain.workout.UndoWorkoutDeletionUseCase
import com.repz.app.core.domain.workout.WorkoutRepository
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

/**
 * `core:domain` stays Koin-free (AGENTS §4), so its shared use cases are provided here (ADR-0008
 * reason 4), the same way [CommonModule] provides `core:common`'s. No `@ComponentScan`: there is
 * nothing else in `com.repz.app.shared.di` for this module to pick up.
 */
@Module
class DomainModule {
    @Factory
    fun recalculatePersonalRecordsUseCase(
        workouts: WorkoutRepository,
        records: PersonalRecordRepository,
        transactions: TransactionRunner,
    ): RecalculatePersonalRecordsUseCase = RecalculatePersonalRecordsUseCase(workouts, records, transactions)

    @Factory
    fun recalculateAllPersonalRecordsUseCase(
        workouts: WorkoutRepository,
        records: PersonalRecordRepository,
        transactions: TransactionRunner,
    ): RecalculateAllPersonalRecordsUseCase = RecalculateAllPersonalRecordsUseCase(workouts, records, transactions)

    @Factory
    fun saveFinishedWorkoutUseCase(
        workouts: WorkoutRepository,
        recalculate: RecalculatePersonalRecordsUseCase,
    ): SaveFinishedWorkoutUseCase = SaveFinishedWorkoutUseCase(workouts, recalculate)

    @Factory
    fun deleteWorkoutUseCase(
        workouts: WorkoutRepository,
        recalculate: RecalculatePersonalRecordsUseCase,
    ): DeleteWorkoutUseCase = DeleteWorkoutUseCase(workouts, recalculate)

    @Factory
    fun undoWorkoutDeletionUseCase(
        workouts: WorkoutRepository,
        recalculate: RecalculatePersonalRecordsUseCase,
    ): UndoWorkoutDeletionUseCase = UndoWorkoutDeletionUseCase(workouts, recalculate)
}
