package com.repz.app.core.sync.schedule

import co.touchlab.kermit.Logger
import com.repz.app.core.sync.engine.SyncOutcome
import com.repz.app.core.sync.logging.SYNC_LOG_TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import platform.UIKit.UIApplication
import platform.UIKit.UIBackgroundTaskIdentifier
import platform.UIKit.UIBackgroundTaskInvalid
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/** Must match `mobile/iosApp/iosApp/Info.plist`'s `BGTaskSchedulerPermittedIdentifiers` entry. */
internal const val SYNC_REFRESH_TASK_IDENTIFIER = "com.repz.app.sync.refresh"

/**
 * Drives [BackgroundSyncScheduler] on iOS from one in-process conflated trigger — local writes,
 * the shared foreground trigger (`docs/ARCHITECTURE.md` §5.6) and a regained network path — plus
 * the system's periodic `BGAppRefreshTask`. [runSync] is `SyncEngine::sync`, injected so this
 * class never sees the engine's internals.
 *
 * The constructor stays side-effect free, as [BackgroundSyncScheduler] requires: nothing starts
 * until [start] runs. The public two-parameter constructor is what callers use; the `internal`
 * one below lets tests substitute fakes for every system collaborator.
 */
class IosSyncScheduler internal constructor(
    private val runSync: suspend () -> SyncOutcome,
    private val applicationScope: CoroutineScope,
    private val backgroundRefreshTask: BackgroundRefreshTask,
    private val connectivity: NetworkPathConnectivity,
    private val foregroundRunAssertion: ForegroundRunAssertion,
    private val periodicInterval: Duration = 1.hours,
    private val logger: Logger = Logger.withTag(SYNC_LOG_TAG),
) : BackgroundSyncScheduler {
    constructor(runSync: suspend () -> SyncOutcome, applicationScope: CoroutineScope) : this(
        runSync = runSync,
        applicationScope = applicationScope,
        backgroundRefreshTask = SystemBackgroundRefreshTask(),
        connectivity = NWPathMonitorConnectivity(),
        foregroundRunAssertion = SystemForegroundRunAssertion,
        periodicInterval = 1.hours,
        logger = Logger.withTag(SYNC_LOG_TAG),
    )

    /** Capacity 1: a burst of requests while a run is in flight coalesces into at most one more. */
    private val requestChannel = Channel<Unit>(Channel.CONFLATED)

    /** Null until the first [onPathUpdate]: that call only reports the current path, not a regain. */
    private var pathSatisfied: Boolean? = null

    override fun requestSync() {
        requestChannel.trySend(Unit)
    }

    override fun start() {
        applicationScope.launch {
            for (signal in requestChannel) {
                runTriggeredSync()
            }
        }
        backgroundRefreshTask.register(SYNC_REFRESH_TASK_IDENTIFIER, ::onBackgroundRefresh)
        connectivity.start(::onPathUpdate)
    }

    override fun schedulePeriodic() {
        backgroundRefreshTask.submit(SYNC_REFRESH_TASK_IDENTIFIER, periodicInterval)
    }

    override fun cancelAll() {
        backgroundRefreshTask.cancel(SYNC_REFRESH_TASK_IDENTIFIER)
        // Drops a request already buffered but not yet picked up by the consumer loop, mirroring
        // WorkManager's cancelUniqueWork: a later requestSync() still starts a fresh run.
        requestChannel.tryReceive()
    }

    private fun onPathUpdate(satisfied: Boolean) {
        val wasSatisfied = pathSatisfied
        pathSatisfied = satisfied
        if (satisfied && wasSatisfied == false) {
            logger.i { "Network path regained, requesting a sync" }
            requestSync()
        }
    }

    private fun onBackgroundRefresh(run: BackgroundRefreshTaskRun) {
        schedulePeriodic()
        val job =
            applicationScope.launch {
                val outcome = runSync()
                logger.i { "Background refresh finished: $outcome" }
                run.complete(outcome is SyncOutcome.Completed || outcome is SyncOutcome.SkippedSignedOut)
            }
        run.setExpirationHandler {
            logger.w { "Background refresh expired before completing" }
            job.cancel()
        }
        // Any non-null cause (cancellation or an unhandled exception from runSync) still owes the
        // system a completion; a never-completed refresh task is penalised by BGTaskScheduler.
        job.invokeOnCompletion { cause -> if (cause != null) run.complete(false) }
    }

    /**
     * Runs [runSync] under a UIKit background-task assertion so the app isn't suspended mid run,
     * in a child job: an expiry only cancels that child, never this forever-running consumer loop.
     */
    private suspend fun runTriggeredSync() =
        coroutineScope {
            val job =
                launch {
                    val outcome = runSync()
                    logger.i { "Triggered sync finished: $outcome" }
                }
            val assertionId = foregroundRunAssertion.begin { job.cancel() }
            job.join()
            foregroundRunAssertion.end(assertionId)
        }
}

/** Wraps `UIApplication`'s background-task assertion behind a seam for tests, which never run a real app. */
internal interface ForegroundRunAssertion {
    fun begin(onExpired: () -> Unit): UIBackgroundTaskIdentifier

    fun end(identifier: UIBackgroundTaskIdentifier)
}

internal object SystemForegroundRunAssertion : ForegroundRunAssertion {
    override fun begin(onExpired: () -> Unit): UIBackgroundTaskIdentifier =
        UIApplication.sharedApplication.beginBackgroundTaskWithName(
            taskName = "com.repz.app.sync.triggered",
            expirationHandler = onExpired,
        )

    override fun end(identifier: UIBackgroundTaskIdentifier) {
        if (identifier != UIBackgroundTaskInvalid) {
            UIApplication.sharedApplication.endBackgroundTask(identifier)
        }
    }
}
