package com.repz.app.shared.di

import com.repz.app.core.common.id.IdGenerator
import com.repz.app.core.domain.record.PersonalRecordRepository
import com.repz.app.core.domain.record.RecalculateAllPersonalRecordsUseCase
import com.repz.app.core.domain.record.RecalculatePersonalRecordsUseCase
import com.repz.app.core.domain.template.WorkoutTemplateRepository
import com.repz.app.core.domain.transaction.TransactionRunner
import com.repz.app.core.domain.workout.DeleteWorkoutUseCase
import com.repz.app.core.domain.workout.FindLastPerformanceUseCase
import com.repz.app.core.domain.workout.FinishActiveWorkoutUseCase
import com.repz.app.core.domain.workout.SaveFinishedWorkoutUseCase
import com.repz.app.core.domain.workout.StartEmptyWorkoutUseCase
import com.repz.app.core.domain.workout.StartWorkoutFromTemplateUseCase
import com.repz.app.core.domain.workout.UndoWorkoutDeletionUseCase
import com.repz.app.core.domain.workout.WorkoutRepository
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import kotlin.time.Clock

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

    /** Reason 4: [FindLastPerformanceUseCase] is declared in core:domain, which never sees Koin. */
    @Factory
    fun findLastPerformanceUseCase(workouts: WorkoutRepository): FindLastPerformanceUseCase = FindLastPerformanceUseCase(workouts)

    /** Reason 4: [StartEmptyWorkoutUseCase] is declared in core:domain, which never sees Koin. */
    @Factory
    fun startEmptyWorkoutUseCase(
        workouts: WorkoutRepository,
        clock: Clock,
        idGenerator: IdGenerator,
    ): StartEmptyWorkoutUseCase = StartEmptyWorkoutUseCase(workouts, clock, idGenerator)

    /** Reason 4: [StartWorkoutFromTemplateUseCase] is declared in core:domain, which never sees Koin. */
    @Factory
    fun startWorkoutFromTemplateUseCase(
        workouts: WorkoutRepository,
        templates: WorkoutTemplateRepository,
        lastPerformance: FindLastPerformanceUseCase,
        clock: Clock,
        idGenerator: IdGenerator,
    ): StartWorkoutFromTemplateUseCase = StartWorkoutFromTemplateUseCase(workouts, templates, lastPerformance, clock, idGenerator)

    /** Reason 4: [FinishActiveWorkoutUseCase] is declared in core:domain, which never sees Koin. */
    @Factory
    fun finishActiveWorkoutUseCase(
        workouts: WorkoutRepository,
        saveFinished: SaveFinishedWorkoutUseCase,
        clock: Clock,
    ): FinishActiveWorkoutUseCase = FinishActiveWorkoutUseCase(workouts, saveFinished, clock)
}
