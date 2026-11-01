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
import com.rushit.app.data.auth.UsernameAvailability
import com.rushit.app.data.auth.phone.PhoneNumbers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The orchestration contract for Firebase identity plus Supabase data.
 *
 * The tests here are the specification for the migration's security-relevant behaviour: one profile
 * per Firebase UID, the Supabase client only ever holding the current user's token, logout dropping
 * that token even when Firebase fails, and account deletion never losing data it cannot prove it
 * deleted.
 */
class FirebaseAuthRepositoryTest {

    private val auth = FakeAuthGateway()
    private val profiles = FakeProfileGateway()
    private val tokens = RecordingTokenBridge()
    private var now = 1_700_000_000_000L

    private fun repository() = FirebaseAuthRepository(auth, profiles, tokens) { now }

    private val india = PhoneNumbers.default
    private val phoneNumber = PhoneNumbers.normalize(india, "9876543210")!!

    // ---------------------------------------------------------------- start-up

    @Test
    fun `an unconfigured Firebase build reports itself instead of pretending to sign in`() = runTest {
        auth.isConfigured = false
        val repository = repository()

        repository.initialize()

        val state = repository.authState.value
        assertTrue(state is AuthState.RecoverableError)
        assertEquals(AuthFailureKind.UNCONFIGURED, (state as AuthState.RecoverableError).kind)
        assertEquals(RecoveryAction.DISMISS, state.action)
    }

    @Test
    fun `an unconfigured Supabase build reports the missing project, not a signed-in user`() = runTest {
        auth.identity = googleIdentity()
        profiles.isConfigured = false
        val repository = repository()

        repository.initialize()

        val state = repository.authState.value
        assertTrue(state is AuthState.RecoverableError)
        assertEquals(AuthFailureKind.UNCONFIGURED, (state as AuthState.RecoverableError).kind)
    }

    @Test
    fun `no Firebase session lands on unauthenticated and drops any stale backend token`() = runTest {
        auth.identity = null
        val repository = repository()

        repository.initialize()

        assertEquals(AuthState.Unauthenticated, repository.authState.value)
        assertEquals(1, tokens.clears)
        assertNull(tokens.token)
    }

    @Test
    fun `a restored session installs the Firebase token and publishes the profile`() = runTest {
        auth.identity = googleIdentity()
        profiles.profiles[TestConfig.USER_A] = completedProfile()
        val repository = repository()

        repository.initialize()

        val state = repository.authState.value
        assertTrue(state is AuthState.Authenticated)
        val account = (state as AuthState.Authenticated).account
        assertEquals(TestConfig.USER_A, account.uid)
        assertEquals("shopper@example.com", account.email)
        assertEquals("rushit", account.username)
        assertEquals(TestConfig.TOKEN, tokens.token?.token)
        // A restore must not force a token refresh: Firebase already refreshes when needed.
        assertEquals(listOf(false), auth.tokenRequests)
    }

    @Test
    fun `a first-time user gets exactly one profile row and is sent to onboarding`() = runTest {
        auth.identity = googleIdentity()
        val repository = repository()

        repository.initialize()

        val state = repository.authState.value
        assertTrue(state is AuthState.ProfileIncomplete)
        assertEquals(TestConfig.USER_A, (state as AuthState.ProfileIncomplete).draft.id)
        assertEquals(listOf(TestConfig.USER_A), profiles.provisioned)
        assertTrue(profiles.settings.contains(TestConfig.USER_A))
        assertEquals("Rush It", profiles.profiles.getValue(TestConfig.USER_A).fullName)
        assertFalse(profiles.profiles.getValue(TestConfig.USER_A).onboardingCompleted)
    }

    @Test
    fun `a returning user whose profile is complete never re-provisions or re-overwrites it`() = runTest {
        auth.identity = googleIdentity(fullName = "Google Name")
        profiles.profiles[TestConfig.USER_A] = completedProfile(fullName = "Name I Chose", username = "mine")
        val repository = repository()

        repository.initialize()

        assertEquals("Name I Chose", (repository.authState.value as AuthState.Authenticated).account.displayName)
        assertEquals("mine", (repository.authState.value as AuthState.Authenticated).account.username)
        assertTrue(profiles.provisioned.isEmpty())
    }

