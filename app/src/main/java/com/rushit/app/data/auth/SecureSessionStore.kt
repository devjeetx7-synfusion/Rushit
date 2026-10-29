package com.rushit.app.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal data class StoredSession(val accessToken: String, val refreshToken: String)

internal interface SessionStore {
    fun write(session: StoredSession)
    fun read(): StoredSession?
    fun clear()
}

/** Tokens are encrypted with a non-exportable Android Keystore key; app backup is disabled. */
internal class SecureSessionStore(context: Context) : SessionStore {
    private val preferences = context.getSharedPreferences("rushit_secure_session", Context.MODE_PRIVATE)
    private val alias = "rushit.session.aes.v1"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generateKey()
        }
    }

    override fun write(session: StoredSession) {
        val json = JSONObject().put("access", session.accessToken).put("refresh", session.refreshToken).toString()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        check(cipher.iv.size == 12)
        val bytes = cipher.iv + cipher.doFinal(json.toByteArray(Charsets.UTF_8))
        check(preferences.edit().putString("session", Base64.encodeToString(bytes, Base64.NO_WRAP)).commit())
    }

    override fun read(): StoredSession? {
        val encoded = preferences.getString("session", null) ?: return null
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        require(bytes.size > 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        val json = JSONObject(String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8))
        return StoredSession(json.getString("access"), json.getString("refresh"))
    }

    override fun clear() { check(preferences.edit().remove("session").commit()) }
}
