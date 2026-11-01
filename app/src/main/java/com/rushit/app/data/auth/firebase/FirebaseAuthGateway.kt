package com.rushit.app.data.auth.firebase

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.rushit.app.data.auth.AccessToken
import com.rushit.app.data.auth.AuthFailure
import com.rushit.app.data.auth.AuthFailureKind
import com.rushit.app.data.auth.AuthGateway
import com.rushit.app.data.auth.AuthGatewayException
import com.rushit.app.data.auth.AuthIdentity
import com.rushit.app.data.auth.AuthProviderId
import com.rushit.app.data.auth.PhoneChallenge
import com.rushit.app.data.auth.PhoneChallengeStart
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * [AuthGateway] over the Firebase Authentication Android SDK.
 *
 * Everything Firebase- or Android-specific lives here, so [com.rushit.app.data.auth.FirebaseAuthRepository]
 * and its tests never touch an SDK type:
 *
 * * Google sign-in consumes a Google ID token obtained by Credential Manager and exchanges it with
 *   `signInWithCredential(GoogleAuthProvider.getCredential(...))`. The Google token never reaches
 *   Supabase, and there is no browser OAuth flow and no redirect URI anywhere in the app.
 * * Phone sign-in uses `PhoneAuthProvider.verifyPhoneNumber(PhoneAuthOptions)`, which supports
 *   instant verification and SMS auto-retrieval, plus manual code entry and re-sending.
 *
 * @param firebaseAuth the initialized FirebaseAuth instance, or null when this build has no
 * `google-services.json`. A null instance makes every call fail with an actionable configuration
 * message instead of a crash at start-up.
 * @param activityProvider resolves the current resumed Activity; phone verification needs one
 */
