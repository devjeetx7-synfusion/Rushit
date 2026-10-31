package com.rushit.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.rushit.app.data.auth.AuthResult
import com.rushit.app.data.auth.AuthState
import com.rushit.app.data.auth.AuthRepository
import com.rushit.app.BuildConfig
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface AuthEvent {
    data object OpenHome : AuthEvent
    data object OpenLogin : AuthEvent
    data object OpenProfileSetup : AuthEvent
}

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    val authState = repository.authState

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    var startupRouteHandled: Boolean = false
        private set
    fun markStartupRouted() { startupRouteHandled = true }

    init {
        viewModelScope.launch {
            repository.initialize()
            
            repository.authState.collect { state ->
                when (state) {
                    is AuthState.Authenticated -> {
                        _events.send(AuthEvent.OpenHome)
                    }
                    is AuthState.ProfileIncomplete -> {
                        _events.send(AuthEvent.OpenProfileSetup)
                    }
                    is AuthState.Unauthenticated -> {
                        if (startupRouteHandled) _events.send(AuthEvent.OpenLogin)
                    }
                    is AuthState.Error -> {
                        _error.value = state.message
                        if (startupRouteHandled) _events.send(AuthEvent.OpenLogin)
                    }
                    else -> {}
                }
            }
        }
    }

    fun initiateGoogleSignIn(context: Context) {
        viewModelScope.launch {
            _error.value = null
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    
                    val authResult = repository.signInWithGoogle(idToken)
                    if (authResult is AuthResult.Failure) {
                        _error.value = authResult.message
                    }
                } else {
                    _error.value = "Unexpected credential type"
                }
            } catch (e: GetCredentialCancellationException) {
                // User cancelled, do nothing
            } catch (e: Exception) {
                _error.value = "Google Sign-In failed: ${e.message}"
            }
        }
    }

    fun completeProfile(username: String, fullName: String) {
        viewModelScope.launch {
            _error.value = null
            val result = repository.updateProfile(username, fullName)
            if (result is AuthResult.Failure) {
                _error.value = result.message
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            val result = repository.signOut()
            if (result is AuthResult.Failure) {
                _error.value = result.message
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            val result = repository.deleteAccount()
            if (result is AuthResult.Failure) {
                _error.value = result.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    companion object {
        fun factory(repository: AuthRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(AuthViewModel::class.java))
                return AuthViewModel(repository) as T
            }
        }
    }
}
