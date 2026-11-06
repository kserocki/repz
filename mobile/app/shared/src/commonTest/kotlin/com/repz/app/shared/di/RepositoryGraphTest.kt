package com.repz.app.shared.di

import com.repz.app.core.data.exercise.OfflineFirstExerciseRepository
import com.repz.app.core.data.preferences.OfflineFirstUserPreferencesRepository
import com.repz.app.core.data.record.OfflineFirstPersonalRecordRepository
import com.repz.app.core.data.sync.NoOpSyncScheduler
import com.repz.app.core.data.template.OfflineFirstWorkoutTemplateRepository
import com.repz.app.core.data.transaction.RoomTransactionRunner
import com.repz.app.core.data.workout.OfflineFirstWorkoutRepository
import com.repz.app.core.domain.exercise.ExerciseRepository
import com.repz.app.core.domain.preferences.UserPreferencesRepository
import com.repz.app.core.domain.record.PersonalRecordRepository
import com.repz.app.core.domain.sync.SyncScheduler
import com.repz.app.core.domain.template.WorkoutTemplateRepository
import com.repz.app.core.domain.transaction.TransactionRunner
import com.repz.app.core.domain.workout.WorkoutRepository
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatformTools
import org.koin.plugin.module.dsl.startKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertSame

/**
 * Regression test: every offline-first repository, plus [TransactionRunner] and [SyncScheduler], is
 * a singleton bound to the same instance as its `OfflineFirst*`/impl class (ADR-0008), resolving
 * from the real `RepzApp` graph on every target, the same way [DataModuleGraphTest] does for
 * `SessionRepository`.
 */
class RepositoryGraphTest {
    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun givenRepzApp_whenStarted_thenWorkoutRepositoryIsASingleton() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertSame(koin.get<WorkoutRepository>(), koin.get<WorkoutRepository>())
    }

    @Test
    fun givenRepzApp_whenStarted_thenWorkoutRepositoryIsTheSameInstanceAsOfflineFirstWorkoutRepository() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertSame(koin.get<WorkoutRepository>(), koin.get<OfflineFirstWorkoutRepository>())
    }

    @Test
    fun givenRepzApp_whenStarted_thenWorkoutTemplateRepositoryIsASingleton() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertSame(koin.get<WorkoutTemplateRepository>(), koin.get<WorkoutTemplateRepository>())
    }

    @Test
    fun givenRepzApp_whenStarted_thenWorkoutTemplateRepositoryIsTheSameInstanceAsOfflineFirstWorkoutTemplateRepository() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertSame(koin.get<WorkoutTemplateRepository>(), koin.get<OfflineFirstWorkoutTemplateRepository>())
    }

    @Test
    fun givenRepzApp_whenStarted_thenExerciseRepositoryIsASingleton() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertSame(koin.get<ExerciseRepository>(), koin.get<ExerciseRepository>())
    }

    @Test
    fun givenRepzApp_whenStarted_thenExerciseRepositoryIsTheSameInstanceAsOfflineFirstExerciseRepository() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertSame(koin.get<ExerciseRepository>(), koin.get<OfflineFirstExerciseRepository>())
    }

    @Test
    fun givenRepzApp_whenStarted_thenPersonalRecordRepositoryIsASingleton() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertSame(koin.get<PersonalRecordRepository>(), koin.get<PersonalRecordRepository>())
    }

    @Test
    fun givenRepzApp_whenStarted_thenPersonalRecordRepositoryIsTheSameInstanceAsOfflineFirstPersonalRecordRepository() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertSame(koin.get<PersonalRecordRepository>(), koin.get<OfflineFirstPersonalRecordRepository>())
    }

    @Test
    fun givenRepzApp_whenStarted_thenUserPreferencesRepositoryIsASingleton() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertSame(koin.get<UserPreferencesRepository>(), koin.get<UserPreferencesRepository>())
    }

    @Test
    fun givenRepzApp_whenStarted_thenUserPreferencesRepositoryIsTheSameInstanceAsOfflineFirstUserPreferencesRepository() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertSame(koin.get<UserPreferencesRepository>(), koin.get<OfflineFirstUserPreferencesRepository>())
    }

    @Test
    fun givenRepzApp_whenStarted_thenTransactionRunnerIsASingleton() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertSame(koin.get<TransactionRunner>(), koin.get<TransactionRunner>())
    }

    @Test
    fun givenRepzApp_whenStarted_thenTransactionRunnerIsTheSameInstanceAsRoomTransactionRunner() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertSame(koin.get<TransactionRunner>(), koin.get<RoomTransactionRunner>())
    }

    @Test
    fun givenRepzApp_whenStarted_thenSyncSchedulerIsASingleton() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertSame(koin.get<SyncScheduler>(), koin.get<SyncScheduler>())
    }

    @Test
    fun givenRepzApp_whenStarted_thenSyncSchedulerIsTheSameInstanceAsNoOpSyncScheduler() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertSame(koin.get<SyncScheduler>(), koin.get<NoOpSyncScheduler>())
    }
}
