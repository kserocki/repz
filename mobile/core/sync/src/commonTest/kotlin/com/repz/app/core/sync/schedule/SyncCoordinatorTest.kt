package com.repz.app.core.sync.schedule

import com.repz.app.core.domain.session.SessionState
import com.repz.app.core.domain.session.SignOutReason
import com.repz.app.core.sync.fakes.FakeBackgroundSyncScheduler
import com.repz.app.core.sync.testSyncEngine
import com.repz.app.core.testing.session.FakeSessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SyncCoordinatorTest {
    @Test
    fun givenStart_whenCalled_thenTheSchedulerStartsBeforeAnySessionStateIsObserved() =
        runTest {
            val scheduler = FakeBackgroundSyncScheduler()
            val sessionRepository = FakeSessionRepository()
            val harness = testSyncEngine(backgroundScope, sessionRepository = sessionRepository)
            val coordinator = SyncCoordinator(sessionRepository, scheduler, harness.engine, backgroundScope)

            coordinator.start()
            runCurrent()

            assertEquals(1, scheduler.startCallCount)
            assertEquals(0, scheduler.schedulePeriodicCallCount)
            assertEquals(0, scheduler.cancelAllCallCount)
        }

    @Test
    fun givenSignedIn_whenObserved_thenSchedulesPeriodicSync() =
        runTest {
            val scheduler = FakeBackgroundSyncScheduler()
            val sessionRepository = FakeSessionRepository()
            val harness = testSyncEngine(backgroundScope, sessionRepository = sessionRepository)
            val coordinator = SyncCoordinator(sessionRepository, scheduler, harness.engine, backgroundScope)
            coordinator.start()
            runCurrent()

            sessionRepository.emit(SessionState.SignedIn)
            runCurrent()

            assertEquals(1, scheduler.schedulePeriodicCallCount)
            assertEquals(0, scheduler.cancelAllCallCount)
        }

    @Test
    fun givenSignedOut_whenObserved_thenCancelsAllScheduledWorkAndStopsTheEngine() =
        runTest {
            val scheduler = FakeBackgroundSyncScheduler()
            val sessionRepository = FakeSessionRepository()
            val harness = testSyncEngine(backgroundScope, sessionRepository = sessionRepository)
            val coordinator = SyncCoordinator(sessionRepository, scheduler, harness.engine, backgroundScope)
            coordinator.start()
            sessionRepository.emit(SessionState.SignedIn)
            runCurrent()

            sessionRepository.emit(SessionState.SignedOut(SignOutReason.LoggedOut))
            runCurrent()

            assertEquals(1, scheduler.cancelAllCallCount)
        }

    @Test
    fun givenUnknown_whenObserved_thenNothingIsScheduledOrCancelled() =
        runTest {
            val scheduler = FakeBackgroundSyncScheduler()
            val sessionRepository = FakeSessionRepository()
            val harness = testSyncEngine(backgroundScope, sessionRepository = sessionRepository)
            val coordinator = SyncCoordinator(sessionRepository, scheduler, harness.engine, backgroundScope)

            coordinator.start()
            runCurrent()

            assertEquals(0, scheduler.schedulePeriodicCallCount)
            assertEquals(0, scheduler.cancelAllCallCount)
        }
}
