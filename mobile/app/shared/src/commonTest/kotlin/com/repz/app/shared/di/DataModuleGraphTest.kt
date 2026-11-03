package com.repz.app.shared.di

import com.repz.app.core.domain.session.SessionRepository
import com.repz.app.shared.shell.AuthGateViewModel
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatformTools
import org.koin.plugin.module.dsl.startKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * Regression test: `DataModule`'s `SessionRepository` and `AppModule`'s `AuthGateViewModel`
 * (a `@KoinViewModel` provider function) both resolve from the real `RepzApp` graph on every
 * target, the same way [CommonModuleTest] and [NetworkModuleGraphTest] do.
 */
class DataModuleGraphTest {
    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun givenRepzApp_whenStarted_thenSessionRepositoryIsASingleton() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertSame(koin.get<SessionRepository>(), koin.get<SessionRepository>())
    }

    @Test
    fun givenRepzApp_whenStarted_thenAuthGateViewModelResolves() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertNotNull(koin.get<AuthGateViewModel>())
    }
}
