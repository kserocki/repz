package com.repz.app.core.network.session

import co.touchlab.kermit.Logger
import com.repz.app.core.network.api.RefreshTokensCall
import com.repz.app.core.network.result.NetworkError
import com.repz.app.core.network.result.NetworkResult
import io.ktor.client.plugins.auth.providers.BearerTokens
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal fun AuthTokens.toBearerTokens(): BearerTokens = BearerTokens(accessToken.value, refreshToken.value)

/**
 * Single-flight, grace-window-safe (ADR-0011) refresh: Ktor's own [BearerTokens] holder already
 * shares one refresh among concurrent 401s, but this mutex plus the "already refreshed" check below
 * makes that guarantee hold regardless of Ktor internals and across `loadTokens` races.
 */
internal class TokenRefresher(
    private val session: SessionManager,
    private val refreshTokensCall: RefreshTokensCall,
    private val logger: Logger,
) {
    private val mutex = Mutex()

    suspend fun refresh(oldTokens: BearerTokens?): BearerTokens? =
        mutex.withLock {
            val current = session.currentTokens() ?: return@withLock null

            // Another concurrent caller already refreshed while this one was waiting for the lock.
            if (oldTokens != null && current.accessToken.value != oldTokens.accessToken) {
                return@withLock current.toBearerTokens()
            }

            when (val result = refreshTokensCall(current.refreshToken)) {
                is NetworkResult.Success -> {
                    val replaced = session.replace(current, result.value)
                    if (replaced) {
                        logger.i { "token refresh succeeded" }
                        result.value.toBearerTokens()
                    } else {
                        null
                    }
                }

                is NetworkResult.Failure -> {
                    handleRefreshFailure(result.error)
                }
            }
        }

    private suspend fun handleRefreshFailure(error: NetworkError): BearerTokens? =
        when {
            error is NetworkError.Api && (error.status == INVALID_REFRESH_TOKEN_STATUS || error.status == MALFORMED_REFRESH_STATUS) -> {
                // Any 401 on refresh - reuse included, the server doesn't distinguish - signs the session out.
                logger.w { "session expired: refresh token rejected" }
                session.expire()
                null
            }

            else -> {
                logger.w { "token refresh unavailable: ${error::class.simpleName}" }
                throw RefreshUnavailableException(error)
            }
        }

    private companion object {
        const val INVALID_REFRESH_TOKEN_STATUS = 401
        const val MALFORMED_REFRESH_STATUS = 400
    }
}
