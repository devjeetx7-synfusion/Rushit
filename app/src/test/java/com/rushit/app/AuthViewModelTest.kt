package com.rushit.app

import com.rushit.app.data.auth.AuthFailureKind
import com.rushit.app.data.auth.AuthResult
import com.rushit.app.data.auth.AuthState
import com.rushit.app.data.auth.LocalAuthRepository
import com.rushit.app.data.auth.ProfileDraft
import com.rushit.app.data.auth.UsernameAvailability
import com.rushit.app.data.auth.google.GoogleSignInClient
import com.rushit.app.data.auth.google.GoogleSignInOutcome
import com.rushit.app.data.auth.phone.PhoneNumbers
import com.rushit.app.ui.viewmodel.AuthEvent
import com.rushit.app.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val india = PhoneNumbers.default

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        repository: LocalAuthRepository = LocalAuthRepository(),
        google: GoogleSignInClient = FakeGoogleSignIn(),
    ) = AuthViewModel(repository, google)

    @Test
    fun `a signed-out start lands on unauthenticated without an error`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(AuthState.Unauthenticated, vm.authState.value)
        assertNull(vm.error.value)
    }

    @Test
    fun `cancelling the account picker is silent`() = runTest(dispatcher) {
        val google = FakeGoogleSignIn(GoogleSignInOutcome.Cancelled)
        val vm = viewModel(google = google)
        advanceUntilIdle()

        vm.initiateGoogleSignIn()
        advanceUntilIdle()

        assertNull(vm.error.value)
        assertEquals(AuthState.Unauthenticated, vm.authState.value)
        assertFalse(vm.uiState.value.signingIn)
    }

    @Test
    fun `Google sign-in failure communicates that backend is removed`() = runTest(dispatcher) {
        val google = FakeGoogleSignIn(GoogleSignInOutcome.Failed("Google Sign-In is unavailable because the backend has been removed."))
        val vm = viewModel(google = google)
        advanceUntilIdle()

        vm.initiateGoogleSignIn()
        advanceUntilIdle()

        assertEquals("Google Sign-In is unavailable because the backend has been removed.", vm.error.value)
        assertEquals(AuthState.Unauthenticated, vm.authState.value)
    }

    @Test
    fun `an invalid phone number is rejected before repository is called`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.startPhoneVerification(india, "12345")
        runCurrent()

        assertNotNull(vm.error.value)
        assertFalse(vm.phoneState.value.codeSent)
    }

    @Test
    fun `starting phone verification reports that phone auth is unavailable honestly`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.startPhoneVerification(india, "9876543210")
        advanceUntilIdle()

        assertEquals("Phone authentication is unavailable because the backend has been removed.", vm.error.value)
        assertFalse(vm.phoneState.value.codeSent)
    }

    @Test
    fun `saving a profile with incomplete profile succeeds`() = runTest(dispatcher) {
        val draft = ProfileDraft(
            id = "test-user",
            suggestedFullName = "Test",
            suggestedUsername = "test",
            suggestedAvatarUrl = null,
            savedFullName = null,
            savedUsername = null,
            savedAvatarUrl = null,
            email = null,
        )
        val repo = LocalAuthRepository(initialState = AuthState.ProfileIncomplete(draft))
        val vm = viewModel(repository = repo)
        advanceUntilIdle()

        vm.saveProfile("Rush It Shopper", "fresh.handle")
        advanceUntilIdle()

        assertEquals(UsernameAvailability.Available, vm.usernameAvailability.value)
        assertFalse(vm.uiState.value.savingProfile)
        assertTrue(vm.authState.value is AuthState.Authenticated)
    }

    @Test
    fun `signing out returns to unauthenticated`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.signOut()
        advanceUntilIdle()

        assertEquals(AuthState.Unauthenticated, vm.authState.value)
        assertFalse(vm.uiState.value.signingOut)
    }

    private class FakeGoogleSignIn(
        var outcome: GoogleSignInOutcome = GoogleSignInOutcome.Success("google-id-token"),
        private val gate: CompletableDeferred<Unit>? = null,
    ) : GoogleSignInClient {
        var calls = 0

        override suspend fun requestGoogleIdToken(): GoogleSignInOutcome {
            calls++
            gate?.await()
            return outcome
        }
    }
}
