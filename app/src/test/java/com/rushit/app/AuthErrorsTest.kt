package com.rushit.app

import com.rushit.app.data.auth.AuthErrors
import com.rushit.app.data.auth.AuthFailureKind
import com.rushit.app.data.auth.AuthGatewayException
import com.rushit.app.data.auth.firebase.FirebaseAuthMessages
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AuthErrorsTest {

    @Test
    fun `gateway exception failure is passed through unchanged`() {
        val failure = com.rushit.app.data.auth.AuthFailure("Phone invalid", AuthFailureKind.REJECTED)
        val described = AuthErrors.describe(AuthGatewayException(failure))
        assertEquals(failure, described)
    }

    @Test
    fun `network exceptions map to OFFLINE`() {
        assertEquals(AuthFailureKind.NETWORK, AuthErrors.describe(IOException("broken pipe")).kind)
        assertEquals(AuthFailureKind.NETWORK, AuthErrors.describe(UnknownHostException("firebase.google.com")).kind)
        assertEquals(AuthFailureKind.NETWORK, AuthErrors.describe(ConnectException("connection refused")).kind)
    }

    @Test
    fun `timeout exceptions map to SLOW`() {
        assertEquals(AuthFailureKind.TIMEOUT, AuthErrors.describe(SocketTimeoutException("timeout")).kind)
    }

    @Test
    fun `cancellation exception is rethrown`() {
        try {
            AuthErrors.describeOrRethrow(CancellationException("cancelled"))
            fail("Expected CancellationException to be rethrown")
        } catch (e: CancellationException) {
            assertEquals("cancelled", e.message)
        }
    }
}
