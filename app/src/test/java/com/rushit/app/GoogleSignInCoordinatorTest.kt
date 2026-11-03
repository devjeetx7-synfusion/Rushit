package com.rushit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.rushit.app.data.auth.google.CredentialManagerGoogleSignIn
import com.rushit.app.data.auth.google.GoogleSignInClient
import com.rushit.app.data.auth.google.GoogleSignInCoordinator
import com.rushit.app.data.auth.google.GoogleSignInMessages
import com.rushit.app.data.auth.google.GoogleSignInOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoogleSignInCoordinatorTest {

    private val clientId = "1234567890-abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com"
    private val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()

    @Test
    fun `a valid Google ID token comes back as a success`() = runTest {
        val client = client { success("id-token-value") }
        assertEquals(GoogleSignInOutcome.Success("id-token-value"), client.requestGoogleIdToken())
    }

    @Test
    fun `the request targets the web client and asks for authorized accounts first`() = runTest {
        val requests = mutableListOf<GetCredentialRequest>()
        val client = client { request ->
            requests += request
            success("id-token-value")
        }

        client.requestGoogleIdToken()

        val option = requests.single().credentialOptions.filterIsInstance<GetGoogleIdOption>().single()
        assertEquals(clientId, option.serverClientId)
        assertTrue(option.filterByAuthorizedAccounts)
    }

    @Test
    fun `a blank web client id never opens a picker`() = runTest {
        var called = false
        val client = CredentialManagerGoogleSignIn({ activity }, " ") { _, _ ->
            called = true
            success("unused")
        }

        assertEquals(GoogleSignInOutcome.Unconfigured, client.requestGoogleIdToken())
        assertFalse(called)
    }

    @Test
    fun `no resumed host is a retryable failure`() = runTest {
        var called = false
        val client = CredentialManagerGoogleSignIn({ null }, clientId) { _, _ ->
            called = true
            success("unused")
        }

        val outcome = client.requestGoogleIdToken() as GoogleSignInOutcome.Failed
        assertEquals(GoogleSignInMessages.NO_ACTIVITY, outcome.message)
        assertFalse(called)
    }

    @Test
    fun `a paused host during credential selection remains valid`() = runTest {
        val pausedActivity = Robolectric.buildActivity(ComponentActivity::class.java).setup().pause().get()
        val client = CredentialManagerGoogleSignIn({ pausedActivity }, clientId) { _, _ ->
            success("paused-token")
        }

        val outcome = client.requestGoogleIdToken()
        assertEquals(GoogleSignInOutcome.Success("paused-token"), outcome)
    }

    @Test
    fun `a credential that arrives after host is destroyed is discarded`() = runTest {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val host = controller.get()
        val client = CredentialManagerGoogleSignIn({ host }, clientId) { _, _ ->
            controller.destroy()
            success("late-token")
        }

        val outcome = client.requestGoogleIdToken() as GoogleSignInOutcome.Failed
        assertEquals(GoogleSignInMessages.NO_ACTIVITY, outcome.message)
    }

    @Test
    fun `a non-Google credential type is rejected`() = runTest {
        val client = client {
            GetCredentialResponse(CustomCredential("some.other.credential", Bundle()))
        }

        val outcome = client.requestGoogleIdToken() as GoogleSignInOutcome.Failed
        assertEquals(GoogleSignInMessages.UNEXPECTED_CREDENTIAL, outcome.message)
    }

    @Test
    fun `dismissing the picker is a cancellation`() = runTest {
        val client = client { throw GetCredentialCancellationException() }
        assertEquals(GoogleSignInOutcome.Cancelled, client.requestGoogleIdToken())
    }

    @Test
    fun `a second concurrent tap is refused`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var nativeCalls = 0
        val coordinator = GoogleSignInCoordinator(object : GoogleSignInClient {
            override suspend fun requestGoogleIdToken(): GoogleSignInOutcome {
                nativeCalls++
                gate.await()
                return GoogleSignInOutcome.Success("token")
            }
        })

        val first = async { coordinator.requestGoogleIdToken() }
        runCurrent()
        val second = coordinator.requestGoogleIdToken()

        assertEquals(
            GoogleSignInOutcome.Failed(GoogleSignInMessages.ALREADY_SIGNING_IN),
            second,
        )

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(GoogleSignInOutcome.Success("token"), first.await())
        assertEquals(1, nativeCalls)
    }

    private fun client(
        respond: suspend (GetCredentialRequest) -> GetCredentialResponse,
    ): CredentialManagerGoogleSignIn =
        CredentialManagerGoogleSignIn({ activity }, clientId) { _, request -> respond(request) }

    private fun success(token: String) = GetCredentialResponse(
        CustomCredential(
            type = GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
            data = Bundle().apply {
                putString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID", "google-subject")
                putString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN", token)
            },
        ),
    )
}
