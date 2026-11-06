package com.repz.app.core.sync.schedule

import com.repz.app.core.sync.engine.SyncOutcome
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import platform.UIKit.UIBackgroundTaskIdentifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration

private object NoOpForegroundRunAssertion : ForegroundRunAssertion {
    override fun begin(onExpired: () -> Unit): UIBackgroundTaskIdentifier = 0UL

    override fun end(identifier: UIBackgroundTaskIdentifier) = Unit
}

private class FakeNetworkPathConnectivity : NetworkPathConnectivity {
    private var onUpdate: ((Boolean) -> Unit)? = null
    var cancelled = false
        private set

    override fun start(onPathUpdate: (Boolean) -> Unit) {
        onUpdate = onPathUpdate
    }

    override fun cancel() {
        cancelled = true
    }

    fun simulate(satisfied: Boolean) = onUpdate?.invoke(satisfied)
}

/**
 * [complete] is backed by a [CompletableDeferred] rather than a plain field: [awaitCompletion]
 * lets a test suspend until the scheduler actually runs the launch handler's job to completion,
 * instead of racing it with [kotlinx.coroutines.test.TestCoroutineScheduler.advanceUntilIdle],
 * which doesn't reliably drain a job freshly dispatched from outside any coroutine body.
 */
private class FakeBackgroundRefreshTaskRun : BackgroundRefreshTaskRun {
    private var expirationHandler: (() -> Unit)? = null
    private val completion = CompletableDeferred<Boolean>()

    override fun setExpirationHandler(onExpired: () -> Unit) {
        expirationHandler = onExpired
    }

    override fun complete(success: Boolean) {
        completion.complete(success)
    }

    fun expire() = expirationHandler?.invoke()

    suspend fun awaitCompletion(): Boolean = completion.await()
}

private class FakeBackgroundRefreshTask : BackgroundRefreshTask {
    private var onLaunch: ((BackgroundRefreshTaskRun) -> Unit)? = null
    var registeredIdentifier: String? = null
        private set
    var submittedCount = 0
        private set
    var cancelledIdentifier: String? = null
        private set

    override fun register(
        identifier: String,
        onLaunch: (BackgroundRefreshTaskRun) -> Unit,
    ) {
        registeredIdentifier = identifier
        this.onLaunch = onLaunch
    }

    override fun submit(
        identifier: String,
        earliestBeginIn: Duration,
    ) {
        submittedCount++
    }

    override fun cancel(identifier: String) {
        cancelledIdentifier = identifier
    }

    fun launch(run: BackgroundRefreshTaskRun) = onLaunch?.invoke(run)
}

/**
 * [IosSyncScheduler.start] launches a consumer loop that never completes on its own (it keeps
 * consuming the conflated channel for the scheduler's whole lifetime, by design): passed `runTest`'s
 * own test-body scope, that leftover job would fail every test with `UncompletedCoroutinesError`.
 * `TestScope.backgroundScope` is built for exactly this.
 */
class IosSyncSchedulerTest {
    @Test
    fun givenABurstOfRequestsWhileARunIsInFlight_whenConsumed_thenExactlyOneMoreRunHappens() =
        runTest {
            var runCount = 0
            val firstRunStarted = CompletableDeferred<Unit>()
            val proceedWithFirstRun = CompletableDeferred<Unit>()
            val secondRunDone = CompletableDeferred<Unit>()
            val scheduler =
                IosSyncScheduler(
                    runSync = {
                        when (++runCount) {
                            1 -> {
                                firstRunStarted.complete(Unit)
                                proceedWithFirstRun.await()
                            }

                            else -> {
                                secondRunDone.complete(Unit)
                            }
                        }
                        SyncOutcome.Completed
                    },
                    applicationScope = backgroundScope,
                    backgroundRefreshTask = FakeBackgroundRefreshTask(),
                    connectivity = FakeNetworkPathConnectivity(),
                    foregroundRunAssertion = NoOpForegroundRunAssertion,
                )
            scheduler.start()

            scheduler.requestSync()
            firstRunStarted.await()
            repeat(9) { scheduler.requestSync() }
            proceedWithFirstRun.complete(Unit)
            secondRunDone.await()

            assertEquals(2, runCount)
        }

