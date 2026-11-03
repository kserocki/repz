package com.repz.app.core.network.session

/**
 * The one platform-specific piece of session persistence: a single named secret string, backed by
 * Android Keystore (AES-GCM) or the iOS Keychain. No caching here; [PersistentTokenStore] holds the
 * in-memory copy.
 */
interface SecureStringStorage {
    /** `null` when nothing is stored, or when the stored value can't be decrypted (its entry is then deleted). */
    suspend fun read(): String?

    suspend fun write(value: String): Boolean

    suspend fun delete()
}
