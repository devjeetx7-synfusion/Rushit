package com.rushit.app

import com.rushit.app.data.auth.AuthFailureKind
import com.rushit.app.data.auth.AuthRepository
import com.rushit.app.data.auth.AuthResult
import com.rushit.app.data.auth.AuthState
import com.rushit.app.data.auth.DeleteAccountOutcome
import com.rushit.app.data.auth.RecoveryAction
import com.rushit.app.data.auth.SessionRestoreResult
import com.rushit.app.data.auth.SupabaseAuthRepository
import com.rushit.app.data.auth.UsernameAvailability
import com.rushit.app.data.model.Profile
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Account creation, returning-user login, session restoration, onboarding, sign-out and deletion.
 *
 * The gateway is faked, so these run on the JVM with no device, no network and no Supabase project.
 */
class SupabaseAuthRepositoryTest {

    private val userId = TestConfig.USER_A

    private fun repository(gateway: FakeAuthGateway) = SupabaseAuthRepository(gateway)

    // -----------------------------------------------------------------------------------------
    // New account creation
    // -----------------------------------------------------------------------------------------

    @Test
    fun `a first-time Google user gets one profile, default settings and onboarding`() = runTest {
        val gateway = FakeAuthGateway().apply { session = googleSession(userId) }
        val repo = repository(gateway)

        repo.initialize()
        assertEquals(AuthState.Unauthenticated, repo.authState.value)

        val result = repo.signInWithGoogle("id-token", "raw-nonce")

        assertEquals(AuthResult.Success, result)
        assertEquals(1, gateway.signIns)
        assertEquals(listOf(userId), gateway.provisionedUsers)
        assertEquals(setOf(userId), gateway.settings)

        val state = repo.authState.value as AuthState.ProfileIncomplete
        assertEquals(userId, state.draft.id)
        // Google's data arrives as a suggestion, pre-filled for the person to confirm.
        assertEquals("Google Name", state.draft.initialFullName)
        assertEquals("https://lh3.googleusercontent.com/a/photo", state.draft.initialAvatarUrl)
        assertFalse(gateway.profiles.getValue(userId).onboardingCompleted)
    }

    @Test
    fun `profile provisioning cannot create a duplicate under concurrent sign-ins`() = runTest {
        val gateway = FakeAuthGateway().apply { session = googleSession(userId) }
        val repo = repository(gateway)
        repo.initialize()

        val first = async { repo.signInWithGoogle("id-token", "nonce") }
        val second = async { repo.signInWithGoogle("id-token", "nonce") }

        assertEquals(AuthResult.Success, first.await())
        assertEquals(AuthResult.Success, second.await())
        assertEquals(1, gateway.profiles.size)
        assertEquals(2, gateway.signIns)
        // Both attempts recorded activity, but only one row exists.
        assertEquals(1, gateway.profiles.values.count { it.id == userId })
    }

    @Test
    fun `activity logging failing never blocks a sign-in`() = runTest {
        val gateway = FakeAuthGateway().apply {
            session = googleSession(userId)
            activityError = IllegalStateException("activity table unavailable")
        }
        val repo = repository(gateway)
        repo.initialize()

        assertEquals(AuthResult.Success, repo.signInWithGoogle("id-token", "nonce"))
        assertTrue(repo.authState.value is AuthState.ProfileIncomplete)
        assertTrue(gateway.activityLog.isEmpty())
    }

    @Test
    fun `activity log metadata carries no tokens or personal data`() = runTest {
        val gateway = FakeAuthGateway().apply { session = googleSession(userId) }
        val repo = repository(gateway)
        repo.initialize()
        repo.signInWithGoogle("secret-id-token", "secret-nonce")

        val record = gateway.activityLog.single()
        assertEquals("sign_in", record.eventType)
        assertEquals(mapOf("provider" to "google"), record.metadata)
        record.metadata.values.forEach { value ->
            assertFalse(value.contains("secret"))
        }
    }

    // -----------------------------------------------------------------------------------------
    // Returning users
    // -----------------------------------------------------------------------------------------

    @Test
    fun `a returning user with a completed profile is restored straight to authenticated`() = runTest {
        val gateway = FakeAuthGateway().apply {
            session = googleSession(userId)
            profiles[userId] = completedProfile(userId)
            restoreResult = SessionRestoreResult.Restored(googleSession(userId))
        }
        val repo = repository(gateway)

        repo.initialize()

        val state = repo.authState.value as AuthState.Authenticated
        assertEquals(userId, state.account.id)
        assertEquals("shopper@example.com", state.account.email)
        assertEquals("Custom Name", state.account.displayName)
        assertEquals("shopper", state.account.username)
        assertEquals(1_700_000_000L, state.account.memberSinceEpochSeconds)
        // No sign-in happened and nothing was provisioned: this was a restored session.
        assertEquals(0, gateway.signIns)
        assertTrue(gateway.provisionedUsers.isEmpty())
    }

