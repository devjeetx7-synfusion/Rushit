package com.rushit.app.data.auth.google

import java.net.URI
import java.net.URLDecoder

/** Exact callback registered both in AndroidManifest.xml and the Supabase redirect allow-list. */
object SupabaseOAuthRedirect {
    const val SCHEME = "rushit"
    const val HOST = "auth"
    const val PATH = "/callback"
    const val URL = "$SCHEME://$HOST$PATH"

    sealed interface Result {
        /** Not our callback. Never exchange data from another link or clear a pending sign-in. */
        data object Ignored : Result
        data class Code(val value: String) : Result
        data object Cancelled : Result
        data object Rejected : Result
    }

    /**
     * Validate before the Supabase SDK sees any callback data. In particular, do not use the SDK's
     * scheme/host-only check: a different path, embedded credentials, fragment tokens or duplicate
     * parameters are not valid PKCE callbacks. Provider error descriptions are never echoed.
     */
    fun parse(value: String?): Result {
        if (value.isNullOrBlank() || value.length > 8192) return Result.Ignored
        val uri = runCatching { URI(value) }.getOrNull()
            ?: return if (value.startsWith("$SCHEME://", ignoreCase = true)) Result.Rejected else Result.Ignored
        if (!uri.scheme.equals(SCHEME, ignoreCase = true) || !uri.host.equals(HOST, ignoreCase = true)) {
            return Result.Ignored
        }
        if (uri.rawAuthority != HOST || uri.rawPath != PATH || uri.userInfo != null ||
            uri.port != -1 || uri.fragment != null || uri.rawQuery == null
        ) return Result.Rejected

        val parameters = mutableMapOf<String, String>()
        for (part in uri.rawQuery.split('&')) {
            val pair = part.split('=', limit = 2)
            if (pair.size != 2) return Result.Rejected
            val key = decode(pair[0]) ?: return Result.Rejected
            val decoded = decode(pair[1]) ?: return Result.Rejected
            if (key !in setOf("code", "error", "error_code", "error_description") ||
                parameters.put(key, decoded) != null
            ) return Result.Rejected
        }
        val code = parameters["code"]
        if (code != null) {
            return if (parameters.size == 1 && code.matches(Regex("[A-Za-z0-9._~-]{16,4096}"))) {
                Result.Code(code)
            } else Result.Rejected
        }
        return when (parameters["error"]) {
            "access_denied", "user_cancelled" -> Result.Cancelled
            null -> Result.Rejected
            else -> Result.Rejected
        }
    }

    private fun decode(value: String): String? = runCatching {
        // Check escapes before URLDecoder, which otherwise silently accepts malformed '%' input.
        if (Regex("%(?![0-9a-fA-F]{2})").containsMatchIn(value)) return@runCatching null
        URLDecoder.decode(value, "UTF-8").takeIf { decoded -> decoded.none(Char::isISOControl) }
    }.getOrNull()
}
