package com.rushit.app.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypted-at-rest storage for one opaque payload.
 *
 * Kept as a string store rather than a session-shaped store so the Supabase SDK owns the session
 * model and this class owns only the encryption. That is what prevents a second, conflicting
 * session system from growing alongside the SDK.
 */
internal interface SessionStore {
    fun write(payload: String)
    fun read(): String?
    fun clear()
}

/**
 * [SessionStore] backed by SharedPreferences, encrypted with a non-exportable Android Keystore key.
 *
 * Refresh tokens are therefore never written to ordinary preferences in plaintext. `allowBackup` is
 * false in the manifest, so the ciphertext is not copied to another device where the key would be
 * missing.
 *
 * Every method is defensive: a Keystore key that can no longer decrypt (restored data, revoked key,
 * corrupted value) is treated as "no session", which lands the person on the login screen instead of
 * crashing at start-up.
 */
internal class SecureSessionStore(context: Context) : SessionStore {

    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val alias = KEY_ALIAS

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .setKeySize(KEY_SIZE_BITS)
                    .build(),
            )
            generateKey()
        }
    }

    override fun write(payload: String) {
        runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key())
            check(cipher.iv.size == IV_LENGTH_BYTES) { "Unexpected GCM IV length" }
            val bytes = cipher.iv + cipher.doFinal(payload.toByteArray(Charsets.UTF_8))
            preferences.edit()
                .putString(SESSION_KEY, Base64.encodeToString(bytes, Base64.NO_WRAP))
                .commit()
        }.onFailure {
            // A failed write must leave no half-updated ciphertext behind.
            runCatching { preferences.edit().remove(SESSION_KEY).commit() }
        }
    }

    override fun read(): String? {
        val encoded = preferences.getString(SESSION_KEY, null) ?: return null
        val decoded = runCatching {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            if (bytes.size <= IV_LENGTH_BYTES) return@runCatching null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                key(),
                GCMParameterSpec(TAG_LENGTH_BITS, bytes.copyOfRange(0, IV_LENGTH_BYTES)),
            )
            String(
                cipher.doFinal(bytes.copyOfRange(IV_LENGTH_BYTES, bytes.size)),
                Charsets.UTF_8,
            )
        }.getOrNull()

        if (decoded == null) clear()
        return decoded
    }

    override fun clear() {
        runCatching { preferences.edit().remove(SESSION_KEY).commit() }
    }

    private companion object {
        const val PREFERENCES_NAME = "rushit_secure_session"
        const val SESSION_KEY = "session"
        const val KEY_ALIAS = "rushit.session.aes.v1"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH_BYTES = 12
        const val TAG_LENGTH_BITS = 128
        const val KEY_SIZE_BITS = 256
    }
}

/** In-memory store used by tests and by builds that must not touch the Keystore. */
internal class InMemorySessionStore : SessionStore {
    var payload: String? = null
    override fun write(payload: String) { this.payload = payload }
    override fun read(): String? = payload
    override fun clear() { payload = null }
}
