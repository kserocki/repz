package com.repz.app.feature.settings.impl.domain

import com.repz.app.core.domain.model.User
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.user.UserRefreshError
import com.repz.app.core.domain.user.UserRepository
import org.koin.core.annotation.Factory

@Factory
class RefreshCurrentUserUseCase(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(): DomainResult<User, UserRefreshError> = userRepository.refreshCurrentUser()
}
