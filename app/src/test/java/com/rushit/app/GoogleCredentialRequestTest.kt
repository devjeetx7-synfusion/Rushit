package com.rushit.app

import android.os.Bundle
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.rushit.app.data.auth.google.GoogleCredentialRequest
import com.rushit.app.data.auth.google.GoogleIdRequestSpec
import com.rushit.app.data.auth.google.GoogleSignInMessages
import com.rushit.app.data.auth.google.GoogleSignInOutcome
import com.rushit.app.data.auth.google.googleCredentialRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * The two-stage Credential Manager request.
 *
 * The behaviour under test is subtle and easy to regress: `NoCredentialException` from the
 * authorized-accounts-only stage means "retry without the filter", NOT "this device has no Google
 * account", and no failure may ever be turned into a success.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoogleCredentialRequestTest {
    private val clientId = "1234567890-abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com"

    @Test
    fun `the request uses the web client with authorized accounts first`() {
        val restricted = googleIdOption(GoogleIdRequestSpec(clientId, authorizedAccountsOnly = true))
        assertEquals(clientId, restricted.serverClientId)
        assertTrue(restricted.filterByAuthorizedAccounts)
        assertTrue(restricted.autoSelectEnabled)

        val unrestricted = googleIdOption(GoogleIdRequestSpec(clientId, authorizedAccountsOnly = false))
        assertFalse(unrestricted.filterByAuthorizedAccounts)
        assertFalse(unrestricted.autoSelectEnabled)

        assertEquals(1, googleCredentialRequest(GoogleIdRequestSpec(clientId, true)).credentialOptions.size)
    }

    @Test
    fun `authorized account unavailable retries unrestricted and succeeds`() = runTest {
        val seen = mutableListOf<GoogleIdRequestSpec>()
        val request = GoogleCredentialRequest(clientId) { spec ->
            seen += spec
            if (spec.authorizedAccountsOnly) throw NoCredentialException("Not authorized yet")
            success("second-stage-token")
        }

        val outcome = request.execute()

        assertEquals(listOf(true, false), seen.map { it.authorizedAccountsOnly })
        assertEquals(listOf(clientId, clientId), seen.map { it.webClientId })
        assertEquals("second-stage-token", (outcome as GoogleSignInOutcome.Success).idToken)
    }

    @Test
    fun `an authorized account succeeds without opening the unrestricted picker`() = runTest {
        var calls = 0
        val request = GoogleCredentialRequest(clientId) { spec ->
            calls++
            assertTrue(spec.authorizedAccountsOnly)
            success("authorized-token")
        }

        assertEquals("authorized-token", (request.execute() as GoogleSignInOutcome.Success).idToken)
        assertEquals(1, calls)
    }

    @Test
    fun `both stages without credentials never claim there is no device account`() = runTest {
        val seen = mutableListOf<Boolean>()
        val request = GoogleCredentialRequest(clientId) { spec ->
            seen += spec.authorizedAccountsOnly
            throw NoCredentialException("No matching credential")
        }

        assertEquals(GoogleSignInOutcome.NoCredentialAvailable, request.execute())
        assertEquals(listOf(true, false), seen)
    }

    @Test
    fun `cancelling either picker stops without a retry`() = runTest {
        for (cancelAuthorized in listOf(true, false)) {
            var calls = 0
            val request = GoogleCredentialRequest(clientId) { spec ->
                calls++
                if (spec.authorizedAccountsOnly == cancelAuthorized) {
                    throw GetCredentialCancellationException("User dismissed picker")
                }
                throw NoCredentialException("No credential")
            }

            assertEquals(GoogleSignInOutcome.Cancelled, request.execute())
            assertEquals(if (cancelAuthorized) 1 else 2, calls)
        }
    }

    @Test
    fun `an unavailable provider is reported as unsupported, not as a user error`() = runTest {
        for (error in listOf(
            GetCredentialUnsupportedException("Unsupported"),
            GetCredentialProviderConfigurationException("Missing provider"),
        )) {
            val request = GoogleCredentialRequest(clientId) { throw error }

            assertEquals(GoogleSignInOutcome.Unsupported, request.execute())
        }
    }

    @Test
    fun `a network failure is reported once and not retried`() = runTest {
        var calls = 0
        val request = GoogleCredentialRequest(clientId) {
            calls++
            throw IOException("Offline")
        }

        val outcome = request.execute() as GoogleSignInOutcome.Failed

        assertEquals(GoogleSignInMessages.OFFLINE, outcome.message)
        assertTrue(outcome.offline)
        assertEquals(1, calls)
    }

    @Test
    fun `a missing web client is reported before any request is built`() = runTest {
        val request = GoogleCredentialRequest(" ") { error("Must not request credentials") }

        assertEquals(GoogleSignInOutcome.Unconfigured, request.execute())
    }

    @Test
    fun `a blank ID token is a failure, never an empty success`() = runTest {
        val request = GoogleCredentialRequest(clientId) { success("") }

        val outcome = request.execute() as GoogleSignInOutcome.Failed
        assertEquals(GoogleSignInMessages.MISSING_TOKEN, outcome.message)
    }

    @Test
    fun `a whitespace-only ID token is also treated as a missing token failure`() = runTest {
        val request = GoogleCredentialRequest(clientId) { success("   ") }

        val outcome = request.execute() as GoogleSignInOutcome.Failed
        assertEquals(GoogleSignInMessages.MISSING_TOKEN, outcome.message)
    }

    @Test
    fun `a credential that is not a Google ID token is rejected`() = runTest {
        val request = GoogleCredentialRequest(clientId) {
            GetCredentialResponse(CustomCredential("password", Bundle()))
        }

        val outcome = request.execute() as GoogleSignInOutcome.Failed
        assertEquals(GoogleSignInMessages.UNEXPECTED_CREDENTIAL, outcome.message)
    }

    private fun success(token: String): GetCredentialResponse = GetCredentialResponse(
        CustomCredential(
            type = GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
            data = Bundle().apply {
                putString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID", "google-subject")
                putString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN", token)
            },
        ),
    )

    private fun googleIdOption(spec: GoogleIdRequestSpec): GetGoogleIdOption =
        googleCredentialRequest(spec).credentialOptions.filterIsInstance<GetGoogleIdOption>().single()
}
