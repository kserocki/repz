package com.repz.app.feature.settings.impl.domain

import com.repz.app.core.domain.model.User
import com.repz.app.core.domain.user.UserRepository
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory
class ObserveCurrentUserUseCase(
    private val userRepository: UserRepository,
) {
    operator fun invoke(): Flow<User?> = userRepository.observeCurrentUser()
}
