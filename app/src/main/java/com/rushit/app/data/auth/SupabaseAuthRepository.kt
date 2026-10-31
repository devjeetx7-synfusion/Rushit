package com.rushit.app.data.auth

import android.content.Context
import com.rushit.app.data.model.Profile
import com.rushit.app.data.model.UserSettings
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SupabaseAuthRepository(
    private val context: Context,
    private val supabase: SupabaseClient
) : AuthRepository {

    val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState = _authState.asStateFlow()

    override suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            val session = supabase.auth.currentSessionOrNull()
            if (session != null) {
                checkProfile()
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Could not restore session: ${e.message}")
        }
    }

    private suspend fun checkProfile() {
        try {
            val user = supabase.auth.currentUserOrNull()
            if (user == null) {
                _authState.value = AuthState.Unauthenticated
                return
            }
            val profile = supabase.postgrest["profiles"]
                .select { filter { eq("id", user.id) } }
                .decodeSingleOrNull<Profile>()

            if (profile == null || !profile.onboardingCompleted) {
                val googleData = user.userMetadata
                val fullName = googleData?.get("full_name")?.toString()?.replace("\"", "")
                val avatarUrl = googleData?.get("avatar_url")?.toString()?.replace("\"", "")
                
                // create or update profile
                if (profile == null) {
                    val newProfile = Profile(
                        id = user.id,
                        fullName = fullName,
                        avatarUrl = avatarUrl,
                        onboardingCompleted = false
                    )
                    supabase.postgrest["profiles"].insert(newProfile)
                    supabase.postgrest["user_settings"].insert(UserSettings(userId = user.id))
                }
                
                _authState.value = AuthState.ProfileIncomplete(user.id, fullName, avatarUrl)
            } else {
                _authState.value = AuthState.Authenticated(profile)
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Failed to load profile: ${e.message}")
        }
    }

    override suspend fun signInWithGoogle(idToken: String): AuthResult = withContext(Dispatchers.IO) {
        _authState.value = AuthState.Authenticating
        try {
            supabase.auth.signInWith(IDToken) {
                this.idToken = idToken
                provider = Google
            }
            checkProfile()
            AuthResult.Success
        } catch (e: Exception) {
            _authState.value = AuthState.Unauthenticated
            AuthResult.Failure(e.message ?: "Authentication failed")
        }
    }

    override suspend fun updateProfile(username: String, fullName: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val user = supabase.auth.currentUserOrNull() ?: return@withContext AuthResult.Failure("User not found")
            val updateData = mapOf(
                "username" to username,
                "full_name" to fullName,
                "onboarding_completed" to true
            )
            supabase.postgrest["profiles"]
                .update(updateData) { filter { eq("id", user.id) } }
            checkProfile()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Failure(e.message ?: "Could not update profile")
        }
    }

    override suspend fun signOut(): AuthResult = withContext(Dispatchers.IO) {
        try {
            supabase.auth.signOut()
            _authState.value = AuthState.Unauthenticated
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Failure(e.message ?: "Failed to sign out")
        }
    }

    override suspend fun deleteAccount(): AuthResult = withContext(Dispatchers.IO) {
        // Safe implementation via RPC or backend. For now, since no service key, we just error or sign out.
        // The prompt says "Account deletion must use a trusted authenticated server-side flow. Never expose a service role credential in the client."
        // We can call an RPC function.
        try {
            supabase.postgrest.rpc("delete_user_account")
            supabase.auth.signOut()
            _authState.value = AuthState.Unauthenticated
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Failure("Could not delete account. ${e.message}")
        }
    }

    override suspend fun requestOtp(phone: String): AuthResult = AuthResult.Failure("Phone auth not implemented")
    override suspend fun verifyOtp(phone: String, token: String): AuthResult = AuthResult.Failure("Phone auth not implemented")
}
