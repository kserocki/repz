package com.repz.app.core.domain.workout

import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.testing.id.SequentialIdGenerator
import com.repz.app.core.testing.time.MutableClock
import com.repz.app.core.testing.workout.FakeWorkoutRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class StartEmptyWorkoutUseCaseTest {
    @Test
    fun givenNoActiveWorkout_whenStarted_thenAnEmptyWorkoutIsSavedAsActive() =
        runTest {
            val workouts = FakeWorkoutRepository()
            val clock = MutableClock(Instant.fromEpochSeconds(1_000))
            val useCase = StartEmptyWorkoutUseCase(workouts, clock, SequentialIdGenerator())

            val result = useCase("Wednesday workout") as DomainResult.Success

            val saved = workouts.findWorkoutIncludingDeleted(result.value)
            assertEquals("Wednesday workout", saved?.name)
            assertEquals(Instant.fromEpochSeconds(1_000), saved?.startedAt)
            assertEquals(null, saved?.finishedAt)
            assertEquals(null, saved?.templateId)
            assertTrue(saved?.exercises.orEmpty().isEmpty())
        }

    @Test
    fun givenAnActiveWorkoutAlreadyExists_whenStarted_thenItFailsWithoutCreatingASecondOne() =
        runTest {
            val existingActive = testActiveWorkout(seed = 1)
            val workouts = FakeWorkoutRepository(listOf(existingActive))
            val useCase = StartEmptyWorkoutUseCase(workouts, MutableClock(Instant.fromEpochSeconds(0)), SequentialIdGenerator())

            val result = useCase("Thursday workout")

            assertEquals(DomainResult.Failure(StartWorkoutError.WorkoutAlreadyActive(existingActive.id)), result)
        }
}
