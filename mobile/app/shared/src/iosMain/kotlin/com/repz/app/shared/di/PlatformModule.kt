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
     * (what `app:shared`'s shell and the offline-first repositories ask for), so it has one owner
     * for both.
     *
     * [engine] is [Lazy] rather than a direct [SyncEngine] to break a real cycle the compiler
     * plugin's circular-dependency check caught: `SyncEngine` depends on `PersonalRecordsRefresher`
     * (`PersonalRecordsRefresherAdapter`), whose recompute use cases depend on an offline-first
     * workout repository, which depends on [SyncScheduler] itself to wake a sync after a local
     * write. `IosSyncScheduler` only calls `engine.value` from inside [IosSyncScheduler.start]'s
     * triggered runs, never at construction, so deferring the lookup is exactly what the compiler
     * plugin's own diagnostic recommends and changes no runtime behaviour. Android's
     * `AndroidSyncScheduler` never took a `SyncEngine` dependency, so it never had this cycle.
     */
    @Single(binds = [SyncScheduler::class, BackgroundSyncScheduler::class])
    fun syncScheduler(
        engine: Lazy<SyncEngine>,
        applicationScope: CoroutineScope,
    ): BackgroundSyncScheduler = IosSyncScheduler({ engine.value.sync() }, applicationScope)
}
