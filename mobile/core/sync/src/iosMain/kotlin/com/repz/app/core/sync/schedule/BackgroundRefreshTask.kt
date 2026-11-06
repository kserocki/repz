package com.repz.app.core.sync.schedule

import co.touchlab.kermit.Logger
import com.repz.app.core.sync.logging.SYNC_LOG_TAG
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.BackgroundTasks.BGAppRefreshTask
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.dateWithTimeIntervalSinceNow
import kotlin.time.Duration

/**
 * Wraps `BGTaskScheduler` behind a seam [IosSyncScheduler] can drive with a fake: `submit` always
 * fails with `.unavailable` on the iOS Simulator (ADR-0027), so a real run is only checkable
 * manually, on a device.
 */
internal interface BackgroundRefreshTask {
    /** Registers the launch handler; must run before the app finishes launching. Idempotent. */
    fun register(
        identifier: String,
        onLaunch: (BackgroundRefreshTaskRun) -> Unit,
    )

    /** Submits (or re-submits) a request to run no earlier than [earliestBeginIn] from now. */
    fun submit(
        identifier: String,
        earliestBeginIn: Duration,
    )

    fun cancel(identifier: String)
}

/** What a launch handler gets: watch for expiry, and complete the task exactly once. */
internal interface BackgroundRefreshTaskRun {
    fun setExpirationHandler(onExpired: () -> Unit)

    fun complete(success: Boolean)
}

internal class SystemBackgroundRefreshTask(
    private val logger: Logger = Logger.withTag(SYNC_LOG_TAG),
) : BackgroundRefreshTask {
    override fun register(
        identifier: String,
        onLaunch: (BackgroundRefreshTaskRun) -> Unit,
    ) {
        val registered =
            BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(identifier, usingQueue = null) { task ->
                val refreshTask = task as? BGAppRefreshTask
                if (refreshTask != null) {
                    onLaunch(SystemBackgroundRefreshTaskRun(refreshTask))
                } else {
                    logger.w { "BGTaskScheduler launched '$identifier' with an unexpected task type" }
                    task?.setTaskCompletedWithSuccess(false)
                }
            }
        if (!registered) {
            logger.w { "BGTaskScheduler.register('$identifier') failed: check Info.plist's BGTaskSchedulerPermittedIdentifiers" }
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override fun submit(
        identifier: String,
        earliestBeginIn: Duration,
    ) {
        val request = BGAppRefreshTaskRequest(identifier)
        request.earliestBeginDate = NSDate.dateWithTimeIntervalSinceNow(earliestBeginIn.inWholeSeconds.toDouble())
        memScoped {
            val errorVar = alloc<ObjCObjectVar<NSError?>>()
            val submitted = BGTaskScheduler.sharedScheduler.submitTaskRequest(request, errorVar.ptr)
            if (!submitted) {
                logger.w { "BGTaskScheduler.submit('$identifier') failed: ${errorVar.value?.localizedDescription}" }
            }
        }
    }

    override fun cancel(identifier: String) {
        BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(identifier)
    }
}

private class SystemBackgroundRefreshTaskRun(
    private val task: BGAppRefreshTask,
) : BackgroundRefreshTaskRun {
    override fun setExpirationHandler(onExpired: () -> Unit) {
        task.expirationHandler = onExpired
    }

    override fun complete(success: Boolean) {
        task.setTaskCompletedWithSuccess(success)
    }
}
