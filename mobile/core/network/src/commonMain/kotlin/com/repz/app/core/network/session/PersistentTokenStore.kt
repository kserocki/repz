package com.repz.app.core.network.session

import com.repz.app.core.network.client.NetworkJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

private const val PERSISTED_TOKENS_VERSION = 1

/** One JSON string holding both tokens, so they're always written and read as a single atomic entry. */
@Serializable
private data class PersistedTokens(
    val version: Int,
    val accessToken: String,
    val refreshToken: String,
)

class PersistentTokenStore(
    private val storage: SecureStringStorage,
) : TokenStore {
    override suspend fun load(): AuthTokens? {
        val raw = storage.read() ?: return null
        val persisted = decodeOrNull(raw)
        return if (persisted == null || persisted.version != PERSISTED_TOKENS_VERSION) {
            storage.delete()
            null
        } else {
            AuthTokens(AccessToken(persisted.accessToken), RefreshToken(persisted.refreshToken))
        }
    }

    override suspend fun save(tokens: AuthTokens): TokenSaveResult {
        val persisted =
            PersistedTokens(
                version = PERSISTED_TOKENS_VERSION,
                accessToken = tokens.accessToken.value,
                refreshToken = tokens.refreshToken.value,
            )
        val raw = NetworkJson.encodeToString(PersistedTokens.serializer(), persisted)
        return if (storage.write(raw)) TokenSaveResult.Saved else TokenSaveResult.Failed
    }

    override suspend fun clear() {
        storage.delete()
    }

    // The stored value is deliberately discarded on any decode failure (corrupt or old-format
    // data signs the session out cleanly, by design), so there is nothing to do with `e` here.
    @Suppress("SwallowedException")
    private fun decodeOrNull(raw: String): PersistedTokens? =
        try {
            NetworkJson.decodeFromString(PersistedTokens.serializer(), raw)
        } catch (e: SerializationException) {
            null
        }
}