    @Test
    fun `a token that cannot be minted keeps the person on a retryable error`() = runTest {
        auth.identity = googleIdentity()
        auth.tokenError = AuthGatewayException(AuthFailure(AuthErrors.OFFLINE, AuthFailureKind.NETWORK))
        val repository = repository()

        repository.initialize()

        val state = repository.authState.value
        assertTrue(state is AuthState.RecoverableError)
        assertEquals(RecoveryAction.RETRY_SESSION, (state as AuthState.RecoverableError).action)
    }

    @Test
    fun `a database that refuses the request surfaces the backend authorization message`() = runTest {
        auth.identity = googleIdentity()
        profiles.findError = AuthGatewayException(
            AuthFailure(AuthErrors.BACKEND_NOT_AUTHORIZED, AuthFailureKind.POLICY_DENIED),
        )
        val repository = repository()

        repository.initialize()

        val state = repository.authState.value as AuthState.RecoverableError
        assertEquals(AuthFailureKind.POLICY_DENIED, state.kind)
        assertEquals(AuthErrors.BACKEND_NOT_AUTHORIZED, state.message)
    }

    // ---------------------------------------------------------------- Google

    @Test
    fun `Google sign-in exchanges the ID token with Firebase and forces a fresh backend token`() = runTest {
        auth.identity = googleIdentity()
        profiles.profiles[TestConfig.USER_A] = completedProfile()
        val repository = repository()

        val result = repository.signInWithGoogle("google-id-token")

        assertEquals(AuthResult.Success, result)
        assertEquals(1, auth.googleSignInCount)
        // forceRefresh = true: a `role: authenticated` custom claim only lands in a new token.
        assertEquals(listOf(true), auth.tokenRequests)
        assertTrue(repository.authState.value is AuthState.Authenticated)
    }

    @Test
    fun `a rejected Google sign-in ends on the login screen with a safe message`() = runTest {
        auth.googleError = AuthGatewayException(
            AuthFailure("That sign-in was not accepted. Please try again.", AuthFailureKind.INVALID_TOKEN),
        )
        val repository = repository()

        val result = repository.signInWithGoogle("google-id-token")

        assertTrue(result is AuthResult.Failure)
        assertEquals(AuthFailureKind.INVALID_TOKEN, (result as AuthResult.Failure).kind)
        assertEquals(AuthState.Unauthenticated, repository.authState.value)
    }

    @Test
    fun `a failed re-authentication never downgrades an already signed-in person`() = runTest {
        auth.identity = googleIdentity()
        profiles.profiles[TestConfig.USER_A] = completedProfile()
        val repository = repository()
        repository.initialize()
        auth.googleError = AuthGatewayException(AuthFailure("nope", AuthFailureKind.REJECTED))

        repository.signInWithGoogle("google-id-token")

        assertTrue(repository.authState.value is AuthState.Authenticated)
    }

    @Test
    fun `a blank Google token is rejected before Firebase is asked`() = runTest {
        val repository = repository()

        val result = repository.signInWithGoogle("   ")

        assertTrue(result is AuthResult.Failure)
        assertEquals(0, auth.googleSignInCount)
    }

    // ---------------------------------------------------------------- phone

    @Test
    fun `requesting a code sends the E.164 number to Firebase`() = runTest {
        val repository = repository()

        val outcome = repository.startPhoneVerification(phoneNumber)

        assertEquals(PhoneVerificationOutcome.CodeSent, outcome)
        assertEquals("+919876543210", auth.lastPhoneNumber)
    }

    @Test
    fun `a second request while a code is pending never costs another SMS`() = runTest {
        val repository = repository()
        repository.startPhoneVerification(phoneNumber)

        val second = repository.startPhoneVerification(phoneNumber)

        assertTrue(second is PhoneVerificationOutcome.Failure)
        assertEquals(1, auth.startPhoneCount)
    }

    @Test
    fun `a valid code signs the user in and binds the backend token`() = runTest {
        auth.identity = phoneIdentity()
        profiles.profiles[TestConfig.USER_B] = completedProfile(firebaseUid = TestConfig.USER_B)
        val repository = repository()
        repository.startPhoneVerification(phoneNumber)

        val result = repository.submitPhoneCode(" 123456 ")

        assertEquals(AuthResult.Success, result)
        assertEquals("123456", auth.lastCode)
        assertTrue(repository.authState.value is AuthState.Authenticated)
        val account = (repository.authState.value as AuthState.Authenticated).account
        // Phone-only accounts have no email, and the UI must still have something to show.
        assertNull(account.email)
        assertEquals("+919876543210", account.contactLine)
        assertEquals(TestConfig.TOKEN, tokens.token?.token)
    }

