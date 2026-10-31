package com.rushit.app.data.auth

import com.rushit.app.data.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.NotFoundRestException
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.SignOutScope
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import io.github.jan.supabase.gotrue.user.UserSession
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * [AuthGateway] over a real [SupabaseClient].
 *
 * Everything Supabase-specific lives here so the orchestration in [SupabaseAuthRepository] can be
 * tested without a device, a network, or the SDK.
 */
class SupabaseAuthGateway(private val supabase: SupabaseClient) : AuthGateway {

    private val auth get() = supabase.auth
    private val postgrest get() = supabase.postgrest

    /**
     * Serializes explicit refresh attempts so two callers can never rotate the same refresh token
     * concurrently. The SDK's own auto-refresh job is separate and already single-flight.
     */
    private val refreshMutex = Mutex()

    override val isConfigured: Boolean = supabase.supabaseUrl.isNotBlank()

    override suspend fun restoreSession(): SessionRestoreResult {
        // Read storage first: once a refresh is rejected the SDK deletes it, and that difference is
        // what separates "expired, please sign in again" from "never signed in".
        val hadStoredSession = loadStoredSession() != null

        auth.awaitInitialization()
        return when (val status = auth.sessionStatus.value) {
            is SessionStatus.Authenticated -> SessionRestoreResult.Restored(snapshotFor(status.session))

            // Supabase was unreachable. The SDK keeps retrying in the background; the stored session
            // is deliberately left alone so it can be used once connectivity returns.
            SessionStatus.NetworkError ->
                SessionRestoreResult.Unreachable(sessionStillStored = loadStoredSession() != null)

            is SessionStatus.NotAuthenticated ->
                if (hadStoredSession) SessionRestoreResult.Rejected else SessionRestoreResult.NoSession

            SessionStatus.LoadingFromStorage -> SessionRestoreResult.NoSession
        }
    }

    override suspend fun refreshSession(): Boolean = refreshMutex.withLock {
        withTimeoutOrNull(REFRESH_TIMEOUT_MILLIS) {
            runCatching {
                if (auth.currentSessionOrNull() != null) auth.refreshCurrentSession()
            }.isSuccess
        } ?: false
    }

    override suspend fun signInWithIdToken(
        provider: AuthProviderId,
        idToken: String,
        rawNonce: String?,
    ): SessionSnapshot {
        require(provider == AuthProviderId.GOOGLE) {
            "Only Google ID-token sign-in is wired up in this release"
        }
        auth.signInWith(IDToken) {
            this.idToken = idToken
            this.provider = Google
            // Supabase re-hashes this value and compares it with the ID token's nonce claim.
            this.nonce = rawNonce?.takeIf { it.isNotBlank() }
        }
        val session = auth.currentSessionOrNull()
            ?: error("Supabase accepted the sign-in but returned no session")
        return snapshotFor(session)
    }

    override suspend fun currentSession(): SessionSnapshot? =
        auth.currentSessionOrNull()?.let { snapshotFor(it) }

    override suspend fun signOut(): Boolean {
        var revokedRemotely = true
        try {
            // scope=local revokes this device's refresh token without signing out other devices.
            if (auth.currentSessionOrNull() != null) auth.signOut(SignOutScope.LOCAL)
        } catch (error: Throwable) {
            error.rethrowIfCancelled()
            revokedRemotely = false
        }
        // The SDK skips its own cleanup when the network call throws, so local state is cleared here
        // unconditionally. A failed logout must never leave a session on the device.
        runCatching { auth.clearSession() }
        return revokedRemotely
    }

    override suspend fun deleteAccount(): DeleteAccountOutcome {
        // Preferred path: an Edge Function that verifies the caller's JWT and uses the Admin API.
        // The service-role key never reaches the app.
        try {
            supabase.functions(DELETE_ACCOUNT_FUNCTION)
            return DeleteAccountOutcome.Deleted
        } catch (error: NotFoundRestException) {
            // Not deployed; fall through to the SQL RPC.
        } catch (error: Throwable) {
            error.rethrowIfCancelled()
            throw error
        }

        // Fallback: a SECURITY DEFINER RPC that authorizes with auth.uid() and deletes the auth user.
        return try {
            postgrest.rpc(DELETE_ACCOUNT_RPC)
            DeleteAccountOutcome.Deleted
        } catch (error: NotFoundRestException) {
            DeleteAccountOutcome.NotDeployed
        }
    }

    override suspend fun fetchProfile(userId: String): Profile? =
        postgrest[PROFILES]
            .select(columns = Columns.type<Profile>()) { filter { eq("id", userId) } }
            .decodeSingleOrNull()

    override suspend fun provisionProfile(
        userId: String,
        fullName: String?,
        avatarUrl: String?,
    ): Profile? {
        // ignoreDuplicates makes this safe under concurrent sign-ins and on every later login: the
        // row is created only if it is absent, so saved values are never overwritten here.
        postgrest[PROFILES].upsert(
            ProfileInsert(id = userId, fullName = fullName, avatarUrl = avatarUrl),
            onConflict = "id",
            ignoreDuplicates = true,
        )
        return fetchProfile(userId)
    }

    override suspend fun ensureSettings(userId: String) {
        postgrest[USER_SETTINGS].upsert(
            SettingsInsert(userId = userId),
            onConflict = USER_ID_COLUMN,
            ignoreDuplicates = true,
        )
    }

