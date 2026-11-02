package com.rushit.app.data.auth

import com.rushit.app.data.auth.phone.PhoneNumber
import com.rushit.app.data.model.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocalAuthRepository(
    initialState: AuthState = AuthState.Unauthenticated,
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(initialState)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override suspend fun initialize() {
    }

    override suspend fun retrySessionRestore() {
        _authState.value = AuthState.Unauthenticated
    }

    override suspend fun signInWithGoogle(idToken: String): AuthResult {
        return AuthResult.Failure(
            message = AuthErrors.AUTH_BACKEND_REMOVED,
            kind = AuthFailureKind.UNCONFIGURED,
        )
    }

    override suspend fun startPhoneVerification(phoneNumber: PhoneNumber): PhoneVerificationOutcome {
        return PhoneVerificationOutcome.Failure(
            message = "Phone authentication is unavailable because the backend has been removed.",
            kind = AuthFailureKind.UNCONFIGURED,
        )
    }

    override suspend fun resendPhoneCode(): PhoneVerificationOutcome {
        return PhoneVerificationOutcome.Failure(
            message = "Phone authentication is unavailable because the backend has been removed.",
            kind = AuthFailureKind.UNCONFIGURED,
        )
    }

    override suspend fun submitPhoneCode(code: String): AuthResult {
        return AuthResult.Failure(
            message = "Phone verification is unavailable because the backend has been removed.",
            kind = AuthFailureKind.UNCONFIGURED,
        )
    }

    override suspend fun cancelPhoneVerification() {
    }

    override suspend fun saveProfile(fullName: String, username: String?, avatarUrl: String?): AuthResult {
        val current = _authState.value
        if (current is AuthState.Authenticated) {
            val updatedProfile = current.account.profile.copy(
                fullName = fullName,
                username = username,
                avatarUrl = avatarUrl,
                onboardingCompleted = true,
            )
            _authState.value = AuthState.Authenticated(current.account.copy(profile = updatedProfile))
            return AuthResult.Success
        } else if (current is AuthState.ProfileIncomplete) {
            val profile = Profile(
                id = current.draft.id,
                fullName = fullName,
                username = username,
                avatarUrl = avatarUrl,
                onboardingCompleted = true,
            )
            val account = AuthAccount(
                uid = current.draft.id,
                email = current.draft.email,
                phoneNumber = null,
                provider = AuthProviderId.GOOGLE,
                memberSinceEpochSeconds = System.currentTimeMillis() / 1000L,
                profile = profile,
            )
            _authState.value = AuthState.Authenticated(account)
            return AuthResult.Success
        }
        return AuthResult.Failure("No active profile to save.", AuthFailureKind.NOT_FOUND)
    }

    override suspend fun refreshAccount(): AuthResult {
        return AuthResult.Success
    }

    override suspend fun checkUsername(username: String): UsernameAvailability {
        return UsernameAvailability.Available
    }

    override suspend fun signOut(): AuthResult {
        _authState.value = AuthState.Unauthenticated
        return AuthResult.Success
    }

    override suspend fun deleteAccount(): AuthResult {
        _authState.value = AuthState.Unauthenticated
        return AuthResult.Success
    }
}
