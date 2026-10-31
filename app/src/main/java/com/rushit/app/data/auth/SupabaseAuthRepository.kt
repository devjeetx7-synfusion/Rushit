package com.rushit.app.data.auth

import com.rushit.app.data.model.Profile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Orchestrates RushIt authentication on top of an [AuthGateway].
 *
 * This class holds no Android and no Supabase types, which is what makes the whole sign-in,
 * provisioning, onboarding, sign-out and deletion flow unit-testable.
 *
 * Invariants enforced here:
 * - `auth.users.id` is the only account identifier; email is display data.
 * - A profile row is created at most once per user, and saved values are never overwritten by
 *   provider metadata on a later sign-in.
 * - A single [Mutex] serializes restore, sign-in and account mutation, so a double tap or a
 *   background refresh cannot interleave two writes to the same account.
 */
class SupabaseAuthRepository(
    private val gateway: AuthGateway,
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val mutex = Mutex()

    override suspend fun initialize() {
        if (!gateway.isConfigured) {
            _authState.value = AuthState.RecoverableError(
                message = Messages.SUPABASE_UNCONFIGURED,
                kind = AuthFailureKind.UNCONFIGURED,
                action = RecoveryAction.DISMISS,
            )
            return
        }
        restore(announceLoading = true)
    }

    override suspend fun retrySessionRestore() = restore(announceLoading = true)

    override suspend fun signInWithGoogle(idToken: String, rawNonce: String?): AuthResult {
        if (!gateway.isConfigured) {
            return AuthResult.Failure(Messages.SUPABASE_UNCONFIGURED, AuthFailureKind.UNCONFIGURED)
        }
        if (idToken.isBlank()) {
            return AuthResult.Failure(Messages.MISSING_TOKEN, AuthFailureKind.INVALID_TOKEN)
        }

        return mutex.withLock {
            val previous = _authState.value
            _authState.value = AuthState.Authenticating
            try {
                val session = gateway.signInWithIdToken(AuthProviderId.GOOGLE, idToken, rawNonce)
                recordActivity(session.userId, EVENT_SIGN_IN, mapOf("provider" to AuthProviderId.GOOGLE.wireValue))
                resolveAccount(session)
            } catch (error: Throwable) {
                val failure = AuthErrors.describeOrRethrow(error)
                // Never leave the UI stuck on "Authenticating", and never downgrade a person who was
                // already signed in to the login screen because a re-authentication attempt failed.
                _authState.value = when {
                    previous is AuthState.Authenticated || previous is AuthState.ProfileIncomplete -> previous
                    else -> AuthState.Unauthenticated
                }
                AuthResult.Failure(failure.message, failure.kind)
            }
        }
    }

    override suspend fun saveProfile(
        fullName: String,
        username: String?,
        avatarUrl: String?,
    ): AuthResult {
        val userId = currentUserId()
            ?: return AuthResult.Failure(Messages.SIGN_IN_AGAIN, AuthFailureKind.SESSION_EXPIRED)

        val submission = when (val result = ProfileOnboarding.submissionFor(fullName, username, avatarUrl)) {
            is SubmissionResult.Rejected ->
                return AuthResult.Failure(result.message, AuthFailureKind.UNKNOWN)

            is SubmissionResult.Accepted -> result.submission
        }

        // Keep a photo the person already has when the form did not carry one forward.
        val avatar = submission.avatarUrl ?: savedAvatarUrl()

        // Friendly pre-check only. The database unique index is the real guard, because two people
        // can pass this check at the same moment.
        val requestedUsername = submission.username
        if (requestedUsername != null) {
            val taken = runCatching { gateway.usernameTaken(requestedUsername) }.getOrDefault(false)
            if (taken) {
                return AuthResult.Failure(Messages.USERNAME_TAKEN, AuthFailureKind.CONFLICT)
            }
        }

        return try {
            val profile = gateway.saveProfile(
                userId = userId,
                fullName = submission.fullName,
                username = requestedUsername,
                avatarUrl = avatar,
            )
            val session = gateway.currentSession()
                ?: return AuthResult.Failure(Messages.SIGN_IN_AGAIN, AuthFailureKind.SESSION_EXPIRED)

            recordActivity(userId, EVENT_PROFILE_COMPLETED, emptyMap())
            _authState.value = AuthState.Authenticated(accountFor(session, profile))
            AuthResult.Success
        } catch (error: Throwable) {
            val failure = AuthErrors.describeOrRethrow(error)
            // Stay in onboarding so the entered values are not lost.
            val message = if (failure.kind == AuthFailureKind.CONFLICT) Messages.USERNAME_TAKEN else failure.message
            AuthResult.Failure(message, failure.kind)
        }
    }

    override suspend fun refreshAccount(): AuthResult = restore(announceLoading = false)

    override suspend fun checkUsername(username: String): UsernameAvailability {
        val normalized = UsernameRules.normalize(username)
        if (normalized.isEmpty()) return UsernameAvailability.Available
        UsernameRules.validate(normalized)?.let { return UsernameAvailability.Unknown }
        return try {
            if (gateway.usernameTaken(normalized)) UsernameAvailability.Taken else UsernameAvailability.Available
        } catch (error: Throwable) {
            if (error is CancellationException && error !is TimeoutCancellationException) throw error
            // An unavailable check must not block onboarding; the database still enforces uniqueness.
            UsernameAvailability.Unknown
        }
    }

    override suspend fun signOut(): AuthResult = mutex.withLock {
        val userId = currentUserId()
        if (userId != null) recordActivity(userId, EVENT_SIGN_OUT, emptyMap())

        val revokedRemotely = try {
            gateway.signOut()
        } catch (error: Throwable) {
            val failure = AuthErrors.describeOrRethrow(error)
            _authState.value = AuthState.Unauthenticated
            return@withLock AuthResult.Failure(failure.message, failure.kind)
        }
        _authState.value = AuthState.Unauthenticated
        if (revokedRemotely) {
            AuthResult.Success
        } else {
            // Local state is already cleared; report that the server-side revocation did not happen
            // instead of swallowing it.
            AuthResult.Failure(Messages.SIGN_OUT_PARTIAL, AuthFailureKind.NETWORK)
        }
    }

    override suspend fun deleteAccount(): AuthResult = mutex.withLock {
        val current = _authState.value
        if (current !is AuthState.Authenticated && current !is AuthState.ProfileIncomplete) {
            return@withLock AuthResult.Failure(Messages.SIGN_IN_AGAIN, AuthFailureKind.SESSION_EXPIRED)
        }
        if (!gateway.isConfigured) {
            return@withLock AuthResult.Failure(Messages.SUPABASE_UNCONFIGURED, AuthFailureKind.UNCONFIGURED)
        }

        try {
            when (gateway.deleteAccount()) {
                DeleteAccountOutcome.Deleted -> {
                    // Clear the local session too; the auth user no longer exists.
                    runCatching { gateway.signOut() }
                    _authState.value = AuthState.Unauthenticated
                    AuthResult.Success
                }

                DeleteAccountOutcome.NotDeployed ->
                    AuthResult.Failure(Messages.DELETE_NOT_DEPLOYED, AuthFailureKind.UNAVAILABLE)
            }
        } catch (error: Throwable) {
            val failure = AuthErrors.describeOrRethrow(error)
            AuthResult.Failure(failure.message, failure.kind)
        }
    }

    /**
     * Restore the stored session and publish the resulting state.
     *
     * @param announceLoading whether to move to [AuthState.Loading] first. A background refresh keeps
     * the current state so the UI does not flicker.
     */
    private suspend fun restore(announceLoading: Boolean): AuthResult = mutex.withLock {
        if (announceLoading) _authState.value = AuthState.Loading

        // A background refresh must never throw an already signed-in person out because of a
        // transient network failure. Only a definitively gone session may clear the state.
        val alreadySignedIn = _authState.value.let {
            it is AuthState.Authenticated || it is AuthState.ProfileIncomplete
        }

        val restored = try {
            gateway.restoreSession()
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

        when (restored) {
            is SessionRestoreResult.Restored -> resolveAccount(restored.session)

            SessionRestoreResult.NoSession -> {
                _authState.value = AuthState.Unauthenticated
                AuthResult.Success
            }

            // The refresh token was rejected and the SDK already cleared local storage.
            SessionRestoreResult.Rejected -> {
                _authState.value = AuthState.RecoverableError(
                    message = Messages.SESSION_EXPIRED,
                    kind = AuthFailureKind.SESSION_EXPIRED,
                    action = RecoveryAction.SIGN_IN_AGAIN,
                )
                AuthResult.Failure(Messages.SESSION_EXPIRED, AuthFailureKind.SESSION_EXPIRED)
            }

            is SessionRestoreResult.Unreachable -> {
                if (alreadySignedIn) {
                    // Keep the person signed in; the SDK keeps retrying the refresh in the background.
                    AuthResult.Failure(Messages.OFFLINE_WITH_SESSION, AuthFailureKind.NETWORK)
                } else if (restored.sessionStillStored) {
                    _authState.value = AuthState.RecoverableError(
                        message = Messages.OFFLINE_WITH_SESSION,
                        kind = AuthFailureKind.NETWORK,
                        action = RecoveryAction.RETRY_SESSION,
                    )
                    AuthResult.Failure(Messages.OFFLINE_WITH_SESSION, AuthFailureKind.NETWORK)
                } else {
                    _authState.value = AuthState.Unauthenticated
                    AuthResult.Success
                }
            }
        }
    }

    /**
     * Load the profile for [session], provisioning it on first use, and publish the resulting state.
     *
     * Called with [mutex] held.
     */
    private suspend fun resolveAccount(session: SessionSnapshot): AuthResult {
        val alreadySignedIn = _authState.value.let {
            it is AuthState.Authenticated || it is AuthState.ProfileIncomplete
        }
        val existing = try {
            gateway.fetchProfile(session.userId)
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
            _authState.value = AuthState.Authenticated(accountFor(session, existing))
            return AuthResult.Success
        }

        // First-time user: create the row once. ignoreDuplicates plus the primary key means two
        // concurrent sign-ins for the same user cannot produce two rows, and a returning user's
        // saved name or avatar is never replaced by Google's copy.
        val provisioned = if (existing == null) {
            val values = ProfileOnboarding.newProfileValues(session)
            runCatching { gateway.provisionProfile(values.userId, values.fullName, values.avatarUrl) }
                .getOrNull()
                .also { runCatching { gateway.ensureSettings(session.userId) } }
        } else {
            existing
        }

        _authState.value = AuthState.ProfileIncomplete(
            ProfileOnboarding.draftFor(session, provisioned ?: existing),
        )
        return AuthResult.Success
    }

    private fun accountFor(session: SessionSnapshot, profile: Profile) = AuthAccount(
        id = session.userId,
        email = session.email,
        provider = session.provider,
        memberSinceEpochSeconds = session.memberSinceEpochSeconds,
        profile = profile,
    )

    private fun currentUserId(): String? = when (val state = _authState.value) {
        is AuthState.Authenticated -> state.account.id
        is AuthState.ProfileIncomplete -> state.draft.id
        else -> null
    }

    private fun savedAvatarUrl(): String? = when (val state = _authState.value) {
        is AuthState.Authenticated -> state.account.avatarUrl
        is AuthState.ProfileIncomplete -> state.draft.initialAvatarUrl
        else -> null
    }

    /** Activity logging is best-effort: it must never fail a sign-in, save or sign-out. */
    private suspend fun recordActivity(
        userId: String,
        eventType: String,
        metadata: Map<String, String>,
    ) {
        // Metadata is limited to non-sensitive labels; never tokens, emails, or device identifiers.
        runCatching { gateway.recordActivity(userId, eventType, metadata) }
    }

    object Messages {
        const val SUPABASE_UNCONFIGURED =
            "RushIt is not connected to its account service in this build. Set SUPABASE_URL and " +
                "SUPABASE_ANON_KEY, then rebuild."
        const val MISSING_TOKEN = "Google did not return a sign-in token. Please try again."
        const val SIGN_IN_AGAIN = "Please sign in again to continue."
        const val SESSION_EXPIRED = "Your session has expired. Please sign in again."
        const val OFFLINE_WITH_SESSION =
            "RushIt could not reach its servers, so your saved session could not be confirmed. " +
                "Check your connection and try again."
        const val USERNAME_TAKEN = "That username is already taken. Please choose another one."
        const val SIGN_OUT_PARTIAL =
            "You are signed out on this device. RushIt could not revoke the session on the server, " +
                "so it will expire on its own."
        const val DELETE_NOT_DEPLOYED =
            "Account deletion is not available yet. Deploy the delete-account Edge Function (or the " +
                "delete_user_account RPC) for this Supabase project, then try again."
    }

    private companion object {
        const val EVENT_SIGN_IN = "sign_in"
        const val EVENT_SIGN_OUT = "sign_out"
        const val EVENT_PROFILE_COMPLETED = "profile_completed"
    }
}
