package com.repz.app.feature.workout.impl.presentation.picker

import app.cash.turbine.test
import com.repz.app.core.domain.exercise.RecentExerciseUse
import com.repz.app.core.domain.model.Muscle
import com.repz.app.core.domain.model.WorkoutExerciseId
import com.repz.app.core.domain.validation.ValidationLimits
import com.repz.app.core.domain.workout.FindLastPerformanceUseCase
import com.repz.app.core.testing.exercise.FakeExerciseRepository
import com.repz.app.core.testing.exercise.FakeExerciseUsageRepository
import com.repz.app.core.testing.id.SequentialIdGenerator
import com.repz.app.core.testing.time.MutableClock
import com.repz.app.core.testing.workout.FakeWorkoutRepository
import com.repz.app.feature.workout.impl.domain.ActiveWorkoutWriteLock
import com.repz.app.feature.workout.impl.domain.AddExercisesToActiveWorkoutUseCase
import com.repz.app.feature.workout.impl.domain.ObserveExercisePickerUseCase
import com.repz.app.feature.workout.impl.domain.ReplaceExerciseInActiveWorkoutUseCase
import com.repz.app.feature.workout.impl.testActiveWorkout
import com.repz.app.feature.workout.impl.testExercise
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
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ExercisePickerViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = MutableClock(Instant.parse("2024-01-10T10:00:00Z"))

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        replacingWorkoutExerciseId: WorkoutExerciseId? = null,
        exercises: FakeExerciseRepository = FakeExerciseRepository(),
        exerciseUsage: FakeExerciseUsageRepository = FakeExerciseUsageRepository(),
        workouts: FakeWorkoutRepository = FakeWorkoutRepository(listOf(testActiveWorkout(exercises = emptyList()))),
    ): ExercisePickerViewModel {
        val lock = ActiveWorkoutWriteLock()
        val idGenerator = SequentialIdGenerator()
        val findLastPerformance = FindLastPerformanceUseCase(workouts)
        return ExercisePickerViewModel(
            observeExercisePicker = ObserveExercisePickerUseCase(exercises, exerciseUsage, workouts),
            addExercisesToActiveWorkout = AddExercisesToActiveWorkoutUseCase(workouts, findLastPerformance, idGenerator, lock),
            replaceExerciseInActiveWorkout = ReplaceExerciseInActiveWorkoutUseCase(workouts, findLastPerformance, idGenerator, lock),
            clock = clock,
            replacingWorkoutExerciseId = replacingWorkoutExerciseId,
            languageTag = { "en" },
            timeZone = { TimeZone.UTC },
        )
    }

    @Test
    fun givenNoQueryOrFilter_whenLoaded_thenRecentAndAllExercisesShowWithAllExercisesHeader() =
        runTest(dispatcher) {
            val bench = testExercise(seed = 1, name = "Bench press", primaryMuscle = Muscle.CHEST)
            val squat = testExercise(seed = 2, name = "Air squat", primaryMuscle = Muscle.QUADRICEPS)
            val recent = FakeExerciseUsageRepository(listOf(RecentExerciseUse(bench.id, clock.instant)))
            val viewModel = viewModel(exercises = FakeExerciseRepository(listOf(bench, squat)), exerciseUsage = recent)
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(false, state.isLoading)
            assertEquals(listOf("Bench press"), state.recent.map { it.name })
            assertEquals(listOf("Air squat", "Bench press"), state.results.map { it.name })
            assertEquals(PickerResultsHeader.AllExercises, state.resultsHeader)
        }

    @Test
    fun givenANonBlankQuery_whenLoaded_thenRecentIsHiddenAndTheHeaderShowsTheResultCount() =
        runTest(dispatcher) {
            val bench = testExercise(seed = 1, name = "Bench press")
            val recentRepository = FakeExerciseUsageRepository(listOf(RecentExerciseUse(bench.id, clock.instant)))
            val viewModel = viewModel(exercises = FakeExerciseRepository(listOf(bench)), exerciseUsage = recentRepository)
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.onIntent(ExercisePickerIntent.QueryChanged("bench"))
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(emptyList(), state.recent)
            assertEquals(PickerResultsHeader.Results(1), state.resultsHeader)
        }

    @Test
    fun givenRecentUsesAtDifferentTimes_whenLoaded_thenDaysAgoIsTodayYesterdayAndClampedForAFutureDate() =
        runTest(dispatcher) {
            val today = testExercise(seed = 1, name = "Today")
            val yesterday = testExercise(seed = 2, name = "Yesterday")
            val future = testExercise(seed = 3, name = "Future")
            val recentRepository =
                FakeExerciseUsageRepository(
                    listOf(
                        RecentExerciseUse(today.id, clock.instant),
                        RecentExerciseUse(yesterday.id, clock.instant - 1.days),
                        RecentExerciseUse(future.id, clock.instant + 1.days),
                    ),
                )
            val viewModel =
                viewModel(exercises = FakeExerciseRepository(listOf(today, yesterday, future)), exerciseUsage = recentRepository)
            dispatcher.scheduler.advanceUntilIdle()

            val daysAgoByName =
                viewModel.state.value.recent
                    .associate { it.name to it.lastUsedDaysAgo }
            assertEquals(0, daysAgoByName["Today"])
            assertEquals(1, daysAgoByName["Yesterday"])
            assertEquals(0, daysAgoByName["Future"])
        }

    @Test
    fun givenExercisesClickedInOrder_whenOneIsDeselectedAndReselected_thenAddUsesTheReorderedPickOrder() =
        runTest(dispatcher) {
            val a = testExercise(seed = 1, name = "A")
            val b = testExercise(seed = 2, name = "B")
            val c = testExercise(seed = 3, name = "C")
            val workouts = FakeWorkoutRepository(listOf(testActiveWorkout(exercises = emptyList())))
            val viewModel = viewModel(exercises = FakeExerciseRepository(listOf(a, b, c)), workouts = workouts)
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.onIntent(ExercisePickerIntent.ExerciseClicked(c.id))
            viewModel.onIntent(ExercisePickerIntent.ExerciseClicked(a.id))
            viewModel.onIntent(ExercisePickerIntent.ExerciseClicked(b.id))
            viewModel.onIntent(ExercisePickerIntent.ExerciseClicked(a.id))
            viewModel.onIntent(ExercisePickerIntent.ExerciseClicked(a.id))
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(3, viewModel.state.value.selectedCount)

            viewModel.onIntent(ExercisePickerIntent.AddClicked)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(
                listOf(c.id, b.id, a.id),
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.map { it.exerciseId },
            )
        }

    @Test
    fun givenASelectionAtTheLimit_whenOneMoreIsClicked_thenItIsRejectedWithTheLimitEffect() =
        runTest(dispatcher) {
            val fullExercises = (1..ValidationLimits.EXERCISES_PER_WORKOUT_MAX).map { testWorkoutExercise(seed = it) }
            val activeWorkout = testActiveWorkout(exercises = fullExercises)
            val extra = testExercise(seed = 1_000, name = "Extra")
            val viewModel =
                viewModel(
                    exercises = FakeExerciseRepository(listOf(extra)),
                    workouts = FakeWorkoutRepository(listOf(activeWorkout)),
                )
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onIntent(ExercisePickerIntent.ExerciseClicked(extra.id))
                dispatcher.scheduler.advanceUntilIdle()

                assertEquals(ExercisePickerEffect.PlayRejectHaptic, awaitItem())
                assertEquals(
                    ExercisePickerEffect.ShowSelectionLimit(ValidationLimits.EXERCISES_PER_WORKOUT_MAX),
                    awaitItem(),
                )
            }
        }

    @Test
    fun givenADoubleTapOnAddClicked_whenBothAreHandled_thenOnlyOneWriteHappens() =
        runTest(dispatcher) {
            val bench = testExercise(seed = 1, name = "Bench press")
            val workouts = FakeWorkoutRepository(listOf(testActiveWorkout(exercises = emptyList())))
            val viewModel = viewModel(exercises = FakeExerciseRepository(listOf(bench)), workouts = workouts)
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.onIntent(ExercisePickerIntent.ExerciseClicked(bench.id))
            viewModel.onIntent(ExercisePickerIntent.AddClicked)
            viewModel.onIntent(ExercisePickerIntent.AddClicked)
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
    fun givenASuccessfulAdd_whenHandled_thenConfirmHapticThenCloseAreSentInOrder() =
        runTest(dispatcher) {
            val bench = testExercise(seed = 1, name = "Bench press")
            val viewModel =
                viewModel(
                    exercises = FakeExerciseRepository(listOf(bench)),
                    workouts = FakeWorkoutRepository(listOf(testActiveWorkout(exercises = emptyList()))),
                )
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onIntent(ExercisePickerIntent.ExerciseClicked(bench.id))
                assertEquals(ExercisePickerEffect.PlaySelectionHaptic, awaitItem())
                viewModel.onIntent(ExercisePickerIntent.AddClicked)
                dispatcher.scheduler.advanceUntilIdle()

                assertEquals(ExercisePickerEffect.PlayConfirmHaptic, awaitItem())
                assertEquals(ExercisePickerEffect.Close, awaitItem())
            }
        }

    @Test
    fun givenReplaceModeAndATargetWithNoCompletedSets_whenAnotherExerciseIsClicked_thenItSubmitsAtOnce() =
        runTest(dispatcher) {
            val targetExercise = testWorkoutExercise(seed = 1, sets = listOf(testWorkoutSet(seed = 10)))
            val workout = testActiveWorkout(exercises = listOf(targetExercise))
            val workouts = FakeWorkoutRepository(listOf(workout))
            val replacement = testExercise(seed = 2, name = "Replacement")
            val viewModel =
                viewModel(
                    replacingWorkoutExerciseId = targetExercise.id,
                    exercises = FakeExerciseRepository(listOf(replacement)),
                    workouts = workouts,
                )
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(ExercisePickerMode.Replace, viewModel.state.value.mode)

            viewModel.effects.test {
                viewModel.onIntent(ExercisePickerIntent.ExerciseClicked(replacement.id))
                dispatcher.scheduler.advanceUntilIdle()

                assertEquals(ExercisePickerEffect.PlayConfirmHaptic, awaitItem())
                assertEquals(ExercisePickerEffect.Close, awaitItem())
            }
            assertEquals(
                replacement.id,
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.single()
                    ?.exerciseId,
            )
        }

    @Test
    fun givenReplaceModeAndATargetWithACompletedSet_whenAnotherExerciseIsClicked_thenTheDialogShowsAndNothingIsWrittenYet() =
        runTest(dispatcher) {
            val completedSet = testWorkoutSet(seed = 10, completedAt = Instant.parse("2024-01-01T10:00:00Z"))
            val targetExercise = testWorkoutExercise(seed = 1, sets = listOf(completedSet))
            val workout = testActiveWorkout(exercises = listOf(targetExercise))
            val workouts = FakeWorkoutRepository(listOf(workout))
            val replacement = testExercise(seed = 2, name = "Replacement")
            val viewModel =
                viewModel(
                    replacingWorkoutExerciseId = targetExercise.id,
                    exercises = FakeExerciseRepository(listOf(replacement)),
                    workouts = workouts,
                )
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.onIntent(ExercisePickerIntent.ExerciseClicked(replacement.id))
            dispatcher.scheduler.advanceUntilIdle()

            val confirmation = viewModel.state.value.replaceConfirmation
            assertEquals(1, confirmation?.completedSetCount)
            assertEquals(
                targetExercise.exerciseId,
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.single()
                    ?.exerciseId,
            )
        }

    @Test
    fun givenAReplaceConfirmationShowing_whenConfirmedTwice_thenOnlyOneWriteHappens() =
        runTest(dispatcher) {
            val completedSet = testWorkoutSet(seed = 10, completedAt = Instant.parse("2024-01-01T10:00:00Z"))
            val targetExercise = testWorkoutExercise(seed = 1, sets = listOf(completedSet))
            val workout = testActiveWorkout(exercises = listOf(targetExercise))
            val workouts = FakeWorkoutRepository(listOf(workout))
            val replacement = testExercise(seed = 2, name = "Replacement")
            val viewModel =
                viewModel(
                    replacingWorkoutExerciseId = targetExercise.id,
                    exercises = FakeExerciseRepository(listOf(replacement)),
                    workouts = workouts,
                )
            dispatcher.scheduler.advanceUntilIdle()
            viewModel.onIntent(ExercisePickerIntent.ExerciseClicked(replacement.id))
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.onIntent(ExercisePickerIntent.ReplaceConfirmed)
            viewModel.onIntent(ExercisePickerIntent.ReplaceConfirmed)
            dispatcher.scheduler.advanceUntilIdle()

            val resultExercise =
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.single()
            assertEquals(replacement.id, resultExercise?.exerciseId)
            assertEquals(0, resultExercise?.sets?.count { it.isCompleted })
        }

    @Test
    fun givenAReplaceConfirmationShowing_whenDismissed_thenItClosesAndNothingIsWritten() =
        runTest(dispatcher) {
            val completedSet = testWorkoutSet(seed = 10, completedAt = Instant.parse("2024-01-01T10:00:00Z"))
            val targetExercise = testWorkoutExercise(seed = 1, sets = listOf(completedSet))
            val workout = testActiveWorkout(exercises = listOf(targetExercise))
            val workouts = FakeWorkoutRepository(listOf(workout))
            val replacement = testExercise(seed = 2, name = "Replacement")
            val viewModel =
                viewModel(
                    replacingWorkoutExerciseId = targetExercise.id,
                    exercises = FakeExerciseRepository(listOf(replacement)),
                    workouts = workouts,
                )
            dispatcher.scheduler.advanceUntilIdle()
            viewModel.onIntent(ExercisePickerIntent.ExerciseClicked(replacement.id))
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.onIntent(ExercisePickerIntent.ReplaceDismissed)
            dispatcher.scheduler.advanceUntilIdle()

            assertNull(viewModel.state.value.replaceConfirmation)
            assertEquals(
                targetExercise.exerciseId,
                workouts
                    .observeActiveWorkout()
                    .first()
                    ?.exercises
                    ?.single()
                    ?.exerciseId,
            )
        }

    @Test
    fun givenReplaceModeAndTheSameExerciseAsTheTarget_whenClicked_thenItClosesWithNoChange() =
        runTest(dispatcher) {
            val targetExercise = testWorkoutExercise(seed = 1)
            val workout = testActiveWorkout(exercises = listOf(targetExercise))
            val workouts = FakeWorkoutRepository(listOf(workout))
            val viewModel = viewModel(replacingWorkoutExerciseId = targetExercise.id, workouts = workouts)
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onIntent(ExercisePickerIntent.ExerciseClicked(targetExercise.exerciseId))
                assertEquals(ExercisePickerEffect.Close, awaitItem())
            }
        }

    @Test
    fun givenReplaceModeAndAMissingTarget_whenAnyExerciseIsClicked_thenItClosesWithNoChange() =
        runTest(dispatcher) {
            val workout = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1)))
            val workouts = FakeWorkoutRepository(listOf(workout))
            val replacement = testExercise(seed = 2, name = "Replacement")
            val viewModel =
                viewModel(
                    replacingWorkoutExerciseId = WorkoutExerciseId(testUuid(99)),
                    exercises = FakeExerciseRepository(listOf(replacement)),
                    workouts = workouts,
                )
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onIntent(ExercisePickerIntent.ExerciseClicked(replacement.id))
                assertEquals(ExercisePickerEffect.Close, awaitItem())
            }
        }

    @Test
    fun givenNoActiveWorkout_whenTheFirstDataEmissionArrives_thenCloseIsSent() =
        runTest(dispatcher) {
            val viewModel = viewModel(workouts = FakeWorkoutRepository())

            viewModel.effects.test {
                dispatcher.scheduler.advanceUntilIdle()

                assertEquals(ExercisePickerEffect.Close, awaitItem())
            }
        }
}
