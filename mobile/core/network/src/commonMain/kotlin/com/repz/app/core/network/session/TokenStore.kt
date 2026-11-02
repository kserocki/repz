package com.repz.app.core.network.session

/** Persistence only, no in-memory caching: [SessionManager] holds the in-memory copy. */
interface TokenStore {
    /** `null` when there is no session, or the stored value can't be read (it's then already deleted). */
    suspend fun load(): AuthTokens?

    suspend fun save(tokens: AuthTokens): TokenSaveResult

    suspend fun clear()
}

enum class TokenSaveResult {
    Saved,
    Failed,
}
