package com.repz.app.feature.settings.impl.domain

import com.repz.app.core.domain.auth.AuthRepository

class LogOutUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke() = authRepository.logOut()
}
