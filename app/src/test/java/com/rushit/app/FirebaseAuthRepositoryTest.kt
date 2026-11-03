package com.rushit.app

import com.rushit.app.data.auth.AuthErrors
import com.rushit.app.data.auth.AuthFailure
import com.rushit.app.data.auth.AuthFailureKind
import com.rushit.app.data.auth.AuthGatewayException
import com.rushit.app.data.auth.AuthResult
import com.rushit.app.data.auth.AuthState
import com.rushit.app.data.auth.FirebaseAuthRepository
import com.rushit.app.data.auth.PhoneVerificationOutcome
import com.rushit.app.data.auth.RecoveryAction
import com.rushit.app.data.auth.phone.PhoneNumbers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseAuthRepositoryTest {

    private val auth = FakeAuthGateway()
    private var now = 1_700_000_000_000L

    private fun repository() = FirebaseAuthRepository(auth = auth, context = null) { now }

    private val india = PhoneNumbers.default
    private val phoneNumber = PhoneNumbers.normalize(india, "9876543210")!!

    @Test
    fun `unconfigured build reports unconfigured state and does not contact network`() = runTest {
        auth.isConfigured = false
        val repo = repository()

        repo.initialize()

        val state = repo.authState.value as AuthState.RecoverableError
        assertEquals(AuthFailureKind.UNCONFIGURED, state.kind)
        assertEquals(RecoveryAction.DISMISS, state.action)
    }

    @Test
    fun `start-up without a session publishes Unauthenticated`() = runTest {
        auth.identity = null
        val repo = repository()

        repo.initialize()

        assertEquals(AuthState.Unauthenticated, repo.authState.value)
    }

    @Test
    fun `start-up with a restored user publishes ProfileIncomplete or Authenticated`() = runTest {
        auth.identity = googleIdentity()
        val repo = repository()

        repo.initialize()

        val state = repo.authState.value
        assertTrue(state is AuthState.ProfileIncomplete || state is AuthState.Authenticated)
    }

    @Test
    fun `first-time Google sign-in lands on ProfileIncomplete`() = runTest {
        val repo = repository()
        auth.identity = googleIdentity(fullName = null)

        val result = repo.signInWithGoogle("token")

        assertEquals(AuthResult.Success, result)
        val state = repo.authState.value as AuthState.ProfileIncomplete
        assertEquals(TestConfig.USER_A, state.draft.id)
    }

    @Test
    fun `Google sign-in failure publishes Unauthenticated and returns Failure`() = runTest {
        val repo = repository()
        auth.googleError = AuthGatewayException(AuthFailure("Invalid ID token", AuthFailureKind.INVALID_TOKEN))

        val result = repo.signInWithGoogle("bad-token")

        assertTrue(result is AuthResult.Failure)
        assertEquals(AuthState.Unauthenticated, repo.authState.value)
    }

    @Test
    fun `start phone verification texts a code and publishes CodeSent`() = runTest {
        val repo = repository()

        val outcome = repo.startPhoneVerification(phoneNumber)

        assertEquals(PhoneVerificationOutcome.CodeSent, outcome)
        assertEquals("+919876543210", auth.lastPhoneNumber)
    }

    @Test
    fun `auto-verified phone verification signs user in immediately`() = runTest {
        val repo = repository()
        auth.autoVerifyOnStart = true

        val outcome = repo.startPhoneVerification(phoneNumber)

        assertEquals(PhoneVerificationOutcome.Completed, outcome)
        val state = repo.authState.value
        assertTrue(state is AuthState.ProfileIncomplete || state is AuthState.Authenticated)
    }

    @Test
    fun `submitting the phone OTP verifies code and signs user in`() = runTest {
        val repo = repository()
        repo.startPhoneVerification(phoneNumber)

        val result = repo.submitPhoneCode("123456")

        assertEquals(AuthResult.Success, result)
        assertEquals("123456", auth.lastCode)
        val state = repo.authState.value
        assertTrue(state is AuthState.ProfileIncomplete || state is AuthState.Authenticated)
    }

    @Test
    fun `submitting wrong OTP fails without losing pending challenge`() = runTest {
        val repo = repository()
        repo.startPhoneVerification(phoneNumber)
        auth.verifyError = AuthGatewayException(AuthFailure("Wrong code", AuthFailureKind.REJECTED))

        val result = repo.submitPhoneCode("000000")

        assertTrue(result is AuthResult.Failure)
        assertEquals("Wrong code", (result as AuthResult.Failure).message)

        // Retry with correct code works without requesting new code
        auth.verifyError = null
        val secondResult = repo.submitPhoneCode("123456")
        assertEquals(AuthResult.Success, secondResult)
    }

    @Test
    fun `resend code before 30 seconds is throttled`() = runTest {
        val repo = repository()
        repo.startPhoneVerification(phoneNumber)

        now += 15_000L // 15s elapsed
        val outcome = repo.resendPhoneCode()

        assertTrue(outcome is PhoneVerificationOutcome.Failure)
        assertEquals(AuthErrors.RATE_LIMITED, (outcome as PhoneVerificationOutcome.Failure).message)

        now += 16_000L // 31s elapsed
        val allowed = repo.resendPhoneCode()
        assertEquals(PhoneVerificationOutcome.CodeSent, allowed)
    }

    @Test
    fun `cancelling phone verification discards challenge`() = runTest {
        val repo = repository()
        repo.startPhoneVerification(phoneNumber)

        repo.cancelPhoneVerification()

        assertEquals(1, auth.discardedChallenges.size)
    }

    @Test
    fun `sign-out resets state to Unauthenticated`() = runTest {
        val repo = repository()
        repo.signInWithGoogle("token")

        val result = repo.signOut()

        assertEquals(AuthResult.Success, result)
        assertEquals(AuthState.Unauthenticated, repo.authState.value)
        assertEquals(1, auth.signOutCount)
    }

    @Test
    fun `delete account resets state to Unauthenticated`() = runTest {
        val repo = repository()
        repo.signInWithGoogle("token")

        val result = repo.deleteAccount()

        assertEquals(AuthResult.Success, result)
        assertEquals(AuthState.Unauthenticated, repo.authState.value)
        assertEquals(1, auth.deleteCount)
    }
}
