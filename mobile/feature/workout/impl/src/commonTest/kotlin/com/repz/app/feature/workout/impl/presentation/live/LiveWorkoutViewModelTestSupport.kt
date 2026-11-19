package com.repz.app.feature.workout.impl.presentation.live

import com.repz.app.core.domain.record.RecalculatePersonalRecordsUseCase
import com.repz.app.core.domain.workout.FindLastPerformanceUseCase
import com.repz.app.core.domain.workout.FinishActiveWorkoutUseCase
import com.repz.app.core.domain.workout.SaveFinishedWorkoutUseCase
import com.repz.app.core.testing.exercise.FakeExerciseRepository
import com.repz.app.core.testing.id.SequentialIdGenerator
import com.repz.app.core.testing.preferences.FakeUserPreferencesRepository
import com.repz.app.core.testing.record.FakePersonalRecordRepository
import com.repz.app.core.testing.time.MutableClock
import com.repz.app.core.testing.transaction.FakeTransactionRunner
import com.repz.app.core.testing.workout.FakeWorkoutRepository
import com.repz.app.feature.workout.impl.domain.ActiveWorkoutWriteLock
import com.repz.app.feature.workout.impl.domain.DiscardActiveWorkoutUseCase
import com.repz.app.feature.workout.impl.domain.EditActiveWorkoutUseCase
import com.repz.app.feature.workout.impl.domain.FinishLiveWorkoutUseCase
import com.repz.app.feature.workout.impl.domain.ObserveLiveWorkoutUseCase
import com.repz.app.feature.workout.impl.domain.SetWeightStepUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlin.time.Instant

/**
 * Shared `LiveWorkoutViewModel` test factory, used by [LiveWorkoutViewModelLifecycleTest],
 * [LiveWorkoutViewModelSetEditingTest] and [LiveWorkoutViewModelExerciseTest] (split from one test
 * class per Detekt's `LargeClass`). Subscribes [LiveWorkoutViewModel.state] on [TestScope]'s
 * `backgroundScope` and runs [dispatcher] once, so every test sees a populated first state.
 * [backgroundScope] also stands in for the app-scoped `CoroutineScope` the real app injects for
 * `onCleared`'s own flush (ADR-0029): both outlive `viewModelScope`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun TestScope.liveWorkoutViewModel(
    dispatcher: TestDispatcher,
    workouts: FakeWorkoutRepository,
    exercises: FakeExerciseRepository = FakeExerciseRepository(),
    clock: MutableClock = MutableClock(Instant.fromEpochSeconds(0)),
    preferences: FakeUserPreferencesRepository = FakeUserPreferencesRepository(),
): LiveWorkoutViewModel {
    val findLastPerformance = FindLastPerformanceUseCase(workouts)
    val recalculate = RecalculatePersonalRecordsUseCase(workouts, FakePersonalRecordRepository(), FakeTransactionRunner())
    val lock = ActiveWorkoutWriteLock()
    val viewModel =
        LiveWorkoutViewModel(
            observeLiveWorkout = ObserveLiveWorkoutUseCase(workouts, exercises, findLastPerformance, preferences),
            workouts = workouts,
            editActiveWorkout = EditActiveWorkoutUseCase(workouts, lock),
            discardActiveWorkout = DiscardActiveWorkoutUseCase(workouts, lock),
            finishActiveWorkout =
                FinishLiveWorkoutUseCase(
                    FinishActiveWorkoutUseCase(workouts, SaveFinishedWorkoutUseCase(workouts, recalculate), clock),
                    lock,
                ),
            setWeightStep = SetWeightStepUseCase(preferences),
            idGenerator = SequentialIdGenerator(),
            clock = clock,
            applicationScope = backgroundScope,
            languageTag = { "en" },
        )
    backgroundScope.launch { viewModel.state.collect {} }
    dispatcher.scheduler.runCurrent()
    return viewModel
}