class FirebaseAuthGateway(
    private val firebaseAuth: FirebaseAuth?,
    private val activityProvider: () -> Activity?,
    private val newChallengeId: () -> String = { UUID.randomUUID().toString() },
) : AuthGateway {

    override val isConfigured: Boolean
        get() = firebaseAuth != null

    private val lock = Any()
    private val pending = mutableMapOf<String, PendingVerification>()

    /** The verification id and resend token Firebase issued, kept away from the UI layer. */
    private class PendingVerification(
        val verificationId: String,
        val phoneNumber: String,
        val resendToken: PhoneAuthProvider.ForceResendingToken?,
    )

    /** Intermediate result of one `verifyPhoneNumber` attempt. */
    private sealed interface Step {
        data class Code(val challenge: PhoneChallenge) : Step
        data class AutoCredential(val credential: PhoneAuthCredential) : Step
    }

    override suspend fun awaitInitialIdentity(): AuthIdentity? {
        val auth = firebaseAuth ?: return null
        // The listener is invoked immediately with the restored state, and again on every later
        // change. Waiting for it is what stops a cold start from briefly looking signed out.
        val identity = withTimeoutOrNull(INITIAL_STATE_TIMEOUT_MILLIS) {
            suspendCancellableCoroutine<AuthIdentity?> { continuation ->
                var listener: FirebaseAuth.AuthStateListener? = null
                listener = FirebaseAuth.AuthStateListener { state ->
                    listener?.let { state.removeAuthStateListener(it) }
                    if (continuation.isActive) continuation.resume(identityOf(state.currentUser))
                }
                continuation.invokeOnCancellation { listener?.let { auth.removeAuthStateListener(it) } }
                auth.addAuthStateListener(listener)
            }
        }
        return identity ?: identityOf(auth.currentUser)
    }

    override suspend fun currentIdentity(): AuthIdentity? = identityOf(firebaseAuth?.currentUser)

    override suspend fun signInWithGoogleIdToken(idToken: String): AuthIdentity {
        val auth = requireAuth()
        if (idToken.isBlank()) {
            throw AuthGatewayException(
                AuthFailure(FirebaseAuthMessages.MISSING_TOKEN, AuthFailureKind.INVALID_TOKEN),
            )
        }
        return guarded {
            auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
            requireIdentity(auth)
        }
    }

    override suspend fun startPhoneVerification(phoneNumberE164: String): PhoneChallengeStart {
        val auth = requireAuth()
        return guarded { requestCode(auth, phoneNumberE164, null, null) }
    }

    override suspend fun resendPhoneCode(challenge: PhoneChallenge): PhoneChallengeStart {
        val auth = requireAuth()
        val existing = synchronized(lock) { pending[challenge.id] }
            ?: throw AuthGatewayException(
                AuthFailure(FirebaseAuthMessages.EXPIRED_CODE, AuthFailureKind.SESSION_EXPIRED),
            )
        // Same challenge id, fresh verification id and resend token from Firebase.
        return guarded { requestCode(auth, existing.phoneNumber, existing.resendToken, challenge.id) }
    }

    override suspend fun verifyPhoneCode(challenge: PhoneChallenge, code: String): AuthIdentity {
        val auth = requireAuth()
        val trimmed = code.trim()
        if (trimmed.isEmpty()) {
            throw AuthGatewayException(
                AuthFailure(FirebaseAuthMessages.CODE_REQUIRED, AuthFailureKind.REJECTED),
            )
        }
        // Keep the challenge in the map: an incorrect code must not force the person to request a
        // new SMS, and Firebase allows another attempt against the same verification id.
        val entry = synchronized(lock) { pending[challenge.id] }
            ?: throw AuthGatewayException(
                AuthFailure(FirebaseAuthMessages.EXPIRED_CODE, AuthFailureKind.SESSION_EXPIRED),
            )
        val identity = guarded {
            auth.signInWithCredential(
                PhoneAuthProvider.getCredential(entry.verificationId, trimmed),
            ).await()
            requireIdentity(auth)
        }
        synchronized(lock) { pending.remove(challenge.id) }
        return identity
    }

    override suspend fun discardPhoneChallenge(challenge: PhoneChallenge) {
        synchronized(lock) { pending.remove(challenge.id) }
    }

    override suspend fun accessToken(forceRefresh: Boolean): AccessToken? {
        val user = firebaseAuth?.currentUser ?: return null
        return guarded {
            val result = user.getIdToken(forceRefresh).await()
            val token = result.token
                ?: throw AuthGatewayException(
                    AuthFailure(FirebaseAuthMessages.MISSING_TOKEN, AuthFailureKind.INVALID_TOKEN),
                )
            // Firebase reports milliseconds; the bridge and Supabase reason in seconds.
            val expiresSeconds = (result.expirationTimestamp / 1000L)
                .takeIf { it > 0 }
                ?: ((System.currentTimeMillis() + DEFAULT_TOKEN_LIFETIME_MILLIS) / 1000L)
            AccessToken(token = token, expiresAtEpochSeconds = expiresSeconds)
        }
    }

    override suspend fun signOut() {
        synchronized(lock) { pending.clear() }
        firebaseAuth?.signOut()
    }

    override suspend fun deleteAccount() {
        val auth = requireAuth()
        val user = auth.currentUser
            ?: throw AuthGatewayException(
                AuthFailure(FirebaseAuthMessages.REQUIRES_RECENT_LOGIN, AuthFailureKind.SESSION_EXPIRED),
            )
        guarded { user.delete().await() }
    }

    /**
     * Ask Firebase for a code.
     *
     * Firebase's callbacks arrive on the main thread; this bridges them to a single suspension so the
     * repository can treat the whole verification as one coroutine. Only the first outcome matters —
     * a late `onVerificationCompleted` after the code was already accepted is ignored.
     */
    private suspend fun requestCode(
        auth: FirebaseAuth,
        phoneNumber: String,
        forceResendToken: PhoneAuthProvider.ForceResendingToken?,
        existingChallengeId: String?,
    ): PhoneChallengeStart {
        val activity = activityProvider()?.takeIf { !it.isFinishing && !it.isDestroyed }
            ?: throw AuthGatewayException(
                AuthFailure(FirebaseAuthMessages.NO_ACTIVITY, AuthFailureKind.UNKNOWN),
            )

        val step = withTimeoutOrNull(VERIFICATION_TIMEOUT_MILLIS) {
            suspendCancellableCoroutine<Step> { continuation ->
                val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        if (continuation.isActive) continuation.resume(Step.AutoCredential(credential))
                    }

                    override fun onVerificationFailed(exception: FirebaseException) {
                        if (!continuation.isActive) return
                        continuation.resumeWithException(
                            AuthGatewayException(failureFor(exception)),
                        )
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken,
                    ) {
                        val challenge = PhoneChallenge(existingChallengeId ?: newChallengeId())
                        synchronized(lock) {
                            pending[challenge.id] =
                                PendingVerification(verificationId, phoneNumber, token)
                        }
                        if (continuation.isActive) continuation.resume(Step.Code(challenge))
                    }
                }

                val builder = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(phoneNumber)
                    .setTimeout(OTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(callbacks)
                forceResendToken?.let { builder.setForceResendingToken(it) }
                PhoneAuthProvider.verifyPhoneNumber(builder.build())
            }
        } ?: throw AuthGatewayException(
            AuthFailure(FirebaseAuthMessages.TIMEOUT, AuthFailureKind.TIMEOUT),
        )

        return when (step) {
            is Step.Code -> PhoneChallengeStart.CodeSent(step.challenge)
            is Step.AutoCredential -> {
                // Instant verification or SMS auto-retrieval already produced a credential.
                guarded {
                    auth.signInWithCredential(step.credential).await()
                    requireIdentity(auth)
                }
                PhoneChallengeStart.AutoVerified
            }
        }
    }

    private fun requireAuth(): FirebaseAuth = firebaseAuth ?: throw AuthGatewayException(
        AuthFailure(FirebaseAuthMessages.UNCONFIGURED, AuthFailureKind.UNCONFIGURED),
    )

    private fun requireIdentity(auth: FirebaseAuth): AuthIdentity = identityOf(auth.currentUser)
        ?: throw AuthGatewayException(
            AuthFailure(FirebaseAuthMessages.INVALID_CREDENTIAL, AuthFailureKind.INVALID_TOKEN),
        )

    private fun identityOf(user: FirebaseUser?): AuthIdentity? {
        if (user == null) return null
        val provider = AuthProviderId.fromFirebaseProviderId(user.providerData.firstOrNull()?.providerId)
            ?: if (user.phoneNumber != null) AuthProviderId.PHONE else AuthProviderId.GOOGLE
        val createdMillis = user.metadata?.creationTimestamp ?: 0L
        return AuthIdentity(
            uid = user.uid,
            email = user.email?.takeIf { it.isNotBlank() },
            phoneNumber = user.phoneNumber?.takeIf { it.isNotBlank() },
            provider = provider,
            memberSinceEpochSeconds = createdMillis.takeIf { it > 0 }?.div(1000L),
            suggestedFullName = user.displayName?.trim()?.takeIf { it.isNotEmpty() },
            // Firebase has no username, so onboarding always lets the person choose one.
            suggestedUsername = null,
            suggestedAvatarUrl = user.photoUrl?.toString()?.takeIf { it.isNotBlank() },
        )
    }

    private fun failureFor(exception: FirebaseException): AuthFailure = when (exception) {
        is FirebaseNetworkException ->
            AuthFailure(FirebaseAuthMessages.OFFLINE, AuthFailureKind.NETWORK)

        is FirebaseAuthException ->
            FirebaseAuthErrors.forCode(exception.errorCode, exception.message)

        else -> FirebaseAuthErrors.forCode(null, exception.message)
    }

    /** Converts every Firebase failure into an already-safe [AuthGatewayException]. */
    private suspend fun <T> guarded(block: suspend () -> T): T = try {
        block()
    } catch (error: AuthGatewayException) {
        throw error
    } catch (error: FirebaseException) {
        throw AuthGatewayException(failureFor(error))
    }

    private companion object {
        const val INITIAL_STATE_TIMEOUT_MILLIS = 5_000L
        const val VERIFICATION_TIMEOUT_MILLIS = 65_000L
        const val OTP_TIMEOUT_SECONDS = 60L
        const val DEFAULT_TOKEN_LIFETIME_MILLIS = 55L * 60L * 1000L
    }
}
