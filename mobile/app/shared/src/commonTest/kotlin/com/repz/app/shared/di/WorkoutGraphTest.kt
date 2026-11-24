package com.repz.app.shared.di

import com.repz.app.feature.workout.impl.domain.ActiveWorkoutWriteLock
import com.repz.app.feature.workout.impl.domain.AddExercisesToActiveWorkoutUseCase
import com.repz.app.feature.workout.impl.domain.DiscardActiveWorkoutUseCase
import com.repz.app.feature.workout.impl.domain.EditActiveWorkoutUseCase
import com.repz.app.feature.workout.impl.domain.FinishLiveWorkoutUseCase
import com.repz.app.feature.workout.impl.domain.ObserveExercisePickerUseCase
import com.repz.app.feature.workout.impl.domain.ObserveLiveWorkoutUseCase
import com.repz.app.feature.workout.impl.domain.ObserveTrainOverviewUseCase
import com.repz.app.feature.workout.impl.domain.ReplaceExerciseInActiveWorkoutUseCase
import com.repz.app.feature.workout.impl.domain.StartEmptyLiveWorkoutUseCase
import com.repz.app.feature.workout.impl.domain.StartLiveWorkoutFromTemplateUseCase
import com.repz.app.feature.workout.impl.presentation.live.LiveWorkoutViewModel
import com.repz.app.feature.workout.impl.presentation.picker.ExercisePickerViewModel
import com.repz.app.feature.workout.impl.presentation.train.TrainViewModel
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatformTools
import org.koin.plugin.module.dsl.startKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame

/**
 * Regression test: `WorkoutModule`'s definitions resolve from the real `RepzApp` graph on every
 * target, the same way [AuthGraphTest] and [SettingsGraphTest] do.
 */
class WorkoutGraphTest {
    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun givenRepzApp_whenStarted_thenActiveWorkoutWriteLockResolvesAsASingleton() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertSame(koin.get<ActiveWorkoutWriteLock>(), koin.get<ActiveWorkoutWriteLock>())
    }

    @Test
    fun givenRepzApp_whenStarted_thenUseCasesResolveAsNewInstances() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertNotSame(koin.get<EditActiveWorkoutUseCase>(), koin.get<EditActiveWorkoutUseCase>())
        assertNotSame(koin.get<AddExercisesToActiveWorkoutUseCase>(), koin.get<AddExercisesToActiveWorkoutUseCase>())
        assertNotSame(koin.get<ReplaceExerciseInActiveWorkoutUseCase>(), koin.get<ReplaceExerciseInActiveWorkoutUseCase>())
        assertNotSame(koin.get<DiscardActiveWorkoutUseCase>(), koin.get<DiscardActiveWorkoutUseCase>())
        assertNotSame(koin.get<FinishLiveWorkoutUseCase>(), koin.get<FinishLiveWorkoutUseCase>())
        assertNotSame(koin.get<StartEmptyLiveWorkoutUseCase>(), koin.get<StartEmptyLiveWorkoutUseCase>())
        assertNotSame(koin.get<StartLiveWorkoutFromTemplateUseCase>(), koin.get<StartLiveWorkoutFromTemplateUseCase>())
        assertNotSame(koin.get<ObserveLiveWorkoutUseCase>(), koin.get<ObserveLiveWorkoutUseCase>())
        assertNotSame(koin.get<ObserveTrainOverviewUseCase>(), koin.get<ObserveTrainOverviewUseCase>())
        assertNotSame(koin.get<ObserveExercisePickerUseCase>(), koin.get<ObserveExercisePickerUseCase>())
    }

    @Test
    fun givenRepzApp_whenStarted_thenEveryViewModelResolves() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertNotNull(koin.get<LiveWorkoutViewModel>())
        assertNotNull(koin.get<TrainViewModel>())
        assertNotNull(koin.get<ExercisePickerViewModel> { parametersOf(null) })
    }
}
