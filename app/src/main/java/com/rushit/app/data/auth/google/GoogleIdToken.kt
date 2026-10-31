package com.rushit.app.data.auth.google

import com.rushit.app.data.auth.AuthFailureKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * The claims RushIt inspects in a Google ID token.
 *
 * Only the payload is decoded, locally and without verification — the signature is verified by
 * Supabase/Google. This exists to fail fast with a useful message when a build is pointed at the
 * wrong OAuth client, instead of surfacing an opaque server rejection.
 */
data class GoogleIdTokenClaims(
    val issuer: String?,
    val audiences: List<String>,
    val subject: String?,
    val issuedAtEpochSeconds: Long?,
    val expiresAtEpochSeconds: Long?,
    val nonce: String?,
    val email: String?,
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** Returns null when [idToken] is not a three-part JWT with a readable JSON payload. */
        fun parse(idToken: String): GoogleIdTokenClaims? {
            val parts = idToken.split('.')
            if (parts.size != 3) return null
            val payload = Base64Url.decodeToString(parts[1]) ?: return null
            val body = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull() ?: return null
            return GoogleIdTokenClaims(
                issuer = body.string("iss"),
                audiences = body.audiences(),
                subject = body.string("sub"),
                issuedAtEpochSeconds = body.number("iat"),
                expiresAtEpochSeconds = body.number("exp"),
                nonce = body.string("nonce"),
                email = body.string("email"),
            )
        }

        private fun JsonObject.string(key: String): String? =
            (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

        private fun JsonObject.number(key: String): Long? =
            (this[key] as? JsonPrimitive)?.longOrNull

        private fun JsonObject.audiences(): List<String> = when (val value = this["aud"]) {
            is JsonPrimitive -> listOfNotNull(value.contentOrNull?.takeIf { it.isNotBlank() })
            is JsonArray -> value.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank) }
            else -> emptyList()
        }
    }
}

/** Result of the local pre-flight check on a Google ID token. */
sealed interface IdTokenCheck {
    data object Accepted : IdTokenCheck
    data class Rejected(val message: String, val kind: AuthFailureKind) : IdTokenCheck
}

/**
 * Local pre-flight validation of a Google ID token before it is sent to Supabase.
 *
 * Supabase performs the authoritative verification. These checks only turn the two configuration
 * mistakes that are easy to make — the wrong Web OAuth Client ID, and a device clock far enough off
 * to expire tokens immediately — into messages a person can act on.
 */
object GoogleIdTokenValidator {

    /** Both forms appear in Google ID tokens; GoTrue's OIDC verifier uses the https form. */
    private val GOOGLE_ISSUERS = setOf("https://accounts.google.com", "accounts.google.com")

    /** Tolerance for device clock skew, in seconds. */
    const val CLOCK_SKEW_SECONDS = 120L

    fun validate(
        idToken: String,
        expectedAudience: String,
        rawNonce: String?,
        nowEpochSeconds: Long,
    ): IdTokenCheck {
        if (idToken.isBlank()) {
            return reject(GoogleSignInMessages.MISSING_TOKEN, AuthFailureKind.INVALID_TOKEN)
        }
        val claims = GoogleIdTokenClaims.parse(idToken)
            ?: return reject(GoogleSignInMessages.UNREADABLE_TOKEN, AuthFailureKind.INVALID_TOKEN)

        if (claims.issuer == null || claims.issuer !in GOOGLE_ISSUERS) {
            return reject(GoogleSignInMessages.NOT_GOOGLE_TOKEN, AuthFailureKind.INVALID_TOKEN)
        }
        if (claims.subject.isNullOrBlank()) {
            return reject(GoogleSignInMessages.UNREADABLE_TOKEN, AuthFailureKind.INVALID_TOKEN)
        }

        // The audience must be the Web OAuth Client ID configured for the Supabase Google provider.
        val audience = expectedAudience.trim()
        if (audience.isNotBlank() && claims.audiences.none { it == audience }) {
            return reject(GoogleSignInMessages.AUDIENCE_MISMATCH, AuthFailureKind.UNCONFIGURED)
        }

        val expiresAt = claims.expiresAtEpochSeconds
        if (expiresAt != null && expiresAt + CLOCK_SKEW_SECONDS < nowEpochSeconds) {
            return reject(GoogleSignInMessages.EXPIRED_TOKEN, AuthFailureKind.INVALID_TOKEN)
        }

        // Mirror GoTrue: either both sides carry a nonce or neither does, and the values must match.
        val tokenNonce = claims.nonce
        if (rawNonce.isNullOrBlank() != tokenNonce.isNullOrBlank()) {
            return reject(GoogleSignInMessages.NONCE_MISMATCH, AuthFailureKind.INVALID_TOKEN)
        }
        if (!rawNonce.isNullOrBlank() && tokenNonce != null && tokenNonce != SignInNonce.hash(rawNonce)) {
            return reject(GoogleSignInMessages.NONCE_MISMATCH, AuthFailureKind.INVALID_TOKEN)
        }

        return IdTokenCheck.Accepted
    }

    private fun reject(message: String, kind: AuthFailureKind) = IdTokenCheck.Rejected(message, kind)
}
