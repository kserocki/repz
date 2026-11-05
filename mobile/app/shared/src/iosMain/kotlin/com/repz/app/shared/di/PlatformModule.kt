package com.repz.app.shared.di

import com.repz.app.core.common.coroutines.AppDispatchers
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.buildRepzDatabase
import com.repz.app.core.database.repzDatabaseBuilder
import com.repz.app.core.network.session.KeychainSecureStringStorage
import com.repz.app.core.network.session.SecureStringStorage
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
actual class PlatformModule {
    @Single
    fun secureStringStorage(dispatchers: AppDispatchers): SecureStringStorage = KeychainSecureStringStorage(dispatchers)

    @Single
    fun repzDatabase(dispatchers: AppDispatchers): RepzDatabase = repzDatabaseBuilder().buildRepzDatabase(dispatchers.io)
}
