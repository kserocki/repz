package com.repz.app.core.sync.schedule

import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_monitor_t
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_main_queue

/**
 * Wraps `nw_path_monitor` (Network.framework) behind a seam [IosSyncScheduler] can drive with a
 * fake: a real path transition can't be triggered from a unit test.
 */
internal interface NetworkPathConnectivity {
    /** Starts monitoring; [onPathUpdate] fires on every path change with whether it is satisfied. */
    fun start(onPathUpdate: (satisfied: Boolean) -> Unit)

    fun cancel()
}

/** The update handler runs on the main dispatch queue, serially, so no extra locking is needed. */
internal class NWPathMonitorConnectivity : NetworkPathConnectivity {
    private var monitor: nw_path_monitor_t = null

    override fun start(onPathUpdate: (Boolean) -> Unit) {
        val created = nw_path_monitor_create()
        monitor = created
        nw_path_monitor_set_update_handler(created) { path ->
            onPathUpdate(nw_path_get_status(path) == nw_path_status_satisfied)
        }
        nw_path_monitor_set_queue(created, dispatch_get_main_queue())
        nw_path_monitor_start(created)
    }

    override fun cancel() {
        nw_path_monitor_cancel(monitor)
        monitor = null
    }
}
