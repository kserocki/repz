package com.repz.app.core.network.session

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The one in-memory copy of the current session: the access-token cache the bearer plugin reads
 * from ([TokenRefresher], `loadTokens`), and the [status] the auth gate (REP-28) and the rest of the
 * app observe. A `StateFlow`, not a one-shot event, so a late collector still sees `SignedOut(Expired)`.
 */
class SessionManager(
    private val tokenStore: TokenStore,
    private val logger: Logger,
) {
    private val mutex = Mutex()
    private var tokens: AuthTokens? = null

    private val mutableStatus = MutableStateFlow<SessionStatus>(SessionStatus.Unknown)
    val status: StateFlow<SessionStatus> = mutableStatus.asStateFlow()

    /** Loads the stored session once into memory; calling it again is a no-op that just returns the current status. */
    suspend fun restore(): SessionStatus =
        mutex.withLock {
            val alreadyRestored = mutableStatus.value
            if (alreadyRestored != SessionStatus.Unknown) return@withLock alreadyRestored

            val loaded = tokenStore.load()
            tokens = loaded
            val restoredStatus = if (loaded != null) SessionStatus.SignedIn else SessionStatus.SignedOut(SignOutReason.NoSession)
            mutableStatus.value = restoredStatus
            restoredStatus
        }

    /** After a successful login/register (REP-27/30). The session is usable for this process even when persisting it fails. */
    suspend fun start(newTokens: AuthTokens): TokenSaveResult =
        mutex.withLock {
            tokens = newTokens
            mutableStatus.value = SessionStatus.SignedIn
            val saveResult = tokenStore.save(newTokens)
            if (saveResult == TokenSaveResult.Failed) {
                logger.w { "session token save failed, keeping the session in memory for this process" }
            }
            saveResult
        }

    /** A user-initiated logout. */
    suspend fun end() =
        mutex.withLock {
            tokens = null
            tokenStore.clear()
            mutableStatus.value = SessionStatus.SignedOut(SignOutReason.LoggedOut)
        }

    /** For [com.repz.app.core.network.api.AuthApi.logout], which invalidates the refresh token server-side too. */
    suspend fun currentRefreshToken(): RefreshToken? = mutex.withLock { tokens?.refreshToken }

    internal suspend fun currentTokens(): AuthTokens? = mutex.withLock { tokens }

    /** Compare-and-set: fails (and discards [refreshed]) when a logout happened while the refresh call was in flight. */
    internal suspend fun replace(
        expected: AuthTokens,
        refreshed: AuthTokens,
    ): Boolean =
        mutex.withLock {
            if (tokens != expected) return@withLock false
            tokens = refreshed
            tokenStore.save(refreshed)
            true
        }

    /** The refresh token was rejected (expired or reused): the session ends, regardless of how it started. */
    internal suspend fun expire() =
        mutex.withLock {
            tokens = null
            tokenStore.clear()
            mutableStatus.value = SessionStatus.SignedOut(SignOutReason.Expired)
        }
}
