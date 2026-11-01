package com.rushit.app

import android.os.Bundle
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
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
import com.rushit.app.data.auth.google.SignInNonce
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoogleCredentialRequestTest {
    private val clientId = "1234567890-abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com"

    @Test
    fun `request uses the web client and hashed nonce with authorized accounts first`() {
        val spec = GoogleIdRequestSpec(clientId, "hashed-nonce", authorizedAccountsOnly = true)
        val option = googleIdOption(spec)
        assertEquals(clientId, option.serverClientId)
        assertEquals("hashed-nonce", option.nonce)
        assertTrue(option.filterByAuthorizedAccounts)
        assertTrue(option.autoSelectEnabled)

        val unrestricted = googleIdOption(spec.copy(authorizedAccountsOnly = false))
        assertFalse(unrestricted.filterByAuthorizedAccounts)
        assertFalse(unrestricted.autoSelectEnabled)
        assertEquals(1, googleCredentialRequest(spec).credentialOptions.size)
    }

    @Test
    fun `authorized account unavailable retries unrestricted with the same nonce and succeeds`() = runTest {
        val seen = mutableListOf<GoogleIdRequestSpec>()
        var nonceCount = 0
        val request = GoogleCredentialRequest(clientId, { nonceCount++; "raw-$nonceCount" }) { spec ->
            seen += spec
            if (spec.authorizedAccountsOnly) throw NoCredentialException("Not authorized yet")
            success("second-stage-token")
        }

        val outcome = request.execute()

        assertEquals(listOf(true, false), seen.map { it.authorizedAccountsOnly })
        assertEquals(listOf(SignInNonce.hash("raw-1"), SignInNonce.hash("raw-1")), seen.map { it.hashedNonce })
        assertEquals(1, nonceCount)
        assertEquals("second-stage-token", (outcome as GoogleSignInOutcome.Success).idToken)
        assertEquals("raw-1", outcome.rawNonce)
    }

    @Test
    fun `authorized account succeeds without opening unrestricted picker`() = runTest {
        var calls = 0
        val request = GoogleCredentialRequest(clientId, { "nonce" }) { spec ->
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
        val request = GoogleCredentialRequest(clientId, { "nonce" }) { spec ->
            seen += spec.authorizedAccountsOnly
            throw NoCredentialException("No matching credential")
        }
        assertEquals(GoogleSignInOutcome.NoCredentialAvailable, request.execute())
        assertEquals(listOf(true, false), seen)
    }

    @Test
    fun `cancelling either picker stops without retry or browser fallback`() = runTest {
        for (cancelAuthorized in listOf(true, false)) {
            var calls = 0
            val request = GoogleCredentialRequest(clientId, { "nonce" }) { spec ->
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
    fun `Play Services unavailable is an internal fallback signal not an account error`() = runTest {
        for (error in listOf(
            GetCredentialUnsupportedException("Unsupported"),
            GetCredentialProviderConfigurationException("Missing provider"),
        )) {
            val request = GoogleCredentialRequest(clientId, { "nonce" }) { throw error }
            assertEquals(GoogleSignInOutcome.Unsupported, request.execute())
        }
    }

    @Test
    fun `network failure is not retried as no credential`() = runTest {
        var calls = 0
        val request = GoogleCredentialRequest(clientId, { "nonce" }) {
            calls++
            throw IOException("Offline")
        }
        val outcome = request.execute() as GoogleSignInOutcome.Failed
        assertEquals(GoogleSignInMessages.OFFLINE, outcome.message)
        assertTrue(outcome.offline)
        assertEquals(1, calls)
    }

    @Test
    fun `missing web client is reported before creating a request`() = runTest {
        val request = GoogleCredentialRequest(" ", { error("Must not generate nonce") }) {
            error("Must not request credentials")
        }
        assertEquals(GoogleSignInOutcome.Unconfigured, request.execute())
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
