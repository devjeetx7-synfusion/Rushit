package com.rushit.app.data.auth

import com.rushit.app.data.model.Profile

/**
 * Application profile storage.
 *
 * This is deliberately separate from [AuthGateway]: identity is owned by Firebase, while the
 * profile, settings and activity rows are ordinary Supabase business data keyed by the Firebase UID.
 * Every method is addressed by the Firebase UID, which is what the RLS policies verify, so the
 * repository never has to reason about a second identifier.
 */
interface ProfileGateway {
    /** False when the build has no Supabase URL or key, so no request can ever succeed. */
    val isConfigured: Boolean

    /** The `public.profiles` row owned by [firebaseUid], or null when none exists yet. */
    suspend fun findProfile(firebaseUid: String): Profile?

    /**
     * Create the profile row if, and only if, it does not exist yet.
     *
     * Concurrent calls and repeat sign-ins must not duplicate the row or overwrite saved values, so
     * the write ignores conflicts on `firebase_uid` and the row is read back afterwards.
     */
    suspend fun provisionProfile(firebaseUid: String, fullName: String?, avatarUrl: String?): Profile?

    /** Create default `user_settings` for the user if they are missing. */
    suspend fun ensureSettings(firebaseUid: String)

    /** Save onboarding or profile edits and mark onboarding complete, returning the stored row. */
    suspend fun saveProfile(
        firebaseUid: String,
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
    suspend fun recordActivity(firebaseUid: String, eventType: String, metadata: Map<String, String>)

    /** Delete the profile row (and everything that cascades from it) owned by [firebaseUid]. */
    suspend fun deleteProfile(firebaseUid: String)
}

/** Messages shared by the profile gateway and the repository. */
object ProfileMessages {
    const val SUPABASE_UNCONFIGURED =
        "RushIt is not connected to its account service in this build. Set SUPABASE_URL and " +
            "SUPABASE_ANON_KEY, then rebuild."
    const val PROFILE_MISSING =
        "RushIt could not read your RushIt profile. Please sign in again."
}
