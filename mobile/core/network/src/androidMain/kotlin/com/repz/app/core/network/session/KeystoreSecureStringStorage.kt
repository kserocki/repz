package com.repz.app.core.network.session

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.repz.app.core.common.coroutines.AppDispatchers
import kotlinx.coroutines.withContext
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.ProviderException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// internal, not private: KeystoreSecureStringStorageTest (androidDeviceTest) corrupts the stored
// entry and invalidates the Keystore key directly to exercise the two recovery paths below.
internal const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
internal const val KEY_ALIAS = "repz.session.v1"
internal const val PREFERENCES_FILE_NAME = "repz_secure_session"
internal const val PREFERENCES_KEY = "tokens"
private const val GCM_TAG_LENGTH_BITS = 128
private const val IV_CIPHERTEXT_DELIMITER = "."

// Equivalent to "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}",
// written as a literal so it can be a compile-time `const val`.
private const val TRANSFORMATION = "AES/GCM/NoPadding"

/**
 * Persists the one session secret (access + refresh tokens as a single JSON string, see
 * [PersistentTokenStore]) behind an AES-256-GCM key in the Android Keystore, alias [KEY_ALIAS].
 * No EncryptedSharedPreferences (deprecated since `security-crypto` 1.1.0-alpha07, no Tink
 * dependency pulled in for one string): the IV and ciphertext are the only things that ever leave
 * the Keystore boundary, stored as `base64(iv).base64(ciphertext)` in a private `SharedPreferences`
 * file. The key requires no user authentication, so a background sync can read it.
 */
class KeystoreSecureStringStorage(
    private val context: Context,
    private val dispatchers: AppDispatchers,
) : SecureStringStorage {
    private val preferences: SharedPreferences
        get() = context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)

    // A corrupted or key-less entry (AEADBadTagException, KeyPermanentlyInvalidatedException, or a
    // restored entry whose Keystore key never migrates to this device) signs the session out
    // cleanly instead of crashing: delete both the entry and the key, so a later write starts clean.
    // A `ProviderException` (an unchecked RuntimeException the Keystore provider throws for an
    // internal keystore2/HAL failure, e.g. from `Cipher.init`/`doFinal`) is handled the same way.
    @Suppress("SwallowedException")
    override suspend fun read(): String? =
        withContext(dispatchers.io) {
            val stored = preferences.getString(PREFERENCES_KEY, null) ?: return@withContext null
            try {
                decrypt(stored)
            } catch (e: GeneralSecurityException) {
                deleteEntryAndKey()
                null
            } catch (e: ProviderException) {
                deleteEntryAndKey()
                null
            }
        }

    // A key-generation or encryption failure (e.g. the Keystore is temporarily unavailable, or a
    // ProviderException from an internal keystore2/HAL failure) just fails this write;
    // `SessionManager` keeps the tokens in memory for the rest of the process.
    @Suppress("SwallowedException")
    override suspend fun write(value: String): Boolean =
        withContext(dispatchers.io) {
            try {
                val encrypted = encrypt(value)
                preferences.edit().putString(PREFERENCES_KEY, encrypted).commit()
            } catch (e: GeneralSecurityException) {
                false
            } catch (e: ProviderException) {
                false
            }
        }

    override suspend fun delete() {
        withContext(dispatchers.io) {
            preferences.edit().remove(PREFERENCES_KEY).commit()
        }
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, existingSecretKey() ?: generateSecretKey())
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return encodeEntry(cipher.iv, ciphertext)
    }

    private fun decrypt(stored: String): String {
        val (iv, ciphertext) = decodeEntry(stored)
        val key = existingSecretKey() ?: throw GeneralSecurityException("No Keystore entry for alias $KEY_ALIAS")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    private fun encodeEntry(
        iv: ByteArray,
        ciphertext: ByteArray,
    ): String =
        Base64.encodeToString(iv, Base64.NO_WRAP) + IV_CIPHERTEXT_DELIMITER +
            Base64.encodeToString(ciphertext, Base64.NO_WRAP)

    private fun decodeEntry(stored: String): Pair<ByteArray, ByteArray> {
        val delimiterIndex = stored.indexOf(IV_CIPHERTEXT_DELIMITER)
        if (delimiterIndex < 0) throw GeneralSecurityException("Malformed secure entry: no delimiter")
        return try {
            val iv = Base64.decode(stored.substring(0, delimiterIndex), Base64.NO_WRAP)
            val ciphertext = Base64.decode(stored.substring(delimiterIndex + 1), Base64.NO_WRAP)
            iv to ciphertext
        } catch (e: IllegalArgumentException) {
            throw GeneralSecurityException("Malformed secure entry: invalid base64", e)
        }
    }

    private fun existingSecretKey(): SecretKey? {
        val entry = androidKeyStore().getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return entry?.secretKey
    }

    private fun generateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE_PROVIDER)
        val spec =
            KeyGenParameterSpec
                .Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(false)
                .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    // The Keystore key may itself be the corrupted part (an invalidated or unreadable key), so a
    // failure to delete it here is not escalated: the SharedPreferences entry above is already
    // gone, which is what actually matters for signing the session out.
    @Suppress("SwallowedException")
    private fun deleteEntryAndKey() {
        preferences.edit().remove(PREFERENCES_KEY).commit()
        try {
            androidKeyStore().deleteEntry(KEY_ALIAS)
        } catch (e: GeneralSecurityException) {
            // No-op: see the function comment above.
        }
    }

    private fun androidKeyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply { load(null) }
}
