package com.rushit.app

import com.rushit.app.data.auth.AuthErrors
import com.rushit.app.data.auth.AuthFailure
import com.rushit.app.data.auth.AuthFailureKind
import com.rushit.app.data.auth.AuthGatewayException
import com.rushit.app.data.auth.AuthState
import com.rushit.app.data.auth.FirebaseAuthRepository
import com.rushit.app.data.auth.UsernameAvailability
import com.rushit.app.data.auth.google.GoogleSignInClient
import com.rushit.app.data.auth.google.GoogleSignInMessages
import com.rushit.app.data.auth.google.GoogleSignInOutcome
import com.rushit.app.data.auth.phone.PhoneNumbers
import com.rushit.app.ui.viewmodel.AuthEvent
import com.rushit.app.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.async
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The ViewModel contract.
 *
 * These tests pin the behaviour a person actually experiences: a second tap cannot start a second
 * request, cancelling is silent, a rejected code keeps the OTP screen open, and navigation is driven
 * by authentication state rather than by the screens.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val auth = FakeAuthGateway()
    private val profiles = FakeProfileGateway()
    private val tokens = RecordingTokenBridge()
    private var now = 1_700_000_000_000L

    private val india = PhoneNumbers.default

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun repository() = FirebaseAuthRepository(auth, profiles, tokens) { now }

    private fun viewModel(google: GoogleSignInClient = FakeGoogleSignIn()) =
        AuthViewModel(repository(), google)

    /** A signed-in, onboarded account, ready for a test that needs an authenticated start. */
    private fun signedIn() {
        auth.identity = googleIdentity()
        profiles.profiles[TestConfig.USER_A] = completedProfile()
    }

    // ---------------------------------------------------------------- start-up

    @Test
    fun `a restored session is authenticated in the ViewModel`() = runTest(dispatcher) {
        signedIn()
        val vm = viewModel()

        advanceUntilIdle()

        assertTrue(vm.authState.value is AuthState.Authenticated)
        assertFalse(vm.uiState.value.busy)
    }

    @Test
    fun `a signed-out start lands on unauthenticated without an error`() = runTest(dispatcher) {
        val vm = viewModel()

        advanceUntilIdle()

        assertEquals(AuthState.Unauthenticated, vm.authState.value)
        assertNull(vm.error.value)
    }

    // ---------------------------------------------------------------- Google

    @Test
    fun `Google success signs in through the repository`() = runTest(dispatcher) {
        auth.identity = googleIdentity()
        profiles.profiles[TestConfig.USER_A] = completedProfile()
        val google = FakeGoogleSignIn(GoogleSignInOutcome.Success("google-id-token"))
        val vm = viewModel(google)
        advanceUntilIdle()

        vm.initiateGoogleSignIn()
        advanceUntilIdle()

        assertEquals(1, google.calls)
        assertEquals(1, auth.googleSignInCount)
        assertTrue(vm.authState.value is AuthState.Authenticated)
        assertNull(vm.error.value)
        assertFalse(vm.uiState.value.signingIn)
    }

    @Test
    fun `cancelling the account picker is silent`() = runTest(dispatcher) {
        val google = FakeGoogleSignIn(GoogleSignInOutcome.Cancelled)
        val vm = viewModel(google)
        advanceUntilIdle()

        vm.initiateGoogleSignIn()
        advanceUntilIdle()

        assertNull(vm.error.value)
        assertEquals(AuthState.Unauthenticated, vm.authState.value)
        assertFalse(vm.uiState.value.signingIn)
    }

    @Test
    fun `no usable Google account points at the phone option instead of a generic failure`() = runTest(dispatcher) {
        val google = FakeGoogleSignIn(GoogleSignInOutcome.NoCredentialAvailable)
        val vm = viewModel(google)
        advanceUntilIdle()

        vm.initiateGoogleSignIn()
        advanceUntilIdle()

        assertEquals(GoogleSignInMessages.NO_GOOGLE_ACCOUNT, vm.error.value)
        assertEquals(0, auth.googleSignInCount)
    }

    @Test
    fun `an unconfigured build explains what is missing`() = runTest(dispatcher) {
        val google = FakeGoogleSignIn(GoogleSignInOutcome.Unconfigured)
        val vm = viewModel(google)
        advanceUntilIdle()

        vm.initiateGoogleSignIn()
        advanceUntilIdle()

        assertEquals(GoogleSignInMessages.UNCONFIGURED, vm.error.value)
    }

    @Test
    fun `a rejected Firebase sign-in surfaces the repository message`() = runTest(dispatcher) {
        auth.googleError = AuthGatewayException(
            AuthFailure("That sign-in was not accepted. Please try again.", AuthFailureKind.INVALID_TOKEN),
        )
        val google = FakeGoogleSignIn(GoogleSignInOutcome.Success("google-id-token"))
        val vm = viewModel(google)
        advanceUntilIdle()

        vm.initiateGoogleSignIn()
        advanceUntilIdle()

        assertEquals("That sign-in was not accepted. Please try again.", vm.error.value)
        assertEquals(AuthState.Unauthenticated, vm.authState.value)
    }

    @Test
    fun `a second tap while the picker is open never starts a second request`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val google = FakeGoogleSignIn(GoogleSignInOutcome.Cancelled, gate = gate)
        val vm = viewModel(google)
        advanceUntilIdle()

        vm.initiateGoogleSignIn()
        runCurrent()
        vm.initiateGoogleSignIn()
        runCurrent()

        assertEquals(1, google.calls)
        assertTrue(vm.uiState.value.signingIn)

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.signingIn)
    }

    // ---------------------------------------------------------------- phone

    @Test
    fun `an invalid phone number is rejected before Firebase is called`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.startPhoneVerification(india, "12345")
        runCurrent()

        assertNotNull(vm.error.value)
        assertEquals(0, auth.startPhoneCount)
        assertFalse(vm.phoneState.value.codeSent)
    }

    @Test
    fun `sending a code opens the OTP step and starts the resend countdown`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.startPhoneVerification(india, "9876543210")
        runCurrent()

        assertTrue(vm.phoneState.value.codeSent)
        assertTrue(vm.phoneState.value.resendSecondsRemaining > 0)
        assertFalse(vm.phoneState.value.requesting)
        assertEquals("+91 XXXXX X3210", vm.phoneState.value.maskedPhoneNumber)
        assertEquals("+919876543210", auth.lastPhoneNumber)
    }

    @Test
    fun `masked phone number is set in PhoneLoginUiState when verification starts`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.startPhoneVerification(india, "9876543210")
        runCurrent()

        assertEquals("+91 XXXXX X3210", vm.phoneState.value.maskedPhoneNumber)
    }

    @Test
    fun `a duplicate send while a code is pending does not request a second SMS`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.startPhoneVerification(india, "9876543210")
        runCurrent()
        vm.startPhoneVerification(india, "9876543210")
        runCurrent()

        assertEquals(1, auth.startPhoneCount)
    }

    @Test
    fun `resend is blocked until the countdown reaches zero`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.startPhoneVerification(india, "9876543210")
        runCurrent()

        vm.resendPhoneCode()
        runCurrent()
        assertEquals(0, auth.resendCount)

        advanceTimeBy(31_000L)
        runCurrent()
        now += 31_000L
        vm.resendPhoneCode()
        runCurrent()
        assertEquals(1, auth.resendCount)
    }

    @Test
    fun `a rejected code keeps the OTP step open with a safe message`() = runTest(dispatcher) {
        auth.verifyError = AuthGatewayException(
            AuthFailure("That code is not correct. Check the SMS and try again.", AuthFailureKind.REJECTED),
        )
        val vm = viewModel()
        advanceUntilIdle()
        vm.startPhoneVerification(india, "9876543210")
        runCurrent()

        vm.submitPhoneCode("000000")
        runCurrent()

        assertEquals("That code is not correct. Check the SMS and try again.", vm.error.value)
        assertTrue(vm.phoneState.value.codeSent)
        assertFalse(vm.phoneState.value.verifying)
    }

    @Test
    fun `a valid code completes sign-in and resets the phone step`() = runTest(dispatcher) {
        auth.identity = phoneIdentity()
        profiles.profiles[TestConfig.USER_B] = completedProfile(firebaseUid = TestConfig.USER_B)
        val vm = viewModel()
        advanceUntilIdle()
        vm.startPhoneVerification(india, "9876543210")
        runCurrent()

        vm.submitPhoneCode("123456")
        runCurrent()

        assertTrue(vm.authState.value is AuthState.Authenticated)
        assertFalse(vm.phoneState.value.codeSent)
        assertEquals(0, vm.phoneState.value.resendSecondsRemaining)
    }

    @Test
    fun `an empty code is rejected locally`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.startPhoneVerification(india, "9876543210")
        runCurrent()

        vm.submitPhoneCode("  ")
        runCurrent()

        assertEquals(0, auth.verifyCount)
        assertNotNull(vm.error.value)
    }

    @Test
    fun `a code shorter than six digits never reaches Firebase`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.startPhoneVerification(india, "9876543210")
        runCurrent()

        vm.submitPhoneCode("12345")
        runCurrent()

        assertEquals(0, auth.verifyCount)
        assertNotNull(vm.error.value)
        // The OTP step stays open so the missing digit can be typed.
        assertTrue(vm.phoneState.value.codeSent)
        assertFalse(vm.phoneState.value.verifying)
    }

    @Test
    fun `a pasted code is normalized before Firebase verifies it`() = runTest(dispatcher) {
        auth.identity = phoneIdentity()
        profiles.profiles[TestConfig.USER_B] = completedProfile(firebaseUid = TestConfig.USER_B)
        val vm = viewModel()
        advanceUntilIdle()
        vm.startPhoneVerification(india, "9876543210")
        runCurrent()

        vm.submitPhoneCode(" 123-456 ")
        runCurrent()

        assertEquals(1, auth.verifyCount)
        assertEquals("123456", auth.lastCode)
        assertTrue(vm.authState.value is AuthState.Authenticated)
    }

    @Test
    fun `a resend restarts the countdown`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.startPhoneVerification(india, "9876543210")
        runCurrent()
        advanceTimeBy(31_000L)
        runCurrent()
        now += 31_000L

        vm.resendPhoneCode()
        runCurrent()

        assertEquals(1, auth.resendCount)
        assertTrue(vm.phoneState.value.resendSecondsRemaining > 0)
        assertTrue(vm.phoneState.value.codeSent)
    }

    @Test
    fun `changing the number cancels the pending verification`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.startPhoneVerification(india, "9876543210")
        runCurrent()

        vm.changePhoneNumber()
        runCurrent()

        assertFalse(vm.phoneState.value.codeSent)
        assertEquals(1, auth.discardedChallenges.size)
    }

    // ---------------------------------------------------------------- profile

    @Test
    fun `saving a profile clears the taken hint on success`() = runTest(dispatcher) {
        signedIn()
        val vm = viewModel()
        advanceUntilIdle()

        vm.saveProfile("Rush It Shopper", "fresh.handle")
        advanceUntilIdle()

        assertEquals(UsernameAvailability.Available, vm.usernameAvailability.value)
        assertNull(vm.error.value)
        assertTrue(vm.authState.value is AuthState.Authenticated)
    }

    @Test
    fun `a taken username is flagged for the form`() = runTest(dispatcher) {
        auth.identity = googleIdentity()
        profiles.takenUsernames += "taken"
        val vm = viewModel()
        advanceUntilIdle()

        vm.saveProfile("Rush It Shopper", "taken")
        advanceUntilIdle()

        assertEquals(UsernameAvailability.Taken, vm.usernameAvailability.value)
        assertEquals(AuthErrors.USERNAME_TAKEN, vm.error.value)
    }

    // ---------------------------------------------------------------- sign out

    @Test
    fun `signing out returns to unauthenticated and drops the backend token`() = runTest(dispatcher) {
        signedIn()
        val vm = viewModel()
        advanceUntilIdle()

        vm.signOut()
        advanceUntilIdle()

        assertEquals(AuthState.Unauthenticated, vm.authState.value)
        assertNull(tokens.token)
        assertFalse(vm.uiState.value.signingOut)
    }

    // ---------------------------------------------------------------- routing

    @Test
    fun `routing before the splash has run is not duplicated, and later changes do navigate`() = runTest(dispatcher) {
        signedIn()
        val vm = viewModel()
        advanceUntilIdle()
        // The splash already routed on its own; marking it consumed prevents a second navigation.
        vm.markStartupRouted()

        val event = async { vm.events.first() }
        vm.signOut()
        advanceUntilIdle()

        assertEquals(AuthEvent.OpenLogin, event.await())
        assertEquals(AuthState.Unauthenticated, vm.authState.value)
    }

    @Test
    fun `a recoverable error is published as a message with a recovery action`() = runTest(dispatcher) {
        auth.isConfigured = false
        val vm = viewModel()

        advanceUntilIdle()

        assertNotNull(vm.error.value)
        assertFalse(vm.error.value!!.contains("Exception"))
        assertEquals(
            com.rushit.app.data.auth.RecoveryAction.DISMISS,
            vm.recoveryAction,
        )
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
