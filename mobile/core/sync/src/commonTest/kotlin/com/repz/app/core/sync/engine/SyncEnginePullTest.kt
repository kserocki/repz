package com.repz.app.core.sync.engine

import app.cash.turbine.test
import com.repz.app.core.database.sync.OutboxOperation
import com.repz.app.core.database.sync.SyncEntityType
import com.repz.app.core.domain.sync.RestoreState
import com.repz.app.core.domain.sync.SyncFailureReason
import com.repz.app.core.network.dto.SyncTypeDto
import com.repz.app.core.network.dto.sync.PullResponseDto
import com.repz.app.core.network.dto.sync.SyncChangeDto
import com.repz.app.core.network.dto.sync.TotalEstimateDto
import com.repz.app.core.network.dto.sync.WorkoutExercisePayloadDto
import com.repz.app.core.network.dto.sync.WorkoutPayloadDto
import com.repz.app.core.network.result.NetworkError
import com.repz.app.core.network.result.NetworkResult
import com.repz.app.core.sync.fakes.FakeSyncApi
import com.repz.app.core.sync.testSyncEngine
import com.repz.app.core.sync.testUuid
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.uuid.Uuid

private val UPDATED_AT = Instant.fromEpochMilliseconds(2_000)

private fun workoutChange(
    id: Uuid,
    version: Long,
    exerciseIds: List<Uuid>,
): SyncChangeDto.Workout =
    SyncChangeDto.Workout(
        id = id,
        version = version,
        updatedAt = UPDATED_AT,
        payload =
            WorkoutPayloadDto(
                templateId = null,
                name = "Pulled workout",
                notes = "",
                startedAt = UPDATED_AT,
                finishedAt = UPDATED_AT,
                exercises =
                    exerciseIds.mapIndexed {
                        index,
                        exerciseId,
                        ->
                        WorkoutExercisePayloadDto(testUuid("e$index"), exerciseId, "", emptyList())
                    },
            ),
    )

private fun totalEstimate(
    total: Int,
    workout: Int = total,
): TotalEstimateDto = TotalEstimateDto(total, workout, template = 0, exercise = 0)

class SyncEnginePullTest {
    @Test
    fun givenThreePages_whenSyncing_thenAppliesEachPageAndSavesItsCursorEveryTime() =
        runTest {
            val syncApi = FakeSyncApi()
            val harness = testSyncEngine(backgroundScope, syncApi = syncApi)
            // Not a restore, so each page's PR refresh is independent and refreshAll must not run.
            harness.database.syncMetaDao().save(
                harness.database
                    .syncMetaDao()
                    .get()
                    .copy(initialPullCompletedAt = UPDATED_AT),
            )
            syncApi.pullHandler = { cursor, _ ->
                when (cursor) {
                    0L -> {
                        NetworkResult.Success(
                            PullResponseDto(listOf(workoutChange(testUuid("1"), 1, emptyList())), 10L, hasMore = true, totalEstimate(3)),
                        )
                    }

                    10L -> {
                        NetworkResult.Success(
                            PullResponseDto(listOf(workoutChange(testUuid("2"), 2, emptyList())), 20L, hasMore = true, totalEstimate(2)),
                        )
                    }

                    20L -> {
                        NetworkResult.Success(
                            PullResponseDto(listOf(workoutChange(testUuid("3"), 3, emptyList())), 30L, hasMore = false, totalEstimate(1)),
                        )
                    }

                    else -> {
                        error("unexpected cursor $cursor")
                    }
                }
            }

            val outcome = harness.engine.sync()

            assertEquals(SyncOutcome.Completed, outcome)
            assertEquals(listOf(0L to 500, 10L to 500, 20L to 500), syncApi.pullCalls)
            assertEquals(
                30L,
                harness.database
                    .syncMetaDao()
                    .get()
                    .pullCursor,
            )
            assertTrue(harness.database.workoutDao().findWorkoutIncludingDeleted(testUuid("3")) != null)
        }

