package com.rushit.app.data.auth

import com.rushit.app.data.model.Profile
import kotlinx.coroutines.flow.StateFlow

/**
 * Authentication providers RushIt understands.
 *
 * Google ships in this release. A future verified-phone provider is added by extending this enum
 * only: nothing else in the app branches on the provider, because account ownership is always the
 * Supabase `auth.users` UUID. That is what keeps a later phone identity linkable to the same
 * account instead of creating a second profile.
 */
enum class AuthProviderId(val wireValue: String) {
    GOOGLE("google"),
    ;

    companion object {
        fun fromWireValue(value: String?): AuthProviderId? =
            values().firstOrNull { it.wireValue.equals(value, ignoreCase = true) }
    }
}

/**
 * A signed-in RushIt account.
 *
 * @property id the Supabase `auth.users.id` UUID — the canonical account identifier
 * @property email the email Supabase verified with the identity provider, shown read-only
 * @property provider the provider that established this session
 * @property memberSinceEpochSeconds account creation time from Supabase, never from the device
 * @property profile the `public.profiles` row owned by [id]
 */
data class AuthAccount(
    val id: String,
    val email: String?,
    val provider: AuthProviderId,
    val memberSinceEpochSeconds: Long?,
    val profile: Profile,
) {
    val displayName: String?
        get() = profile.fullName?.takeIf { it.isNotBlank() }

    val username: String?
        get() = profile.username?.takeIf { it.isNotBlank() }

    val avatarUrl: String?
        get() = profile.avatarUrl?.takeIf { it.isNotBlank() }
}

/**
 * Everything the onboarding form needs, plus the values suggested by the identity provider.
 *
 * Provider data is only ever a *suggestion*: values the person already saved in `profiles` win, so
 * signing in again never overwrites a customized name or avatar.
 */
data class ProfileDraft(
    val id: String,
    val suggestedFullName: String?,
    val suggestedUsername: String?,
    val suggestedAvatarUrl: String?,
    val savedFullName: String?,
    val savedUsername: String?,
    val savedAvatarUrl: String?,
    val email: String?,
) {
    /** Pre-fill with saved data first, then fall back to the provider suggestion. */
    val initialFullName: String
        get() = savedFullName?.takeIf { it.isNotBlank() } ?: suggestedFullName.orEmpty()

    val initialUsername: String
        get() = savedUsername?.takeIf { it.isNotBlank() } ?: suggestedUsername.orEmpty()

    val initialAvatarUrl: String?
        get() = savedAvatarUrl?.takeIf { it.isNotBlank() } ?: suggestedAvatarUrl?.takeIf { it.isNotBlank() }

    val hasSavedValues: Boolean
        get() = !savedFullName.isNullOrBlank() || !savedUsername.isNullOrBlank()
}

/** What the UI can offer when authentication stopped in a state that can be retried. */
enum class RecoveryAction {
    /** Re-run session restoration; used when the device was offline at start-up. */
    RETRY_SESSION,

    /** Ask the person to sign in again; used when a refresh token was rejected. */
    SIGN_IN_AGAIN,

    /** Nothing can be retried, only acknowledged. */
    DISMISS,
}

/**
 * Centralized authentication state consumed by navigation and every auth-aware screen.
 *
 * The set is deliberately small and exhaustive so a `when` over it cannot silently drop a case.
 */
sealed interface AuthState {
    /** Start-up: the Supabase client is initializing and the stored session is being restored. */
    data object Loading : AuthState

    /** No usable session. The login screen is the only destination. */
    data object Unauthenticated : AuthState

    /** A Google sign-in is in flight. Sign-in entry points must be disabled. */
    data object Authenticating : AuthState

    /** Signed in with a completed profile. Home and the shop are reachable. */
    data class Authenticated(val account: AuthAccount) : AuthState

    /** Signed in but required profile fields are missing. Onboarding is the only destination. */
    data class ProfileIncomplete(val draft: ProfileDraft) : AuthState

    /**
     * Authentication stopped for a reason worth surfacing.
     *
     * This is never used for a person cancelling the Google account picker, which is not an error.
     */
    data class RecoverableError(
        val message: String,
        val kind: AuthFailureKind,
        val action: RecoveryAction,
    ) : AuthState
}

/** Coarse failure classes so the UI never has to pattern-match on transport internals. */
enum class AuthFailureKind {
    /** The person dismissed the Google account picker. Never shown as an error. */
    CANCELLED,

    /** No Google account is available on this device. */
    NO_ACCOUNT,

    /** The build is missing Supabase or Google OAuth configuration. */
    UNCONFIGURED,

    /** The ID token was absent, malformed, expired, or issued for the wrong audience. */
    INVALID_TOKEN,

    /** The device could not reach Supabase. */
    NETWORK,

    /** The request did not complete in time. */
    TIMEOUT,

    /** Supabase refused the credentials. */
    REJECTED,

    /** A uniqueness rule was violated, e.g. the username is already taken. */
    CONFLICT,

    /** The stored session expired and could not be refreshed. */
    SESSION_EXPIRED,

    /** Row Level Security rejected the operation. */
    POLICY_DENIED,

    /** A required row was missing. */
    NOT_FOUND,

    /** A backend capability is not deployed yet, e.g. the account-deletion function. */
    UNAVAILABLE,

    /** Anything else. Still surfaced, never swallowed. */
    UNKNOWN,
}

sealed interface AuthResult {
    data object Success : AuthResult

    /**
     * @property message a person-readable explanation; it never contains a token or stack trace
     * @property kind the class of failure, used to choose navigation and retry affordances
     */
    data class Failure(
        val message: String,
        val kind: AuthFailureKind = AuthFailureKind.UNKNOWN,
    ) : AuthResult
}

/** Result of checking whether a username can still be claimed. */
sealed interface UsernameAvailability {
    data object Available : UsernameAvailability
    data object Taken : UsernameAvailability

    /** The check could not run, e.g. offline. Saving still enforces uniqueness at the database. */
    data object Unknown : UsernameAvailability
}

interface AuthRepository {
    val authState: StateFlow<AuthState>

    /** Restore a stored session once, at start-up, and publish the resulting [AuthState]. */
    suspend fun initialize()

    /** Re-run session restoration after a [AuthState.RecoverableError]. */
    suspend fun retrySessionRestore()

    /**
     * Exchange a Google ID token for a Supabase session, then provision and load the profile.
     *
     * @param idToken the OIDC ID token from Credential Manager
     * @param rawNonce the un-hashed nonce that was hashed into the Credential Manager request
     */
    suspend fun signInWithGoogle(idToken: String, rawNonce: String?): AuthResult

    /** Persist onboarding or profile edits and mark onboarding complete. */
    suspend fun saveProfile(fullName: String, username: String?, avatarUrl: String?): AuthResult

    /** Re-read the profile of the signed-in user without touching the session. */
    suspend fun refreshAccount(): AuthResult

    suspend fun checkUsername(username: String): UsernameAvailability

    /** Revoke the session and clear every local trace of it. */
    suspend fun signOut(): AuthResult

    /** Delete the Supabase auth user through a trusted server-side endpoint. */
    suspend fun deleteAccount(): AuthResult
}
