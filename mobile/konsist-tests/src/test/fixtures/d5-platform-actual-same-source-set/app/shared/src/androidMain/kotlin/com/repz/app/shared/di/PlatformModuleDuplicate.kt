package com.repz.app.shared.di

import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

// Simulates an accidental second `actual` for the same `expect class PlatformModule` landing in
// the same source set (e.g. a bad merge) - D5 must still flag this, unlike the legitimate one
// `actual` per platform case (see the d5-allowed-platform-actual fixture).
@Module
actual class PlatformModule {
    @Single
    fun secureStringStorage(): SecureStringStorage = OtherAndroidSecureStringStorage()
}

class OtherAndroidSecureStringStorage : SecureStringStorage