    @Test
    fun givenAFailureOnPageTwo_whenSyncingAgain_thenResumesFromThePageOneCursor() =
        runTest {
            val syncApi = FakeSyncApi()
            val harness = testSyncEngine(backgroundScope, syncApi = syncApi)
            harness.database.syncMetaDao().save(
                harness.database
                    .syncMetaDao()
                    .get()
                    .copy(initialPullCompletedAt = UPDATED_AT),
            )
            syncApi.pullHandler = { cursor, _ ->
                when (cursor) {
                    0L -> {
                        NetworkResult.Success(
                            PullResponseDto(listOf(workoutChange(testUuid("1"), 1, emptyList())), 10L, hasMore = true, totalEstimate(2)),
                        )
                    }

                    10L -> {
                        NetworkResult.Failure(NetworkError.Offline)
                    }

                    else -> {
                        error("unexpected cursor $cursor")
                    }
                }
            }

            val outcome = harness.engine.sync()

            assertEquals(SyncOutcome.RetryLater(null), outcome)
            assertEquals(
                10L,
                harness.database
                    .syncMetaDao()
                    .get()
                    .pullCursor,
            )

            syncApi.pullHandler = { cursor, _ ->
                when (cursor) {
                    10L -> {
                        NetworkResult.Success(
                            PullResponseDto(listOf(workoutChange(testUuid("2"), 2, emptyList())), 20L, hasMore = false, totalEstimate(1)),
                        )
                    }

                    else -> {
                        error("unexpected cursor $cursor")
                    }
                }
            }
            val secondOutcome = harness.engine.sync()

            assertEquals(SyncOutcome.Completed, secondOutcome)
            assertEquals(
                20L,
                harness.database
                    .syncMetaDao()
                    .get()
                    .pullCursor,
            )
            assertEquals(listOf(0L to 500, 10L to 500, 10L to 500), syncApi.pullCalls)
        }

    @Test
    fun givenAPendingOutboxEntityAmongThePulledChanges_whenSyncing_thenItIsSkipped() =
        runTest {
            val syncApi = FakeSyncApi()
            val harness = testSyncEngine(backgroundScope, syncApi = syncApi)
            harness.database.syncMetaDao().save(
                harness.database
                    .syncMetaDao()
                    .get()
                    .copy(initialPullCompletedAt = UPDATED_AT),
            )
            val pendingId = testUuid("1")
            // Held, so the push pass never looks at it (and, since REP-26's deleteIfUnchanged-on-null
            // fix, never has a chance to drop it either): it stays pending for findPendingEntityIds
            // throughout the whole run, which is the only thing this test cares about.
            harness.database.outboxDao().enqueue(
                SyncEntityType.WORKOUT,
                pendingId,
                OutboxOperation.DELETE,
                UPDATED_AT,
                heldUntil = UPDATED_AT + 10.minutes,
            )
            syncApi.pullHandler = { cursor, _ ->
                if (cursor == 0L) {
                    NetworkResult.Success(
                        PullResponseDto(listOf(workoutChange(pendingId, 9, emptyList())), 10L, hasMore = false, totalEstimate(1)),
                    )
                } else {
                    error("unexpected cursor $cursor")
                }
            }

            harness.engine.sync()

            assertNull(harness.database.workoutDao().findWorkoutIncludingDeleted(pendingId))
        }

    @Test
    fun givenATombstoneForAnUnknownId_whenSyncing_thenNothingIsStoredAndTheRunCompletes() =
        runTest {
            val syncApi = FakeSyncApi()
            val harness = testSyncEngine(backgroundScope, syncApi = syncApi)
            harness.database.syncMetaDao().save(
                harness.database
                    .syncMetaDao()
                    .get()
                    .copy(initialPullCompletedAt = UPDATED_AT),
            )
            val unknownId = testUuid("1")
            syncApi.pullHandler = { cursor, _ ->
                if (cursor == 0L) {
                    NetworkResult.Success(
                        PullResponseDto(
                            listOf(SyncChangeDto.Tombstone(SyncTypeDto.WORKOUT, unknownId, 1, UPDATED_AT)),
                            10L,
                            hasMore = false,
                            totalEstimate(1),
                        ),
                    )
                } else {
                    error("unexpected cursor $cursor")
                }
            }

            val outcome = harness.engine.sync()

            assertEquals(SyncOutcome.Completed, outcome)
            assertNull(harness.database.workoutDao().findWorkoutIncludingDeleted(unknownId))
        }

