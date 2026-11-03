package com.repz.app.shared.di

import com.repz.app.core.domain.session.SessionRepository
import com.repz.app.shared.shell.AuthGate
import kotlinx.coroutines.CoroutineScope
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/**
 * Scans `com.repz.app.shared` for annotated definitions (`@Single`, `@Factory`, `@KoinViewModel`).
 * Every future `@Module` contributed by a core or feature module is added to [RepzApp]'s list.
 */
@Module
@ComponentScan("com.repz.app.shared")
class AppModule {
    @Single
    fun authGate(
        sessionRepository: SessionRepository,
        applicationScope: CoroutineScope,
    ): AuthGate = AuthGate(sessionRepository, applicationScope)
}
