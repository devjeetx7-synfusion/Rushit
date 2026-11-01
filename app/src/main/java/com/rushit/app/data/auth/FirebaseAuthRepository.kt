package com.rushit.app.data.auth

import com.rushit.app.data.auth.firebase.FirebaseAuthMessages
import com.rushit.app.data.model.Profile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Orchestrates RushIt authentication on top of Firebase, and the application profile on top of
 * Supabase.
 *
 * This class holds no Android, Firebase or Supabase types, which is what makes the whole sign-in,
 * provisioning, onboarding, sign-out and deletion flow unit-testable.
 *
 * Invariants enforced here:
 * * The Firebase UID is the only account identifier; email is display data and may be absent for
 *   phone-only accounts.
 * * A profile row is created at most once per Firebase UID, and saved values are never overwritten by
 *   provider metadata on a later sign-in.
 * * The Supabase client only ever holds the token of the user that is currently signed in. The token
 *   is installed before every backend operation and removed on sign-out, on deletion and whenever
 *   Firebase reports no session, so a request can never be silently authorized as the previous user.
 * * A single [Mutex] serializes restore, sign-in and account mutation, so a double tap or a
 *   background refresh cannot interleave two writes to the same account.
 */
class FirebaseAuthRepository(
    private val auth: AuthGateway,
    private val profiles: ProfileGateway,
    private val tokens: BackendTokenBridge,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val mutex = Mutex()

    /** The verification Firebase is currently waiting on, and when it was requested. */
    private var pendingPhone: PendingPhone? = null

    private data class PendingPhone(val challenge: PhoneChallenge, val requestedAtMillis: Long)

    override suspend fun initialize() {
        if (!auth.isConfigured) {
            _authState.value = AuthState.RecoverableError(
                message = Messages.FIREBASE_UNCONFIGURED,
                kind = AuthFailureKind.UNCONFIGURED,
                action = RecoveryAction.DISMISS,
            )
            return
        }
        if (!profiles.isConfigured) {
            _authState.value = AuthState.RecoverableError(
                message = Messages.SUPABASE_UNCONFIGURED,
                kind = AuthFailureKind.UNCONFIGURED,
                action = RecoveryAction.DISMISS,
            )
            return
        }
        restore(announceLoading = true)
    }

    override suspend fun retrySessionRestore() {
        // The restore result is already published as state; a retry has nothing to return.
        restore(announceLoading = true)
    }

    override suspend fun signInWithGoogle(idToken: String): AuthResult {
        if (!auth.isConfigured) {
            return AuthResult.Failure(Messages.FIREBASE_UNCONFIGURED, AuthFailureKind.UNCONFIGURED)
        }
        if (idToken.isBlank()) {
            return AuthResult.Failure(Messages.MISSING_TOKEN, AuthFailureKind.INVALID_TOKEN)
        }
        return mutex.withLock {
            val previous = _authState.value
            _authState.value = AuthState.Authenticating
            try {
                completeSignIn(auth.signInWithGoogleIdToken(idToken))
            } catch (error: Throwable) {
                val failure = AuthErrors.describeOrRethrow(error)
                // Never leave the UI stuck on "Authenticating", and never downgrade a person who was
                // already signed in because a re-authentication attempt failed.
                _authState.value = when (previous) {
                    is AuthState.Authenticated, is AuthState.ProfileIncomplete -> previous
                    else -> AuthState.Unauthenticated
                }
                AuthResult.Failure(failure.message, failure.kind)
            }
        }
    }

    override suspend fun startPhoneVerification(phoneNumber: PhoneNumber): PhoneVerificationOutcome =
        mutex.withLock {
            if (!auth.isConfigured) {
                return@withLock PhoneVerificationOutcome.Failure(
                    Messages.FIREBASE_UNCONFIGURED,
                    AuthFailureKind.UNCONFIGURED,
                )
            }
            // One verification at a time: a second tap must not cost a second SMS.
            if (pendingPhone != null) {
                return@withLock PhoneVerificationOutcome.Failure(
                    Messages.CODE_ALREADY_REQUESTED,
                    AuthFailureKind.REJECTED,
                )
            }
            _authState.value = AuthState.Authenticating
            try {
                when (val start = auth.startPhoneVerification(phoneNumber.e164)) {
                    is PhoneChallengeStart.CodeSent -> {
                        pendingPhone = PendingPhone(start.challenge, clock())
                        _authState.value = AuthState.Unauthenticated
                        PhoneVerificationOutcome.CodeSent
                    }

                    PhoneChallengeStart.AutoVerified -> autoVerifiedOutcome()
                }
            } catch (error: Throwable) {
                val failure = AuthErrors.describeOrRethrow(error)
                _authState.value = AuthState.Unauthenticated
                PhoneVerificationOutcome.Failure(failure.message, failure.kind)
            }
        }

    override suspend fun resendPhoneCode(): PhoneVerificationOutcome = mutex.withLock {
        val pending = pendingPhone
            ?: return@withLock PhoneVerificationOutcome.Failure(
                Messages.CODE_NOT_REQUESTED,
                AuthFailureKind.REJECTED,
            )
        val elapsed = clock() - pending.requestedAtMillis
        if (elapsed < RESEND_INTERVAL_MILLIS) {
            val waitSeconds = ((RESEND_INTERVAL_MILLIS - elapsed) / 1000L).coerceAtLeast(1L)
            return@withLock PhoneVerificationOutcome.Failure(
                Messages.resendWait(waitSeconds),
                AuthFailureKind.REJECTED,
            )
        }
        try {
            when (val start = auth.resendPhoneCode(pending.challenge)) {
                is PhoneChallengeStart.CodeSent -> {
                    pendingPhone = PendingPhone(start.challenge, clock())
                    PhoneVerificationOutcome.CodeSent
                }

                PhoneChallengeStart.AutoVerified -> autoVerifiedOutcome()
            }
        } catch (error: Throwable) {
            val failure = AuthErrors.describeOrRethrow(error)
            PhoneVerificationOutcome.Failure(failure.message, failure.kind)
        }
    }

    override suspend fun submitPhoneCode(code: String): AuthResult = mutex.withLock {
        val pending = pendingPhone
            ?: return@withLock AuthResult.Failure(Messages.CODE_NOT_REQUESTED, AuthFailureKind.REJECTED)
        _authState.value = AuthState.Authenticating
        try {
            val identity = auth.verifyPhoneCode(pending.challenge, code)
            pendingPhone = null
            completeSignIn(identity)
        } catch (error: Throwable) {
            val failure = AuthErrors.describeOrRethrow(error)
            // Keep the challenge so a typo can be corrected without paying for another SMS.
            _authState.value = AuthState.Unauthenticated
            AuthResult.Failure(failure.message, failure.kind)
        }
    }

    override suspend fun cancelPhoneVerification() {
        mutex.withLock {
            val pending = pendingPhone ?: return@withLock
            pendingPhone = null
            runCatching { auth.discardPhoneChallenge(pending.challenge) }
            if (_authState.value == AuthState.Authenticating) {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    override suspend fun saveProfile(
        fullName: String,
        username: String?,
        avatarUrl: String?,
    ): AuthResult {
        val identity = auth.currentIdentity()
            ?: return AuthResult.Failure(Messages.SIGN_IN_AGAIN, AuthFailureKind.SESSION_EXPIRED)

        val submission = when (val result = ProfileOnboarding.submissionFor(fullName, username, avatarUrl)) {
            is SubmissionResult.Rejected -> return AuthResult.Failure(result.message, AuthFailureKind.UNKNOWN)
            is SubmissionResult.Accepted -> result.submission
        }

        // Keep a photo the person already has when the form did not carry one forward.
        val avatar = submission.avatarUrl ?: savedAvatarUrl()

        return mutex.withLock {
            try {
                installToken(forceRefresh = false)
                // Friendly pre-check only. The database unique index is the real guard, because two
                // people can pass this check at the same moment.
                val requestedUsername = submission.username
                if (requestedUsername != null &&
                    runCatching { profiles.usernameTaken(requestedUsername) }.getOrDefault(false)
                ) {
                    return@withLock AuthResult.Failure(AuthErrors.USERNAME_TAKEN, AuthFailureKind.CONFLICT)
                }

                val profile = profiles.saveProfile(
                    firebaseUid = identity.uid,
                    fullName = submission.fullName,
                    username = requestedUsername,
                    avatarUrl = avatar,
                )
                recordActivity(identity.uid, EVENT_PROFILE_COMPLETED, emptyMap())
                _authState.value = AuthState.Authenticated(accountFor(identity, profile))
                AuthResult.Success
            } catch (error: Throwable) {
                val failure = AuthErrors.describeOrRethrow(error)
                // Stay in onboarding so the entered values are not lost.
                val message = if (failure.kind == AuthFailureKind.CONFLICT) {
                    AuthErrors.USERNAME_TAKEN
                } else {
                    failure.message
                }
                AuthResult.Failure(message, failure.kind)
            }
        }
    }

    override suspend fun refreshAccount(): AuthResult = restore(announceLoading = false)

    override suspend fun checkUsername(username: String): UsernameAvailability {
        val normalized = UsernameRules.normalize(username)
        if (normalized.isEmpty()) return UsernameAvailability.Available
        UsernameRules.validate(normalized)?.let { return UsernameAvailability.Unknown }
        return try {
            installToken(forceRefresh = false)
            if (profiles.usernameTaken(normalized)) UsernameAvailability.Taken else UsernameAvailability.Available
        } catch (error: Throwable) {
            if (error is CancellationException && error !is TimeoutCancellationException) throw error
            // An unavailable check must not block onboarding; the database still enforces uniqueness.
            UsernameAvailability.Unknown
        }
    }

    override suspend fun signOut(): AuthResult = mutex.withLock {
        auth.currentIdentity()?.let { recordActivity(it.uid, EVENT_SIGN_OUT, emptyMap()) }
        pendingPhone?.let { runCatching { auth.discardPhoneChallenge(it.challenge) } }
        pendingPhone = null

        var failure: AuthFailure? = null
        try {
            auth.signOut()
        } catch (error: Throwable) {
            failure = AuthErrors.describeOrRethrow(error)
        } finally {
            // Unconditional: a failed Firebase sign-out must never leave the previous user's bearer
            // token installed on the Supabase client.
            runCatching { tokens.clear() }
            _authState.value = AuthState.Unauthenticated
        }
        failure?.let { AuthResult.Failure(it.message, it.kind) } ?: AuthResult.Success
    }

    override suspend fun deleteAccount(): AuthResult = mutex.withLock {
        val identity = auth.currentIdentity()
            ?: return@withLock AuthResult.Failure(Messages.SIGN_IN_AGAIN, AuthFailureKind.SESSION_EXPIRED)
        if (!profiles.isConfigured) {
            return@withLock AuthResult.Failure(
                Messages.SUPABASE_UNCONFIGURED,
                AuthFailureKind.UNCONFIGURED,
            )
        }

        // A freshly minted token is required: it must still be accepted by Supabase after Firebase
        // has deleted the account, so the profile purge in the last step can be authorized.
        val token = try {
            auth.accessToken(forceRefresh = true)
        } catch (error: Throwable) {
            val failure = AuthErrors.describeOrRethrow(error)
            return@withLock AuthResult.Failure(failure.message, failure.kind)
        } ?: return@withLock AuthResult.Failure(Messages.SIGN_IN_AGAIN, AuthFailureKind.SESSION_EXPIRED)

        try {
            tokens.use(token)
            auth.deleteAccount()
        } catch (error: Throwable) {
            // Firebase refused (usually a stale sign-in that needs reauthentication). Nothing has been
            // deleted at this point, so the account and profile are both intact.
            val failure = AuthErrors.describeOrRethrow(error)
            return@withLock AuthResult.Failure(failure.message, failure.kind)
        }

        // The Firebase account no longer exists. Purge the profile row — and, by cascade, settings,
        // devices and activity — with the token that was still valid a moment ago.
        val purge = runCatching { profiles.deleteProfile(identity.uid) }
        runCatching { tokens.clear() }
        pendingPhone = null
        _authState.value = AuthState.Unauthenticated

        if (purge.isSuccess) {
            AuthResult.Success
        } else {
            AuthResult.Failure(Messages.DELETE_PARTIAL, AuthFailureKind.UNAVAILABLE)
        }
    }

    /**
     * Restore the persisted Firebase session and publish the resulting state.
     *
     * @param announceLoading whether to move to [AuthState.Loading] first. A background refresh keeps
     * the current state so the UI does not flicker.
     */
    private suspend fun restore(announceLoading: Boolean): AuthResult = mutex.withLock {
        if (announceLoading) _authState.value = AuthState.Loading

        // A background refresh must never throw an already signed-in person out because of a
        // transient network failure. Only a definitively gone session may clear the state.
        val alreadySignedIn = _authState.value.isSignedIn()

        val identity = try {
            auth.awaitInitialIdentity()
        } catch (error: Throwable) {
            val failure = AuthErrors.describeOrRethrow(error)
            if (!alreadySignedIn) {
                _authState.value = AuthState.RecoverableError(
                    message = failure.message,
                    kind = failure.kind,
                    action = RecoveryAction.RETRY_SESSION,
                )
            }
            return@withLock AuthResult.Failure(failure.message, failure.kind)
        }

        if (identity == null) {
            // No Firebase session: make sure no stale backend token can be used.
            runCatching { tokens.clear() }
            pendingPhone = null
            _authState.value = AuthState.Unauthenticated
            return@withLock AuthResult.Success
        }

        try {
            installToken(forceRefresh = false)
        } catch (error: Throwable) {
            val failure = AuthErrors.describeOrRethrow(error)
            if (!alreadySignedIn) {
                _authState.value = AuthState.RecoverableError(
                    message = failure.message,
                    kind = failure.kind,
                    action = RecoveryAction.RETRY_SESSION,
                )
            }
            return@withLock AuthResult.Failure(failure.message, failure.kind)
        }

        resolveAccount(identity)
    }

    /**
     * Load the profile for [identity], provisioning it on first use, and publish the resulting state.
     *
     * Called with [mutex] held.
     */
    private suspend fun resolveAccount(identity: AuthIdentity): AuthResult {
        val alreadySignedIn = _authState.value.isSignedIn()
        val existing = try {
            profiles.findProfile(identity.uid)
        } catch (error: Throwable) {
            val failure = AuthErrors.describeOrRethrow(error)
            if (!alreadySignedIn) {
                _authState.value = AuthState.RecoverableError(
                    message = failure.message,
                    kind = failure.kind,
                    action = RecoveryAction.RETRY_SESSION,
                )
            }
            return AuthResult.Failure(failure.message, failure.kind)
        }

        if (existing != null && !ProfileOnboarding.requiresOnboarding(existing)) {
            _authState.value = AuthState.Authenticated(accountFor(identity, existing))
            return AuthResult.Success
        }

        // First-time user: create the row once. Conflicts on firebase_uid are ignored, so two
        // concurrent sign-ins for the same user cannot produce two rows, and a returning user's
        // saved name or avatar is never replaced by the provider's copy.
        val provisioned = if (existing == null) {
            try {
                val values = ProfileOnboarding.newProfileValues(identity)
                val created = profiles.provisionProfile(identity.uid, values.fullName, values.avatarUrl)
                runCatching { profiles.ensureSettings(identity.uid) }
                created
            } catch (error: Throwable) {
                val failure = AuthErrors.describeOrRethrow(error)
                if (!alreadySignedIn) {
                    _authState.value = AuthState.RecoverableError(
                        message = failure.message,
                        kind = failure.kind,
                        action = RecoveryAction.RETRY_SESSION,
                    )
                }
                return AuthResult.Failure(failure.message, failure.kind)
            }
        } else {
            existing
        }

        _authState.value = AuthState.ProfileIncomplete(
            ProfileOnboarding.draftFor(identity, provisioned ?: existing),
        )
        return AuthResult.Success
    }

    /** Sign-in completed at Firebase: install the token, then load or create the profile. */
    private suspend fun completeSignIn(identity: AuthIdentity): AuthResult {
        // A role custom claim assigned by a blocking function or the Admin SDK only appears in a new
        // token, so the first backend request after sign-in forces a refresh.
        installToken(forceRefresh = true)
        val result = resolveAccount(identity)
        if (result is AuthResult.Success) {
            recordActivity(identity.uid, EVENT_SIGN_IN, mapOf("provider" to identity.provider.wireValue))
        }
        return result
    }

    /** Firebase verified the phone without a code; the user is already signed in. */
    private suspend fun autoVerifiedOutcome(): PhoneVerificationOutcome {
        pendingPhone = null
        return try {
            val identity = auth.currentIdentity()
                ?: throw AuthGatewayException(
                    AuthFailure(Messages.SIGN_IN_AGAIN, AuthFailureKind.SESSION_EXPIRED),
                )
            when (val result = completeSignIn(identity)) {
                AuthResult.Success -> PhoneVerificationOutcome.Completed
                is AuthResult.Failure -> PhoneVerificationOutcome.Failure(result.message, result.kind)
            }
        } catch (error: Throwable) {
            val failure = AuthErrors.describeOrRethrow(error)
            _authState.value = AuthState.Unauthenticated
            PhoneVerificationOutcome.Failure(failure.message, failure.kind)
        }
    }

    /** Hand the current Firebase ID token to the Supabase client. */
    private suspend fun installToken(forceRefresh: Boolean) {
        val token = auth.accessToken(forceRefresh)
            ?: throw AuthGatewayException(
                AuthFailure(Messages.SIGN_IN_AGAIN, AuthFailureKind.SESSION_EXPIRED),
            )
        tokens.use(token)
    }

    private fun accountFor(identity: AuthIdentity, profile: Profile) = AuthAccount(
        uid = identity.uid,
        email = identity.email,
        phoneNumber = identity.phoneNumber ?: profile.phoneNumber,
        provider = identity.provider,
        memberSinceEpochSeconds = identity.memberSinceEpochSeconds,
        profile = profile,
    )

    private fun AuthState.isSignedIn(): Boolean =
        this is AuthState.Authenticated || this is AuthState.ProfileIncomplete

    private fun savedAvatarUrl(): String? = when (val state = _authState.value) {
        is AuthState.Authenticated -> state.account.avatarUrl
        is AuthState.ProfileIncomplete -> state.draft.initialAvatarUrl
        else -> null
    }

    /** Activity logging is best-effort: it must never fail a sign-in, save or sign-out. */
    private suspend fun recordActivity(
        firebaseUid: String,
        eventType: String,
        metadata: Map<String, String>,
    ) {
        // Metadata is limited to non-sensitive labels; never tokens, emails, or device identifiers.
        runCatching { profiles.recordActivity(firebaseUid, eventType, metadata) }
    }

    object Messages {
        const val FIREBASE_UNCONFIGURED = FirebaseAuthMessages.UNCONFIGURED
        const val SUPABASE_UNCONFIGURED = ProfileMessages.SUPABASE_UNCONFIGURED
        const val MISSING_TOKEN = "Google did not return a sign-in token. Please try again."
        const val SIGN_IN_AGAIN = "Please sign in again to continue."
        const val CODE_NOT_REQUESTED = "Request a code before entering one."
        const val CODE_ALREADY_REQUESTED =
            "RushIt already sent a code. Enter it, or wait before asking for another."
        const val DELETE_PARTIAL =
            "Your sign-in was removed, but RushIt could not delete your saved profile. Please " +
                "contact RushIt support so it can be cleared."

        fun resendWait(seconds: Long): String =
            "Please wait ${seconds}s before requesting another code."
    }

    private companion object {
        const val EVENT_SIGN_IN = "sign_in"
        const val EVENT_SIGN_OUT = "sign_out"
        const val EVENT_PROFILE_COMPLETED = "profile_completed"

        /** Firebase's own resend throttle is per phone number; this is the UI-facing minimum. */
        const val RESEND_INTERVAL_MILLIS = 30_000L
    }
}