    @Test
    fun givenARestore_whenTheFirstPageArrives_thenStoresRestoreTotalsOnlyFromThatPage() =
        runTest {
            val syncApi = FakeSyncApi()
            val harness = testSyncEngine(backgroundScope, syncApi = syncApi)
            syncApi.pullHandler = { cursor, _ ->
                when (cursor) {
                    0L -> {
                        NetworkResult.Success(
                            PullResponseDto(
                                listOf(workoutChange(testUuid("1"), 1, emptyList())),
                                10L,
                                hasMore = true,
                                totalEstimate(total = 2, workout = 2),
                            ),
                        )
                    }

                    10L -> {
                        NetworkResult.Success(
                            PullResponseDto(
                                listOf(workoutChange(testUuid("2"), 2, emptyList())),
                                20L,
                                hasMore = false,
                                totalEstimate(total = 1, workout = 1),
                            ),
                        )
                    }

                    else -> {
                        error("unexpected cursor $cursor")
                    }
                }
            }

            harness.engine.sync()

            val meta = harness.database.syncMetaDao().get()
            assertEquals(2, meta.restoreTotalChanges)
            assertEquals(2, meta.restoreTotalWorkouts)
        }

    @Test
    fun givenARestore_whenEachPageIsApplied_thenReportsRestoreProgressPerPageThenCompletes() =
        runTest {
            val syncApi = FakeSyncApi()
            val harness = testSyncEngine(backgroundScope, syncApi = syncApi)
            syncApi.pullHandler = { cursor, _ ->
                when (cursor) {
                    0L -> {
                        NetworkResult.Success(
                            PullResponseDto(
                                listOf(workoutChange(testUuid("1"), 1, emptyList())),
                                10L,
                                hasMore = true,
                                totalEstimate(total = 2, workout = 2),
                            ),
                        )
                    }

                    10L -> {
                        NetworkResult.Success(
                            PullResponseDto(
                                listOf(workoutChange(testUuid("2"), 2, emptyList())),
                                20L,
                                hasMore = false,
                                totalEstimate(total = 1, workout = 1),
                            ),
                        )
                    }

                    else -> {
                        error("unexpected cursor $cursor")
                    }
                }
            }

            harness.statusTracker.restoreState.test {
                assertEquals(RestoreState.InProgress(null), awaitItem())
                harness.engine.sync()
                val afterPage1 = awaitItem()
                assertTrue(afterPage1 is RestoreState.InProgress && afterPage1.progress?.restoredWorkouts == 1)
                val afterPage2 = awaitItem()
                assertTrue(afterPage2 is RestoreState.InProgress && afterPage2.progress?.restoredWorkouts == 2)
                assertEquals(RestoreState.Completed, awaitItem())
            }
        }

    @Test
    fun givenARestoreCompletes_whenSyncing_thenRefreshesAllPersonalRecordsOnceBeforeMarkingItComplete() =
        runTest {
            val syncApi = FakeSyncApi()
            val harness = testSyncEngine(backgroundScope, syncApi = syncApi)
            syncApi.pullHandler = { _, _ -> NetworkResult.Success(PullResponseDto(emptyList(), 0L, hasMore = false, totalEstimate(0))) }
            var initialPullCompletedAtDuringRefresh: Instant? = Instant.fromEpochMilliseconds(-1)
            harness.recordsRefresher.onRefreshAll = {
                initialPullCompletedAtDuringRefresh =
                    harness.database
                        .syncMetaDao()
                        .get()
                        .initialPullCompletedAt
            }

            harness.engine.sync()

            assertNull(initialPullCompletedAtDuringRefresh)
            assertEquals(1, harness.recordsRefresher.refreshAllCallCount)
            assertTrue(
                harness.database
                    .syncMetaDao()
                    .get()
                    .initialPullCompletedAt != null,
            )
        }

