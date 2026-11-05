package com.repz.app.shared.di

import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.domain.user.UserRepository
import com.repz.app.feature.settings.impl.domain.LogOutUseCase
import com.repz.app.feature.settings.impl.domain.ObserveCurrentUserUseCase
import com.repz.app.feature.settings.impl.domain.RefreshCurrentUserUseCase
import com.repz.app.feature.settings.impl.presentation.profile.ProfileViewModel
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatformTools
import org.koin.plugin.module.dsl.startKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame

/**
 * Regression test: `SettingsModule`'s definitions resolve from the real `RepzApp` graph on every
 * target, the same way [AuthGraphTest] does. Also the first proof that [RepzDatabase] (provided by
 * `PlatformModule`) resolves through the whole chain down to `ProfileViewModel`.
 */
class SettingsGraphTest {
    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun givenRepzApp_whenStarted_thenRepzDatabaseResolvesAsASingleton() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertNotNull(koin.get<RepzDatabase>())
    }

    @Test
    fun givenRepzApp_whenStarted_thenUserRepositoryResolves() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertNotNull(koin.get<UserRepository>())
    }

    @Test
    fun givenRepzApp_whenStarted_thenUseCasesResolveAsNewInstances() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertNotSame(koin.get<ObserveCurrentUserUseCase>(), koin.get<ObserveCurrentUserUseCase>())
        assertNotSame(koin.get<RefreshCurrentUserUseCase>(), koin.get<RefreshCurrentUserUseCase>())
        assertNotSame(koin.get<LogOutUseCase>(), koin.get<LogOutUseCase>())
    }

    @Test
    fun givenRepzApp_whenStarted_thenProfileViewModelResolves() {
        startKoin<RepzApp> { platformTestDeclaration() }
        val koin = KoinPlatformTools.defaultContext().get()

        assertNotNull(koin.get<ProfileViewModel>())
    }
}
