package com.rushit.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rushit.app.data.auth.AuthFailureKind
import com.rushit.app.data.auth.AuthRepository
import com.rushit.app.data.auth.AuthResult
import com.rushit.app.data.auth.AuthState
import com.rushit.app.data.auth.PhoneVerificationOutcome
import com.rushit.app.data.auth.RecoveryAction
import com.rushit.app.data.auth.UsernameAvailability
import com.rushit.app.data.auth.google.GoogleSignInClient
import com.rushit.app.data.auth.google.GoogleSignInMessages
import com.rushit.app.data.auth.google.GoogleSignInOutcome
import com.rushit.app.data.auth.phone.CountryCode
import com.rushit.app.data.auth.phone.PhoneNumbers
import com.rushit.app.data.auth.phone.VerificationCode
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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

/**
 * State of the phone sign-in screen.
 *
 * The Firebase verification id and resend token deliberately never appear here: they live in the
 * repository, so nothing in the UI layer can leak them into a log, a saved-state bundle or a
 * crash report.
 */
data class PhoneLoginUiState(
    val requesting: Boolean = false,
    val verifying: Boolean = false,
    val codeSent: Boolean = false,
    val resendSecondsRemaining: Int = 0,
) {
    val busy: Boolean get() = requesting || verifying
}

class AuthViewModel(
    private val repository: AuthRepository,
    private val googleSignIn: GoogleSignInClient,
) : ViewModel() {

    val authState: StateFlow<AuthState> = repository.authState

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _phoneState = MutableStateFlow(PhoneLoginUiState())
    val phoneState: StateFlow<PhoneLoginUiState> = _phoneState.asStateFlow()

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

    private var resendJob: Job? = null

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

    /** Launch the Google account picker. The flag is set synchronously, before a second tap queues. */
    fun initiateGoogleSignIn() {
        if (_uiState.value.signingIn) return
        _uiState.update { it.copy(signingIn = true) }
        _error.value = null
        viewModelScope.launch {
            try {
                when (val outcome = googleSignIn.requestGoogleIdToken()) {
                    // Dismissing the account picker is a decision, not a failure.
                    GoogleSignInOutcome.Cancelled -> Unit
                    GoogleSignInOutcome.Unconfigured -> _error.value = GoogleSignInMessages.UNCONFIGURED
                    GoogleSignInOutcome.NoCredentialAvailable -> _error.value = GoogleSignInMessages.NO_GOOGLE_ACCOUNT
                    GoogleSignInOutcome.Unsupported -> _error.value = GoogleSignInMessages.PLAY_SERVICES
                    is GoogleSignInOutcome.Failed -> _error.value = outcome.message
                    is GoogleSignInOutcome.Success ->
                        when (val result = repository.signInWithGoogle(outcome.idToken)) {
                            AuthResult.Success -> Unit
                            is AuthResult.Failure -> _error.value = result.message
                        }
                }
            } finally {
                _uiState.update { it.copy(signingIn = false) }
            }
        }
    }

    /** Validate the number, then ask Firebase to text a code. Ignored while a request is in flight. */
    fun startPhoneVerification(country: CountryCode, nationalNumberInput: String) {
        if (_phoneState.value.busy) return
        val problem = PhoneNumbers.errorFor(country, nationalNumberInput)
        if (problem != null) {
            _error.value = problem
            return
        }
        val phoneNumber = PhoneNumbers.normalize(country, nationalNumberInput)
        if (phoneNumber == null) {
            _error.value = "That number was not accepted. Check it and try again."
            return
        }
        _error.value = null
        _phoneState.update { it.copy(requesting = true) }
        viewModelScope.launch {
            try {
                handlePhoneOutcome(repository.startPhoneVerification(phoneNumber))
            } finally {
                _phoneState.update { it.copy(requesting = false) }
            }
        }
    }

    /** Re-send the code; the repository enforces the minimum interval as well. */
    fun resendPhoneCode() {
        if (_phoneState.value.busy || !_phoneState.value.codeSent) return
        if (_phoneState.value.resendSecondsRemaining > 0) return
        _error.value = null
        _phoneState.update { it.copy(requesting = true) }
        viewModelScope.launch {
            try {
                handlePhoneOutcome(repository.resendPhoneCode())
            } finally {
                _phoneState.update { it.copy(requesting = false) }
            }
        }
    }

    /**
     * Verify the code. Duplicate submissions are rejected before a second request is built, and an
     * incomplete code is never sent to Firebase: six digits are required, so a false "wrong code"
     * can never come from a half-typed entry.
     */
    fun submitPhoneCode(code: String) {
        if (_phoneState.value.verifying) return
        val problem = VerificationCode.validationMessage(code)
        if (problem != null) {
            _error.value = problem
            return
        }
        _error.value = null
        _phoneState.update { it.copy(verifying = true) }
        viewModelScope.launch {
            try {
                when (val result = repository.submitPhoneCode(VerificationCode.normalize(code))) {
                    AuthResult.Success -> {
                        _phoneState.value = PhoneLoginUiState()
                        stopResendCountdown()
                    }

                    is AuthResult.Failure -> _error.value = result.message
                }
            } finally {
                _phoneState.update { it.copy(verifying = false) }
            }
        }
    }

    /** Abandon the pending verification, e.g. when the person wants to change the number. */
    fun changePhoneNumber() {
        stopResendCountdown()
        _phoneState.value = PhoneLoginUiState()
        _error.value = null
        viewModelScope.launch { repository.cancelPhoneVerification() }
    }

    private fun handlePhoneOutcome(outcome: PhoneVerificationOutcome) {
        when (outcome) {
            PhoneVerificationOutcome.CodeSent -> {
                _phoneState.update { it.copy(codeSent = true) }
                startResendCountdown()
            }

            // Instant verification/auto-retrieval signed the person in; navigation follows the state
            // change, so there is nothing else to do here.
            PhoneVerificationOutcome.Completed -> {
                _phoneState.value = PhoneLoginUiState()
                stopResendCountdown()
            }

            is PhoneVerificationOutcome.Failure -> _error.value = outcome.message
        }
    }

    private fun startResendCountdown() {
        stopResendCountdown()
        resendJob = viewModelScope.launch {
            var remaining = RESEND_SECONDS
            while (remaining > 0) {
                _phoneState.update { it.copy(resendSecondsRemaining = remaining) }
                delay(1_000L)
                remaining--
            }
            _phoneState.update { it.copy(resendSecondsRemaining = 0) }
        }
    }

    private fun stopResendCountdown() {
        resendJob?.cancel()
        resendJob = null
        _phoneState.update { it.copy(resendSecondsRemaining = 0) }
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
        when (state) {
            // The state carries its own explanation, so the banner and the retry control agree.
            is AuthState.RecoverableError -> _error.value = state.message
            // Reaching a usable account means the previous failure is no longer true.
            is AuthState.Authenticated, is AuthState.ProfileIncomplete -> _error.value = null
            // Deliberately NOT cleared for Unauthenticated: a failed sign-in publishes
            // Unauthenticated first and sets its message immediately afterwards, so clearing here
            // would wipe the message that was just produced. Each action clears it at the start of
            // its next attempt instead.
            else -> Unit
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
        private const val RESEND_SECONDS = 30

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
