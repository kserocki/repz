package com.repz.app.shared.di

import com.repz.app.core.common.coroutines.AppDispatchers
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.buildRepzDatabase
import com.repz.app.core.database.repzDatabaseBuilder
import com.repz.app.core.datastore.UserPreferencesDataSource
import com.repz.app.core.datastore.repzPreferencesDataStore
import com.repz.app.core.domain.sync.SyncScheduler
import com.repz.app.core.network.session.KeychainSecureStringStorage
import com.repz.app.core.network.session.SecureStringStorage
import com.repz.app.core.sync.engine.SyncEngine
import com.repz.app.core.sync.schedule.BackgroundSyncScheduler
import com.repz.app.core.sync.schedule.IosSyncScheduler
import kotlinx.coroutines.CoroutineScope
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
actual class PlatformModule {
    @Single
    fun secureStringStorage(dispatchers: AppDispatchers): SecureStringStorage = KeychainSecureStringStorage(dispatchers)

    @Single
    fun repzDatabase(dispatchers: AppDispatchers): RepzDatabase = repzDatabaseBuilder().buildRepzDatabase(dispatchers.io)

    @Single
    fun userPreferencesDataSource(dispatchers: AppDispatchers): UserPreferencesDataSource =
        UserPreferencesDataSource(repzPreferencesDataStore(dispatchers.io))

    /**
     * Reason 3 (platform-built type): [IosSyncScheduler]'s public constructor takes `runSync`, a
     * plain function value Koin cannot resolve by type, so it is built explicitly from [SyncEngine]
     * here rather than through a class-level annotation (ADR-0008). `binds` registers it under both
     * [BackgroundSyncScheduler] (what `PlatformModule`/`SyncCoordinator` ask for) and [SyncScheduler]
     * (what `app:shared`'s shell and REP-27's repositories ask for), so it has one owner for both.
     */
    @Single(binds = [SyncScheduler::class, BackgroundSyncScheduler::class])
    fun syncScheduler(
        engine: SyncEngine,
        applicationScope: CoroutineScope,
    ): BackgroundSyncScheduler = IosSyncScheduler(engine::sync, applicationScope)
}
