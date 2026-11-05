package com.repz.app.feature.auth.impl.domain

import com.repz.app.core.domain.auth.AuthError
import com.repz.app.core.domain.auth.AuthRepository
import com.repz.app.core.domain.model.User
import com.repz.app.core.domain.model.account.EmailAddress
import com.repz.app.core.domain.model.account.Password
import com.repz.app.core.domain.result.DomainResult
import org.koin.core.annotation.Factory

@Factory
class LogInUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        email: EmailAddress,
        password: Password,
    ): DomainResult<User, AuthError> = authRepository.logIn(email, password)
}
