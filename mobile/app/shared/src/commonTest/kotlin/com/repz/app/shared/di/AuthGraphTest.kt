package com.repz.app.shared.di

import com.repz.app.core.domain.auth.AuthRepository
import com.repz.app.feature.auth.impl.domain.LogInUseCase
import com.repz.app.feature.auth.impl.domain.RegisterUseCase
import com.repz.app.feature.auth.impl.presentation.login.LoginViewModel
import com.repz.app.feature.auth.impl.presentation.register.RegisterViewModel
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatformTools
import org.koin.plugin.module.dsl.startKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame

/**
 * Regression test: `AuthModule`'s definitions resolve from the real `RepzApp` graph on every
 * target, the same way [DataModuleGraphTest] and [NetworkModuleGraphTest] do. `LoginViewModel` and
 * `RegisterViewModel` are the project's first real class-level `@KoinViewModel` definitions, so
 * this also verifies that shape works end to end (ADR-0008), not only that the Koin compiler
 * plugin accepts it at compile time.
 */
class AuthGraphTest {
    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun givenRepzApp_whenStarted_thenAuthRepositoryResolves() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertNotNull(koin.get<AuthRepository>())
    }

    @Test
    fun givenRepzApp_whenStarted_thenUseCasesResolveAsNewInstances() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertNotSame(koin.get<LogInUseCase>(), koin.get<LogInUseCase>())
        assertNotSame(koin.get<RegisterUseCase>(), koin.get<RegisterUseCase>())
    }

    @Test
    fun givenRepzApp_whenStarted_thenLoginViewModelResolves() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertNotNull(koin.get<LoginViewModel>())
    }

    @Test
    fun givenRepzApp_whenStarted_thenRegisterViewModelResolves() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertNotNull(koin.get<RegisterViewModel>())
    }
}
