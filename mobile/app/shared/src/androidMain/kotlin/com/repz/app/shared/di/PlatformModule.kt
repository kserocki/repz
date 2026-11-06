package com.repz.app.shared.di

import android.content.Context
import com.repz.app.core.common.coroutines.AppDispatchers
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.buildRepzDatabase
import com.repz.app.core.database.repzDatabaseBuilder
import com.repz.app.core.datastore.UserPreferencesDataSource
import com.repz.app.core.datastore.repzPreferencesDataStore
import com.repz.app.core.domain.sync.SyncScheduler
import com.repz.app.core.network.session.KeystoreSecureStringStorage
import com.repz.app.core.network.session.SecureStringStorage
import com.repz.app.core.sync.schedule.AndroidSyncScheduler
import com.repz.app.core.sync.schedule.BackgroundSyncScheduler
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
actual class PlatformModule {
    @Single
    fun secureStringStorage(
        context: Context,
        dispatchers: AppDispatchers,
    ): SecureStringStorage = KeystoreSecureStringStorage(context, dispatchers)

    @Single
    fun repzDatabase(
        context: Context,
        dispatchers: AppDispatchers,
    ): RepzDatabase = repzDatabaseBuilder(context).buildRepzDatabase(dispatchers.io)

    @Single
    fun userPreferencesDataSource(
        context: Context,
        dispatchers: AppDispatchers,
    ): UserPreferencesDataSource = UserPreferencesDataSource(repzPreferencesDataStore(context, dispatchers.io))

    /**
     * Reason 3 (platform-built type): [AndroidSyncScheduler]'s public constructor takes a plain
     * `Context`, which `PlatformModule`'s S1 pattern never puts on an `actual class` itself (ADR-0008),
     * so it is built explicitly here. `binds` registers it under both [BackgroundSyncScheduler] (what
     * `SyncCoordinator` asks for) and [SyncScheduler] (what `app:shared`'s shell and the offline-first
     * repositories ask for), so it has one owner for both.
     */
    @Single(binds = [SyncScheduler::class, BackgroundSyncScheduler::class])
    fun syncScheduler(context: Context): BackgroundSyncScheduler = AndroidSyncScheduler(context)
}
