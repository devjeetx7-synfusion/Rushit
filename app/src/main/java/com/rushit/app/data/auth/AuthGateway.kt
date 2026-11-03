package com.rushit.app.data.auth

/**
 * Authentication providers RushIt understands.
 *
 * Both providers are Firebase Authentication providers. Account ownership is always the Firebase
 * UID, so one person can only ever have one RushIt account per Firebase account, and linking a
 * phone identity to a Google identity is a Firebase account-linking decision rather than an
 * application-side merge.
 */
enum class AuthProviderId(val wireValue: String, val firebaseProviderId: String) {
    GOOGLE("google", "google.com"),
    PHONE("phone", "phone"),
    ;

    companion object {
        fun fromWireValue(value: String?): AuthProviderId? =
            values().firstOrNull { it.wireValue.equals(value, ignoreCase = true) }

        fun fromFirebaseProviderId(value: String?): AuthProviderId? =
            values().firstOrNull { it.firebaseProviderId.equals(value, ignoreCase = true) }
    }
}

/**
 * The facts the app is allowed to depend on from a Firebase session.
 *
 * Reducing the signed-in user to this shape keeps orchestration logic free of the Firebase SDK and
 * of Android, which is what makes it unit-testable on the JVM.
 *
 * @property uid the Firebase UID — the canonical account identifier for every RushIt record
 * @property email the email Firebase reported, when the provider supplied one. Phone-only accounts
 * have no email, so nothing may require it
 * @property phoneNumber the E.164 phone number Firebase verified, when available
 * @property memberSinceEpochSeconds account creation time from Firebase, never from the device
 */
data class AuthIdentity(
    val uid: String,
    val email: String?,
    val phoneNumber: String?,
    val provider: AuthProviderId,
    val memberSinceEpochSeconds: Long?,
    val suggestedFullName: String?,
    val suggestedUsername: String?,
    val suggestedAvatarUrl: String?,
)

/**
 * A Firebase ID token.
 */
data class AccessToken(val token: String, val expiresAtEpochSeconds: Long)

/** Opaque handle for one in-flight Firebase phone verification. */
data class PhoneChallenge(val id: String)

/** Result of asking Firebase to send a verification code. */
sealed interface PhoneChallengeStart {
    /** Firebase sent (or is sending) an SMS and is waiting for the code. */
    data class CodeSent(val challenge: PhoneChallenge) : PhoneChallengeStart

    /**
     * Firebase verified the phone without a code — instant verification or SMS auto-retrieval —
     * and the user is now signed in.
     */
    data object AutoVerified : PhoneChallengeStart
}

/**
 * Transport boundary around Firebase Authentication.
 */
interface AuthGateway {
    /** False when the build has no Firebase configuration, so no request can ever succeed. */
    val isConfigured: Boolean

    /**
     * The identity Firebase restored from its own persisted session, or null when nobody is signed
     * in. Waits for Firebase's first auth-state callback so a cold start cannot be mistaken for a
     * signed-out user.
     */
    suspend fun awaitInitialIdentity(): AuthIdentity?

    /** The currently signed-in identity without contacting the network, or null. */
    suspend fun currentIdentity(): AuthIdentity?

    /**
     * Sign in with a Google ID token obtained from Credential Manager.
     */
    suspend fun signInWithGoogleIdToken(idToken: String): AuthIdentity

    /** Ask Firebase to send a verification code to [phoneNumberE164]. */
    suspend fun startPhoneVerification(phoneNumberE164: String): PhoneChallengeStart

    /** Re-send the code for [challenge], reusing Firebase's resend token. */
    suspend fun resendPhoneCode(challenge: PhoneChallenge): PhoneChallengeStart

    /** Verify [code] for [challenge] and sign the user in. */
    suspend fun verifyPhoneCode(challenge: PhoneChallenge, code: String): AuthIdentity

    /** Forget a challenge that will not be completed. */
    suspend fun discardPhoneChallenge(challenge: PhoneChallenge)

    /**
     * A Firebase ID token for the signed-in user, or null when nobody is signed in.
     */
    suspend fun accessToken(forceRefresh: Boolean): AccessToken?

    /** Sign out of Firebase and drop any pending phone verification. */
    suspend fun signOut()

    /**
     * Delete the Firebase account of the currently signed-in user.
     */
    suspend fun deleteAccount()
}
