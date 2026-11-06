package com.repz.app.shared.di

import com.repz.app.core.domain.sync.SyncMonitor
import com.repz.app.core.domain.sync.SyncScheduler
import com.repz.app.core.sync.engine.SyncEngine
import com.repz.app.core.sync.schedule.BackgroundSyncScheduler
import com.repz.app.core.sync.schedule.SyncCoordinator
import com.repz.app.core.sync.status.SyncStatusTracker
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatformTools
import org.koin.plugin.module.dsl.startKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * Regression test: `core:sync`'s [SyncModule][com.repz.app.core.sync.SyncModule] bindings and the
 * platform `PlatformModule`'s scheduler provider resolve together from the real `RepzApp` graph on
 * every target (ADR-0027), the same way [RepositoryGraphTest] and [DomainModuleGraphTest] do for
 * their own modules.
 */
class SyncGraphTest {
    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun givenRepzApp_whenStarted_thenSyncSchedulerIsTheSameInstanceAsBackgroundSyncScheduler() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertSame(koin.get<SyncScheduler>(), koin.get<BackgroundSyncScheduler>())
    }

    @Test
    fun givenRepzApp_whenStarted_thenSyncMonitorIsTheSameInstanceAsSyncStatusTracker() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertSame(koin.get<SyncMonitor>(), koin.get<SyncStatusTracker>())
    }

    @Test
    fun givenRepzApp_whenStarted_thenSyncEngineResolves() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertNotNull(koin.get<SyncEngine>())
    }

    @Test
    fun givenRepzApp_whenStarted_thenSyncCoordinatorResolves() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertNotNull(koin.get<SyncCoordinator>())
    }
}
