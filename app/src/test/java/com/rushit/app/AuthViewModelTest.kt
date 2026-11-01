package com.rushit.app

import com.rushit.app.data.auth.AuthFailureKind
import com.rushit.app.data.auth.AuthState
import com.rushit.app.data.auth.DeleteAccountOutcome
import com.rushit.app.data.auth.SessionRestoreResult
import com.rushit.app.data.auth.SupabaseAuthRepository
import com.rushit.app.data.auth.google.GoogleSignInClient
import com.rushit.app.data.auth.google.GoogleSignInMessages
import com.rushit.app.data.auth.google.GoogleSignInOutcome
import com.rushit.app.data.auth.google.SignInNonce
import com.rushit.app.ui.viewmodel.AuthEvent
import com.rushit.app.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * The Google sign-in flow as the UI drives it: real Credential Manager outcomes, real token checks,
 * real repository orchestration — only the transport is faked.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val userId = TestConfig.USER_A
    private val now = 1_700_000_100L

    @Before
    fun setup() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private class FakeGoogleSignIn : GoogleSignInClient {
        var outcome: GoogleSignInOutcome = GoogleSignInOutcome.Cancelled
        var gate: CompletableDeferred<Unit>? = null
        var requests = 0
        var redirectOutcome: GoogleSignInOutcome? = null
        var redirects = 0

        override suspend fun completeBrowserRedirect(uri: String?): GoogleSignInOutcome? {
            redirects++
            return redirectOutcome
        }

        override suspend fun requestGoogleIdToken(): GoogleSignInOutcome {
            requests++
            gate?.await()
            return outcome
        }
    }

    private fun googleSuccess(
        nonce: String = "raw-nonce",
        tokenNonce: String? = SignInNonce.hash("raw-nonce"),
        audience: String = TestConfig.WEB_CLIENT_ID,
        expiresAt: Long = now + 3_500,
        idToken: String = buildIdToken(audience = audience, expiresAt = expiresAt, nonce = tokenNonce),
    ) = GoogleSignInOutcome.Success(
        idToken = idToken,
        rawNonce = nonce,
        displayName = "Google Name",
        email = "shopper@example.com",
        photoUri = "https://lh3.googleusercontent.com/a/photo",
    )

    private fun viewModel(
        gateway: FakeAuthGateway,
        google: FakeGoogleSignIn = FakeGoogleSignIn(),
    ) = AuthViewModel(SupabaseAuthRepository(gateway), google, TestConfig.WEB_CLIENT_ID) { now }

    // -----------------------------------------------------------------------------------------
    // Google sign-in
    // -----------------------------------------------------------------------------------------

    @Test
    fun `a successful Google sign-in creates the account and routes to onboarding`() = runTest {
        val gateway = FakeAuthGateway().apply { session = googleSession(userId) }
        val google = FakeGoogleSignIn().apply { outcome = googleSuccess() }
        val vm = viewModel(gateway, google)
        val events = mutableListOf<AuthEvent>()
        val collector = launch { vm.events.toList(events) }
        advanceUntilIdle()
        vm.markStartupRouted()

        vm.initiateGoogleSignIn()
        advanceUntilIdle()

        assertEquals(1, google.requests)
        assertEquals(1, gateway.signIns)
        assertTrue(vm.authState.value is AuthState.ProfileIncomplete)
        assertEquals(listOf(AuthEvent.OpenProfileSetup), events)
        assertNull(vm.error.value)
        assertFalse(vm.uiState.value.signingIn)
        collector.cancel()
    }

    @Test
    fun `a returning user is routed home after signing in`() = runTest {
        val gateway = FakeAuthGateway().apply {
            session = googleSession(userId)
            profiles[userId] = completedProfile(userId)
        }
        val google = FakeGoogleSignIn().apply { outcome = googleSuccess() }
        val vm = viewModel(gateway, google)
        val events = mutableListOf<AuthEvent>()
        val collector = launch { vm.events.toList(events) }
        advanceUntilIdle()
        vm.markStartupRouted()

        vm.initiateGoogleSignIn()
        advanceUntilIdle()

        assertTrue(vm.authState.value is AuthState.Authenticated)
        assertEquals(listOf(AuthEvent.OpenHome), events)
        // The customized profile survived a fresh sign-in.
        assertEquals("Custom Name", (vm.authState.value as AuthState.Authenticated).account.displayName)
        collector.cancel()
    }

    @Test
    fun `cancelling the account picker is not reported as an error`() = runTest {
        val gateway = FakeAuthGateway().apply { session = googleSession(userId) }
        val google = FakeGoogleSignIn().apply { outcome = GoogleSignInOutcome.Cancelled }
        val vm = viewModel(gateway, google)
        advanceUntilIdle()
        vm.markStartupRouted()

        vm.initiateGoogleSignIn()
        advanceUntilIdle()

        assertNull("cancellation must never look like a failure", vm.error.value)
        assertEquals(0, gateway.signIns)
        assertEquals(AuthState.Unauthenticated, vm.authState.value)
        assertFalse(vm.uiState.value.signingIn)
    }

    @Test
    fun `no available credential never claims the device has no Google account`() = runTest {
        val gateway = FakeAuthGateway()
        val google = FakeGoogleSignIn().apply { outcome = GoogleSignInOutcome.NoCredentialAvailable }
        val vm = viewModel(gateway, google)
        advanceUntilIdle()

        vm.initiateGoogleSignIn()
        advanceUntilIdle()

        assertEquals(GoogleSignInMessages.UNKNOWN, vm.error.value)
        assertEquals(0, gateway.signIns)
    }

    @Test
    fun `browser callback refreshes the SDK-imported account without a native token exchange`() = runTest {
        val gateway = FakeAuthGateway().apply {
            session = googleSession(userId)
            restoreResult = SessionRestoreResult.Restored(googleSession(userId))
        }
        val google = FakeGoogleSignIn().apply { redirectOutcome = GoogleSignInOutcome.BrowserSuccess }
        val vm = viewModel(gateway, google)
        advanceUntilIdle()

        vm.handleAuthRedirect("rushit://auth/callback?code=abcdefghijklmnopqrstuvwxyz123456")
        advanceUntilIdle()

        assertEquals(1, google.redirects)
        assertEquals(0, gateway.signIns)
        assertTrue(vm.authState.value is AuthState.Authenticated || vm.authState.value is AuthState.ProfileIncomplete)
        assertFalse(vm.uiState.value.signingIn)
    }

    @Test
    fun `unrelated deep links never start an auth exchange`() = runTest {
        val google = FakeGoogleSignIn()
        val vm = viewModel(FakeAuthGateway(), google)
        advanceUntilIdle()
        vm.handleAuthRedirect("https://example.com/callback?code=abcdefghijklmnopqrstuvwxyz123456")
        advanceUntilIdle()
        assertEquals(0, google.redirects)
    }

    @Test
    fun `an unconfigured build reports configuration, not a fake success`() = runTest {
        val gateway = FakeAuthGateway().apply { session = googleSession(userId) }
        val google = FakeGoogleSignIn().apply { outcome = GoogleSignInOutcome.Unconfigured }
        val vm = viewModel(gateway, google)
        advanceUntilIdle()

        vm.initiateGoogleSignIn()
        advanceUntilIdle()

        assertEquals(GoogleSignInMessages.UNCONFIGURED, vm.error.value)
        assertEquals(0, gateway.signIns)
        assertFalse(vm.authState.value is AuthState.Authenticated)
    }

    @Test
    fun `an unexpected credential is reported instead of being treated as a sign-in`() = runTest {
        val gateway = FakeAuthGateway().apply { session = googleSession(userId) }
        val google = FakeGoogleSignIn().apply {
            outcome = GoogleSignInOutcome.Failed(GoogleSignInMessages.UNEXPECTED_CREDENTIAL)
        }
        val vm = viewModel(gateway, google)
        advanceUntilIdle()

        vm.initiateGoogleSignIn()
        advanceUntilIdle()

        assertEquals(GoogleSignInMessages.UNEXPECTED_CREDENTIAL, vm.error.value)
        assertEquals(0, gateway.signIns)
    }

    @Test
    fun `a token issued for the wrong web client never reaches Supabase`() = runTest {
        val gateway = FakeAuthGateway().apply { session = googleSession(userId) }
        val google = FakeGoogleSignIn().apply {
            outcome = googleSuccess(audience = TestConfig.OTHER_CLIENT_ID)
        }
        val vm = viewModel(gateway, google)
        advanceUntilIdle()

        vm.initiateGoogleSignIn()
        advanceUntilIdle()

        assertEquals(GoogleSignInMessages.AUDIENCE_MISMATCH, vm.error.value)
        assertEquals(0, gateway.signIns)
    }

    @Test
    fun `an expired token is rejected before Supabase is called`() = runTest {
        val gateway = FakeAuthGateway().apply { session = googleSession(userId) }
        val google = FakeGoogleSignIn().apply { outcome = googleSuccess(expiresAt = now - 600) }
        val vm = viewModel(gateway, google)
        advanceUntilIdle()

        vm.initiateGoogleSignIn()
        advanceUntilIdle()

        assertEquals(GoogleSignInMessages.EXPIRED_TOKEN, vm.error.value)
        assertEquals(0, gateway.signIns)
    }

    @Test
    fun `a token whose nonce does not match is rejected`() = runTest {
        val gateway = FakeAuthGateway().apply { session = googleSession(userId) }
        val google = FakeGoogleSignIn().apply { outcome = googleSuccess(tokenNonce = "somebody-elses-nonce") }
        val vm = viewModel(gateway, google)
        advanceUntilIdle()

        vm.initiateGoogleSignIn()
        advanceUntilIdle()

        assertEquals(GoogleSignInMessages.NONCE_MISMATCH, vm.error.value)
        assertEquals(0, gateway.signIns)
    }

    @Test
    fun `a token from a different issuer is rejected`() = runTest {
        val gateway = FakeAuthGateway().apply { session = googleSession(userId) }
        val google = FakeGoogleSignIn().apply {
            outcome = googleSuccess(idToken = buildIdToken(issuer = "https://login.example.test"))
        }
        val vm = viewModel(gateway, google)
        advanceUntilIdle()

        vm.initiateGoogleSignIn()
        advanceUntilIdle()

        assertEquals(GoogleSignInMessages.NOT_GOOGLE_TOKEN, vm.error.value)
        assertEquals(0, gateway.signIns)
    }

    @Test
    fun `in-flight sign-ins cannot be duplicated`() = runTest {
        val gateway = FakeAuthGateway().apply { session = googleSession(userId) }
        val gate = CompletableDeferred<Unit>()
        val google = FakeGoogleSignIn().apply { outcome = googleSuccess(); this.gate = gate }
        val vm = viewModel(gateway, google)
        advanceUntilIdle()

        vm.initiateGoogleSignIn()
        vm.initiateGoogleSignIn()
        vm.initiateGoogleSignIn()
        advanceUntilIdle()

        assertTrue("the button must show itself as busy", vm.uiState.value.signingIn)
        assertTrue(vm.uiState.value.busy)
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, google.requests)
        assertEquals(1, gateway.signIns)
        assertFalse(vm.uiState.value.signingIn)
    }

    @Test
    fun `a Supabase rejection shows a friendly message and returns to the login state`() = runTest {
        val gateway = FakeAuthGateway().apply {
            session = googleSession(userId)
            signInError = IOException("no route to host")
        }
        val google = FakeGoogleSignIn().apply { outcome = googleSuccess() }
        val vm = viewModel(gateway, google)
        advanceUntilIdle()

        vm.initiateGoogleSignIn()
        advanceUntilIdle()

        val error = vm.error.value
        assertTrue("expected an offline explanation, got: $error", error!!.contains("connection"))
        assertFalse(error.contains("IOException"))
        assertEquals(AuthState.Unauthenticated, vm.authState.value)
        assertFalse(vm.uiState.value.signingIn)
    }

    @Test
    fun `a raw transport message is never shown to the person`() = runTest {
        val exceptions = RestExceptionFactory()
        val gateway = FakeAuthGateway().apply {
            session = googleSession(userId)
            signInError = exceptions.rowLevelSecurity()
        }
        val google = FakeGoogleSignIn().apply { outcome = googleSuccess() }
        val vm = viewModel(gateway, google)
        advanceUntilIdle()

        vm.initiateGoogleSignIn()
        advanceUntilIdle()

        val error = vm.error.value!!
        assertFalse(error.contains(RestExceptionFactory.SECRET_ANON_KEY))
        assertFalse(error.contains(RestExceptionFactory.SECRET_JWT))
        assertFalse(error.contains("supabase.co"))
        exceptions.close()
    }

    // -----------------------------------------------------------------------------------------
    // Start-up routing
    // -----------------------------------------------------------------------------------------

    @Test
    fun `a restored session is authenticated without showing the login screen`() = runTest {
        val gateway = FakeAuthGateway().apply {
            profiles[userId] = completedProfile(userId)
            restoreResult = SessionRestoreResult.Restored(googleSession(userId))
        }
        val vm = viewModel(gateway)
        val events = mutableListOf<AuthEvent>()
        val collector = launch { vm.events.toList(events) }
        advanceUntilIdle()

        assertTrue(vm.authState.value is AuthState.Authenticated)
        // The splash performs the first navigation, so no duplicate event is published.
        assertTrue(events.isEmpty())
        vm.markStartupRouted()
        advanceUntilIdle()
        assertTrue("the restored session must not be re-routed", events.isEmpty())
        collector.cancel()
    }

    @Test
    fun `re-publishing the same authenticated state does not re-navigate`() = runTest {
        val gateway = FakeAuthGateway().apply {
            profiles[userId] = completedProfile(userId)
            restoreResult = SessionRestoreResult.Restored(googleSession(userId))
        }
        val vm = viewModel(gateway)
        val events = mutableListOf<AuthEvent>()
        val collector = launch { vm.events.toList(events) }
        advanceUntilIdle()
        vm.markStartupRouted()

        // Somebody shops for a while, the app resumes, the account is re-read.
        repeat(3) { vm.refreshAccount() }
        advanceUntilIdle()

        assertTrue("shopping must not be interrupted by repeated navigation", events.isEmpty())
        assertTrue(vm.authState.value is AuthState.Authenticated)
        collector.cancel()
    }

    @Test
    fun `signing out routes to login and clearing the stack`() = runTest {
        val gateway = FakeAuthGateway().apply {
            session = googleSession(userId)
            profiles[userId] = completedProfile(userId)
            restoreResult = SessionRestoreResult.Restored(googleSession(userId))
        }
        val vm = viewModel(gateway)
        val events = mutableListOf<AuthEvent>()
        val collector = launch { vm.events.toList(events) }
        advanceUntilIdle()
        vm.markStartupRouted()

        vm.signOut()
        advanceUntilIdle()

        assertEquals(AuthState.Unauthenticated, vm.authState.value)
        assertEquals(listOf(AuthEvent.OpenLogin), events)
        assertFalse(vm.uiState.value.signingOut)
        collector.cancel()
    }

    @Test
    fun `an offline start-up offers a retry that succeeds once connectivity returns`() = runTest {
        val gateway = FakeAuthGateway().apply {
            restoreResult = SessionRestoreResult.Unreachable(sessionStillStored = true)
        }
        val vm = viewModel(gateway)
        val events = mutableListOf<AuthEvent>()
        val collector = launch { vm.events.toList(events) }
        advanceUntilIdle()

        assertTrue(vm.authState.value is AuthState.RecoverableError)
        val message = vm.error.value
        assertTrue("expected an offline explanation, got: $message", message!!.contains("connection"))

        // The splash already routed this person to the login screen; recovering from the same
        // screen must not publish a second navigation for the same destination.
        vm.markStartupRouted()
        advanceUntilIdle()
        assertTrue(events.isEmpty())

        gateway.restoreResult = SessionRestoreResult.NoSession
        vm.retrySession()
        advanceUntilIdle()

        assertEquals(AuthState.Unauthenticated, vm.authState.value)
        assertNull(vm.error.value)
        assertTrue(events.isEmpty())
        collector.cancel()
    }

    // -----------------------------------------------------------------------------------------
    // Profile saving and account actions
    // -----------------------------------------------------------------------------------------

    @Test
    fun `saving an invalid display name reports validation without a write`() = runTest {
        val gateway = FakeAuthGateway().apply { session = googleSession(userId) }
        val google = FakeGoogleSignIn().apply { outcome = googleSuccess() }
        val vm = viewModel(gateway, google)
        advanceUntilIdle()
        vm.initiateGoogleSignIn()
        advanceUntilIdle()

        vm.saveProfile(" ", "rushit", null)
        advanceUntilIdle()

        assertTrue(vm.error.value!!.contains("name"))
        assertTrue(gateway.savedProfiles.isEmpty())
        assertTrue(vm.authState.value is AuthState.ProfileIncomplete)
    }

    @Test
    fun `a username conflict keeps the person in onboarding with an explanation`() = runTest {
        val gateway = FakeAuthGateway().apply {
            session = googleSession(userId)
            takenUsernames += "taken"
        }
        val google = FakeGoogleSignIn().apply { outcome = googleSuccess() }
        val vm = viewModel(gateway, google)
        advanceUntilIdle()
        vm.initiateGoogleSignIn()
        advanceUntilIdle()

        vm.saveProfile("Rush It", "taken", null)
        advanceUntilIdle()

        assertEquals(SupabaseAuthRepository.Messages.USERNAME_TAKEN, vm.error.value)
        assertTrue(vm.authState.value is AuthState.ProfileIncomplete)
        assertFalse(vm.uiState.value.savingProfile)
    }

    @Test
    fun `completing onboarding routes home exactly once`() = runTest {
        val gateway = FakeAuthGateway().apply { session = googleSession(userId) }
        val google = FakeGoogleSignIn().apply { outcome = googleSuccess() }
        val vm = viewModel(gateway, google)
        val events = mutableListOf<AuthEvent>()
        val collector = launch { vm.events.toList(events) }
        advanceUntilIdle()
        vm.markStartupRouted()

        vm.initiateGoogleSignIn()
        advanceUntilIdle()
        assertEquals(listOf(AuthEvent.OpenProfileSetup), events)

        vm.saveProfile("Rush It Shopper", "rushit", null)
        advanceUntilIdle()

        assertEquals(listOf(AuthEvent.OpenProfileSetup, AuthEvent.OpenHome), events)
        assertTrue(gateway.profiles.getValue(userId).onboardingCompleted)
        collector.cancel()
    }

    @Test
    fun `in-flight profile saves cannot be duplicated`() = runTest {
        val gateway = FakeAuthGateway().apply { session = googleSession(userId) }
        val google = FakeGoogleSignIn().apply { outcome = googleSuccess() }
        val vm = viewModel(gateway, google)
        advanceUntilIdle()
        vm.initiateGoogleSignIn()
        advanceUntilIdle()

        val gate = CompletableDeferred<Unit>()
        gateway.saveGate = gate
        vm.saveProfile("Rush It", "rushit", null)
        vm.saveProfile("Rush It", "rushit", null)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.savingProfile)
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, gateway.savedProfiles.size)
        assertFalse(vm.uiState.value.savingProfile)
    }

    @Test
    fun `a failed deletion is reported and the account stays signed in`() = runTest {
        val gateway = FakeAuthGateway().apply {
            session = googleSession(userId)
            profiles[userId] = completedProfile(userId)
            restoreResult = SessionRestoreResult.Restored(googleSession(userId))
            deleteOutcome = DeleteAccountOutcome.NotDeployed
        }
        val vm = viewModel(gateway)
        val events = mutableListOf<AuthEvent>()
        val collector = launch { vm.events.toList(events) }
        advanceUntilIdle()
        vm.markStartupRouted()

        vm.deleteAccount()
        advanceUntilIdle()

        assertEquals(SupabaseAuthRepository.Messages.DELETE_NOT_DEPLOYED, vm.error.value)
        assertTrue(vm.authState.value is AuthState.Authenticated)
        assertTrue("a failed deletion must not navigate anywhere", events.isEmpty())
        assertFalse(vm.uiState.value.deletingAccount)
        collector.cancel()
    }

    @Test
    fun `a successful deletion signs the user out and routes to login`() = runTest {
        val gateway = FakeAuthGateway().apply {
            session = googleSession(userId)
            profiles[userId] = completedProfile(userId)
            restoreResult = SessionRestoreResult.Restored(googleSession(userId))
        }
        val vm = viewModel(gateway)
        val events = mutableListOf<AuthEvent>()
        val collector = launch { vm.events.toList(events) }
        advanceUntilIdle()
        vm.markStartupRouted()

        vm.deleteAccount()
        advanceUntilIdle()

        assertNull(vm.error.value)
        assertEquals(AuthState.Unauthenticated, vm.authState.value)
        assertEquals(listOf(AuthEvent.OpenLogin), events)
        assertFalse(gateway.profiles.containsKey(userId))
        collector.cancel()
    }

    @Test
    fun `an unconfigured backend is explained at start-up`() = runTest {
        val gateway = FakeAuthGateway().apply { isConfigured = false }
        val vm = viewModel(gateway)
        advanceUntilIdle()

        val state = vm.authState.value as AuthState.RecoverableError
        assertEquals(AuthFailureKind.UNCONFIGURED, state.kind)
        assertEquals(SupabaseAuthRepository.Messages.SUPABASE_UNCONFIGURED, vm.error.value)
    }

    @Test
    fun `clearing an error removes it`() = runTest {
        val gateway = FakeAuthGateway()
        val google = FakeGoogleSignIn().apply { outcome = GoogleSignInOutcome.NoCredentialAvailable }
        val vm = viewModel(gateway, google)
        advanceUntilIdle()
        vm.initiateGoogleSignIn()
        advanceUntilIdle()
        assertEquals(GoogleSignInMessages.UNKNOWN, vm.error.value)

        vm.clearError()

        assertNull(vm.error.value)
    }
}
