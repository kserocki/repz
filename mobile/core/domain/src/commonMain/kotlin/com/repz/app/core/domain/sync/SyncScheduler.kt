package com.repz.app.core.domain.sync

/** Asks the platform to run a sync soon; fire-and-forget, safe to call often (ARCH §5.6). */
interface SyncScheduler {
    fun requestSync()
}
