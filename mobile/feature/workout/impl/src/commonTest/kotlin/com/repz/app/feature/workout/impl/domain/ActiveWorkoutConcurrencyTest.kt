package com.repz.app.feature.workout.impl.domain

import com.repz.app.core.domain.record.RecalculatePersonalRecordsUseCase
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.workout.FinishActiveWorkoutUseCase
import com.repz.app.core.domain.workout.SaveFinishedWorkoutUseCase
import com.repz.app.core.domain.workout.StartEmptyWorkoutUseCase
import com.repz.app.core.domain.workout.StartWorkoutError
import com.repz.app.core.domain.workout.renamed
import com.repz.app.core.testing.id.SequentialIdGenerator
import com.repz.app.core.testing.record.FakePersonalRecordRepository
import com.repz.app.core.testing.time.MutableClock
import com.repz.app.core.testing.transaction.FakeTransactionRunner
import com.repz.app.core.testing.workout.FakeWorkoutRepository
import com.repz.app.feature.workout.impl.testActiveWorkout
import com.repz.app.feature.workout.impl.testWorkoutExercise
import com.repz.app.feature.workout.impl.testWorkoutSet
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Finish/Discard/Start must never race a queued edit that already read the draft (ADR-0029): each
 * test holds [ActiveWorkoutWriteLock] from a simulated "edit" between its own read and write, so
 * the use case under test is forced to wait for it instead of running on stale data.
 */
class ActiveWorkoutConcurrencyTest {
    @Test
    fun givenAQueuedEditInFlight_whenFinishRunsConcurrently_thenTheFinishedWorkoutIsNotReopened() =
        runTest {
            val completedSet = testWorkoutSet(seed = 1, completedAt = Instant.fromEpochSeconds(1))
            val workout = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1, sets = listOf(completedSet))))
            val workouts = FakeWorkoutRepository(listOf(workout))
            val lock = ActiveWorkoutWriteLock()
            val recalculate = RecalculatePersonalRecordsUseCase(workouts, FakePersonalRecordRepository(), FakeTransactionRunner())
            val finishLiveWorkout =
                FinishLiveWorkoutUseCase(
                    FinishActiveWorkoutUseCase(
                        workouts,
                        SaveFinishedWorkoutUseCase(workouts, recalculate),
                        MutableClock(Instant.fromEpochSeconds(100)),
                    ),
                    lock,
                )

            // A queued edit (e.g. a keystroke) that already read the active draft, racing Finish before it writes back.
            val editHasRead = CompletableDeferred<Unit>()
            val editJob =
                launch {
                    lock.withLock {
                        val current = workouts.observeActiveWorkout().first()!!
                        editHasRead.complete(Unit)
                        yield()
                        workouts.saveActiveWorkout(current.renamed("Renamed mid-edit"))
                    }
                }
            editHasRead.await()

            val result = finishLiveWorkout()
            editJob.join()

            assertTrue(result is DomainResult.Success)
            assertNull(workouts.observeActiveWorkout().first())
            val saved = workouts.findWorkoutIncludingDeleted(workout.id)
            assertEquals("Renamed mid-edit", saved?.name)
            assertNotNull(saved?.finishedAt)
        }

    @Test
    fun givenAQueuedEditInFlight_whenDiscardRunsConcurrently_thenTheDiscardedWorkoutIsNotResurrected() =
        runTest {
            val workout = testActiveWorkout(exercises = listOf(testWorkoutExercise(seed = 1)))
            val workouts = FakeWorkoutRepository(listOf(workout))
            val lock = ActiveWorkoutWriteLock()
            val discardActiveWorkout = DiscardActiveWorkoutUseCase(workouts, lock)

            val editHasRead = CompletableDeferred<Unit>()
            val editJob =
                launch {
                    lock.withLock {
                        val current = workouts.observeActiveWorkout().first()!!
                        editHasRead.complete(Unit)
                        yield()
                        workouts.saveActiveWorkout(current.renamed("Renamed mid-edit"))
                    }
                }
            editHasRead.await()

            discardActiveWorkout()
            editJob.join()

            assertNull(workouts.observeActiveWorkout().first())
            assertNull(workouts.findWorkoutIncludingDeleted(workout.id))
        }

    @Test
    fun givenTwoConcurrentStartEmptyWorkoutCalls_whenBothRun_thenOnlyOneActiveWorkoutIsCreated() =
        runTest {
            val workouts = FakeWorkoutRepository()
            val lock = ActiveWorkoutWriteLock()
            val startEmptyWorkout = StartEmptyWorkoutUseCase(workouts, MutableClock(Instant.fromEpochSeconds(0)), SequentialIdGenerator())
            val startEmptyLiveWorkout = StartEmptyLiveWorkoutUseCase(startEmptyWorkout, lock)

            // The first tap already read "no active workout" and is about to save one, racing a second tap's own read.
            val firstHasRead = CompletableDeferred<Unit>()
            val firstJob =
                launch {
                    lock.withLock {
                        val current = workouts.observeActiveWorkout().first()
                        firstHasRead.complete(Unit)
                        yield()
                        if (current == null) workouts.saveActiveWorkout(testActiveWorkout(name = "First tap"))
                    }
                }
            firstHasRead.await()

            val secondResult = startEmptyLiveWorkout("Second tap")
            firstJob.join()

            assertTrue(secondResult is DomainResult.Failure)
            assertTrue(secondResult.error is StartWorkoutError.WorkoutAlreadyActive)
            assertEquals("First tap", workouts.observeActiveWorkout().first()?.name)
        }
}
