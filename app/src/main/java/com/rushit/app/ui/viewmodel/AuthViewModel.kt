package com.rushit.app.ui.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rushit.app.data.auth.AuthRepository
import com.rushit.app.data.auth.AuthResult
import com.rushit.app.data.auth.SessionResult
import com.rushit.app.domain.isValidIndianMobile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class AuthUiState(
    val initializing: Boolean = true,
    val authenticated: Boolean = false,
    val demo: Boolean = false,
    val phone: String = "",
    val otp: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val resendAfterElapsedMillis: Long = 0,
)

sealed interface AuthEvent {
    data object OpenOtp : AuthEvent
    data object OpenHome : AuthEvent
    data object OpenLogin : AuthEvent
}

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state = _state.asStateFlow()
    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // Retained across configuration changes, but reset after process death so a restored
    // navigation stack cannot bypass a fresh session check.
    var startupRouteHandled: Boolean = false
        private set
    fun markStartupRouted() { startupRouteHandled = true }

    init {
        viewModelScope.launch {
            // Keep the bounded UI wait separate from a potentially blocking network call. Cancelling
            // an await must not hold the splash open while HttpURLConnection finishes on IO.
            val restore = viewModelScope.async { repository.restoreSession() }
            val result = try {
                withTimeoutOrNull(9_000) { restore.await() }
                    ?: SessionResult.Unauthenticated("Session check timed out. Please check your connection and sign in.")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                SessionResult.Unauthenticated("Could not restore your session. Please sign in again.")
            } finally {
                if (restore.isActive) restore.cancel()
            }
            _state.update {
                when (result) {
                    SessionResult.Authenticated -> it.copy(initializing = false, authenticated = true)
                    is SessionResult.Unauthenticated -> it.copy(initializing = false, notice = result.message)
                }
            }
        }
    }

    fun updatePhone(value: String) {
        if (_state.value.busy) return
        _state.update { it.copy(phone = value.filter { c -> c in '0'..'9' }.take(10), error = null) }
    }

    fun updateOtp(value: String) {
        if (_state.value.busy) return
        _state.update { it.copy(otp = value.filter { c -> c in '0'..'9' }.take(6), error = null) }
    }

    fun requestOtp(resend: Boolean = false) {
        val snapshot = _state.value
        if (snapshot.busy) return
        if (!isValidIndianMobile(snapshot.phone)) {
            _state.update { it.copy(error = "Enter a valid 10-digit Indian mobile number.") }
            return
        }
        if (resend && SystemClock.elapsedRealtime() < snapshot.resendAfterElapsedMillis) return
        _state.update { it.copy(busy = true, error = null, notice = null) }
        viewModelScope.launch {
            val result = try { repository.requestOtp("+91${snapshot.phone}") } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) { AuthResult.Failure("Could not send a code. Please try again.") }
            when (result) {
                AuthResult.Success -> {
                    _state.update {
                        it.copy(busy = false, otp = "", resendAfterElapsedMillis = SystemClock.elapsedRealtime() + 30_000,
                            notice = if (resend) "A new code has been sent." else null)
                    }
                    if (!resend) _events.send(AuthEvent.OpenOtp)
                }
                is AuthResult.Failure -> _state.update { it.copy(busy = false, error = result.message) }
            }
        }
    }

    fun verifyOtp() {
        val snapshot = _state.value
        if (snapshot.busy) return
        if (snapshot.otp.length != 6) {
            _state.update { it.copy(error = "Enter the 6-digit code sent to your phone.") }
            return
        }
        _state.update { it.copy(busy = true, error = null, notice = null) }
        viewModelScope.launch {
            val result = try { repository.verifyOtp("+91${snapshot.phone}", snapshot.otp) } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) { AuthResult.Failure("Could not verify the code. Please try again.") }
            when (result) {
                AuthResult.Success -> {
                    _state.update { it.copy(busy = false, authenticated = true, demo = false, otp = "") }
                    _events.send(AuthEvent.OpenHome)
                }
                is AuthResult.Failure -> _state.update { it.copy(busy = false, error = result.message) }
            }
        }
    }

    /** Browsing never marks a user authenticated or creates a session. */
    fun browseSampleStore() {
        if (_state.value.busy || _state.value.demo) return
        _state.update { it.copy(demo = true, authenticated = false, error = null, notice = null) }
        viewModelScope.launch { _events.send(AuthEvent.OpenHome) }
    }

    fun signOutOrSignIn() {
        if (_state.value.busy) return
        if (!_state.value.authenticated) {
            _state.update { it.copy(demo = false, error = null, notice = null) }
            viewModelScope.launch { _events.send(AuthEvent.OpenLogin) }
            return
        }
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val result = try { repository.signOut() } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(busy = false, error = "Could not sign out. Please try again.") }
                return@launch
            }
            _state.update {
                it.copy(busy = false, authenticated = false, demo = false, phone = "", otp = "", error = null,
                    notice = (result as? AuthResult.Failure)?.message)
            }
            _events.send(AuthEvent.OpenLogin)
        }
    }

    fun clearMessage() { _state.update { it.copy(error = null, notice = null) } }

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