    override suspend fun saveProfile(
        userId: String,
        fullName: String,
        username: String?,
        avatarUrl: String?,
    ): Profile {
        // The row is normally created by a database trigger at sign-up; provision it here so an
        // update can never be silently applied to zero rows.
        if (fetchProfile(userId) == null) provisionProfile(userId, fullName, avatarUrl)

        postgrest[PROFILES].update({
            set("full_name", fullName)
            // A blank username is stored as NULL so the partial unique index ignores it.
            set("username", username)
            set("avatar_url", avatarUrl)
            set("onboarding_completed", true)
        }) {
            filter { eq("id", userId) }
        }
        return fetchProfile(userId)
            ?: throw IllegalStateException("RushIt saved the profile but could not read it back")
    }

    override suspend fun usernameTaken(username: String): Boolean {
        val normalized = username.trim().lowercase()
        if (normalized.isEmpty()) return false
        // RLS restricts profiles to their owner, so availability is answered by a SECURITY DEFINER
        // function that returns a boolean and nothing else. A plain SELECT would always say "free".
        return !postgrest.rpc(USERNAME_AVAILABLE_RPC, mapOf("p_username" to normalized))
            .decodeAs<Boolean>()
    }

    override suspend fun recordActivity(
        userId: String,
        eventType: String,
        metadata: Map<String, String>,
    ) {
        postgrest[USER_ACTIVITY_LOGS].insert(
            ActivityInsert(userId = userId, eventType = eventType, metadata = metadata),
        )
    }

    private suspend fun loadStoredSession(): UserSession? =
        runCatching { auth.sessionManager.loadSession() }.getOrNull()

    private suspend fun snapshotFor(session: UserSession): SessionSnapshot {
        val user = session.user ?: auth.retrieveUserForCurrentSession(updateSession = true)
        val metadata = user.userMetadata
        val appMetadata = user.appMetadata

        val providerWire = user.identities?.firstOrNull()?.provider
            ?: appMetadata.string("provider")
            ?: AuthProviderId.GOOGLE.wireValue

        return SessionSnapshot(
            userId = user.id,
            email = user.email?.takeIf { it.isNotBlank() } ?: metadata.string("email"),
            provider = AuthProviderId.fromWireValue(providerWire) ?: AuthProviderId.GOOGLE,
            memberSinceEpochSeconds = user.createdAt?.epochSeconds,
            suggestedFullName = metadata.string("full_name", "name"),
            suggestedUsername = metadata.string("preferred_username", "user_name"),
            suggestedAvatarUrl = metadata.string("avatar_url", "picture"),
        )
    }

    private fun JsonObject?.string(vararg keys: String): String? {
        val source = this ?: return null
        for (key in keys) {
            val value = (source[key] as? JsonPrimitive)?.contentOrNull?.trim()
            if (!value.isNullOrEmpty()) return value
        }
        return null
    }

    private fun Throwable.rethrowIfCancelled() {
        if (this is CancellationException && this !is TimeoutCancellationException) throw this
    }

    @Serializable
    private data class ProfileInsert(
        val id: String,
        @SerialName("full_name") val fullName: String? = null,
        @SerialName("avatar_url") val avatarUrl: String? = null,
    )

    @Serializable
    private data class SettingsInsert(
        @SerialName("user_id") val userId: String,
    )

    @Serializable
    private data class ActivityInsert(
        @SerialName("user_id") val userId: String,
        @SerialName("event_type") val eventType: String,
        val metadata: Map<String, String> = emptyMap(),
    )

    companion object {
        const val PROFILES = "profiles"
        const val USER_SETTINGS = "user_settings"
        const val USER_ACTIVITY_LOGS = "user_activity_logs"
        const val USER_ID_COLUMN = "user_id"
        const val DELETE_ACCOUNT_FUNCTION = "delete-account"
        const val DELETE_ACCOUNT_RPC = "delete_user_account"
        const val USERNAME_AVAILABLE_RPC = "username_available"
        private const val REFRESH_TIMEOUT_MILLIS = 15_000L

        /**
         * Serializer used for every RushIt Supabase client.
         *
         * `ignoreUnknownKeys` keeps the app working when the database gains a column before a client
         * release; `encodeDefaults = false` keeps `created_at`/`updated_at` out of insert payloads so
         * the database defaults and triggers stay in charge.
         */
        @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
        val serializer: KotlinXSerializer = KotlinXSerializer(
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = false
                explicitNulls = false
            },
        )
    }
}

/**
 * Gateway used when the build has no Supabase configuration.
 *
 * It fails with an actionable message instead of letting an unconfigured client crash at start-up or
 * pretend to authenticate anybody.
 */
class UnconfiguredAuthGateway(val reason: String) : AuthGateway {
    override val isConfigured: Boolean = false

    override suspend fun restoreSession(): SessionRestoreResult = SessionRestoreResult.NoSession

    override suspend fun refreshSession(): Boolean = false

    override suspend fun signInWithIdToken(
        provider: AuthProviderId,
        idToken: String,
        rawNonce: String?,
    ): SessionSnapshot = throw IllegalStateException(reason)

    override suspend fun currentSession(): SessionSnapshot? = null

    override suspend fun signOut(): Boolean = true

    override suspend fun deleteAccount(): DeleteAccountOutcome = DeleteAccountOutcome.NotDeployed

    override suspend fun fetchProfile(userId: String): Profile? = null

    override suspend fun provisionProfile(
        userId: String,
        fullName: String?,
        avatarUrl: String?,
    ): Profile? = throw IllegalStateException(reason)

    override suspend fun ensureSettings(userId: String) = throw IllegalStateException(reason)

    override suspend fun saveProfile(
        userId: String,
        fullName: String,
        username: String?,
        avatarUrl: String?,
    ): Profile = throw IllegalStateException(reason)

    override suspend fun usernameTaken(username: String): Boolean =
        throw IllegalStateException(reason)

    override suspend fun recordActivity(
        userId: String,
        eventType: String,
        metadata: Map<String, String>,
    ) = Unit
}
