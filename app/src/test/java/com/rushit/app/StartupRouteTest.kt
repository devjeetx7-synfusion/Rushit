package com.rushit.app

import com.rushit.app.data.auth.AuthFailureKind
import com.rushit.app.data.auth.AuthState
import com.rushit.app.data.auth.RecoveryAction
import com.rushit.app.ui.screens.splash.startupRouteFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Start-up routing.
 *
 * The requirement being protected here is that a person with a valid stored session never sees the
 * login screen flash while it is being restored, and that no state is left unrouted.
 */
class StartupRouteTest {

    @Test
    fun `transient states keep the splash on screen`() {
        assertNull(startupRouteFor(AuthState.Loading))
        assertNull(startupRouteFor(AuthState.Authenticating))
    }

    @Test
    fun `a completed account lands on home`() {
        val account = com.rushit.app.data.auth.AuthAccount(
            id = TestConfig.USER_A,
            email = "shopper@example.com",
            provider = com.rushit.app.data.auth.AuthProviderId.GOOGLE,
            memberSinceEpochSeconds = 1_700_000_000L,
            profile = completedProfile(TestConfig.USER_A),
        )

        assertEquals("home", startupRouteFor(AuthState.Authenticated(account)))
    }

    @Test
    fun `an incomplete profile lands on onboarding, never on home`() {
        val draft = com.rushit.app.data.auth.ProfileOnboarding.draftFor(googleSession(TestConfig.USER_A), null)

        assertEquals("profile-setup", startupRouteFor(AuthState.ProfileIncomplete(draft)))
    }

    @Test
    fun `no session lands on login`() {
        assertEquals("login", startupRouteFor(AuthState.Unauthenticated))
    }

    @Test
    fun `a recoverable error lands on login where the message and retry live`() {
        val state = AuthState.RecoverableError(
            message = "RushIt could not reach its servers.",
            kind = AuthFailureKind.NETWORK,
            action = RecoveryAction.RETRY_SESSION,
        )

        assertEquals("login", startupRouteFor(state))
    }

    @Test
    fun `every authentication state has a routing decision`() {
        val draft = com.rushit.app.data.auth.ProfileOnboarding.draftFor(googleSession(TestConfig.USER_A), null)
        val account = com.rushit.app.data.auth.AuthAccount(
            id = TestConfig.USER_A,
            email = null,
            provider = com.rushit.app.data.auth.AuthProviderId.GOOGLE,
            memberSinceEpochSeconds = null,
            profile = completedProfile(TestConfig.USER_A),
        )
        val states = listOf(
            AuthState.Loading,
            AuthState.Unauthenticated,
            AuthState.Authenticating,
            AuthState.Authenticated(account),
            AuthState.ProfileIncomplete(draft),
            AuthState.RecoverableError("message", AuthFailureKind.UNKNOWN, RecoveryAction.DISMISS),
        )

        // Exhaustive by construction: if a new state is added, startupRouteFor stops compiling until
        // it is handled, and this list reminds the author to add a case here too.
        assertEquals(6, states.size)
        assertEquals(
            listOf(null, "login", null, "home", "profile-setup", "login"),
            states.map { startupRouteFor(it) },
        )
    }
}