    @Test
    fun `signing in again never overwrites a customized name or avatar`() = runTest {
        val gateway = FakeAuthGateway().apply {
            session = googleSession(userId)
            profiles[userId] = completedProfile(
                id = userId,
                fullName = "Name I Chose",
                username = "myhandle",
                avatarUrl = "https://example.test/my-avatar.png",
            )
        }
        val repo = repository(gateway)
        repo.initialize()

        repo.signInWithGoogle("id-token", "nonce")

        val state = repo.authState.value as AuthState.Authenticated
        assertEquals("Name I Chose", state.account.profile.fullName)
        assertEquals("myhandle", state.account.profile.username)
        assertEquals("https://example.test/my-avatar.png", state.account.profile.avatarUrl)
        // The row already existed, so the provider's copy was never written.
        assertTrue(gateway.provisionedUsers.isEmpty())
        assertEquals("Name I Chose", gateway.profiles.getValue(userId).fullName)
    }

    @Test
    fun `an unfinished onboarding resumes with saved values winning over Google's`() = runTest {
        val gateway = FakeAuthGateway().apply {
            session = googleSession(userId)
            profiles[userId] = Profile(
                id = userId,
                fullName = "Half Typed Name",
                username = null,
                avatarUrl = null,
                onboardingCompleted = false,
            )
        }
        val repo = repository(gateway)
        repo.initialize()

        repo.signInWithGoogle("id-token", "nonce")

        val draft = (repo.authState.value as AuthState.ProfileIncomplete).draft
        assertEquals("Half Typed Name", draft.initialFullName)
        assertEquals("https://lh3.googleusercontent.com/a/photo", draft.initialAvatarUrl)
        assertTrue(draft.hasSavedValues)
    }

    @Test
    fun `a returning user whose profile row is missing is provisioned instead of failing`() = runTest {
        val gateway = FakeAuthGateway().apply {
            session = googleSession(userId)
            restoreResult = SessionRestoreResult.Restored(googleSession(userId))
        }
        val repo = repository(gateway)

        repo.initialize()

        val draft = (repo.authState.value as AuthState.ProfileIncomplete).draft
        assertEquals(userId, draft.id)
        assertEquals(listOf(userId), gateway.provisionedUsers)
        assertEquals(setOf(userId), gateway.settings)
    }

    // -----------------------------------------------------------------------------------------
    // Session restoration and its failure modes
    // -----------------------------------------------------------------------------------------

    @Test
    fun `an offline start-up keeps the stored session and offers a retry`() = runTest {
        val session = googleSession(userId)
        val gateway = FakeAuthGateway().apply {
            restoreResult = SessionRestoreResult.Unreachable(sessionStillStored = true)
        }
        val repo = repository(gateway)

        repo.initialize()

        val state = repo.authState.value as AuthState.RecoverableError
        assertEquals(AuthFailureKind.NETWORK, state.kind)
        assertEquals(RecoveryAction.RETRY_SESSION, state.action)
        assertNotNull(state.message)

        // Connectivity returns: the same stored session is restored without a new sign-in.
        gateway.restoreResult = SessionRestoreResult.Restored(session)
        gateway.profiles[userId] = completedProfile(userId)
        repo.retrySessionRestore()

        assertTrue(repo.authState.value is AuthState.Authenticated)
        assertEquals(0, gateway.signIns)
    }

    @Test
    fun `an offline start-up with no stored session is simply unauthenticated`() = runTest {
        val gateway = FakeAuthGateway().apply {
            restoreResult = SessionRestoreResult.Unreachable(sessionStillStored = false)
        }
        val repo = repository(gateway)

        repo.initialize()

        assertEquals(AuthState.Unauthenticated, repo.authState.value)
    }

    @Test
    fun `a rejected refresh token returns to login with an explanation`() = runTest {
        val gateway = FakeAuthGateway().apply { restoreResult = SessionRestoreResult.Rejected }
        val repo = repository(gateway)

        repo.initialize()

        val state = repo.authState.value as AuthState.RecoverableError
        assertEquals(AuthFailureKind.SESSION_EXPIRED, state.kind)
        assertEquals(RecoveryAction.SIGN_IN_AGAIN, state.action)
        assertEquals(SupabaseAuthRepository.Messages.SESSION_EXPIRED, state.message)
        assertNull(gateway.session)
    }

