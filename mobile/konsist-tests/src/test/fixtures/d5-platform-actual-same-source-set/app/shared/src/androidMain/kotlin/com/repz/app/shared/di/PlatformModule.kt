package com.repz.app.shared.di

import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
actual class PlatformModule {
    @Single
    fun secureStringStorage(): SecureStringStorage = AndroidSecureStringStorage()
}

class AndroidSecureStringStorage : SecureStringStorage
