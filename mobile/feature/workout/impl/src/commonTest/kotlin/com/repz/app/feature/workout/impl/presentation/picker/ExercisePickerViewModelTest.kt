package com.repz.app.feature.workout.impl.presentation.picker

import app.cash.turbine.test
import com.repz.app.core.domain.model.Muscle
import com.repz.app.core.domain.workout.FindLastPerformanceUseCase
import com.repz.app.core.testing.exercise.FakeExerciseRepository
import com.repz.app.core.testing.id.SequentialIdGenerator
import com.repz.app.core.testing.template.FakeWorkoutTemplateRepository
import com.repz.app.core.testing.workout.FakeWorkoutRepository
import com.repz.app.feature.workout.impl.domain.ActiveWorkoutWriteLock
import com.repz.app.feature.workout.impl.domain.AddExerciseToActiveWorkoutUseCase
import com.repz.app.feature.workout.impl.domain.ObservePickerExercisesUseCase
import com.repz.app.feature.workout.impl.testActiveWorkout
import com.repz.app.feature.workout.impl.testExercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ExercisePickerViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        exercises: FakeExerciseRepository = FakeExerciseRepository(),
        workouts: FakeWorkoutRepository = FakeWorkoutRepository(listOf(testActiveWorkout(exercises = emptyList()))),
    ): ExercisePickerViewModel =
        ExercisePickerViewModel(
            observeExercises = ObservePickerExercisesUseCase(exercises),
            addExerciseToActiveWorkout =
                AddExerciseToActiveWorkoutUseCase(
                    workouts,
                    FindLastPerformanceUseCase(workouts),
                    SequentialIdGenerator(),
                    ActiveWorkoutWriteLock(),
                ),
            languageTag = { "en" },
        )

    @Test
    fun givenExercises_whenLoaded_thenTheyAreListedSortedByLocalizedName() =
        runTest(dispatcher) {
            val bench = testExercise(seed = 1, name = "Bench press", primaryMuscle = Muscle.CHEST)
            val squat = testExercise(seed = 2, name = "Air squat", primaryMuscle = Muscle.QUADRICEPS)
            val viewModel = viewModel(exercises = FakeExerciseRepository(listOf(bench, squat)))

            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(false, state.isLoading)
            assertEquals(listOf("Air squat", "Bench press"), state.exercises.map { it.name })
            assertEquals(Muscle.QUADRICEPS, state.exercises.first().primaryMuscle)
        }

    @Test
    fun givenAnExerciseClicked_whenHandled_thenItIsAddedToTheActiveWorkoutAndCloseIsSent() =
        runTest(dispatcher) {
            val bench = testExercise(seed = 1, name = "Bench press")
            val workouts = FakeWorkoutRepository(listOf(testActiveWorkout(exercises = emptyList())))
            val viewModel = viewModel(exercises = FakeExerciseRepository(listOf(bench)), workouts = workouts)

            viewModel.effects.test {
                viewModel.onIntent(ExercisePickerIntent.ExerciseClicked(bench.id))
                dispatcher.scheduler.advanceUntilIdle()

                assertEquals(ExercisePickerEffect.Close, awaitItem())
            }
            assertEquals(
                bench.id,
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.single()
                    ?.exerciseId,
            )
        }

    @Test
    fun givenADoubleTapOnAnExercise_whenBothClicksAreHandled_thenItIsAddedOnlyOnce() =
        runTest(dispatcher) {
            val bench = testExercise(seed = 1, name = "Bench press")
            val workouts = FakeWorkoutRepository(listOf(testActiveWorkout(exercises = emptyList())))
            val viewModel = viewModel(exercises = FakeExerciseRepository(listOf(bench)), workouts = workouts)

            viewModel.onIntent(ExercisePickerIntent.ExerciseClicked(bench.id))
            viewModel.onIntent(ExercisePickerIntent.ExerciseClicked(bench.id))
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(
                1,
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.size,
            )
        }

    @Test
    fun givenBackClicked_whenHandled_thenCloseIsSent() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            viewModel.effects.test {
                viewModel.onIntent(ExercisePickerIntent.BackClicked)

                assertEquals(ExercisePickerEffect.Close, awaitItem())
            }
        }
}
