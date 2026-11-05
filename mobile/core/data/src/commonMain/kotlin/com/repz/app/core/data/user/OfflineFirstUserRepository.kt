package com.repz.app.core.data.user

import com.repz.app.core.database.account.CurrentUserDao
import com.repz.app.core.domain.model.User
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.user.UserRefreshError
import com.repz.app.core.domain.user.UserRepository
import com.repz.app.core.network.api.UserApi
import com.repz.app.core.network.result.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room `current_user` is the source of truth (rule 6); [refreshCurrentUser] only ever improves it, never clears it on failure. */
class OfflineFirstUserRepository(
    private val userApi: UserApi,
    private val currentUserDao: CurrentUserDao,
) : UserRepository {
    override fun observeCurrentUser(): Flow<User?> = currentUserDao.observe().map { it?.toDomain() }

    override suspend fun refreshCurrentUser(): DomainResult<User, UserRefreshError> =
        when (val result = userApi.getMe()) {
            is NetworkResult.Success -> {
                currentUserDao.replace(result.value.toCurrentUserEntity())
                DomainResult.Success(result.value.toDomain())
            }

            is NetworkResult.Failure -> {
                DomainResult.Failure(result.error.toUserRefreshError())
            }
        }
}
