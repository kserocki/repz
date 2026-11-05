package com.repz.app.core.testing.user

import com.repz.app.core.domain.model.User
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.user.UserRefreshError
import com.repz.app.core.domain.user.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** A hand-written fake (N7). [emit] drives [observeCurrentUser]; [refreshResult] is returned as-is from [refreshCurrentUser]. */
class FakeUserRepository(
    initial: User? = null,
    var refreshResult: DomainResult<User, UserRefreshError> = DomainResult.Failure(UserRefreshError.Unexpected),
) : UserRepository {
    private val currentUser = MutableStateFlow(initial)

    var refreshCallCount: Int = 0
        private set

    override fun observeCurrentUser(): Flow<User?> = currentUser

    override suspend fun refreshCurrentUser(): DomainResult<User, UserRefreshError> {
        refreshCallCount++
        return refreshResult
    }

    /** Simulates the Room row changing, e.g. after login or logout. */
    fun emit(user: User?) {
        currentUser.value = user
    }
}
