package com.rushit.app.data.auth

import com.rushit.app.data.model.Profile

/**
 * The facts the app is allowed to depend on from a Supabase session.
 *
 * Reducing `UserSession` to this shape keeps orchestration logic free of the SDK and of Android,
 * which is what makes it unit-testable on the JVM.
 */
data class SessionSnapshot(
    val userId: String,
    val email: String?,
    val provider: AuthProviderId,
    val memberSinceEpochSeconds: Long?,
    val suggestedFullName: String?,
    val suggestedUsername: String?,
    val suggestedAvatarUrl: String?,
)

/** Outcome of a session-restore attempt. */
sealed interface SessionRestoreResult {
    /** A session was restored, refreshing it first when it had already expired. */
    data class Restored(val session: SessionSnapshot) : SessionRestoreResult

    /** Nothing is stored, so there is nothing to restore. */
    data object NoSession : SessionRestoreResult

    /**
     * The stored session could not be verified because Supabase was unreachable.
     *
     * @property sessionStillStored whether a session remains on the device and is worth retrying
     */
    data class Unreachable(val sessionStillStored: Boolean) : SessionRestoreResult

    /** The refresh token was rejected and the local session was cleared. */
    data object Rejected : SessionRestoreResult
}

/** Outcome of an account-deletion attempt. */
sealed interface DeleteAccountOutcome {
    /** Supabase deleted the auth user; cascades removed every owned row. */
    data object Deleted : DeleteAccountOutcome

    /** No trusted deletion endpoint is deployed, so nothing was deleted. */
    data object NotDeployed : DeleteAccountOutcome
}

/**
 * Transport boundary around Supabase.
 *
 * Implementations throw on transport failures; [SupabaseAuthRepository] turns those into
 * [AuthFailureKind] values. Only this interface and the models around it are exercised by unit
 * tests, so no method may accept or return an Android or Supabase SDK type.
 */
interface AuthGateway {
    /** False when the build has no Supabase URL or key, so no request can ever succeed. */
    val isConfigured: Boolean

    suspend fun restoreSession(): SessionRestoreResult

    /**
     * Force one access-token refresh now.
     *
     * @return whether the refresh succeeded; implementations must serialize concurrent calls so the
     * same refresh token is never rotated twice
     */
    suspend fun refreshSession(): Boolean

    suspend fun signInWithIdToken(
        provider: AuthProviderId,
        idToken: String,
        rawNonce: String?,
    ): SessionSnapshot

    /** The session of the currently signed-in user, without contacting the network. */
    suspend fun currentSession(): SessionSnapshot?

    /**
     * Revoke the current refresh token and always clear local state.
     *
     * @return whether the server-side revocation succeeded; local clearing is unconditional so a
     * failed logout can never leave a session behind on the device
     */
    suspend fun signOut(): Boolean

    suspend fun deleteAccount(): DeleteAccountOutcome

    suspend fun fetchProfile(userId: String): Profile?

    /**
     * Create the profile row if, and only if, it does not exist yet.
     *
     * Concurrent calls and repeat sign-ins must not duplicate the row or overwrite saved values,
     * so the write ignores primary-key conflicts and the row is read back afterwards.
     */
    suspend fun provisionProfile(userId: String, fullName: String?, avatarUrl: String?): Profile?

    /** Create default `user_settings` for the user if they are missing. */
    suspend fun ensureSettings(userId: String)

    /** Save onboarding or profile edits and mark onboarding complete, returning the stored row. */
    suspend fun saveProfile(
        userId: String,
        fullName: String,
        username: String?,
        avatarUrl: String?,
    ): Profile

    suspend fun usernameTaken(username: String): Boolean

    /**
     * Append a minimal activity record. Best-effort: callers must never fail a flow on this.
     *
     * The metadata map must not contain tokens, secrets, or personally identifying detail.
     */
    suspend fun recordActivity(userId: String, eventType: String, metadata: Map<String, String>)
}