    @Test
    fun `a background refresh never signs out a user on a transient failure`() = runTest {
        val gateway = FakeAuthGateway().apply {
            session = googleSession(userId)
            profiles[userId] = completedProfile(userId)
            restoreResult = SessionRestoreResult.Restored(googleSession(userId))
        }
        val repo = repository(gateway)
        repo.initialize()
        assertTrue(repo.authState.value is AuthState.Authenticated)

        gateway.restoreResult = SessionRestoreResult.Unreachable(sessionStillStored = true)
        val result = repo.refreshAccount()

        assertTrue(result is AuthResult.Failure)
        // Still signed in: losing connectivity must not throw somebody out of the shop.
        assertTrue(repo.authState.value is AuthState.Authenticated)
    }

    @Test
    fun `a profile read failure at start-up is recoverable`() = runTest {
        val gateway = FakeAuthGateway().apply {
            restoreResult = SessionRestoreResult.Restored(googleSession(userId))
            fetchProfileError = IOException("offline")
        }
        val repo = repository(gateway)

        repo.initialize()

        val state = repo.authState.value as AuthState.RecoverableError
        assertEquals(AuthFailureKind.NETWORK, state.kind)
        assertEquals(RecoveryAction.RETRY_SESSION, state.action)
    }

    @Test
    fun `an unconfigured build never contacts Supabase`() = runTest {
        val gateway = FakeAuthGateway().apply {
            isConfigured = false
            session = googleSession(userId)
        }
        val repo = repository(gateway)

        repo.initialize()
        val state = repo.authState.value as AuthState.RecoverableError
        assertEquals(AuthFailureKind.UNCONFIGURED, state.kind)
        assertEquals(RecoveryAction.DISMISS, state.action)

        val result = repo.signInWithGoogle("id-token", "nonce") as AuthResult.Failure
        assertEquals(AuthFailureKind.UNCONFIGURED, result.kind)
        assertEquals(0, gateway.signIns)
    }

    @Test
    fun `a blank id token is rejected without a request`() = runTest {
        val gateway = FakeAuthGateway().apply { session = googleSession(userId) }
        val repo = repository(gateway)
        repo.initialize()

        val result = repo.signInWithGoogle("  ", null) as AuthResult.Failure

        assertEquals(AuthFailureKind.INVALID_TOKEN, result.kind)
        assertEquals(0, gateway.signIns)
    }

    // -----------------------------------------------------------------------------------------
    // Onboarding and profile updates
    // -----------------------------------------------------------------------------------------

    @Test
    fun `completing onboarding persists the profile and authenticates`() = runTest {
        val gateway = FakeAuthGateway().apply { session = googleSession(userId) }
        val repo = repository(gateway)
        repo.initialize()
        repo.signInWithGoogle("id-token", "nonce")

        val result = repo.saveProfile("Rush It Shopper", "  RushIt.Shopper  ", null)

        assertEquals(AuthResult.Success, result)
        val saved = gateway.profiles.getValue(userId)
        assertEquals("Rush It Shopper", saved.fullName)
        // Normalized to match the case-insensitive unique index.
        assertEquals("rushit.shopper", saved.username)
        assertTrue(saved.onboardingCompleted)
        val state = repo.authState.value as AuthState.Authenticated
        assertEquals("Rush It Shopper", state.account.displayName)
        assertTrue(gateway.activityLog.any { it.eventType == "profile_completed" })
    }

    @Test
    fun `a blank username is stored as null, not as an empty string`() = runTest {
        val gateway = FakeAuthGateway().apply { session = googleSession(userId) }
        val repo = repository(gateway)
        repo.initialize()
        repo.signInWithGoogle("id-token", "nonce")

        repo.saveProfile("Rush It Shopper", "   ", null)

        assertNull(gateway.profiles.getValue(userId).username)
    }

    @Test
    fun `an invalid display name is rejected before any write`() = runTest {
        val gateway = FakeAuthGateway().apply { session = googleSession(userId) }
        val repo = repository(gateway)
        repo.initialize()
        repo.signInWithGoogle("id-token", "nonce")

        val result = repo.saveProfile("   ", "rushit", null) as AuthResult.Failure

        assertTrue(result.message.contains("name"))
        assertTrue(gateway.savedProfiles.isEmpty())
        assertTrue(repo.authState.value is AuthState.ProfileIncomplete)
    }

