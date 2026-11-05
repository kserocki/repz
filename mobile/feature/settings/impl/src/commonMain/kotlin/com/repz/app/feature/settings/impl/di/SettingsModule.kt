package com.repz.app.feature.settings.impl.di

import com.repz.app.core.domain.auth.AuthRepository
import com.repz.app.core.domain.user.UserRepository
import com.repz.app.feature.settings.impl.domain.LogOutUseCase
import com.repz.app.feature.settings.impl.domain.ObserveCurrentUserUseCase
import com.repz.app.feature.settings.impl.domain.RefreshCurrentUserUseCase
import com.repz.app.feature.settings.impl.presentation.profile.ProfileViewModel
import org.koin.core.annotation.Factory
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Module

@Module
class SettingsModule {
    @Factory
    fun observeCurrentUserUseCase(userRepository: UserRepository): ObserveCurrentUserUseCase = ObserveCurrentUserUseCase(userRepository)

    @Factory
    fun refreshCurrentUserUseCase(userRepository: UserRepository): RefreshCurrentUserUseCase = RefreshCurrentUserUseCase(userRepository)

    @Factory
    fun logOutUseCase(authRepository: AuthRepository): LogOutUseCase = LogOutUseCase(authRepository)

    @KoinViewModel
    fun profileViewModel(
        observeCurrentUser: ObserveCurrentUserUseCase,
        refreshCurrentUser: RefreshCurrentUserUseCase,
        logOut: LogOutUseCase,
    ): ProfileViewModel = ProfileViewModel(observeCurrentUser, refreshCurrentUser, logOut)
}
