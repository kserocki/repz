package com.repz.app.feature.workout.impl.presentation.live

import app.cash.turbine.test
import com.repz.app.core.testing.workout.FakeWorkoutRepository
import com.repz.app.feature.workout.impl.testActiveWorkout
import com.repz.app.feature.workout.impl.testWorkoutExercise
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
import kotlin.test.assertTrue

/** Exercise notes, Remove exercise with undo and reorder mode. */
@OptIn(ExperimentalCoroutinesApi::class)
class LiveWorkoutViewModelExerciseTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun givenAnExercise_whenNotesEditedAndFinished_thenTheyAreSaved() =
        runTest(dispatcher) {
            val exercise = testWorkoutExercise(seed = 1, notes = "")
            val workout = testActiveWorkout(exercises = listOf(exercise))
            val workouts = FakeWorkoutRepository(listOf(workout))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = workouts)

            viewModel.onIntent(LiveWorkoutIntent.NotesClicked(exercise.id))
            viewModel.onIntent(LiveWorkoutIntent.NotesChanged(exercise.id, "Pause on chest"))
            dispatcher.scheduler.runCurrent()
            assertEquals(
                "Pause on chest",
                viewModel.state.value.exercises
                    .single()
                    .notes,
            )
            assertTrue(
                viewModel.state.value.exercises
                    .single()
                    .isEditingNotes,
            )

            viewModel.onIntent(LiveWorkoutIntent.NotesEditingFinished(exercise.id))
            dispatcher.scheduler.runCurrent()

            assertEquals(
                "Pause on chest",
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.single()
                    ?.notes,
            )
            assertEquals(
                false,
                viewModel.state.value.exercises
                    .single()
                    .isEditingNotes,
            )
        }

    @Test
    fun givenAnExercise_whenRemoved_thenItIsGoneAndUndoRestoresItAtTheSameIndex() =
        runTest(dispatcher) {
            val keptExercise = testWorkoutExercise(seed = 1)
            val removedExercise = testWorkoutExercise(seed = 2)
            val workout = testActiveWorkout(exercises = listOf(keptExercise, removedExercise))
            val workouts = FakeWorkoutRepository(listOf(workout))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = workouts)

            viewModel.effects.test {
                viewModel.onIntent(LiveWorkoutIntent.ExerciseRemoved(removedExercise.id))

                assertEquals(LiveWorkoutEffect.PlayRejectHaptic, awaitItem())
                assertEquals(LiveWorkoutEffect.ShowExerciseRemoved, awaitItem())
            }
            dispatcher.scheduler.runCurrent()
            assertEquals(
                listOf(keptExercise.id),
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.map { it.id },
            )

            viewModel.onIntent(LiveWorkoutIntent.ExerciseRemovalUndone)
            dispatcher.scheduler.runCurrent()

            assertEquals(
                listOf(keptExercise.id, removedExercise.id),
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.map { it.id },
            )
        }

    @Test
    fun givenReorderStarted_whenExerciseMovedAndFinished_thenTheOrderPersistsAndReorderModeEnds() =
        runTest(dispatcher) {
            val first = testWorkoutExercise(seed = 1)
            val second = testWorkoutExercise(seed = 2)
            val workout = testActiveWorkout(exercises = listOf(first, second))
            val workouts = FakeWorkoutRepository(listOf(workout))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = workouts)

            viewModel.onIntent(LiveWorkoutIntent.ReorderStarted)
            dispatcher.scheduler.runCurrent()
            assertTrue(viewModel.state.value.isReordering)

            viewModel.onIntent(LiveWorkoutIntent.ExerciseMoved(0, 1))
            dispatcher.scheduler.runCurrent()
            viewModel.onIntent(LiveWorkoutIntent.ReorderFinished)
            dispatcher.scheduler.runCurrent()

            assertEquals(false, viewModel.state.value.isReordering)
            assertEquals(
                listOf(second.id, first.id),
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.map { it.id },
            )
        }
}
