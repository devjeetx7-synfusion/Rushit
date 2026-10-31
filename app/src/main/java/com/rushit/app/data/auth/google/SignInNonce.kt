package com.rushit.app.data.auth.google

import java.security.MessageDigest
import java.util.UUID

/**
 * Nonce handling for the Google ID-token flow.
 *
 * Supabase GoTrue compares `sha256(nonce)` — lowercase hex — against the `nonce` claim of the ID
 * token (`internal/api/token_oidc.go`). Google copies whatever is passed to
 * `GetGoogleIdOption.setNonce` into that claim. So the *hash* goes to Google and the *raw* value
 * goes to Supabase; sending the raw value to both makes every sign-in fail with "Nonces mismatch".
 */
object SignInNonce {

    /** A fresh single-use raw nonce. Kept in memory only; never logged or persisted. */
    fun newRawNonce(): String = UUID.randomUUID().toString()

    /** Lowercase hex SHA-256, matching what GoTrue recomputes from the raw nonce. */
    fun hash(rawNonce: String): String = sha256Hex(rawNonce)

    fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        val builder = StringBuilder(digest.size * 2)
        for (byte in digest) {
            val text = Integer.toHexString(byte.toInt() and 0xFF)
            if (text.length == 1) builder.append('0')
            builder.append(text)
        }
        return builder.toString()
    }
}
