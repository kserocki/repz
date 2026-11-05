package com.repz.app.core.data.user

import com.repz.app.core.database.account.CurrentUserDao
import com.repz.app.core.domain.model.User
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.user.UserRefreshError
import com.repz.app.core.domain.user.UserRepository
import com.repz.app.core.network.api.UserApi
import com.repz.app.core.network.result.NetworkResult
import com.repz.app.core.network.session.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room `current_user` is the source of truth (rule 6); [refreshCurrentUser] only ever improves it,
 * never clears it on failure. The refresh token is read before and after the network call: if it
 * changed (logout clears it, a new sign-in rotates it) the session this response belongs to is no
 * longer the current one, so the row write is dropped. Without this, a `GET /me` response that is
 * still in flight when the user signs out could re-insert the previous account's row after logout,
 * or overwrite the next account's freshly stored row.
 */
class OfflineFirstUserRepository(
    private val userApi: UserApi,
    private val currentUserDao: CurrentUserDao,
    private val sessionManager: SessionManager,
) : UserRepository {
    override fun observeCurrentUser(): Flow<User?> = currentUserDao.observe().map { it?.toDomain() }

    override suspend fun refreshCurrentUser(): DomainResult<User, UserRefreshError> {
        val refreshTokenAtRequestStart = sessionManager.currentRefreshToken()
        return when (val result = userApi.getMe()) {
            is NetworkResult.Success -> {
                if (sessionManager.currentRefreshToken() == refreshTokenAtRequestStart) {
                    currentUserDao.replace(result.value.toCurrentUserEntity())
                }
                DomainResult.Success(result.value.toDomain())
            }

            is NetworkResult.Failure -> {
                DomainResult.Failure(result.error.toUserRefreshError())
            }
        }
    }
}
