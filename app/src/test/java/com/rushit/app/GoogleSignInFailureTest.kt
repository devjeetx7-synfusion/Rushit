package com.rushit.app

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import com.rushit.app.data.auth.google.GoogleSignInMessages
import com.rushit.app.data.auth.google.GoogleSignInOutcome
import com.rushit.app.data.auth.google.classifyGoogleSignInFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

class GoogleSignInFailureTest {

    @Test
    fun `dismissing the account picker is a cancellation, not a failure`() {
        val outcome = classifyGoogleSignInFailure(GetCredentialCancellationException())
        assertEquals(GoogleSignInOutcome.Cancelled, outcome)
    }

    @Test
    fun `cancellation with an internal message stays a cancellation`() {
        val outcome = classifyGoogleSignInFailure(
            GetCredentialCancellationException("User canceled the operation"),
        )
        assertEquals(GoogleSignInOutcome.Cancelled, outcome)
    }

    @Test
    fun `no credential returns NoCredentialAvailable`() {
        assertEquals(
            GoogleSignInOutcome.NoCredentialAvailable,
            classifyGoogleSignInFailure(NoCredentialException()),
        )
    }

    @Test
    fun `unsupported and misconfigured Play Services return Unsupported`() {
        assertEquals(GoogleSignInOutcome.Unsupported, classifyGoogleSignInFailure(GetCredentialUnsupportedException()))
        assertEquals(GoogleSignInOutcome.Unsupported, classifyGoogleSignInFailure(GetCredentialProviderConfigurationException()))
    }

    @Test
    fun `an unknown credential failure gets a retryable message`() {
        val outcome = classifyGoogleSignInFailure(GetCredentialUnknownException()) as GoogleSignInOutcome.Failed
        assertEquals(GoogleSignInMessages.UNKNOWN, outcome.message)
        assertFalse(outcome.offline)
    }

    @Test
    fun `connectivity problems are reported as offline`() {
        val io = classifyGoogleSignInFailure(IOException("socket closed")) as GoogleSignInOutcome.Failed
        assertEquals(GoogleSignInMessages.OFFLINE, io.message)
        assertTrue(io.offline)

        val dns = classifyGoogleSignInFailure(UnknownHostException("accounts.google.com"))
            as GoogleSignInOutcome.Failed
        assertEquals(GoogleSignInMessages.OFFLINE, dns.message)
        assertTrue(dns.offline)
    }

    @Test
    fun `an unexpected throwable never leaks its own message`() {
        val outcome = classifyGoogleSignInFailure(IllegalStateException("client id 12345 was rejected"))
            as GoogleSignInOutcome.Failed
        assertEquals(GoogleSignInMessages.UNKNOWN, outcome.message)
        assertFalse(outcome.message.contains("12345"))
    }
}
