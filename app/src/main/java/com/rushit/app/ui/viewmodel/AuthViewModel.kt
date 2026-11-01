package com.rushit.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rushit.app.BuildConfig
import com.rushit.app.data.auth.AuthFailureKind
import com.rushit.app.data.auth.AuthRepository
import com.rushit.app.data.auth.AuthResult
import com.rushit.app.data.auth.AuthState
import com.rushit.app.data.auth.RecoveryAction
import com.rushit.app.data.auth.UsernameAvailability
import com.rushit.app.data.auth.google.GoogleIdTokenValidator
import com.rushit.app.data.auth.google.GoogleSignInClient
import com.rushit.app.data.auth.google.GoogleSignInMessages
import com.rushit.app.data.auth.google.GoogleSignInOutcome
import com.rushit.app.data.auth.google.IdTokenCheck
import com.rushit.app.data.auth.google.SupabaseOAuthRedirect
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One-shot navigation requests derived from authentication state. */
sealed interface AuthEvent {
    data object OpenHome : AuthEvent
    data object OpenLogin : AuthEvent
    data object OpenProfileSetup : AuthEvent
}

/**
 * In-flight flags for the auth UI.
 *
 * Kept separate from [AuthState] so a screen can disable exactly the control it owns, and so a
 * second tap on "Continue with Google" can be rejected before a request is even built.
 */
data class AuthUiState(
    val signingIn: Boolean = false,
    val savingProfile: Boolean = false,
    val signingOut: Boolean = false,
    val deletingAccount: Boolean = false,
    val checkingUsername: Boolean = false,
) {
    /** True while any authentication request is in flight. */
    val busy: Boolean get() = signingIn || savingProfile || signingOut || deletingAccount
}

