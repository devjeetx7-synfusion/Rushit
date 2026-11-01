package com.rushit.app.data.auth.supabase

import com.rushit.app.data.auth.AccessToken
import com.rushit.app.data.auth.BackendTokenBridge
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.user.UserSession
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Installs the current Firebase ID token as the bearer token of the Supabase client.
 *
 * ## Why this exists
 *
 * Supabase's official third-party authentication integration verifies Firebase ID tokens, but the
 * Supabase SDK only ever attaches the bearer token of *its own* session. supabase-kt 2.5.0 has no
 * `accessToken` callback (that arrived in 3.x), so the supported way to hand an externally issued
 * token to PostgREST/Storage/Functions in this SDK version is `Auth.importSession` — the SDK's
 * generic "here is an access token I obtained elsewhere" entry point.
 *
 * ## What this is *not*
 *
 * This is not Supabase authentication. Nothing here signs in, signs up, refreshes or exchanges
 * anything with Supabase Auth:
 *
 * * the client is configured with `autoLoadFromStorage = false`, `autoSaveToStorage = false`,
 *   `alwaysAutoRefresh = false` and an in-memory session manager, so the SDK never reads, writes,
 *   refresh-rotates or revokes anything;
 * * the imported session has an empty refresh token, so no refresh request can ever be made;
 * * [AccessToken] is re-imported by the repository before every backend operation, and removed by
 *   [clear] on sign-out and account deletion.
 *
 * The consequence is fail-closed: when no token is installed the SDK falls back to the publishable
 * key, PostgREST runs as `anon`, and Row Level Security denies every user-owned row.
 */
class SupabaseIdentityBridge(private val auth: Auth) : BackendTokenBridge {

    @Volatile
    private var installedToken: String? = null

    override val hasToken: Boolean
        get() = installedToken != null && auth.currentSessionOrNull() != null

    override suspend fun use(token: AccessToken) {
        val nowSeconds = Clock.System.now().epochSeconds
        val expiresIn = (token.expiresAtEpochSeconds - nowSeconds).coerceAtLeast(1L)
        auth.importSession(
            UserSession(
                accessToken = token.token,
                // Empty on purpose: an external identity provider has no Supabase refresh token.
                refreshToken = "",
                expiresIn = expiresIn,
                tokenType = "bearer",
                // Firebase is the source of truth for the user; Supabase never sees a user object.
                user = null,
                type = "",
                expiresAt = Instant.fromEpochSeconds(token.expiresAtEpochSeconds),
            ),
            autoRefresh = false,
        )
        installedToken = token.token
    }

    override suspend fun clear() {
        installedToken = null
        // Local only: drops the imported token so no later request can be authorized as this user.
        auth.clearSession()
    }
}
