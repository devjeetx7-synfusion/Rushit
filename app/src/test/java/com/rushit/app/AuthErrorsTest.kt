package com.rushit.app

import com.rushit.app.data.auth.AuthErrors
import com.rushit.app.data.auth.AuthFailureKind
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.gotrue.exception.AuthErrorCode
import io.github.jan.supabase.gotrue.exception.AuthRestException
import io.github.jan.supabase.gotrue.exception.AuthSessionMissingException
import io.ktor.client.request.HttpRequestBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Error translation.
 *
 * Two properties are asserted throughout: every failure class gets an actionable message, and no
 * message ever carries a credential. `RestException.message` embeds the request URL and headers —
 * including the anon key and the bearer token — so it must never reach a person.
 */
class AuthErrorsTest {

    /**
     * [TimeoutCancellationException]'s constructor is internal, so a real one has to be produced by
     * actually timing out. The classification under test is the same either way.
     */
    private fun timeoutException(): TimeoutCancellationException = runBlocking {
        try {
            withTimeout(1L) { delay(2_000L) }
            throw IllegalStateException("withTimeout should have thrown")
        } catch (error: TimeoutCancellationException) {
            error
        }
    }

    @Test
    fun `connectivity failures are explained as connectivity failures`() {
        listOf(
            IOException("socket closed"),
            UnknownHostException("example.supabase.co"),
            ConnectException("failed to connect"),
            HttpRequestException("timeout", HttpRequestBuilder()),
        ).forEach { error ->
            val failure = AuthErrors.describe(error)
            assertEquals("for $error", AuthFailureKind.NETWORK, failure.kind)
            assertTrue(failure.message.contains("connection"))
        }
    }

    @Test
    fun `timeouts are distinguished from being offline`() {
        assertEquals(AuthFailureKind.TIMEOUT, AuthErrors.describe(timeoutException()).kind)
        assertEquals(AuthFailureKind.TIMEOUT, AuthErrors.describe(SocketTimeoutException()).kind)
        assertTrue(AuthErrors.describe(timeoutException()).message.contains("too long"))
    }

    @Test
    fun `a missing or expired session asks for a new sign-in`() {
        val failure = AuthErrors.describe(AuthSessionMissingException())

        assertEquals(AuthFailureKind.SESSION_EXPIRED, failure.kind)
        assertTrue(failure.message.contains("sign in again"))
    }

    @Test
    fun `auth error codes map to the class a person can act on`() {
        val cases = mapOf(
            AuthErrorCode.BadJwt.value to AuthFailureKind.INVALID_TOKEN,
            AuthErrorCode.UnexpectedAudience.value to AuthFailureKind.INVALID_TOKEN,
            AuthErrorCode.ValidationFailed.value to AuthFailureKind.INVALID_TOKEN,
            AuthErrorCode.SessionNotFound.value to AuthFailureKind.SESSION_EXPIRED,
            AuthErrorCode.UserNotFound.value to AuthFailureKind.SESSION_EXPIRED,
            AuthErrorCode.ProviderDisabled.value to AuthFailureKind.UNCONFIGURED,
            AuthErrorCode.OauthProviderNotSupported.value to AuthFailureKind.UNCONFIGURED,
            AuthErrorCode.NoAuthorization.value to AuthFailureKind.POLICY_DENIED,
            AuthErrorCode.OverRequestRateLimit.value to AuthFailureKind.REJECTED,
            AuthErrorCode.UserBanned.value to AuthFailureKind.REJECTED,
        )

        cases.forEach { (code, expected) ->
            val failure = AuthErrors.describe(AuthRestException(code, "internal detail"))
            assertEquals("for code $code", expected, failure.kind)
            assertFalse("the raw code detail must not be shown", failure.message.contains("internal detail"))
        }
    }

    @Test
    fun `an unknown auth error code still produces a usable message`() {
        val failure = AuthErrors.describe(AuthRestException("some_future_code", "detail"))

        assertEquals(AuthFailureKind.REJECTED, failure.kind)
        assertTrue(failure.message.isNotBlank())
    }

    @Test
    fun `a disabled google provider points at the Supabase setting`() {
        val failure = AuthErrors.describe(AuthRestException(AuthErrorCode.ProviderDisabled.value, "x"))

        assertTrue(failure.message.contains("Google provider"))
        assertEquals(AuthFailureKind.UNCONFIGURED, failure.kind)
    }

    @Test
    fun `row level security rejections never leak request details`() = runTest {
        val exceptions = RestExceptionFactory()

        val failure = AuthErrors.describe(exceptions.rowLevelSecurity())

        assertEquals(AuthFailureKind.POLICY_DENIED, failure.kind)
        assertFalse(failure.message.contains(RestExceptionFactory.SECRET_ANON_KEY))
        assertFalse(failure.message.contains(RestExceptionFactory.SECRET_JWT))
        assertFalse(failure.message.contains("profiles?select"))
        exceptions.close()
    }

    @Test
    fun `a unique violation is reported as a username conflict`() = runTest {
        val exceptions = RestExceptionFactory()

        val failure = AuthErrors.describe(exceptions.duplicateKey())

        assertEquals(AuthFailureKind.CONFLICT, failure.kind)
        assertTrue(failure.message.contains("username"))
        exceptions.close()
    }

    @Test
    fun `an expired jwt from postgrest sends the person back to login`() = runTest {
        val exceptions = RestExceptionFactory()

        assertEquals(AuthFailureKind.SESSION_EXPIRED, AuthErrors.describe(exceptions.jwtExpired()).kind)
        assertEquals(AuthFailureKind.SESSION_EXPIRED, AuthErrors.describe(exceptions.unauthorized()).kind)
        assertEquals(AuthFailureKind.NOT_FOUND, AuthErrors.describe(exceptions.notFound()).kind)
        exceptions.close()
    }

    @Test
    fun `an unexpected throwable is surfaced, not swallowed`() {
        val failure = AuthErrors.describe(IllegalStateException("something no one anticipated"))

        assertEquals(AuthFailureKind.UNKNOWN, failure.kind)
        assertTrue(failure.message.isNotBlank())
        // The raw exception text is deliberately not forwarded: it can contain internals.
        assertFalse(failure.message.contains("no one anticipated"))
    }

    @Test
    fun `a null error still yields a message`() {
        val failure = AuthErrors.describe(null)

        assertEquals(AuthFailureKind.UNKNOWN, failure.kind)
        assertTrue(failure.message.isNotBlank())
    }

    @Test
    fun `genuine coroutine cancellation is rethrown instead of being reported`() {
        try {
            AuthErrors.describeOrRethrow(CancellationException("scope cancelled"))
            fail("cancellation must propagate")
        } catch (expected: CancellationException) {
            assertEquals("scope cancelled", expected.message)
        }
    }

    @Test
    fun `a timeout is a failure, not a cancellation`() {
        val failure = AuthErrors.describeOrRethrow(timeoutException())

        assertEquals(AuthFailureKind.TIMEOUT, failure.kind)
    }
}
