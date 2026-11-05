package com.repz.app.feature.auth.impl.di

import com.repz.app.feature.auth.impl.domain.AuthRepository
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

// A leftover explicit provider for the same AuthRepository that OfflineFirstAuthRepository now
// binds via an explicit `binds = [...]` list - two owners for one definition.
@Module
class LegacyAuthModule {
    @Single
    fun authRepository(): AuthRepository = LegacyAuthRepositoryImpl()
}

class LegacyAuthRepositoryImpl : AuthRepository
