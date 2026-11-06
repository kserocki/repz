package com.repz.app.core.sync.engine

import com.repz.app.core.database.sync.OutboxOperation
import com.repz.app.core.database.sync.SyncEntityType
import com.repz.app.core.domain.session.SessionState
import com.repz.app.core.domain.session.SignOutReason
import com.repz.app.core.network.dto.sync.PullResponseDto
import com.repz.app.core.network.dto.sync.TotalEstimateDto
import com.repz.app.core.network.result.NetworkResult
import com.repz.app.core.sync.fakes.FakeSyncApi
import com.repz.app.core.sync.testSyncEngine
import com.repz.app.core.sync.testUuid
import com.repz.app.core.testing.session.FakeSessionRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class SyncEngineLifecycleTest {
    @Test
    fun givenASignedOutSession_whenSyncing_thenSkipsWithZeroApiCalls() =
        runTest {
            val syncApi = FakeSyncApi()
            val sessionRepository = FakeSessionRepository(SessionState.SignedOut(SignOutReason.LoggedOut))
            val harness = testSyncEngine(backgroundScope, syncApi = syncApi, sessionRepository = sessionRepository)

            val outcome = harness.engine.sync()

            assertEquals(SyncOutcome.SkippedSignedOut, outcome)
            assertTrue(syncApi.pushRequests.isEmpty())
            assertTrue(syncApi.pullCalls.isEmpty())
        }

    @Test
    fun givenTwoConcurrentSyncCalls_whenTheFirstIsStillRunning_thenTheSecondWaitsForIt() =
        runTest {
            val syncApi =
                FakeSyncApi().apply {
                    pushGate = CompletableDeferred()
                    reachedPushGate = CompletableDeferred()
                }
            val sessionRepository = FakeSessionRepository(SessionState.SignedIn)
            val harness = testSyncEngine(backgroundScope, syncApi = syncApi, sessionRepository = sessionRepository)
            harness.database.outboxDao().enqueue(
                SyncEntityType.WORKOUT,
                testUuid("1"),
                OutboxOperation.DELETE,
                Instant.fromEpochMilliseconds(0),
            )

            val firstSync = launch { harness.engine.sync() }
            syncApi.reachedPushGate!!.await()
            val secondSync = launch { harness.engine.sync() }
            runCurrent()

            // The second call is still blocked on the mutex: it hasn't even reached its own session check yet.
            assertEquals(1, sessionRepository.restoreCallCount)

            syncApi.pushGate!!.complete(Unit)
            firstSync.join()
            secondSync.join()

            assertEquals(2, sessionRepository.restoreCallCount)
        }

    @Test
    fun givenStop_whenCalledMidPull_thenTheRunReturnsStoppedWithoutApplyingLaterPages() =
        runTest {
            val syncApi =
                FakeSyncApi().apply {
                    pullGate = CompletableDeferred()
                    reachedPullGate = CompletableDeferred()
                }
            val harness = testSyncEngine(backgroundScope, syncApi = syncApi)
            syncApi.pullHandler = { cursor, _ ->
                when (cursor) {
                    0L -> NetworkResult.Success(PullResponseDto(emptyList(), 10L, hasMore = true, TotalEstimateDto(0, 0, 0, 0)))
                    10L -> NetworkResult.Success(PullResponseDto(emptyList(), 20L, hasMore = true, TotalEstimateDto(0, 0, 0, 0)))
                    else -> error("page 3 must never be fetched once stop() is observed")
                }
            }

            val sync = async { harness.engine.sync() }
            // Let page 1 (cursor 0) flow through, re-arming the gate first so page 2's call is caught too.
            syncApi.reachedPullGate!!.await()
            var gate = syncApi.pullGate!!
            syncApi.pullGate = CompletableDeferred()
            syncApi.reachedPullGate = CompletableDeferred()
            gate.complete(Unit)

            // Page 2 (cursor 10) is now in flight: request stop while it's gated.
            syncApi.reachedPullGate!!.await()
            gate = syncApi.pullGate!!
            val stopJob = launch { harness.engine.stop() }
            runCurrent()
            gate.complete(Unit)

            assertEquals(SyncOutcome.Stopped, sync.await())
            stopJob.join()
            assertEquals(
                20L,
                harness.database
                    .syncMetaDao()
                    .get()
                    .pullCursor,
            )
            assertEquals(2, syncApi.pullCalls.size)
        }
}