    @Test
    fun givenARestoreInterruptedBetweenRefreshAllAndSavingCompletion_whenSyncingAgain_thenRefreshesAllAgain() =
        runTest {
            val syncApi = FakeSyncApi()
            val harness = testSyncEngine(backgroundScope, syncApi = syncApi)
            // The pull already caught up to the end in a previous run, but initialPullCompletedAt was
            // never saved - simulating a kill between refreshAll() and that save.
            syncApi.pullHandler = { _, _ -> NetworkResult.Success(PullResponseDto(emptyList(), 0L, hasMore = false, totalEstimate(0))) }

            harness.engine.sync()

            assertEquals(1, harness.recordsRefresher.refreshAllCallCount)
            harness.database.syncMetaDao().save(
                harness.database
                    .syncMetaDao()
                    .get()
                    .copy(initialPullCompletedAt = null),
            )

            harness.engine.sync()

            assertEquals(2, harness.recordsRefresher.refreshAllCallCount)
        }

    @Test
    fun givenAnIncrementalPullTouchingAWorkout_whenSyncing_thenRefreshesTheUnionOfBeforeAndAfterExerciseIds() =
        runTest {
            val syncApi = FakeSyncApi()
            val harness = testSyncEngine(backgroundScope, syncApi = syncApi)
            val workoutId = testUuid("1")
            val before = listOf(testUuid("b1"), testUuid("b2"))
            val after = listOf(testUuid("b1"), testUuid("a1"))
            harness.database.syncMetaDao().save(
                harness.database
                    .syncMetaDao()
                    .get()
                    .copy(initialPullCompletedAt = UPDATED_AT),
            )
            syncApi.pullHandler = { cursor, _ ->
                if (cursor == 0L) {
                    NetworkResult.Success(
                        PullResponseDto(listOf(workoutChange(workoutId, 1, before)), 1L, hasMore = false, totalEstimate(0)),
                    )
                } else {
                    error("unexpected cursor $cursor")
                }
            }
            harness.engine.sync()
            harness.recordsRefresher.refreshForExercisesCalls.clear()

            syncApi.pullHandler = { cursor, _ ->
                if (cursor == 1L) {
                    NetworkResult.Success(
                        PullResponseDto(listOf(workoutChange(workoutId, 2, after)), 2L, hasMore = false, totalEstimate(0)),
                    )
                } else {
                    error("unexpected cursor $cursor")
                }
            }
            harness.engine.sync()

            assertEquals(listOf(setOf(testUuid("b1"), testUuid("b2"), testUuid("a1"))), harness.recordsRefresher.refreshForExercisesCalls)
        }

    @Test
    fun givenARowBecomesReadyDuringThePull_whenSyncing_thenTheFinalPushPassSendsIt() =
        runTest {
            val syncApi =
                FakeSyncApi().apply {
                    pullGate = CompletableDeferred()
                    reachedPullGate = CompletableDeferred()
                }
            val harness = testSyncEngine(backgroundScope, syncApi = syncApi)
            harness.database.syncMetaDao().save(
                harness.database
                    .syncMetaDao()
                    .get()
                    .copy(initialPullCompletedAt = UPDATED_AT),
            )
            syncApi.pullHandler = { _, _ -> NetworkResult.Success(PullResponseDto(emptyList(), 0L, hasMore = false, totalEstimate(0))) }

            val sync = launch { harness.engine.sync() }
            syncApi.reachedPullGate!!.await()
            harness.database.outboxDao().enqueue(SyncEntityType.WORKOUT, testUuid("1"), OutboxOperation.DELETE, UPDATED_AT)
            syncApi.pullGate!!.complete(Unit)
            sync.join()

            assertEquals(1, syncApi.pushRequests.size)
        }

    @Test
    fun givenAPageWithAnUnknownVocabularyValue_whenSyncing_thenThePageIsNotAppliedAndTheCursorStaysUnchanged() =
        runTest {
            val syncApi = FakeSyncApi()
            val harness = testSyncEngine(backgroundScope, syncApi = syncApi)
            harness.database.syncMetaDao().save(
                harness.database
                    .syncMetaDao()
                    .get()
                    .copy(initialPullCompletedAt = UPDATED_AT),
            )
            syncApi.pullHandler = { _, _ -> NetworkResult.Failure(NetworkError.MalformedResponse("unknown enum value")) }

            val outcome = harness.engine.sync()

            assertEquals(SyncOutcome.Failed(SyncFailureReason.IncompatibleData), outcome)
            assertEquals(
                0L,
                harness.database
                    .syncMetaDao()
                    .get()
                    .pullCursor,
            )
        }
}
