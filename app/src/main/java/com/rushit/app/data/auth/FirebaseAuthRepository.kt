package com.rushit.app.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.rushit.app.data.auth.firebase.FirebaseAuthMessages
import com.rushit.app.data.auth.phone.PhoneNumber
import com.rushit.app.data.model.Profile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Production-ready Firebase Authentication repository for RushIt.
 *
 * Firebase Authentication is the sole source of truth for user identity and session persistence.
 * User profile information (display name, email, phone number, photo) is extracted directly
 * from FirebaseUser. Locally customized profile values (full name, username, avatar) are stored
 * in private SharedPreferences without inventing a fake backend database.
 */
class FirebaseAuthRepository(
    private val auth: AuthGateway,
    private val context: Context? = null,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val mutex = Mutex()
    private var pendingPhone: PendingPhone? = null

    private data class PendingPhone(val challenge: PhoneChallenge, val requestedAtMillis: Long)

    private val prefs: SharedPreferences? by lazy {
        context?.getSharedPreferences("rushit_auth_profile", Context.MODE_PRIVATE)
    }

    override suspend fun initialize() {
        if (!auth.isConfigured) {
            _authState.value = AuthState.RecoverableError(
                message = FirebaseAuthMessages.UNCONFIGURED,
                kind = AuthFailureKind.UNCONFIGURED,
                action = RecoveryAction.DISMISS,
            )
            return
        }
        mutex.withLock {
            try {
                val identity = auth.awaitInitialIdentity()
                if (identity != null) {
                    _authState.value = resolveAccountState(identity)
                } else {
                    _authState.value = AuthState.Unauthenticated
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val failure = AuthErrors.describe(error)
                _authState.value = AuthState.RecoverableError(
                    message = failure.message,
                    kind = failure.kind,
                    action = RecoveryAction.RETRY_SESSION,
                )
            }
        }
    }

    override suspend fun retrySessionRestore() {
        initialize()
    }

    override suspend fun signInWithGoogle(idToken: String): AuthResult = mutex.withLock {
        if (!auth.isConfigured) {
            return AuthResult.Failure(FirebaseAuthMessages.UNCONFIGURED, AuthFailureKind.UNCONFIGURED)
        }
        _authState.value = AuthState.Authenticating
        return try {
            val identity = auth.signInWithGoogleIdToken(idToken)
            _authState.value = resolveAccountState(identity)
            AuthResult.Success
        } catch (error: CancellationException) {
            _authState.value = AuthState.Unauthenticated
            throw error
        } catch (error: Throwable) {
            val failure = AuthErrors.describe(error)
            _authState.value = AuthState.Unauthenticated
            AuthResult.Failure(failure.message, failure.kind)
        }
    }

    override suspend fun startPhoneVerification(phoneNumber: PhoneNumber): PhoneVerificationOutcome = mutex.withLock {
        if (!auth.isConfigured) {
            return PhoneVerificationOutcome.Failure(FirebaseAuthMessages.UNCONFIGURED, AuthFailureKind.UNCONFIGURED)
        }
        return try {
            when (val start = auth.startPhoneVerification(phoneNumber.e164)) {
                is PhoneChallengeStart.CodeSent -> {
                    pendingPhone = PendingPhone(start.challenge, clock())
                    PhoneVerificationOutcome.CodeSent
                }
                PhoneChallengeStart.AutoVerified -> {
                    val identity = auth.currentIdentity()
                    if (identity != null) {
                        _authState.value = resolveAccountState(identity)
                        PhoneVerificationOutcome.Completed
                    } else {
                        PhoneVerificationOutcome.Failure(FirebaseAuthMessages.INVALID_CREDENTIAL, AuthFailureKind.INVALID_TOKEN)
                    }
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            val failure = AuthErrors.describe(error)
            PhoneVerificationOutcome.Failure(failure.message, failure.kind)
        }
    }

    override suspend fun resendPhoneCode(): PhoneVerificationOutcome = mutex.withLock {
        if (!auth.isConfigured) {
            return PhoneVerificationOutcome.Failure(FirebaseAuthMessages.UNCONFIGURED, AuthFailureKind.UNCONFIGURED)
        }
        val current = pendingPhone
            ?: return PhoneVerificationOutcome.Failure(FirebaseAuthMessages.CODE_NOT_REQUESTED, AuthFailureKind.REJECTED)

        val elapsed = clock() - current.requestedAtMillis
        if (elapsed < RESEND_INTERVAL_MILLIS) {
            return PhoneVerificationOutcome.Failure(
                AuthErrors.RATE_LIMITED,
                AuthFailureKind.REJECTED,
            )
        }

        return try {
            when (val start = auth.resendPhoneCode(current.challenge)) {
                is PhoneChallengeStart.CodeSent -> {
                    pendingPhone = PendingPhone(start.challenge, clock())
                    PhoneVerificationOutcome.CodeSent
                }
                PhoneChallengeStart.AutoVerified -> {
                    pendingPhone = null
                    val identity = auth.currentIdentity()
                    if (identity != null) {
                        _authState.value = resolveAccountState(identity)
                        PhoneVerificationOutcome.Completed
                    } else {
                        PhoneVerificationOutcome.Failure(FirebaseAuthMessages.INVALID_CREDENTIAL, AuthFailureKind.INVALID_TOKEN)
                    }
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            val failure = AuthErrors.describe(error)
            PhoneVerificationOutcome.Failure(failure.message, failure.kind)
        }
    }

    override suspend fun submitPhoneCode(code: String): AuthResult = mutex.withLock {
        if (!auth.isConfigured) {
            return AuthResult.Failure(FirebaseAuthMessages.UNCONFIGURED, AuthFailureKind.UNCONFIGURED)
        }
        val current = pendingPhone
            ?: return AuthResult.Failure(FirebaseAuthMessages.CODE_NOT_REQUESTED, AuthFailureKind.REJECTED)

        return try {
            val identity = auth.verifyPhoneCode(current.challenge, code)
            pendingPhone = null
            _authState.value = resolveAccountState(identity)
            AuthResult.Success
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            val failure = AuthErrors.describe(error)
            AuthResult.Failure(failure.message, failure.kind)
        }
    }

    override suspend fun cancelPhoneVerification() {
        mutex.withLock {
            val challenge = pendingPhone?.challenge
            pendingPhone = null
            if (challenge != null) {
                runCatching { auth.discardPhoneChallenge(challenge) }
            }
        }
    }

    override suspend fun saveProfile(fullName: String, username: String?, avatarUrl: String?): AuthResult = mutex.withLock {
        val current = _authState.value
        val uid = when (current) {
            is AuthState.Authenticated -> current.account.uid
            is AuthState.ProfileIncomplete -> current.draft.id
            else -> return AuthResult.Failure("No active session to save profile.", AuthFailureKind.NOT_FOUND)
        }

        prefs?.edit()?.apply {
            putString("${uid}_full_name", fullName)
            putString("${uid}_username", username)
            putString("${uid}_avatar_url", avatarUrl)
            putBoolean("${uid}_onboarding_completed", true)
            apply()
        }

        val identity = auth.currentIdentity()
        if (identity != null) {
            _authState.value = resolveAccountState(identity)
            return AuthResult.Success
        }

        return AuthResult.Failure("Account session lost.", AuthFailureKind.SESSION_EXPIRED)
    }

    override suspend fun refreshAccount(): AuthResult = mutex.withLock {
        if (!auth.isConfigured) return AuthResult.Success
        return try {
            val identity = auth.currentIdentity()
            if (identity != null) {
                _authState.value = resolveAccountState(identity)
            } else {
                _authState.value = AuthState.Unauthenticated
            }
            AuthResult.Success
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            AuthResult.Success // Keep existing session on transient error
        }
    }

    override suspend fun checkUsername(username: String): UsernameAvailability {
        return UsernameAvailability.Available
    }

    override suspend fun signOut(): AuthResult = mutex.withLock {
        return try {
            auth.signOut()
            pendingPhone = null
            _authState.value = AuthState.Unauthenticated
            AuthResult.Success
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            _authState.value = AuthState.Unauthenticated
            val failure = AuthErrors.describe(error)
            AuthResult.Failure(failure.message, failure.kind)
        }
    }

    override suspend fun deleteAccount(): AuthResult = mutex.withLock {
        return try {
            val identity = auth.currentIdentity()
            if (identity != null) {
                prefs?.edit()?.apply {
                    remove("${identity.uid}_full_name")
                    remove("${identity.uid}_username")
                    remove("${identity.uid}_avatar_url")
                    remove("${identity.uid}_onboarding_completed")
                    apply()
                }
            }
            auth.deleteAccount()
            pendingPhone = null
            _authState.value = AuthState.Unauthenticated
            AuthResult.Success
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            val failure = AuthErrors.describe(error)
            AuthResult.Failure(failure.message, failure.kind)
        }
    }

    private fun resolveAccountState(identity: AuthIdentity): AuthState {
        val uid = identity.uid
        val savedFullName = prefs?.getString("${uid}_full_name", null)
        val savedUsername = prefs?.getString("${uid}_username", null)
        val savedAvatarUrl = prefs?.getString("${uid}_avatar_url", null)
        val onboardingCompleted = prefs?.getBoolean("${uid}_onboarding_completed", false) ?: false

        val effectiveFullName = savedFullName ?: identity.suggestedFullName
        val effectiveAvatarUrl = savedAvatarUrl ?: identity.suggestedAvatarUrl

        // If the user already has a saved profile or completed onboarding, or has an established full name
        val hasCompletedOnboarding = onboardingCompleted || (!effectiveFullName.isNullOrBlank() && !savedUsername.isNullOrBlank())

        val profile = Profile(
            id = uid,
            fullName = effectiveFullName,
            username = savedUsername,
            avatarUrl = effectiveAvatarUrl,
            phoneNumber = identity.phoneNumber,
            phoneVerified = identity.phoneNumber != null,
            onboardingCompleted = hasCompletedOnboarding,
        )

        return if (hasCompletedOnboarding) {
            AuthState.Authenticated(
                AuthAccount(
                    uid = uid,
                    email = identity.email,
                    phoneNumber = identity.phoneNumber,
                    provider = identity.provider,
                    memberSinceEpochSeconds = identity.memberSinceEpochSeconds,
                    profile = profile,
                )
            )
        } else {
            AuthState.ProfileIncomplete(
                ProfileDraft(
                    id = uid,
                    suggestedFullName = identity.suggestedFullName,
                    suggestedUsername = identity.suggestedUsername,
                    suggestedAvatarUrl = identity.suggestedAvatarUrl,
                    savedFullName = savedFullName,
                    savedUsername = savedUsername,
                    savedAvatarUrl = savedAvatarUrl,
                    email = identity.email,
                )
            )
        }
    }

    companion object {
        const val RESEND_INTERVAL_MILLIS = 30_000L
    }
}
