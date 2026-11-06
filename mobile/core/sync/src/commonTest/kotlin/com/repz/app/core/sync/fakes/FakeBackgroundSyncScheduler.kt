package com.repz.app.core.sync.fakes

import com.repz.app.core.sync.schedule.BackgroundSyncScheduler

internal class FakeBackgroundSyncScheduler : BackgroundSyncScheduler {
    var requestSyncCallCount = 0
        private set
    var startCallCount = 0
        private set
    var schedulePeriodicCallCount = 0
        private set
    var cancelAllCallCount = 0
        private set

    override fun requestSync() {
        requestSyncCallCount++
    }

    override fun start() {
        startCallCount++
    }

    override fun schedulePeriodic() {
        schedulePeriodicCallCount++
    }

    override fun cancelAll() {
        cancelAllCallCount++
    }
}
