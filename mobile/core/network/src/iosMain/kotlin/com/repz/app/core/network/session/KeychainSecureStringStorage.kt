package com.repz.app.core.network.session

import co.touchlab.kermit.Logger
import com.repz.app.core.common.coroutines.AppDispatchers
import com.repz.app.core.network.client.SESSION_LOG_TAG
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.withContext
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

private const val KEYCHAIN_SERVICE = "com.repz.app.session"
private const val KEYCHAIN_ACCOUNT = "tokens"
internal const val INSTALL_MARKER_DEFAULTS_KEY = "repz.session.installMarker"

/**
 * One `kSecClassGenericPassword` item (fixed service/account above) holds the whole persisted token
 * JSON string as a single atomic entry; [PersistentTokenStore] owns that JSON shape.
 * `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` keeps it readable by a background refresh
 * after the first unlock, and it is never included in an iCloud backup or migrated to another
 * device.
 *
 * The Keychain survives an app deletion (unlike `NSUserDefaults`), so on the first access of a
 * process ([read], [write] or [delete]), an absent [INSTALL_MARKER_DEFAULTS_KEY] flag means this is
 * a fresh install: the stale item is wiped before anything else happens, so a reinstall always
 * starts signed out.
 *
 * The query is assembled through Core Foundation calls directly, not a bridged Kotlin `Map`: the
 * `kSec*` constants are `CFStringRef` pointers, and bridging a `Map` boxes them as Kotlin objects on
 * the way in, which makes every Security call fail with `errSecParam` (-50).
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class KeychainSecureStringStorage(
    private val dispatchers: AppDispatchers,
) : SecureStringStorage {
    private val logger = Logger.withTag(SESSION_LOG_TAG)
    private var reinstallChecked = false

    override suspend fun read(): String? =
        withContext(dispatchers.io) {
            clearOnReinstallOnce()
            readTokenData()?.let { data -> NSString.create(data, NSUTF8StringEncoding)?.toString() }
        }

    override suspend fun write(value: String): Boolean =
        withContext(dispatchers.io) {
            clearOnReinstallOnce()
            val data =
                requireNotNull(NSString.create(string = value).dataUsingEncoding(NSUTF8StringEncoding)) {
                    "Failed to UTF-8 encode the persisted token JSON"
                }
            writeTokenData(data)
        }

    override suspend fun delete() {
        withContext(dispatchers.io) {
            clearOnReinstallOnce()
            val status = deleteKeychainItem()
            if (status != errSecSuccess && status != errSecItemNotFound) {
                logger.w { "Keychain delete failed: OSStatus $status" }
            }
        }
    }

    private fun clearOnReinstallOnce() {
        if (reinstallChecked) return
        reinstallChecked = true
        val defaults = NSUserDefaults.standardUserDefaults
        if (defaults.objectForKey(INSTALL_MARKER_DEFAULTS_KEY) == null) {
            deleteKeychainItem()
            defaults.setBool(true, INSTALL_MARKER_DEFAULTS_KEY)
        }
    }

    private fun baseQuery(): SecQuery =
        SecQuery().apply {
            put(kSecClass, kSecClassGenericPassword)
            putBridged(kSecAttrService, KEYCHAIN_SERVICE)
            putBridged(kSecAttrAccount, KEYCHAIN_ACCOUNT)
        }

    private fun deleteKeychainItem(): Int {
        val query = baseQuery()
        return try {
            SecItemDelete(query.dictionary())
        } finally {
            query.close()
        }
    }

    private fun readTokenData(): NSData? {
        val query = baseQuery()
        query.put(kSecReturnData, kCFBooleanTrue)
        try {
            return memScoped {
                val result = alloc<CFTypeRefVar>()
                val status = SecItemCopyMatching(query.dictionary(), result.ptr)
                when {
                    status == errSecSuccess -> {
                        CFBridgingRelease(result.value) as? NSData
                    }

                    status == errSecItemNotFound -> {
                        null
                    }

                    else -> {
                        logger.w { "Keychain read failed: OSStatus $status" }
                        null
                    }
                }
            }
        } finally {
            query.close()
        }
    }

    private fun writeTokenData(data: NSData): Boolean {
        val updateStatus = updateKeychainItem(data)
        return when {
            updateStatus == errSecSuccess -> {
                true
            }

            updateStatus != errSecItemNotFound -> {
                logger.w { "Keychain write failed: SecItemUpdate OSStatus $updateStatus" }
                false
            }

            else -> {
                val addStatus = addKeychainItem(data)
                if (addStatus != errSecSuccess) logger.w { "Keychain write failed: SecItemAdd OSStatus $addStatus" }
                addStatus == errSecSuccess
            }
        }
    }

    private fun updateKeychainItem(data: NSData): Int {
        val query = baseQuery()
        val attributes = SecQuery()
        return try {
            attributes.putBridged(kSecValueData, data)
            SecItemUpdate(query.dictionary(), attributes.dictionary())
        } finally {
            query.close()
            attributes.close()
        }
    }

    private fun addKeychainItem(data: NSData): Int {
        val query = baseQuery()
        return try {
            query.put(kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
            query.putBridged(kSecValueData, data)
            SecItemAdd(query.dictionary(), null)
        } finally {
            query.close()
        }
    }

    /**
     * A `CFDictionary` under construction, retaining its own keys/values (the standard "CFType"
     * callbacks). [putBridged] hands a Kotlin value to Core Foundation with a +1 retain that the
     * dictionary retains again, so [close] gives that first reference back; otherwise every call
     * would leak one object.
     */
    private class SecQuery {
        private val dict: CFMutableDictionaryRef? =
            CFDictionaryCreateMutable(
                kCFAllocatorDefault,
                0,
                kCFTypeDictionaryKeyCallBacks.ptr,
                kCFTypeDictionaryValueCallBacks.ptr,
            )
        private val bridged = mutableListOf<COpaquePointer?>()

        /** For values that are already Core Foundation objects: the `kSec*` constants, `kCFBooleanTrue`. */
        fun put(
            key: COpaquePointer?,
            value: COpaquePointer?,
        ) = CFDictionaryAddValue(dict, key, value)

        /** For Kotlin values (strings, `NSData`), which must cross into Core Foundation first. */
        fun putBridged(
            key: COpaquePointer?,
            value: Any,
        ) {
            val retained = CFBridgingRetain(value)
            bridged += retained
            CFDictionaryAddValue(dict, key, retained)
        }

        fun dictionary(): CFDictionaryRef? = dict

        fun close() {
            bridged.forEach { CFRelease(it) }
            bridged.clear()
            CFRelease(dict)
        }
    }
}
