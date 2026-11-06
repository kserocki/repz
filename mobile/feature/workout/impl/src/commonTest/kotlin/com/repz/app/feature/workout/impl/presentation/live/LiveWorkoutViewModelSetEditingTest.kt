package com.repz.app.feature.workout.impl.presentation.live

import app.cash.turbine.test
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.Rpe
import com.repz.app.core.domain.model.SetType
import com.repz.app.core.domain.model.Weight
import com.repz.app.core.testing.time.MutableClock
import com.repz.app.core.testing.workout.FakeWorkoutRepository
import com.repz.app.feature.workout.impl.testActiveWorkout
import com.repz.app.feature.workout.impl.testFinishedWorkout
import com.repz.app.feature.workout.impl.testUuid
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
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** Add exercise/set, set completion (Q5), set type, delete/undo, keypad targeting/entry, Next field (T12) and the elapsed ticker. */
@OptIn(ExperimentalCoroutinesApi::class)
class LiveWorkoutViewModelSetEditingTest {
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
    fun givenAddExerciseClicked_whenHandled_thenOpenExercisePickerIsSent() =
        runTest(dispatcher) {
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = FakeWorkoutRepository(listOf(testActiveWorkout())))

            viewModel.effects.test {
                viewModel.onIntent(LiveWorkoutIntent.AddExerciseClicked)

                assertEquals(LiveWorkoutEffect.OpenExercisePicker, awaitItem())
            }
        }

    @Test
    fun givenAnExercise_whenAddSetClicked_thenANewSetIsAppended() =
        runTest(dispatcher) {
            val exercise = testWorkoutExercise(seed = 1, sets = listOf(testWorkoutSet(seed = 1, weight = Weight.fromKilograms(60.0))))
            val workout = testActiveWorkout(exercises = listOf(exercise))
            val workouts = FakeWorkoutRepository(listOf(workout))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = workouts)

            viewModel.onIntent(LiveWorkoutIntent.AddSetClicked(exercise.id))
            dispatcher.scheduler.runCurrent()

            val savedSets =
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.single()
                    ?.sets
            assertEquals(2, savedSets?.size)
            assertEquals(Weight.fromKilograms(60.0), savedSets?.get(1)?.weight)
        }

    @Test
    fun givenAnUncompletedSetWithReps_whenCompletionToggled_thenItCompletesAndPlaysConfirmHaptic() =
        runTest(dispatcher) {
            val set = testWorkoutSet(seed = 1, reps = 8, completedAt = null)
            val workout = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(set))))
            val workouts = FakeWorkoutRepository(listOf(workout))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = workouts)

            viewModel.effects.test {
                viewModel.onIntent(LiveWorkoutIntent.SetCompletionToggled(set.id))

                assertEquals(LiveWorkoutEffect.PlayConfirmHaptic, awaitItem())
            }
            dispatcher.scheduler.runCurrent()
            assertTrue(
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.single()
                    ?.sets
                    ?.single()
                    ?.isCompleted == true,
            )
        }

    @Test
    fun givenASetWithZeroReps_whenCompletionToggled_thenNothingIsSavedAndRepsBecomesTheEditingField() =
        runTest(dispatcher) {
            val set = testWorkoutSet(seed = 1, reps = 0, completedAt = null)
            val workout = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(set))))
            val workouts = FakeWorkoutRepository(listOf(workout))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = workouts)

            viewModel.effects.test {
                viewModel.onIntent(LiveWorkoutIntent.SetCompletionToggled(set.id))

                assertEquals(LiveWorkoutEffect.PlayRejectHaptic, awaitItem())
            }
            dispatcher.scheduler.runCurrent()
            assertEquals(SetFieldRef(set.id, SetField.Reps), viewModel.state.value.editingField)
            assertEquals(
                false,
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.single()
                    ?.sets
                    ?.single()
                    ?.isCompleted,
            )
        }

    @Test
    fun givenACompletedSet_whenCompletionToggledAgain_thenItBecomesUncompleted() =
        runTest(dispatcher) {
            val set = testWorkoutSet(seed = 1, reps = 8, completedAt = Instant.fromEpochSeconds(1))
            val workout = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(set))))
            val workouts = FakeWorkoutRepository(listOf(workout))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = workouts)

            viewModel.onIntent(LiveWorkoutIntent.SetCompletionToggled(set.id))
            dispatcher.scheduler.runCurrent()

            assertEquals(
                false,
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.single()
                    ?.sets
                    ?.single()
                    ?.isCompleted,
            )
        }

    @Test
    fun givenASet_whenTypeSelected_thenItsTypeChanges() =
        runTest(dispatcher) {
            val set = testWorkoutSet(seed = 1)
            val workout = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(set))))
            val workouts = FakeWorkoutRepository(listOf(workout))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = workouts)

            viewModel.onIntent(LiveWorkoutIntent.SetTypeSelected(set.id, SetType.WARMUP))
            dispatcher.scheduler.runCurrent()

            assertEquals(
                SetType.WARMUP,
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.single()
                    ?.sets
                    ?.single()
                    ?.type,
            )
        }

    @Test
    fun givenASet_whenDeleted_thenItIsRemovedAndUndoRestoresItAtTheSamePosition() =
        runTest(dispatcher) {
            val keptSet = testWorkoutSet(seed = 1)
            val deletedSet = testWorkoutSet(seed = 2)
            val exercise = testWorkoutExercise(seed = 1, sets = listOf(keptSet, deletedSet))
            val workout = testActiveWorkout(exercises = listOf(exercise))
            val workouts = FakeWorkoutRepository(listOf(workout))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = workouts)

            viewModel.effects.test {
                viewModel.onIntent(LiveWorkoutIntent.SetDeleted(deletedSet.id))

                assertEquals(LiveWorkoutEffect.PlayRejectHaptic, awaitItem())
                assertEquals(LiveWorkoutEffect.ShowSetDeleted, awaitItem())
            }
            dispatcher.scheduler.runCurrent()
            assertEquals(
                listOf(keptSet.id),
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.single()
                    ?.sets
                    ?.map { it.id },
            )

            viewModel.onIntent(LiveWorkoutIntent.SetDeletionUndone)
            dispatcher.scheduler.runCurrent()

            assertEquals(
                listOf(keptSet.id, deletedSet.id),
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.single()
                    ?.sets
                    ?.map { it.id },
            )
        }

    @Test
    fun givenASetFieldClicked_whenHandled_thenEditingFieldIsSetAndKeypadTargetIsDerived() =
        runTest(dispatcher) {
            val set = testWorkoutSet(seed = 1, weight = Weight.fromKilograms(82.5), reps = 8)
            val workout = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(set))))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = FakeWorkoutRepository(listOf(workout)))

            viewModel.onIntent(LiveWorkoutIntent.SetFieldClicked(set.id, SetField.Weight))
            dispatcher.scheduler.runCurrent()

            val state = viewModel.state.value
            assertEquals(SetFieldRef(set.id, SetField.Weight), state.editingField)
            assertEquals(set.id, state.keypadTarget?.setId)
            assertEquals(SetField.Weight, state.keypadTarget?.field)
            assertTrue(state.keypadTarget?.isLastSetOfWorkout == true)
        }

    @Test
    fun givenAValidWeight_whenEntered_thenItIsSavedAndTheSetIsTouched() =
        runTest(dispatcher) {
            val set = testWorkoutSet(seed = 1, weight = Weight.ZERO)
            val workout = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(set))))
            val workouts = FakeWorkoutRepository(listOf(workout))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = workouts)

            viewModel.onIntent(LiveWorkoutIntent.SetWeightEntered(set.id, Weight.fromKilograms(82.5)))
            dispatcher.scheduler.runCurrent()

            assertEquals(
                Weight.fromKilograms(82.5),
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.single()
                    ?.sets
                    ?.single()
                    ?.weight,
            )
            assertEquals(
                Weight.fromKilograms(82.5),
                viewModel.state.value.exercises
                    .single()
                    .sets
                    .single()
                    .weight,
            )
            assertEquals(
                false,
                viewModel.state.value.exercises
                    .single()
                    .sets
                    .single()
                    .isPrefilled,
            )
        }

    @Test
    fun givenAnOutOfRangeWeight_whenEntered_thenItIsIgnored() =
        runTest(dispatcher) {
            val set = testWorkoutSet(seed = 1, weight = Weight.fromKilograms(50.0))
            val workout = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(set))))
            val workouts = FakeWorkoutRepository(listOf(workout))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = workouts)

            viewModel.onIntent(LiveWorkoutIntent.SetWeightEntered(set.id, Weight(Long.MAX_VALUE)))
            dispatcher.scheduler.runCurrent()

            assertEquals(
                Weight.fromKilograms(50.0),
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.single()
                    ?.sets
                    ?.single()
                    ?.weight,
            )
        }

    @Test
    fun givenAClearedWeight_whenNullEntered_thenItStoresZero() =
        runTest(dispatcher) {
            val set = testWorkoutSet(seed = 1, weight = Weight.fromKilograms(50.0))
            val workout = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(set))))
            val workouts = FakeWorkoutRepository(listOf(workout))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = workouts)

            viewModel.onIntent(LiveWorkoutIntent.SetWeightEntered(set.id, null))
            dispatcher.scheduler.runCurrent()

            assertEquals(
                Weight.ZERO,
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.single()
                    ?.sets
                    ?.single()
                    ?.weight,
            )
            // Touched and 0: shown as an actual zero, not blank (only untouched zero is blank, Q4).
            assertEquals(
                Weight.ZERO,
                viewModel.state.value.exercises
                    .single()
                    .sets
                    .single()
                    .weight,
            )
        }

    @Test
    fun givenAValidReps_whenEntered_thenItIsSavedAndTheSetIsTouched() =
        runTest(dispatcher) {
            val set = testWorkoutSet(seed = 1, reps = 0)
            val workout = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(set))))
            val workouts = FakeWorkoutRepository(listOf(workout))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = workouts)

            viewModel.onIntent(LiveWorkoutIntent.SetRepsEntered(set.id, 8))
            dispatcher.scheduler.runCurrent()

            assertEquals(
                8,
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.single()
                    ?.sets
                    ?.single()
                    ?.reps,
            )
        }

    @Test
    fun givenAnRpe_whenEntered_thenItIsSaved() =
        runTest(dispatcher) {
            val set = testWorkoutSet(seed = 1)
            val workout = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(set))))
            val workouts = FakeWorkoutRepository(listOf(workout))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = workouts)
            val rpe = Rpe.ALL.first()

            viewModel.onIntent(LiveWorkoutIntent.SetRpeEntered(set.id, rpe))
            dispatcher.scheduler.runCurrent()

            assertEquals(
                rpe,
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.single()
                    ?.sets
                    ?.single()
                    ?.rpe,
            )
        }

    @Test
    fun givenAnEditingField_whenKeypadFieldSelected_thenItRetargetsTheFieldOnTheSameSet() =
        runTest(dispatcher) {
            val set = testWorkoutSet(seed = 1)
            val workout = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(set))))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = FakeWorkoutRepository(listOf(workout)))
            viewModel.onIntent(LiveWorkoutIntent.SetFieldClicked(set.id, SetField.Weight))

            viewModel.onIntent(LiveWorkoutIntent.KeypadFieldSelected(SetField.Reps))
            dispatcher.scheduler.runCurrent()

            assertEquals(SetFieldRef(set.id, SetField.Reps), viewModel.state.value.editingField)
        }

    @Test
    fun givenAnEditingField_whenKeypadDismissed_thenItIsCleared() =
        runTest(dispatcher) {
            val set = testWorkoutSet(seed = 1)
            val workout = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(set))))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = FakeWorkoutRepository(listOf(workout)))
            viewModel.onIntent(LiveWorkoutIntent.SetFieldClicked(set.id, SetField.Weight))

            viewModel.onIntent(LiveWorkoutIntent.KeypadDismissed)
            dispatcher.scheduler.runCurrent()

            assertNull(viewModel.state.value.editingField)
        }

    @Test
    fun givenWeightIsEditing_whenNextFieldRequested_thenItMovesToRepsOfTheSameSet() =
        runTest(dispatcher) {
            val set = testWorkoutSet(seed = 1)
            val workout = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(set))))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = FakeWorkoutRepository(listOf(workout)))
            viewModel.onIntent(LiveWorkoutIntent.SetFieldClicked(set.id, SetField.Weight))

            viewModel.onIntent(LiveWorkoutIntent.NextFieldRequested)
            dispatcher.scheduler.runCurrent()

            assertEquals(SetFieldRef(set.id, SetField.Reps), viewModel.state.value.editingField)
        }

    @Test
    fun givenRepsOfANonLastSet_whenNextFieldRequested_thenItMovesToWeightOfTheNextSetInTheSameCard() =
        runTest(dispatcher) {
            val firstSet = testWorkoutSet(seed = 1)
            val secondSet = testWorkoutSet(seed = 2)
            val workout = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(firstSet, secondSet))))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = FakeWorkoutRepository(listOf(workout)))
            viewModel.onIntent(LiveWorkoutIntent.SetFieldClicked(firstSet.id, SetField.Reps))

            viewModel.onIntent(LiveWorkoutIntent.NextFieldRequested)
            dispatcher.scheduler.runCurrent()

            assertEquals(SetFieldRef(secondSet.id, SetField.Weight), viewModel.state.value.editingField)
        }

    @Test
    fun givenRepsOfTheLastSetOfACard_whenNextFieldRequested_thenItMovesToWeightOfTheNextCardsFirstSet() =
        runTest(dispatcher) {
            val firstCardSet = testWorkoutSet(seed = 1)
            val secondCardSet = testWorkoutSet(seed = 2)
            val workout =
                testActiveWorkout(
                    exercises =
                        listOf(
                            testWorkoutExercise(seed = 1, sets = listOf(firstCardSet)),
                            testWorkoutExercise(seed = 2, sets = listOf(secondCardSet)),
                        ),
                )
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = FakeWorkoutRepository(listOf(workout)))
            viewModel.onIntent(LiveWorkoutIntent.SetFieldClicked(firstCardSet.id, SetField.Reps))

            viewModel.onIntent(LiveWorkoutIntent.NextFieldRequested)
            dispatcher.scheduler.runCurrent()

            assertEquals(SetFieldRef(secondCardSet.id, SetField.Weight), viewModel.state.value.editingField)
        }

    @Test
    fun givenRepsOfTheWorkoutsLastSet_whenNextFieldRequested_thenTheKeypadHides() =
        runTest(dispatcher) {
            val onlySet = testWorkoutSet(seed = 1)
            val workout = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(onlySet))))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = FakeWorkoutRepository(listOf(workout)))
            viewModel.onIntent(LiveWorkoutIntent.SetFieldClicked(onlySet.id, SetField.Reps))

            viewModel.onIntent(LiveWorkoutIntent.NextFieldRequested)
            dispatcher.scheduler.runCurrent()

            assertNull(viewModel.state.value.editingField)
            assertNull(viewModel.state.value.keypadTarget)
        }

    @Test
    fun givenTheFirstSetOfAnExercise_whenItsKeypadTargetIsDerived_thenSameAsLastIsThePreviousColumnSet() =
        runTest(dispatcher) {
            val exerciseId = ExerciseId(testUuid(1))
            val lastPerformanceSet = testWorkoutSet(seed = 10, weight = Weight.fromKilograms(70.0), reps = 7)
            val pastWorkout =
                testFinishedWorkout(
                    seed = 2,
                    exercises = listOf(testWorkoutExercise(seed = 2, exerciseId = exerciseId, sets = listOf(lastPerformanceSet))),
                )
            val currentSet = testWorkoutSet(seed = 1)
            val workout =
                testActiveWorkout(
                    seed = 3,
                    exercises = listOf(testWorkoutExercise(seed = 1, exerciseId = exerciseId, sets = listOf(currentSet))),
                )
            val workouts = FakeWorkoutRepository(listOf(pastWorkout, workout))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = workouts)

            viewModel.onIntent(LiveWorkoutIntent.SetFieldClicked(currentSet.id, SetField.Weight))
            dispatcher.scheduler.runCurrent()

            val last =
                viewModel.state.value.keypadTarget
                    ?.last
            assertEquals(Weight.fromKilograms(70.0), last?.weight)
            assertEquals(7, last?.reps)
        }

    @Test
    fun givenANonFirstSet_whenItsKeypadTargetIsDerived_thenSameAsLastIsThePreviousSetInThisWorkout() =
        runTest(dispatcher) {
            val firstSet = testWorkoutSet(seed = 1, weight = Weight.fromKilograms(60.0), reps = 10)
            val secondSet = testWorkoutSet(seed = 2, weight = Weight.ZERO, reps = 0)
            val workout = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(firstSet, secondSet))))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = FakeWorkoutRepository(listOf(workout)))

            viewModel.onIntent(LiveWorkoutIntent.SetFieldClicked(secondSet.id, SetField.Weight))
            dispatcher.scheduler.runCurrent()

            val last =
                viewModel.state.value.keypadTarget
                    ?.last
            assertEquals(Weight.fromKilograms(60.0), last?.weight)
            assertEquals(10, last?.reps)
        }

    @Test
    fun givenAnActiveWorkout_whenTimePasses_thenElapsedAdvances() =
        runTest(dispatcher) {
            val clock = MutableClock(Instant.fromEpochSeconds(0))
            val workout = testActiveWorkout(startedAt = Instant.fromEpochSeconds(0))
            val viewModel = liveWorkoutViewModel(dispatcher, workouts = FakeWorkoutRepository(listOf(workout)), clock = clock)
            assertEquals(0.seconds, viewModel.state.value.elapsed)

            clock.advanceBy(5.seconds)
            dispatcher.scheduler.advanceTimeBy(5_000)
            dispatcher.scheduler.runCurrent()

            assertEquals(5.seconds, viewModel.state.value.elapsed)
        }
}
