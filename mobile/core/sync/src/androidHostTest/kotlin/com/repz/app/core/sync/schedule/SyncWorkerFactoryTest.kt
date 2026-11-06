package com.repz.app.core.sync.schedule

import android.app.Application
import com.repz.app.core.sync.engine.SyncOutcome
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNull

class SyncWorkerFactoryTest {
    private val context = Application()
    private val factory = SyncWorkerFactory(runSync = { SyncOutcome.Completed })

    @Test
    fun givenSyncWorkersClassName_whenCreateWorkerIsCalled_thenASyncWorkerIsReturned() {
        val worker =
            factory.createWorker(
                context,
                SyncWorker::class.java.name,
                fakeWorkerParameters(),
            )

        assertIs<SyncWorker>(worker)
    }

    @Test
    fun givenAnUnknownClassName_whenCreateWorkerIsCalled_thenNullIsReturned() {
        val worker =
            factory.createWorker(
                context,
                "com.repz.app.core.sync.schedule.SomeOtherWorker",
                fakeWorkerParameters(),
            )

        assertNull(worker)
    }
}
