package com.repz.app.core.data

import co.touchlab.kermit.Logger
import com.repz.app.core.data.auth.AUTH_LOG_TAG
import com.repz.app.core.data.auth.OfflineFirstAuthRepository
import com.repz.app.core.data.session.OfflineFirstSessionRepository
import com.repz.app.core.data.user.OfflineFirstUserRepository
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.domain.auth.AuthRepository
import com.repz.app.core.domain.session.SessionRepository
import com.repz.app.core.domain.user.UserRepository
import com.repz.app.core.network.api.AuthApi
import com.repz.app.core.network.api.UserApi
import com.repz.app.core.network.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/** `core:data`'s one `@Module` (ARCH §9, konsist D3). Later repositories are added here too. */
@Module
class DataModule {
    @Single
    fun sessionRepository(
        sessionManager: SessionManager,
        applicationScope: CoroutineScope,
    ): SessionRepository = OfflineFirstSessionRepository(sessionManager, applicationScope)

    @Single
    fun authRepository(
        authApi: AuthApi,
        sessionManager: SessionManager,
        database: RepzDatabase,
        applicationScope: CoroutineScope,
    ): AuthRepository =
        OfflineFirstAuthRepository(authApi, sessionManager, database.currentUserDao(), applicationScope, Logger.withTag(AUTH_LOG_TAG))

    @Single
    fun userRepository(
        userApi: UserApi,
        database: RepzDatabase,
    ): UserRepository = OfflineFirstUserRepository(userApi, database.currentUserDao())
}
