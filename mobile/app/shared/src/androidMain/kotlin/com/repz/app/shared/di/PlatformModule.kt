package com.repz.app.shared.di

import android.content.Context
import com.repz.app.core.common.coroutines.AppDispatchers
import com.repz.app.core.network.session.KeystoreSecureStringStorage
import com.repz.app.core.network.session.SecureStringStorage
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
actual class PlatformModule {
    @Single
    fun secureStringStorage(
        context: Context,
        dispatchers: AppDispatchers,
    ): SecureStringStorage = KeystoreSecureStringStorage(context, dispatchers)
}
