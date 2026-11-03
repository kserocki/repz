package com.repz.app.shared.di

import com.repz.app.core.network.api.AuthApi
import com.repz.app.core.network.api.HealthApi
import com.repz.app.core.network.api.SyncApi
import com.repz.app.core.network.api.UserApi
import com.repz.app.core.network.session.SecureStringStorage
import com.repz.app.core.network.session.SessionManager
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatformTools
import org.koin.plugin.module.dsl.startKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * Regression test: `NetworkModule`'s and `PlatformModule`'s definitions resolve from the real
 * `RepzApp` graph on every target, including the iOS simulator. `startKoin<RepzApp>()` is called
 * directly (not through [startRepzKoin]) so the Koin compiler plugin's compile-time graph check
 * verifies every `get<T>()` below, the same way [CommonModuleTest] does.
 *
 * `SecureStringStorage` is only resolved here, never read from or written to: on iOS that would
 * hit the Keychain, which isn't available in a bare simulator test process (`errSecNotAvailable`).
 */
class NetworkModuleGraphTest {
    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun givenRepzApp_whenStarted_thenSessionManagerIsASingleton() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertSame(koin.get<SessionManager>(), koin.get<SessionManager>())
    }

    @Test
    fun givenRepzApp_whenStarted_thenEveryApiServiceResolves() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertNotNull(koin.get<AuthApi>())
        assertNotNull(koin.get<UserApi>())
        assertNotNull(koin.get<SyncApi>())
        assertNotNull(koin.get<HealthApi>())
    }

    @Test
    fun givenRepzApp_whenStarted_thenSecureStringStorageResolves() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertNotNull(koin.get<SecureStringStorage>())
    }
}
