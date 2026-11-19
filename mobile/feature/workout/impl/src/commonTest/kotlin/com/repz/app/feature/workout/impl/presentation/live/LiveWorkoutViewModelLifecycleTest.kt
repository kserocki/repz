package com.repz.app.feature.workout.impl.presentation.live

import app.cash.turbine.test
import com.repz.app.core.domain.validation.ValidationLimits
import com.repz.app.core.testing.workout.FakeWorkoutRepository
import com.repz.app.feature.workout.impl.testActiveWorkout
import com.repz.app.feature.workout.impl.testWorkoutExercise
import com.repz.app.feature.workout.impl.testWorkoutSet
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/** Loading, Minimize, workout name editing, Finish and Discard. */
@OptIn(ExperimentalCoroutinesApi::class)
class LiveWorkoutViewModelLifecycleTest {
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
    fun givenNoActiveWorkoutAtAll_whenCreated_thenCloseIsSentOnce() =
        runTest(dispatcher) {
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = FakeWorkoutRepository())

            viewModel.effects.test {
                assertEquals(LiveWorkoutEffect.Close, awaitItem())
            }
        }

    @Test
    fun givenAnActiveWorkout_whenLoaded_thenStateReflectsItsNameAndExercises() =
        runTest(dispatcher) {
            val exercise = testWorkoutExercise(seed = 1)
            val workout = testActiveWorkout(name = "Push day", exercises = listOf(exercise))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = FakeWorkoutRepository(listOf(workout)))

            val state = viewModel.state.value
            assertEquals(false, state.isLoading)
            assertEquals("Push day", state.workoutName)
            assertEquals(1, state.exercises.size)
        }

    @Test
    fun givenAnActiveWorkout_whenMinimizeClicked_thenCloseIsSent() =
        runTest(dispatcher) {
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = FakeWorkoutRepository(listOf(testActiveWorkout())))

            viewModel.effects.test {
                viewModel.onIntent(LiveWorkoutIntent.MinimizeClicked)

                assertEquals(LiveWorkoutEffect.Close, awaitItem())
            }
        }

    @Test
    fun givenANameEdit_whenFinishedWithANonBlankValue_thenTheNameIsSaved() =
        runTest(dispatcher) {
            val workout = testActiveWorkout(name = "Old name")
            val workouts = FakeWorkoutRepository(listOf(workout))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = workouts)

            viewModel.onIntent(LiveWorkoutIntent.NameClicked)
            viewModel.onIntent(LiveWorkoutIntent.NameChanged("New name"))
            dispatcher.scheduler.runCurrent()
            assertEquals("New name", viewModel.state.value.workoutName)
            assertTrue(viewModel.state.value.isEditingName)

            viewModel.onIntent(LiveWorkoutIntent.NameEditingFinished)
            dispatcher.scheduler.runCurrent()

            assertEquals(false, viewModel.state.value.isEditingName)
            assertEquals("New name", workouts.observeActiveWorkout().first()?.name)
        }

    @Test
    fun givenANameEdit_whenFinishedBlank_thenThePreviousNameIsRestored() =
        runTest(dispatcher) {
            val workout = testActiveWorkout(name = "Old name")
            val workouts = FakeWorkoutRepository(listOf(workout))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = workouts)

            viewModel.onIntent(LiveWorkoutIntent.NameClicked)
            viewModel.onIntent(LiveWorkoutIntent.NameChanged("   "))
            viewModel.onIntent(LiveWorkoutIntent.NameEditingFinished)
            dispatcher.scheduler.runCurrent()

            assertEquals("Old name", viewModel.state.value.workoutName)
            assertEquals("Old name", workouts.observeActiveWorkout().first()?.name)
        }

    @Test
    fun givenANameLongerThanTheLimit_whenChanged_thenItIsTruncatedToTheLimit() =
        runTest(dispatcher) {
            val workout = testActiveWorkout(name = "Old name")
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = FakeWorkoutRepository(listOf(workout)))
            val tooLong = "x".repeat(ValidationLimits.NAME_MAX_LENGTH + 50)

            viewModel.onIntent(LiveWorkoutIntent.NameClicked)
            viewModel.onIntent(LiveWorkoutIntent.NameChanged(tooLong))
            dispatcher.scheduler.runCurrent()

            assertEquals(ValidationLimits.NAME_MAX_LENGTH, viewModel.state.value.workoutName.length)
        }

    @Test
    fun givenANameEdit_whenTheDebounceElapses_thenItIsPersistedWithoutEndingEditing() =
        runTest(dispatcher) {
            val workout = testActiveWorkout(name = "Old name")
            val workouts = FakeWorkoutRepository(listOf(workout))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = workouts)

            viewModel.onIntent(LiveWorkoutIntent.NameClicked)
            viewModel.onIntent(LiveWorkoutIntent.NameChanged("New name"))
            dispatcher.scheduler.advanceTimeBy(DraftPersistDebounce)
            dispatcher.scheduler.runCurrent()

            assertEquals("New name", workouts.observeActiveWorkout().first()?.name)
            assertTrue(viewModel.state.value.isEditingName)
        }

    @Test
    fun givenANameEditNotYetDebounced_whenMinimizeClicked_thenItIsFlushedBeforeClosing() =
        runTest(dispatcher) {
            val workout = testActiveWorkout(name = "Old name")
            val workouts = FakeWorkoutRepository(listOf(workout))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = workouts)

            viewModel.onIntent(LiveWorkoutIntent.NameClicked)
            viewModel.onIntent(LiveWorkoutIntent.NameChanged("New name"))

            viewModel.effects.test {
                viewModel.onIntent(LiveWorkoutIntent.MinimizeClicked)

                assertEquals(LiveWorkoutEffect.Close, awaitItem())
            }
            assertEquals("New name", workouts.observeActiveWorkout().first()?.name)
        }

    @Test
    fun givenACompletedSet_whenFinishClicked_thenItSucceedsAndCloses() =
        runTest(dispatcher) {
            val completedSet = testWorkoutSet(seed = 1, completedAt = Instant.fromEpochSeconds(10))
            val workout = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(completedSet))))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = FakeWorkoutRepository(listOf(workout)))

            viewModel.effects.test {
                viewModel.onIntent(LiveWorkoutIntent.FinishClicked)

                assertEquals(LiveWorkoutEffect.Close, awaitItem())
            }
        }

    @Test
    fun givenANameEditNotYetDebounced_whenFinishClicked_thenItIsFlushedBeforeFinishing() =
        runTest(dispatcher) {
            val completedSet = testWorkoutSet(seed = 1, completedAt = Instant.fromEpochSeconds(10))
            val workout =
                testActiveWorkout(name = "Old name", exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(completedSet))))
            val workouts = FakeWorkoutRepository(listOf(workout))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = workouts)

            viewModel.onIntent(LiveWorkoutIntent.NameClicked)
            viewModel.onIntent(LiveWorkoutIntent.NameChanged("New name"))

            viewModel.effects.test {
                viewModel.onIntent(LiveWorkoutIntent.FinishClicked)

                assertEquals(LiveWorkoutEffect.Close, awaitItem())
            }
            assertEquals("New name", workouts.findWorkoutIncludingDeleted(workout.id)?.name)
        }

    @Test
    fun givenNoCompletedSet_whenFinishClicked_thenItFailsAndShowsFinishFailed() =
        runTest(dispatcher) {
            val uncompletedSet = testWorkoutSet(seed = 1, completedAt = null)
            val workout = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(uncompletedSet))))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = FakeWorkoutRepository(listOf(workout)))

            viewModel.effects.test {
                viewModel.onIntent(LiveWorkoutIntent.FinishClicked)

                assertEquals(LiveWorkoutEffect.ShowFinishFailed, awaitItem())
            }
            dispatcher.scheduler.runCurrent()
            assertEquals(false, viewModel.state.value.isFinishing)
        }

    @Test
    fun givenNoCompletedSets_whenDiscardClicked_thenItDiscardsSilentlyAndCloses() =
        runTest(dispatcher) {
            val workout = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(testWorkoutSet(seed = 1)))))
            val workouts = FakeWorkoutRepository(listOf(workout))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = workouts)

            viewModel.effects.test {
                viewModel.onIntent(LiveWorkoutIntent.DiscardClicked)

                assertEquals(LiveWorkoutEffect.Close, awaitItem())
            }
            assertNull(workouts.findWorkoutIncludingDeleted(workout.id))
        }

    @Test
    fun givenCompletedSets_whenDiscardClicked_thenAConfirmationIsShownInstead() =
        runTest(dispatcher) {
            val completedSet = testWorkoutSet(seed = 1, completedAt = Instant.fromEpochSeconds(1))
            val workout = testActiveWorkout(name = "Push A", exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(completedSet))))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = FakeWorkoutRepository(listOf(workout)))

            viewModel.onIntent(LiveWorkoutIntent.DiscardClicked)
            dispatcher.scheduler.runCurrent()

            val confirmation = viewModel.state.value.discardConfirmation
            assertEquals("Push A", confirmation?.workoutName)
            assertEquals(1, confirmation?.completedSetCount)
        }

    @Test
    fun givenADiscardConfirmation_whenDismissed_thenItIsCleared() =
        runTest(dispatcher) {
            val completedSet = testWorkoutSet(seed = 1, completedAt = Instant.fromEpochSeconds(1))
            val workout = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(completedSet))))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = FakeWorkoutRepository(listOf(workout)))
            viewModel.onIntent(LiveWorkoutIntent.DiscardClicked)

            viewModel.onIntent(LiveWorkoutIntent.DiscardDismissed)
            dispatcher.scheduler.runCurrent()

            assertNull(viewModel.state.value.discardConfirmation)
        }

    @Test
    fun givenADiscardConfirmation_whenConfirmed_thenItDiscardsAndCloses() =
        runTest(dispatcher) {
            val completedSet = testWorkoutSet(seed = 1, completedAt = Instant.fromEpochSeconds(1))
            val workout = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(completedSet))))
            val workouts = FakeWorkoutRepository(listOf(workout))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = workouts)
            viewModel.onIntent(LiveWorkoutIntent.DiscardClicked)

            viewModel.effects.test {
                viewModel.onIntent(LiveWorkoutIntent.DiscardConfirmed)

                assertEquals(LiveWorkoutEffect.Close, awaitItem())
            }
            assertNull(workouts.findWorkoutIncludingDeleted(workout.id))
        }
}