    @Test
    fun `an invalid code keeps the challenge so a typo does not need a new SMS`() = runTest {
        auth.verifyError = AuthGatewayException(
            AuthFailure("That code is not correct. Check the SMS and try again.", AuthFailureKind.REJECTED),
        )
        val repository = repository()
        repository.startPhoneVerification(phoneNumber)

        val failed = repository.submitPhoneCode("000000")
        assertTrue(failed is AuthResult.Failure)
        assertEquals(AuthState.Unauthenticated, repository.authState.value)

        // The challenge survived, so a corrected code is accepted without another request.
        auth.verifyError = null
        auth.identity = phoneIdentity()
        profiles.profiles[TestConfig.USER_B] = completedProfile(firebaseUid = TestConfig.USER_B)
        assertEquals(AuthResult.Success, repository.submitPhoneCode("123456"))
        assertEquals(1, auth.startPhoneCount)
        assertEquals(2, auth.verifyCount)
    }

    @Test
    fun `a code without a pending challenge is rejected`() = runTest {
        val repository = repository()

        val result = repository.submitPhoneCode("123456")

        assertTrue(result is AuthResult.Failure)
        assertEquals(0, auth.verifyCount)
    }

    @Test
    fun `resending is throttled for thirty seconds`() = runTest {
        val repository = repository()
        repository.startPhoneVerification(phoneNumber)

        assertTrue(repository.resendPhoneCode() is PhoneVerificationOutcome.Failure)
        assertEquals(0, auth.resendCount)

        now += 30_000L
        assertEquals(PhoneVerificationOutcome.CodeSent, repository.resendPhoneCode())
        assertEquals(1, auth.resendCount)
    }

    @Test
    fun `instant verification signs in without asking for a code`() = runTest {
        auth.autoVerifyOnStart = true
        auth.identity = phoneIdentity()
        profiles.profiles[TestConfig.USER_B] = completedProfile(firebaseUid = TestConfig.USER_B)
        val repository = repository()

        val outcome = repository.startPhoneVerification(phoneNumber)

        assertEquals(PhoneVerificationOutcome.Completed, outcome)
        assertTrue(repository.authState.value is AuthState.Authenticated)
    }

    @Test
    fun `cancelling a phone verification discards the Firebase challenge`() = runTest {
        val repository = repository()
        repository.startPhoneVerification(phoneNumber)

        repository.cancelPhoneVerification()

        assertEquals(1, auth.discardedChallenges.size)
        assertTrue(repository.submitPhoneCode("123456") is AuthResult.Failure)
    }

    // ---------------------------------------------------------------- profile

    @Test
    fun `saving a profile stores it by Firebase UID and completes onboarding`() = runTest {
        auth.identity = googleIdentity()
        val repository = repository()
        repository.initialize()

        val result = repository.saveProfile("Rush It Shopper", "rushit.shopper", null)

        assertEquals(AuthResult.Success, result)
        assertEquals("rushit.shopper", profiles.profiles.getValue(TestConfig.USER_A).username)
        assertTrue(profiles.profiles.getValue(TestConfig.USER_A).onboardingCompleted)
        assertTrue(repository.authState.value is AuthState.Authenticated)
    }

    @Test
    fun `a taken username is reported before the write`() = runTest {
        auth.identity = googleIdentity()
        profiles.takenUsernames += "taken"
        val repository = repository()
        repository.initialize()

        val result = repository.saveProfile("Rush It Shopper", "taken", null)

        val failure = result as AuthResult.Failure
        assertEquals(AuthFailureKind.CONFLICT, failure.kind)
        assertEquals(AuthErrors.USERNAME_TAKEN, failure.message)
        assertTrue(profiles.savedProfiles.isEmpty())
    }

    @Test
    fun `an invalid display name never reaches the database`() = runTest {
        auth.identity = googleIdentity()
        val repository = repository()
        repository.initialize()

        val result = repository.saveProfile(" ", "someone", null)

        assertTrue(result is AuthResult.Failure)
        assertTrue(profiles.savedProfiles.isEmpty())
    }

    @Test
    fun `an unavailable username check does not block onboarding`() = runTest {
        auth.identity = googleIdentity()
        profiles.usernameCheckError = IllegalStateException("offline")
        val repository = repository()

        assertEquals(UsernameAvailability.Unknown, repository.checkUsername("someone"))
    }

    // ---------------------------------------------------------------- sign out

