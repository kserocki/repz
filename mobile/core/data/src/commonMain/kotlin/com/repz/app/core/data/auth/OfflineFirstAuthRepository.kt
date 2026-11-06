package com.repz.app.core.data.auth

import co.touchlab.kermit.Logger
import com.repz.app.core.common.coroutines.suspendRunCatching
import com.repz.app.core.data.user.toCurrentUserEntity
import com.repz.app.core.data.user.toDomain
import com.repz.app.core.database.account.CurrentUserDao
import com.repz.app.core.datastore.UserPreferencesDataSource
import com.repz.app.core.domain.auth.AuthError
import com.repz.app.core.domain.auth.AuthRepository
import com.repz.app.core.domain.model.User
import com.repz.app.core.domain.model.account.DisplayName
import com.repz.app.core.domain.model.account.EmailAddress
import com.repz.app.core.domain.model.account.Password
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.network.api.AuthApi
import com.repz.app.core.network.dto.auth.AuthSessionDto
import com.repz.app.core.network.dto.auth.LoginRequestDto
import com.repz.app.core.network.dto.auth.RegisterRequestDto
import com.repz.app.core.network.result.NetworkResult
import com.repz.app.core.network.session.SessionManager
import com.repz.app.core.network.session.toAuthTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

/**
 * Auth is online-only: register/login always call the API, no local fallback. The `OfflineFirst`
 * prefix is this module's naming convention for every `*Repository` implementation (Konsist N5),
 * not a claim that this one works offline. A failed token save still signs this process in
 * (`SessionManager.start` keeps the session in memory and logs the failure itself).
 *
 * [logOut] is offline-first by design (ADR-0011 amendment): the session ends and the cached
 * profile is wiped on this device at once, in [NonCancellable] steps so the auth gate's epoch
 * bump can't cut them short, and the server-side revoke is a best-effort, fire-and-forget call on
 * [applicationScope]. It also clears the device unit preference (Q1/ADR-0026): that preference
 * belongs to the account, so the next account falls back to its own server value. Clearing that
 * preference is itself best-effort: a DataStore [okio.IOException] there is logged and swallowed
 * (Konsist E1) rather than thrown, so it can never skip the server-side revoke below it.
 *
 * [logger] defaults to a tagged [Logger] (ADR-0008): with the Koin compiler plugin's default
 * `skipDefaultValues = true`, a non-null, unqualified parameter with a default isn't injected, so
 * this stays a plain class-level `@Single` instead of a provider function just for the tag.
 */
@Single
class OfflineFirstAuthRepository(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager,
    private val currentUserDao: CurrentUserDao,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val applicationScope: CoroutineScope,
    private val logger: Logger = Logger.withTag(AUTH_LOG_TAG),
) : AuthRepository {
    override suspend fun register(
        email: EmailAddress,
        password: Password,
        displayName: DisplayName,
    ): DomainResult<User, AuthError> = signIn { authApi.register(RegisterRequestDto(email.value, password.value, displayName.value)) }

    override suspend fun logIn(
        email: EmailAddress,
        password: Password,
    ): DomainResult<User, AuthError> = signIn { authApi.login(LoginRequestDto(email.value, password.value)) }

    override suspend fun logOut() {
        withContext(NonCancellable) {
            val refreshToken = sessionManager.currentRefreshToken()
            sessionManager.end()
            currentUserDao.clear()
            clearDeviceUnitPreference()

            // Launched while still NonCancellable, so the call is made even if the caller (e.g. a
            // ViewModel cleared by the auth gate's epoch bump) is cancelled the instant this
            // withContext block returns. applicationScope itself then carries the revoke to completion.
            if (refreshToken != null) {
                applicationScope.launch {
                    when (val result = authApi.logout(refreshToken)) {
                        is NetworkResult.Success -> Unit
                        is NetworkResult.Failure -> logger.w { "logout revoke failed: ${result.error::class.simpleName}" }
                    }
                }
            }
        }
    }

    /**
     * A failed clear (DataStore's documented [okio.IOException], e.g. disk full) is logged and
     * swallowed instead of thrown, so it can never stop [logOut] from reaching the revoke launch
     * right after it.
     */
    private suspend fun clearDeviceUnitPreference() {
        suspendRunCatching { userPreferencesDataSource.setUnitSystem(null) }
            .onFailure { logger.w(it) { "Failed to clear the device unit preference on logout." } }
    }

    private suspend fun signIn(call: suspend () -> NetworkResult<AuthSessionDto>): DomainResult<User, AuthError> =
        when (val result = call()) {
            is NetworkResult.Success -> {
                // Stored before the session starts, so Profile never observes a signed-in session without a row.
                currentUserDao.replace(result.value.user.toCurrentUserEntity())
                sessionManager.start(result.value.toAuthTokens())
                DomainResult.Success(result.value.user.toDomain())
            }

            is NetworkResult.Failure -> {
                DomainResult.Failure(result.error.toAuthError())
            }
        }
}
