package com.rushit.app.data.auth

import com.rushit.app.data.model.Profile
import kotlinx.coroutines.flow.StateFlow

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

/**
 * A signed-in RushIt account.
 *
 * @property uid the Firebase UID — the canonical account identifier
 * @property email the email Firebase reports, shown read-only. Null for phone-only accounts
 * @property phoneNumber the verified phone number, when the account has one
 * @property provider the provider that established this session
 * @property memberSinceEpochSeconds account creation time from Firebase, never from the device
 * @property profile the `public.profiles` row owned by this Firebase UID
 */
data class AuthAccount(
    val uid: String,
    val email: String?,
    val phoneNumber: String?,
    val provider: AuthProviderId,
    val memberSinceEpochSeconds: Long?,
    val profile: Profile,
) {
    /** Retained for the existing account UI, which shows the canonical identifier. */
    val id: String get() = uid

    val displayName: String?
        get() = profile.fullName?.takeIf { it.isNotBlank() }

    val username: String?
        get() = profile.username?.takeIf { it.isNotBlank() }

    val avatarUrl: String?
        get() = profile.avatarUrl?.takeIf { it.isNotBlank() }

    /** What to show under the name: an email, or a phone number for phone-only accounts. */
    val contactLine: String?
        get() = email?.takeIf { it.isNotBlank() } ?: phoneNumber?.takeIf { it.isNotBlank() }
}

/** What the UI can offer when authentication stopped in a state that can be retried. */
enum class RecoveryAction {
    /** Re-run session restoration; used when the device was offline at start-up. */
    RETRY_SESSION,

    /** Ask the person to sign in again; used when a session was rejected. */
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
    /** Start-up: Firebase is restoring its persisted session and the profile is being read. */
    data object Loading : AuthState

    /** No usable session. The login screen is the only destination. */
    data object Unauthenticated : AuthState

    /** A Google or phone sign-in is in flight. Sign-in entry points must be disabled. */
    data object Authenticating : AuthState

    /** Signed in with a completed profile. Home and the shop are reachable. */
    data class Authenticated(val account: AuthAccount) : AuthState

    /** Signed in but required profile fields are missing. Onboarding is the only destination. */
    data class ProfileIncomplete(val draft: ProfileDraft) : AuthState

    /**
     * Authentication stopped for a reason worth surfacing.
     *
     * This is never used for a person cancelling the Google account picker or the OTP screen, which
     * is not an error.
     */
    data class RecoverableError(
        val message: String,
        val kind: AuthFailureKind,
        val action: RecoveryAction,
    ) : AuthState
}

/** Coarse failure classes so the UI never has to pattern-match on transport internals. */
enum class AuthFailureKind {
    /** The person dismissed the Google account picker or left the OTP screen. Never an error. */
    CANCELLED,

    /** The build (or the server project) is missing Firebase, Google or Supabase configuration. */
    UNCONFIGURED,

    /** A credential or token was absent, malformed, expired, or issued for the wrong audience. */
    INVALID_TOKEN,

    /** The device could not reach Firebase or Supabase. */
    NETWORK,

    /** The request did not complete in time. */
    TIMEOUT,

    /** Firebase refused the credentials. */
    REJECTED,

    /** A uniqueness rule was violated, e.g. the username is already taken. */
    CONFLICT,

    /** The stored session expired and could not be refreshed. */
    SESSION_EXPIRED,

    /** Row Level Security rejected the operation. */
    POLICY_DENIED,

    /** A required row was missing. */
    NOT_FOUND,

    /** A backend capability is not deployed yet. */
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

/** Result of asking Firebase to send or resend a phone verification code. */
sealed interface PhoneVerificationOutcome {
    /** The code was sent; the OTP screen must be shown. */
    data object CodeSent : PhoneVerificationOutcome

    /** Firebase completed verification on its own and the user is now signed in. */
    data object Completed : PhoneVerificationOutcome

    data class Failure(
        val message: String,
        val kind: AuthFailureKind = AuthFailureKind.UNKNOWN,
    ) : PhoneVerificationOutcome
}

/** Result of checking whether a username can still be claimed. */
sealed interface UsernameAvailability {
    data object Available : UsernameAvailability
    data object Taken : UsernameAvailability

    /** The check could not run, e.g. offline. Saving still enforces uniqueness at the database. */
    data object Unknown : UsernameAvailability
}

/**
 * Application-facing authentication contract.
 *
 * The UI never talks to Firebase or Supabase directly; everything goes through this interface so
 * navigation, error handling and tests stay independent of both SDKs.
 */
interface AuthRepository {
    val authState: StateFlow<AuthState>

    /** Restore the persisted Firebase session once, at start-up, and publish the result. */
    suspend fun initialize()

    /** Re-run session restoration after an [AuthState.RecoverableError]. */
    suspend fun retrySessionRestore()

    /**
     * Sign in to Firebase with a Google ID token, then load or create the application profile.
     *
     * @param idToken the Google OIDC ID token from Credential Manager
     */
    suspend fun signInWithGoogle(idToken: String): AuthResult

    /** Ask Firebase to send an OTP to [phoneNumber]. */
    suspend fun startPhoneVerification(phoneNumber: PhoneNumber): PhoneVerificationOutcome

    /** Re-send the OTP for the challenge that is currently pending. */
    suspend fun resendPhoneCode(): PhoneVerificationOutcome

    /** Verify the OTP and sign in. */
    suspend fun submitPhoneCode(code: String): AuthResult

    /** Abandon the pending phone verification, e.g. when the person edits their number. */
    suspend fun cancelPhoneVerification()

    /** Persist onboarding or profile edits and mark onboarding complete. */
    suspend fun saveProfile(fullName: String, username: String?, avatarUrl: String?): AuthResult

    /** Re-read the session and profile of the signed-in user. */
    suspend fun refreshAccount(): AuthResult

    suspend fun checkUsername(username: String): UsernameAvailability

    /** Sign out of Firebase and drop every credential the backend could still use. */
    suspend fun signOut(): AuthResult

    /** Delete the Firebase account and the user's profile row. */
    suspend fun deleteAccount(): AuthResult
}
