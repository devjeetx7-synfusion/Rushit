package com.rushit.app

import com.rushit.app.data.auth.AuthErrors
import com.rushit.app.data.auth.AuthFailure
import com.rushit.app.data.auth.AuthFailureKind
import com.rushit.app.data.auth.AuthGatewayException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Failure classification.
 *
 * Two rules are load-bearing: a genuine coroutine cancellation is rethrown rather than swallowed,
 * and no message ever contains a token, a stack trace or a transport exception's own text.
 */
class AuthErrorsTest {

    @Test
    fun `a failure classified by an adapter is passed through unchanged`() {
        val original = AuthFailure("That code is not correct.", AuthFailureKind.REJECTED)

        assertEquals(original, AuthErrors.describe(AuthGatewayException(original)))
    }

    @Test
    fun `connectivity failures are offline, never a credential rejection`() {
        listOf(
            IOException("socket closed"),
            UnknownHostException("firebaseio.com"),
            ConnectException("refused"),
        ).forEach { error ->
            val failure = AuthErrors.describe(error)
            assertEquals(AuthErrors.OFFLINE, failure.message)
            assertEquals(AuthFailureKind.NETWORK, failure.kind)
        }
    }

    @Test
    fun `slow requests are distinguished from offline ones`() {
        val failure = AuthErrors.describe(SocketTimeoutException("read timed out"))

        assertEquals(AuthFailureKind.TIMEOUT, failure.kind)
        assertEquals(AuthErrors.SLOW, failure.message)
    }

    @Test
    fun `a real cancellation is rethrown so structured concurrency is preserved`() {
        var rethrown = false
        try {
            AuthErrors.describeOrRethrow(CancellationException("scope closed"))
        } catch (expected: CancellationException) {
            rethrown = true
        }
        assertTrue("cancellation must propagate", rethrown)
    }

    @Test
    fun `a timeout cancellation is reported, not rethrown`() {
        // Produced by the library itself: TimeoutCancellationException has an internal constructor.
        val timeout = runCatching {
            runBlocking { withTimeout(1L) { delay(50L) } }
        }.exceptionOrNull()!!

        val failure = AuthErrors.describeOrRethrow(timeout)

        assertEquals(AuthFailureKind.TIMEOUT, failure.kind)
    }

    @Test
    fun `unknown failures get a generic message with no internal detail`() {
        val failure = AuthErrors.describe(IllegalStateException("user 42 token abc.def.ghi leaked"))

        assertEquals(AuthErrors.UNKNOWN, failure.message)
        assertFalse(failure.message.contains("abc.def.ghi"))
        assertFalse(failure.message.contains("42"))
    }

    @Test
    fun `every message is a sentence a person can act on`() {
        val messages = listOf(
            AuthErrors.OFFLINE,
            AuthErrors.SLOW,
            AuthErrors.SESSION_GONE,
            AuthErrors.POLICY,
            AuthErrors.UNKNOWN,
            AuthErrors.RATE_LIMITED,
            AuthErrors.USERNAME_TAKEN,
            AuthErrors.BACKEND_NOT_AUTHORIZED,
        )
        messages.forEach { message ->
            assertTrue(message.endsWith("."))
            assertTrue(message.length > 20)
            assertFalse(message.contains("Exception"))
            assertFalse(message.contains("http"))
        }
    }
}