    @Test
    fun `signing out records the event, clears the backend token and lands on login`() = runTest {
        auth.identity = googleIdentity()
        profiles.profiles[TestConfig.USER_A] = completedProfile()
        val repository = repository()
        repository.initialize()

        val result = repository.signOut()

        assertEquals(AuthResult.Success, result)
        assertEquals(1, auth.signOutCount)
        assertEquals(AuthState.Unauthenticated, repository.authState.value)
        assertNull(tokens.token)
        assertTrue(auth.discardedChallenges.isEmpty())
        assertTrue(profiles.activityLog.any { it.second == "sign_out" })
    }

    @Test
    fun `a failing Firebase sign-out still drops the token and the session`() = runTest {
        auth.identity = googleIdentity()
        profiles.profiles[TestConfig.USER_A] = completedProfile()
        val repository = repository()
        repository.initialize()
        auth.signOutError = AuthGatewayException(AuthFailure(AuthErrors.OFFLINE, AuthFailureKind.NETWORK))

        val result = repository.signOut()

        assertTrue(result is AuthResult.Failure)
        assertNull(tokens.token)
        assertEquals(AuthState.Unauthenticated, repository.authState.value)
    }

    // ---------------------------------------------------------------- deletion

    @Test
    fun `deleting an account removes Firebase first, then the profile, then the token`() = runTest {
        auth.identity = googleIdentity()
        profiles.profiles[TestConfig.USER_A] = completedProfile()
        val repository = repository()
        repository.initialize()

        val result = repository.deleteAccount()

        assertEquals(AuthResult.Success, result)
        assertEquals(1, auth.deleteCount)
        assertEquals(listOf(TestConfig.USER_A), profiles.deleted)
        assertNull(tokens.token)
        assertEquals(AuthState.Unauthenticated, repository.authState.value)
        // The last token request must be a forced refresh so the purge is still authorized.
        assertEquals(true, auth.tokenRequests.last())
    }

    @Test
    fun `a refused deletion keeps the account and the profile intact`() = runTest {
        auth.identity = googleIdentity()
        profiles.profiles[TestConfig.USER_A] = completedProfile()
        val repository = repository()
        repository.initialize()
        auth.deleteError = AuthGatewayException(
            AuthFailure("For your security, please sign in again.", AuthFailureKind.SESSION_EXPIRED),
        )

        val result = repository.deleteAccount()

        assertTrue(result is AuthResult.Failure)
        assertTrue(profiles.deleted.isEmpty())
        assertNotNull(profiles.profiles[TestConfig.USER_A])
        assertNotNull(tokens.token)
        assertTrue(repository.authState.value is AuthState.Authenticated)
    }

    @Test
    fun `a profile purge that fails after Firebase deletion is reported, not hidden`() = runTest {
        auth.identity = googleIdentity()
        profiles.profiles[TestConfig.USER_A] = completedProfile()
        val repository = repository()
        repository.initialize()
        profiles.deleteError = IllegalStateException("row level security")

        val result = repository.deleteAccount()

        val failure = result as AuthResult.Failure
        assertEquals(FirebaseAuthRepository.Messages.DELETE_PARTIAL, failure.message)
        assertEquals(1, auth.deleteCount)
        assertNull(tokens.token)
        assertEquals(AuthState.Unauthenticated, repository.authState.value)
    }

    @Test
    fun `deleting while signed out does nothing`() = runTest {
        val repository = repository()

        val result = repository.deleteAccount()

        assertTrue(result is AuthResult.Failure)
        assertEquals(0, auth.deleteCount)
    }

    // ---------------------------------------------------------------- refresh

    @Test
    fun `a foreground refresh installs the current token again`() = runTest {
        auth.identity = googleIdentity()
        profiles.profiles[TestConfig.USER_A] = completedProfile()
        val repository = repository()
        repository.initialize()
        tokens.clear()

        repository.refreshAccount()

        assertEquals(TestConfig.TOKEN, tokens.token?.token)
        assertTrue(repository.authState.value is AuthState.Authenticated)
    }

    @Test
    fun `a refresh after the Firebase user vanished signs the app out`() = runTest {
        auth.identity = googleIdentity()
        profiles.profiles[TestConfig.USER_A] = completedProfile()
        val repository = repository()
        repository.initialize()
        auth.identity = null

        repository.refreshAccount()

        assertEquals(AuthState.Unauthenticated, repository.authState.value)
        assertNull(tokens.token)
    }
}
