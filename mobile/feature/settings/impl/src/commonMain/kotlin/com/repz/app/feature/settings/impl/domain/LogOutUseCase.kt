package com.repz.app.feature.settings.impl.domain

import com.repz.app.core.domain.auth.AuthRepository
import org.koin.core.annotation.Factory

@Factory
class LogOutUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke() = authRepository.logOut()
}
