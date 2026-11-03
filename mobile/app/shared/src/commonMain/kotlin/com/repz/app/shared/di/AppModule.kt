package com.repz.app.shared.di

import com.repz.app.core.domain.session.SessionRepository
import com.repz.app.shared.shell.AuthGateViewModel
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Module

/**
 * Scans `com.repz.app.shared` for annotated definitions (`@Single`, `@Factory`, `@KoinViewModel`).
 * Every future `@Module` contributed by a core or feature module is added to [RepzApp]'s list.
 */
@Module
@ComponentScan("com.repz.app.shared")
class AppModule {
    @KoinViewModel
    fun authGateViewModel(sessionRepository: SessionRepository): AuthGateViewModel = AuthGateViewModel(sessionRepository)
}