    @Test
    fun givenARegainedNetworkPath_whenItStaysSatisfied_thenOnlyTheTransitionRequestsASync() =
        runTest {
            var runCount = 0
            val firstRunDone = CompletableDeferred<Unit>()
            val connectivity = FakeNetworkPathConnectivity()
            val scheduler =
                IosSyncScheduler(
                    runSync = {
                        runCount++
                        firstRunDone.complete(Unit)
                        SyncOutcome.Completed
                    },
                    applicationScope = backgroundScope,
                    backgroundRefreshTask = FakeBackgroundRefreshTask(),
                    connectivity = connectivity,
                    foregroundRunAssertion = NoOpForegroundRunAssertion,
                )
            scheduler.start()

            connectivity.simulate(satisfied = false)
            connectivity.simulate(satisfied = true)
            firstRunDone.await()
            connectivity.simulate(satisfied = true)
            testScheduler.advanceUntilIdle()

            assertEquals(1, runCount)
        }

    @Test
    fun givenCancelAll_whenARequestWasBufferedDuringARun_thenNoFurtherRunStartsForIt() =
        runTest {
            var runCount = 0
            val firstRunStarted = CompletableDeferred<Unit>()
            val proceedWithFirstRun = CompletableDeferred<Unit>()
            val backgroundRefreshTask = FakeBackgroundRefreshTask()
            val scheduler =
                IosSyncScheduler(
                    runSync = {
                        if (++runCount == 1) {
                            firstRunStarted.complete(Unit)
                            proceedWithFirstRun.await()
                        }
                        SyncOutcome.Completed
                    },
                    applicationScope = backgroundScope,
                    backgroundRefreshTask = backgroundRefreshTask,
                    connectivity = FakeNetworkPathConnectivity(),
                    foregroundRunAssertion = NoOpForegroundRunAssertion,
                )
            scheduler.start()

            scheduler.requestSync()
            firstRunStarted.await()
            scheduler.requestSync()
            scheduler.cancelAll()
            proceedWithFirstRun.complete(Unit)
            testScheduler.advanceUntilIdle()

            assertEquals(1, runCount)
            assertEquals(SYNC_REFRESH_TASK_IDENTIFIER, backgroundRefreshTask.cancelledIdentifier)
        }

    @Test
    fun givenABackgroundRefreshLaunch_whenRunSyncCompletes_thenItResubmitsFirstAndCompletesWithSuccess() =
        runTest {
            val backgroundRefreshTask = FakeBackgroundRefreshTask()
            val run = FakeBackgroundRefreshTaskRun()
            val scheduler =
                IosSyncScheduler(
                    runSync = { SyncOutcome.Completed },
                    applicationScope = backgroundScope,
                    backgroundRefreshTask = backgroundRefreshTask,
                    connectivity = FakeNetworkPathConnectivity(),
                    foregroundRunAssertion = NoOpForegroundRunAssertion,
                )
            scheduler.start()
            val submittedBeforeLaunch = backgroundRefreshTask.submittedCount

            backgroundRefreshTask.launch(run)

            assertEquals(true, run.awaitCompletion())
            assertEquals(submittedBeforeLaunch + 1, backgroundRefreshTask.submittedCount)
        }

    @Test
    fun givenABackgroundRefreshExpires_whenTheExpirationHandlerFires_thenTheRunCompletesAsFailed() =
        runTest {
            val backgroundRefreshTask = FakeBackgroundRefreshTask()
            val run = FakeBackgroundRefreshTaskRun()
            val runStarted = CompletableDeferred<Unit>()
            val neverCompletes = CompletableDeferred<Unit>()
            val scheduler =
                IosSyncScheduler(
                    runSync = {
                        runStarted.complete(Unit)
                        neverCompletes.await()
                        SyncOutcome.Completed
                    },
                    applicationScope = backgroundScope,
                    backgroundRefreshTask = backgroundRefreshTask,
                    connectivity = FakeNetworkPathConnectivity(),
                    foregroundRunAssertion = NoOpForegroundRunAssertion,
                )
            scheduler.start()

            backgroundRefreshTask.launch(run)
            runStarted.await()
            run.expire()

            assertEquals(false, run.awaitCompletion())
        }

    @Test
    fun givenStart_whenCalled_thenTheBackgroundTaskIsRegisteredAndConnectivityIsStarted() =
        runTest {
            val backgroundRefreshTask = FakeBackgroundRefreshTask()
            val connectivity = FakeNetworkPathConnectivity()
            val scheduler =
                IosSyncScheduler(
                    runSync = { SyncOutcome.Completed },
                    applicationScope = backgroundScope,
                    backgroundRefreshTask = backgroundRefreshTask,
                    connectivity = connectivity,
                    foregroundRunAssertion = NoOpForegroundRunAssertion,
                )

            scheduler.start()

            assertEquals(SYNC_REFRESH_TASK_IDENTIFIER, backgroundRefreshTask.registeredIdentifier)
        }
}
