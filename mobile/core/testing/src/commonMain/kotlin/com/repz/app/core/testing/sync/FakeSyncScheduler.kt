package com.repz.app.core.testing.sync

import com.repz.app.core.domain.sync.SyncScheduler

/** A hand-written fake (N7): [requestSyncCallCount] counts calls to [requestSync]. */
class FakeSyncScheduler : SyncScheduler {
    var requestSyncCallCount: Int = 0
        private set

    override fun requestSync() {
        requestSyncCallCount++
    }
}
