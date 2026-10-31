package com.rushit.app.data.auth

import com.rushit.app.data.model.Profile
import kotlinx.coroutines.flow.StateFlow

sealed interface AuthState {
    data object Loading : AuthState
    data object Unauthenticated : AuthState
    data object Authenticating : AuthState
    data class Authenticated(val profile: Profile) : AuthState
    data class ProfileIncomplete(val id: String, val fullName: String?, val avatarUrl: String?) : AuthState
    data class Error(val message: String) : AuthState
}

interface AuthRepository {
    val authState: StateFlow<AuthState>
    
    suspend fun initialize()
    suspend fun signInWithGoogle(idToken: String): AuthResult
    suspend fun signOut(): AuthResult
    suspend fun updateProfile(username: String, fullName: String): AuthResult
    suspend fun deleteAccount(): AuthResult
    
    // Future Phone Login
    suspend fun requestOtp(phone: String): AuthResult
    suspend fun verifyOtp(phone: String, token: String): AuthResult
}

sealed interface AuthResult {
    data object Success : AuthResult
    data class Failure(val message: String) : AuthResult
}
