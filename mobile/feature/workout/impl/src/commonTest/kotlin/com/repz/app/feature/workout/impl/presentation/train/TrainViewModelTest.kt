package com.repz.app.feature.workout.impl.presentation.train

import app.cash.turbine.test
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.workout.FindLastPerformanceUseCase
import com.repz.app.core.domain.workout.StartEmptyWorkoutUseCase
import com.repz.app.core.domain.workout.StartWorkoutFromTemplateUseCase
import com.repz.app.core.testing.id.SequentialIdGenerator
import com.repz.app.core.testing.template.FakeWorkoutTemplateRepository
import com.repz.app.core.testing.time.MutableClock
import com.repz.app.core.testing.workout.FakeWorkoutRepository
import com.repz.app.feature.workout.impl.domain.ObserveTrainOverviewUseCase
import com.repz.app.feature.workout.impl.testActiveWorkout
import com.repz.app.feature.workout.impl.testWorkoutTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class TrainViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 2024-01-03T00:00:00Z is a Wednesday.
    private val wednesday = Instant.parse("2024-01-03T00:00:00Z")

    private fun viewModel(
        workouts: FakeWorkoutRepository = FakeWorkoutRepository(),
        templates: FakeWorkoutTemplateRepository = FakeWorkoutTemplateRepository(),
    ): TrainViewModel =
        TrainViewModel(
            observeTrainOverview = ObserveTrainOverviewUseCase(workouts, templates),
            startEmptyWorkout = StartEmptyWorkoutUseCase(workouts, MutableClock(wednesday), SequentialIdGenerator()),
            startWorkoutFromTemplate =
                StartWorkoutFromTemplateUseCase(
                    workouts,
                    templates,
                    FindLastPerformanceUseCase(workouts),
                    MutableClock(wednesday),
                    SequentialIdGenerator(),
                ),
            clock = MutableClock(wednesday),
            timeZone = { TimeZone.UTC },
        )

    @Test
    fun givenANewViewModel_whenCreated_thenTodayIsResolvedFromTheInjectedClock() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            assertEquals(DayOfWeek.WEDNESDAY, viewModel.state.value.today)
        }

    @Test
    fun givenNoActiveWorkoutAndSomeTemplates_whenLoaded_thenStateListsThemAndHasNoResumeRow() =
        runTest(dispatcher) {
            val template = testWorkoutTemplate(seed = 1, name = "Push A")
            val viewModel = viewModel(templates = FakeWorkoutTemplateRepository(listOf(template)))

            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(false, state.isLoading)
            assertNull(state.activeWorkoutName)
            assertEquals("Push A", state.templates.single().name)
            assertEquals(template.exercises.size, state.templates.single().exerciseCount)
        }

    @Test
    fun givenAnActiveWorkout_whenLoaded_thenItsNameIsShown() =
        runTest(dispatcher) {
            val active = testActiveWorkout(name = "Wednesday workout")
            val viewModel = viewModel(workouts = FakeWorkoutRepository(listOf(active)))

            dispatcher.scheduler.advanceUntilIdle()

            assertEquals("Wednesday workout", viewModel.state.value.activeWorkoutName)
        }

    @Test
    fun givenNoActiveWorkout_whenStartEmptyWorkoutClicked_thenItStartsOneAndOpensLiveWorkout() =
        runTest(dispatcher) {
            val workouts = FakeWorkoutRepository()
            val viewModel = viewModel(workouts = workouts)

            viewModel.effects.test {
                viewModel.onIntent(TrainIntent.StartEmptyWorkoutClicked("Wednesday workout"))
                dispatcher.scheduler.advanceUntilIdle()

                assertEquals(TrainEffect.OpenLiveWorkout, awaitItem())
            }
            assertEquals("Wednesday workout", workouts.observeActiveWorkout().first()?.name)
        }

    @Test
    fun givenATemplate_whenStartTemplateClicked_thenItStartsFromItAndOpensLiveWorkout() =
        runTest(dispatcher) {
            val template = testWorkoutTemplate(seed = 1, name = "Push A")
            val templates = FakeWorkoutTemplateRepository(listOf(template))
            val viewModel = viewModel(templates = templates)

            viewModel.effects.test {
                viewModel.onIntent(TrainIntent.StartTemplateClicked(template.id))
                dispatcher.scheduler.advanceUntilIdle()

                assertEquals(TrainEffect.OpenLiveWorkout, awaitItem())
            }
        }

    @Test
    fun givenAnActiveWorkoutAlreadyExists_whenResumeClicked_thenItOpensLiveWorkoutDirectly() =
        runTest(dispatcher) {
            val viewModel = viewModel(workouts = FakeWorkoutRepository(listOf(testActiveWorkout())))

            viewModel.effects.test {
                viewModel.onIntent(TrainIntent.ResumeClicked)

                assertEquals(TrainEffect.OpenLiveWorkout, awaitItem())
            }
        }
}