    @Test
    fun `a taken username is reported and onboarding is not left`() = runTest {
        val gateway = FakeAuthGateway().apply {
            session = googleSession(userId)
            takenUsernames += "taken"
        }
        val repo = repository(gateway)
        repo.initialize()
        repo.signInWithGoogle("id-token", "nonce")

        val result = repo.saveProfile("Rush It", "taken", null) as AuthResult.Failure

        assertEquals(AuthFailureKind.CONFLICT, result.kind)
        assertEquals(SupabaseAuthRepository.Messages.USERNAME_TAKEN, result.message)
        assertTrue(repo.authState.value is AuthState.ProfileIncomplete)
        assertTrue(gateway.savedProfiles.isEmpty())
    }

    @Test
    fun `a concurrent username conflict from the database is surfaced as a conflict`() = runTest {
        val exceptions = RestExceptionFactory()
        val gateway = FakeAuthGateway().apply {
            session = googleSession(userId)
            saveError = exceptions.duplicateKey()
        }
        val repo = repository(gateway)
        repo.initialize()
        repo.signInWithGoogle("id-token", "nonce")

        val result = repo.saveProfile("Rush It", "rushit", null) as AuthResult.Failure

        assertEquals(AuthFailureKind.CONFLICT, result.kind)
        assertFalse(result.message.contains(RestExceptionFactory.SECRET_ANON_KEY))
        assertTrue(repo.authState.value is AuthState.ProfileIncomplete)
        exceptions.close()
    }

    @Test
    fun `a row level security rejection is reported without leaking internals`() = runTest {
        val exceptions = RestExceptionFactory()
        val gateway = FakeAuthGateway().apply {
            session = googleSession(userId)
            saveError = exceptions.rowLevelSecurity()
        }
        val repo = repository(gateway)
        repo.initialize()
        repo.signInWithGoogle("id-token", "nonce")

        val result = repo.saveProfile("Rush It", "rushit", null) as AuthResult.Failure

        assertEquals(AuthFailureKind.POLICY_DENIED, result.kind)
        assertFalse(result.message.contains(RestExceptionFactory.SECRET_ANON_KEY))
        assertFalse(result.message.contains(RestExceptionFactory.SECRET_JWT))
        exceptions.close()
    }

    @Test
    fun `saving a profile while signed out asks for a new sign-in`() = runTest {
        val gateway = FakeAuthGateway()
        val repo = repository(gateway)
        repo.initialize()

        val result = repo.saveProfile("Rush It", "rushit", null) as AuthResult.Failure

        assertEquals(AuthFailureKind.SESSION_EXPIRED, result.kind)
        assertTrue(gateway.savedProfiles.isEmpty())
    }

    @Test
    fun `an unavailable username check does not block saving`() = runTest {
        val gateway = FakeAuthGateway().apply { session = googleSession(userId) }
        val repo = repository(gateway)
        repo.initialize()
        repo.signInWithGoogle("id-token", "nonce")

        // The availability RPC is missing; the write must still be attempted.
        gateway.usernameCheckError = IOException("rpc username_available not found")
        val availability = repo.checkUsername("rushit")
        assertEquals(UsernameAvailability.Unknown, availability)

        assertEquals(AuthResult.Success, repo.saveProfile("Rush It", "rushit", null))
        assertEquals("rushit", gateway.profiles.getValue(userId).username)
    }

    @Test
    fun `an existing avatar is kept when the form carries none forward`() = runTest {
        val gateway = FakeAuthGateway().apply {
            session = googleSession(userId)
            profiles[userId] = completedProfile(userId, avatarUrl = "https://example.test/mine.png")
        }
        val repo = repository(gateway)
        repo.initialize()

        repo.saveProfile("New Name", "shopper", null)

        assertEquals("https://example.test/mine.png", gateway.profiles.getValue(userId).avatarUrl)
    }

    // -----------------------------------------------------------------------------------------
    // Sign-out
    // -----------------------------------------------------------------------------------------

    @Test
    fun `signing out clears the session and returns to the login state`() = runTest {
        val gateway = FakeAuthGateway().apply {
            session = googleSession(userId)
            profiles[userId] = completedProfile(userId)
            restoreResult = SessionRestoreResult.Restored(googleSession(userId))
        }
        val repo = repository(gateway)
        repo.initialize()
        assertTrue(repo.authState.value is AuthState.Authenticated)

        assertEquals(AuthResult.Success, repo.signOut())

        assertEquals(AuthState.Unauthenticated, repo.authState.value)
        assertNull(gateway.session)
        assertEquals(1, gateway.signOutRequests)
        // The profile row belongs to the account, not the device: signing out must not delete it.
        assertTrue(gateway.profiles.containsKey(userId))
    }

