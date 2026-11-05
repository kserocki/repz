package com.repz.app.feature.auth.impl.domain

import com.repz.app.core.domain.auth.AuthError
import com.repz.app.core.domain.auth.AuthRepository
import com.repz.app.core.domain.model.User
import com.repz.app.core.domain.model.account.DisplayName
import com.repz.app.core.domain.model.account.EmailAddress
import com.repz.app.core.domain.model.account.Password
import com.repz.app.core.domain.result.DomainResult
import org.koin.core.annotation.Factory

@Factory
class RegisterUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        email: EmailAddress,
        password: Password,
        displayName: DisplayName,
    ): DomainResult<User, AuthError> = authRepository.register(email, password, displayName)
}