class AuthViewModel(
    private val repository: AuthRepository,
    private val googleSignIn: GoogleSignInClient,
    private val webClientId: String = BuildConfig.GOOGLE_WEB_CLIENT_ID,
    private val nowEpochSeconds: () -> Long = { System.currentTimeMillis() / 1000L },
) : ViewModel() {

    val authState: StateFlow<AuthState> = repository.authState

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _usernameAvailability = MutableStateFlow<UsernameAvailability>(UsernameAvailability.Unknown)
    val usernameAvailability: StateFlow<UsernameAvailability> = _usernameAvailability.asStateFlow()

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events: Flow<AuthEvent> = _events.receiveAsFlow()

    var startupRouteHandled: Boolean = false
        private set

    /** The last route published, so a repeated identical state cannot re-navigate. */
    private var lastRouted: AuthEvent? = null

    init {
        // Collect before initializing so no state emitted during restoration is missed.
        viewModelScope.launch { repository.authState.collect { publish(it) } }
        viewModelScope.launch { repository.initialize() }
    }

    /**
     * Called once the start-up splash has routed. Recording the route it chose is what stops the
     * collector from immediately navigating a second time.
     */
    fun markStartupRouted() {
        startupRouteHandled = true
        lastRouted = routeFor(repository.authState.value)
    }

    /** Launch the two-stage flow. The flag is set synchronously, before a second tap can queue. */
    fun initiateGoogleSignIn() {
        if (_uiState.value.signingIn) return
        _uiState.update { it.copy(signingIn = true) }
        _error.value = null
        viewModelScope.launch {
            try {
                handleGoogleOutcome(googleSignIn.requestGoogleIdToken())
            } finally {
                _uiState.update { it.copy(signingIn = false) }
            }
        }
    }

    /**
     * Consume only RushIt's exact OAuth callback. The application-scoped client also deduplicates
     * the code, so recreating an Activity with the same Intent cannot exchange it twice.
     */
    fun handleAuthRedirect(uri: String?) {
        if (SupabaseOAuthRedirect.parse(uri) == SupabaseOAuthRedirect.Result.Ignored) return
        if (_uiState.value.signingIn) return
        _uiState.update { it.copy(signingIn = true) }
        viewModelScope.launch {
            try {
                googleSignIn.completeBrowserRedirect(uri)?.let { handleGoogleOutcome(it) }
            } finally {
                _uiState.update { it.copy(signingIn = false) }
            }
        }
    }

    /**
     * A browser return without a callback is a cancellation. A callback already being exchanged,
     * or a native request still in flight, is protected by the client's single-flight locks.
     */
    fun onForeground() {
        viewModelScope.launch {
            val outcome = googleSignIn.cancelBrowserSignIn()
            if (outcome == GoogleSignInOutcome.Cancelled) {
                _uiState.update { it.copy(signingIn = false) }
            }
        }
    }

    private suspend fun handleGoogleOutcome(outcome: GoogleSignInOutcome) {
        when (outcome) {
            // Dismissing the account picker is a decision, not a failure. Showing an error here
            // would punish people for changing their mind.
            GoogleSignInOutcome.Cancelled -> Unit

            // These are internal signals. The concrete client retries/falls back before returning;
            // a fake or future client must never turn them into a false "no device account" claim.
            GoogleSignInOutcome.NoCredentialAvailable -> _error.value = GoogleSignInMessages.UNKNOWN
            GoogleSignInOutcome.Unsupported -> _error.value = GoogleSignInMessages.PLAY_SERVICES
            GoogleSignInOutcome.BrowserPending -> Unit
            GoogleSignInOutcome.BrowserSuccess -> {
                when (val result = repository.refreshAccount()) {
                    AuthResult.Success -> Unit
                    is AuthResult.Failure -> _error.value = result.message
                }
            }

            GoogleSignInOutcome.Unconfigured -> _error.value = GoogleSignInMessages.UNCONFIGURED

            is GoogleSignInOutcome.Failed -> _error.value = outcome.message

            is GoogleSignInOutcome.Success -> {
                val check = GoogleIdTokenValidator.validate(
                    idToken = outcome.idToken,
                    expectedAudience = webClientId,
                    rawNonce = outcome.rawNonce,
                    nowEpochSeconds = nowEpochSeconds(),
                )
                if (check is IdTokenCheck.Rejected) {
                    _error.value = check.message
                    return
                }
                when (val result = repository.signInWithGoogle(outcome.idToken, outcome.rawNonce)) {
                    AuthResult.Success -> Unit
                    is AuthResult.Failure -> _error.value = result.message
                }
            }
        }
    }

    /** Save onboarding or a profile edit. Ignored while another save is in flight. */
    fun saveProfile(fullName: String, username: String?, avatarUrl: String? = null) {
        if (_uiState.value.savingProfile) return
        viewModelScope.launch {
            if (_uiState.value.savingProfile) return@launch
            _uiState.update { it.copy(savingProfile = true) }
            _error.value = null
            try {
                when (val result = repository.saveProfile(fullName, username, avatarUrl)) {
                    AuthResult.Success -> _usernameAvailability.value = UsernameAvailability.Available
                    is AuthResult.Failure -> {
                        _error.value = result.message
                        if (result.kind == AuthFailureKind.CONFLICT) {
                            _usernameAvailability.value = UsernameAvailability.Taken
                        }
                    }
                }
            } finally {
                _uiState.update { it.copy(savingProfile = false) }
            }
        }
    }

    /** Non-blocking availability hint. The database unique index remains the authority. */
    fun checkUsername(username: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(checkingUsername = true) }
            try {
                _usernameAvailability.value = repository.checkUsername(username)
            } finally {
                _uiState.update { it.copy(checkingUsername = false) }
            }
        }
    }

    fun signOut() {
        if (_uiState.value.signingOut) return
        viewModelScope.launch {
            _uiState.update { it.copy(signingOut = true) }
            _error.value = null
            try {
                when (val result = repository.signOut()) {
                    AuthResult.Success -> Unit
                    is AuthResult.Failure -> _error.value = result.message
                }
            } finally {
                _uiState.update { it.copy(signingOut = false) }
            }
        }
    }

    fun deleteAccount() {
        if (_uiState.value.deletingAccount) return
        viewModelScope.launch {
            _uiState.update { it.copy(deletingAccount = true) }
            _error.value = null
            try {
                when (val result = repository.deleteAccount()) {
                    AuthResult.Success -> Unit
                    is AuthResult.Failure -> _error.value = result.message
                }
            } finally {
                _uiState.update { it.copy(deletingAccount = false) }
            }
        }
    }

    /** Retry after a [AuthState.RecoverableError] that offered [RecoveryAction.RETRY_SESSION]. */
    fun retrySession() {
        viewModelScope.launch {
            _error.value = null
            repository.retrySessionRestore()
        }
    }

    /** Re-read the signed-in account, e.g. when the app returns to the foreground. */
    fun refreshAccount() {
        viewModelScope.launch { repository.refreshAccount() }
    }

    fun clearError() {
        _error.value = null
    }

    /** The recovery offered by the current state, if any. */
    val recoveryAction: RecoveryAction?
        get() = (repository.authState.value as? AuthState.RecoverableError)?.action

    private fun publish(state: AuthState) {
        _error.value = when (state) {
            is AuthState.RecoverableError -> state.message
            // A failed sign-in sets its own message after this state is published, so clearing here
            // does not hide it.
            else -> null
        }

        val event = routeFor(state) ?: return
        // The splash owns the very first navigation; duplicating it would clear the back stack twice.
        if (!startupRouteHandled) return
        if (event == lastRouted) return
        lastRouted = event
        viewModelScope.launch { _events.send(event) }
    }

    private fun routeFor(state: AuthState): AuthEvent? = when (state) {
        is AuthState.Authenticated -> AuthEvent.OpenHome
        is AuthState.ProfileIncomplete -> AuthEvent.OpenProfileSetup
        AuthState.Unauthenticated -> AuthEvent.OpenLogin
        is AuthState.RecoverableError -> AuthEvent.OpenLogin
        AuthState.Loading, AuthState.Authenticating -> null
    }

    companion object {
        fun factory(repository: AuthRepository, googleSignIn: GoogleSignInClient) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(AuthViewModel::class.java))
                    return AuthViewModel(repository, googleSignIn) as T
                }
            }
    }
}