    @Test
    fun `a failed remote sign-out still clears local state and is reported`() = runTest {
        val gateway = FakeAuthGateway().apply {
            session = googleSession(userId)
            profiles[userId] = completedProfile(userId)
            restoreResult = SessionRestoreResult.Restored(googleSession(userId))
            signOutRevokesRemotely = false
        }
        val repo = repository(gateway)
        repo.initialize()

        val result = repo.signOut() as AuthResult.Failure

        assertEquals(AuthFailureKind.NETWORK, result.kind)
        assertEquals(SupabaseAuthRepository.Messages.SIGN_OUT_PARTIAL, result.message)
        assertEquals(AuthState.Unauthenticated, repo.authState.value)
        assertNull(gateway.session)
    }

    // -----------------------------------------------------------------------------------------
    // Account deletion
    // -----------------------------------------------------------------------------------------

    @Test
    fun `deleting an account signs the user out and removes their data`() = runTest {
        val gateway = FakeAuthGateway().apply {
            session = googleSession(userId)
            profiles[userId] = completedProfile(userId)
            restoreResult = SessionRestoreResult.Restored(googleSession(userId))
            deleteOutcome = DeleteAccountOutcome.Deleted
        }
        val repo = repository(gateway)
        repo.initialize()

        assertEquals(AuthResult.Success, repo.deleteAccount())

        assertEquals(1, gateway.deleteRequests)
        assertEquals(AuthState.Unauthenticated, repo.authState.value)
        assertNull(gateway.session)
        assertFalse(gateway.profiles.containsKey(userId))
        assertFalse(gateway.settings.contains(userId))
    }

    @Test
    fun `deletion is refused when no trusted backend is deployed`() = runTest {
        val gateway = FakeAuthGateway().apply {
            session = googleSession(userId)
            profiles[userId] = completedProfile(userId)
            restoreResult = SessionRestoreResult.Restored(googleSession(userId))
            deleteOutcome = DeleteAccountOutcome.NotDeployed
        }
        val repo = repository(gateway)
        repo.initialize()

        val result = repo.deleteAccount() as AuthResult.Failure

        assertEquals(AuthFailureKind.UNAVAILABLE, result.kind)
        assertEquals(SupabaseAuthRepository.Messages.DELETE_NOT_DEPLOYED, result.message)
        // The account must still be intact and signed in.
        assertTrue(repo.authState.value is AuthState.Authenticated)
        assertTrue(gateway.profiles.containsKey(userId))
        assertNotNull(gateway.session)
    }

    @Test
    fun `deletion requires a signed-in user`() = runTest {
        val gateway = FakeAuthGateway().apply { deleteOutcome = DeleteAccountOutcome.Deleted }
        val repo = repository(gateway)
        repo.initialize()

        val result = repo.deleteAccount() as AuthResult.Failure

        assertEquals(AuthFailureKind.SESSION_EXPIRED, result.kind)
        assertEquals(0, gateway.deleteRequests)
    }

    @Test
    fun `a server-side authorization failure during deletion is reported, not swallowed`() = runTest {
        val exceptions = RestExceptionFactory()
        val gateway = FakeAuthGateway().apply {
            session = googleSession(userId)
            profiles[userId] = completedProfile(userId)
            restoreResult = SessionRestoreResult.Restored(googleSession(userId))
            // Simulates the delete-account Edge Function rejecting the caller's JWT.
            deleteError = exceptions.unauthorized()
        }
        val repo = repository(gateway)
        repo.initialize()

        val result = repo.deleteAccount() as AuthResult.Failure

        assertEquals(AuthFailureKind.SESSION_EXPIRED, result.kind)
        assertFalse(result.message.contains(RestExceptionFactory.SECRET_ANON_KEY))
        // The account is untouched: a failed deletion must not sign anybody out.
        assertTrue(repo.authState.value is AuthState.Authenticated)
        assertTrue(gateway.profiles.containsKey(userId))
        exceptions.close()
    }

    // -----------------------------------------------------------------------------------------
    // Interface shape
    // -----------------------------------------------------------------------------------------

    @Test
    fun `account ownership is the supabase uuid, never the email`() = runTest {
        val gateway = FakeAuthGateway().apply {
            session = googleSession(userId, email = "shopper@example.com")
            profiles[userId] = completedProfile(userId)
            restoreResult = SessionRestoreResult.Restored(googleSession(userId, email = "shopper@example.com"))
        }
        val repo: AuthRepository = repository(gateway)
        repo.initialize()

        val account = (repo.authState.value as AuthState.Authenticated).account

        assertEquals(userId, account.id)
        assertEquals(userId, gateway.profiles.keys.single())
        // Email is display data only.
        assertEquals("shopper@example.com", account.email)
        assertFalse(account.id.contains("@"))
    }
}
