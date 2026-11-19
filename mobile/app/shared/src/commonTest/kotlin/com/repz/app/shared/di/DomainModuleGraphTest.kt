package com.repz.app.shared.di

import com.repz.app.core.domain.record.RecalculateAllPersonalRecordsUseCase
import com.repz.app.core.domain.record.RecalculatePersonalRecordsUseCase
import com.repz.app.core.domain.workout.DeleteWorkoutUseCase
import com.repz.app.core.domain.workout.FindLastPerformanceUseCase
import com.repz.app.core.domain.workout.FinishActiveWorkoutUseCase
import com.repz.app.core.domain.workout.SaveFinishedWorkoutUseCase
import com.repz.app.core.domain.workout.StartEmptyWorkoutUseCase
import com.repz.app.core.domain.workout.StartWorkoutFromTemplateUseCase
import com.repz.app.core.domain.workout.UndoWorkoutDeletionUseCase
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatformTools
import org.koin.plugin.module.dsl.startKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotSame

/**
 * Regression test: [DomainModule]'s use cases resolve from the real `RepzApp` graph on every
 * target, each as a new instance per `get` (`@Factory`), the same way [SettingsGraphTest] and
 * [AuthGraphTest] do for their own modules' use cases.
 */
class DomainModuleGraphTest {
    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun givenRepzApp_whenStarted_thenUseCasesResolveAsNewInstances() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertNotSame(koin.get<RecalculatePersonalRecordsUseCase>(), koin.get<RecalculatePersonalRecordsUseCase>())
        assertNotSame(koin.get<RecalculateAllPersonalRecordsUseCase>(), koin.get<RecalculateAllPersonalRecordsUseCase>())
        assertNotSame(koin.get<SaveFinishedWorkoutUseCase>(), koin.get<SaveFinishedWorkoutUseCase>())
        assertNotSame(koin.get<DeleteWorkoutUseCase>(), koin.get<DeleteWorkoutUseCase>())
        assertNotSame(koin.get<UndoWorkoutDeletionUseCase>(), koin.get<UndoWorkoutDeletionUseCase>())
        assertNotSame(koin.get<FindLastPerformanceUseCase>(), koin.get<FindLastPerformanceUseCase>())
        assertNotSame(koin.get<StartEmptyWorkoutUseCase>(), koin.get<StartEmptyWorkoutUseCase>())
        assertNotSame(koin.get<StartWorkoutFromTemplateUseCase>(), koin.get<StartWorkoutFromTemplateUseCase>())
        assertNotSame(koin.get<FinishActiveWorkoutUseCase>(), koin.get<FinishActiveWorkoutUseCase>())
    }
}
