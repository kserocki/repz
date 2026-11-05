package com.repz.app.feature.auth.impl.di

import com.repz.app.core.domain.auth.AuthRepository
import com.repz.app.feature.auth.impl.domain.LogInUseCase
import com.repz.app.feature.auth.impl.domain.RegisterUseCase
import com.repz.app.feature.auth.impl.presentation.login.LoginViewModel
import com.repz.app.feature.auth.impl.presentation.register.RegisterViewModel
import org.koin.core.annotation.Factory
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Module

@Module
class AuthModule {
    @Factory
    fun logInUseCase(authRepository: AuthRepository): LogInUseCase = LogInUseCase(authRepository)

    @Factory
    fun registerUseCase(authRepository: AuthRepository): RegisterUseCase = RegisterUseCase(authRepository)

    @KoinViewModel
    fun loginViewModel(logIn: LogInUseCase): LoginViewModel = LoginViewModel(logIn)

    @KoinViewModel
    fun registerViewModel(register: RegisterUseCase): RegisterViewModel = RegisterViewModel(register)
}
