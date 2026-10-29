package com.rushit.app.data.auth

/** A deliberately small boundary around Supabase's public Phone OTP API. */
interface AuthRepository {
    suspend fun restoreSession(): SessionResult
    suspend fun requestOtp(phone: String): AuthResult
    suspend fun verifyOtp(phone: String, token: String): AuthResult
    suspend fun signOut(): AuthResult
}

sealed interface AuthResult {
    data object Success : AuthResult
    data class Failure(val message: String) : AuthResult
}

sealed interface SessionResult {
    data object Authenticated : SessionResult
    data class Unauthenticated(val message: String? = null) : SessionResult
}
