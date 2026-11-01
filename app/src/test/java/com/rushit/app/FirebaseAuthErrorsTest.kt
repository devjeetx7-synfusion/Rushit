package com.rushit.app

import com.rushit.app.data.auth.AuthFailureKind
import com.rushit.app.data.auth.firebase.FirebaseAuthErrors
import com.rushit.app.data.auth.firebase.FirebaseAuthMessages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Firebase error-code translation.
 *
 * It is tested without the Firebase SDK on the classpath (the mapping is pure strings), which is
 * exactly why the adapter is structured that way. The important property is that no user-facing
 * message ever contains the raw Firebase code.
 */
class FirebaseAuthErrorsTest {

    @Test
    fun `an incorrect OTP is a retryable rejection`() {
        val failure = FirebaseAuthErrors.forCode("ERROR_INVALID_VERIFICATION_CODE")

        assertEquals(FirebaseAuthMessages.INVALID_CODE, failure.message)
        assertEquals(AuthFailureKind.REJECTED, failure.kind)
    }

    @Test
    fun `an expired verification session asks for a new code`() {
        val failure = FirebaseAuthErrors.forCode("ERROR_SESSION_EXPIRED")

        assertEquals(FirebaseAuthMessages.EXPIRED_CODE, failure.message)
        assertEquals(AuthFailureKind.REJECTED, failure.kind)
    }

    @Test
    fun `rate limiting and SMS quota are not credentials failures`() {
        listOf("ERROR_TOO_MANY_REQUESTS", "ERROR_QUOTA_EXCEEDED").forEach { code ->
            val failure = FirebaseAuthErrors.forCode(code)
            assertEquals(FirebaseAuthMessages.TOO_MANY_REQUESTS, failure.message)
            assertNotEquals(AuthFailureKind.INVALID_TOKEN, failure.kind)
        }
        assertEquals(
            AuthFailureKind.UNAVAILABLE,
            FirebaseAuthErrors.forCode("ERROR_BILLING_NOT_ENABLED").kind,
        )
    }

    @Test
    fun `an invalid phone number is reported before anything is charged`() {
        val failure = FirebaseAuthErrors.forCode("ERROR_INVALID_PHONE_NUMBER")

        assertEquals(FirebaseAuthMessages.INVALID_PHONE, failure.message)
        assertEquals(AuthFailureKind.REJECTED, failure.kind)
    }

    @Test
    fun `app verification failures point at configuration, not at the person`() {
        listOf(
            "ERROR_APP_NOT_AUTHORIZED",
            "ERROR_INVALID_APP_CREDENTIAL",
            "ERROR_CAPTCHA_CHECK_FAILED",
            "ERROR_INVALID_CERT_HASH",
        ).forEach { code ->
            val failure = FirebaseAuthErrors.forCode(code)
            assertEquals(AuthFailureKind.UNCONFIGURED, failure.kind)
            assertEquals(FirebaseAuthMessages.APP_VERIFICATION_FAILED, failure.message)
        }
    }

    @Test
    fun `a stale sign-in for account deletion asks for reauthentication`() {
        val failure = FirebaseAuthErrors.forCode("ERROR_REQUIRES_RECENT_LOGIN")

        assertEquals(FirebaseAuthMessages.REQUIRES_RECENT_LOGIN, failure.message)
        assertEquals(AuthFailureKind.SESSION_EXPIRED, failure.kind)
    }

    @Test
    fun `a network failure is classified as offline`() {
        assertEquals(
            AuthFailureKind.NETWORK,
            FirebaseAuthErrors.forCode("ERROR_NETWORK_REQUEST_FAILED").kind,
        )
    }

    @Test
    fun `an unknown code never leaks the code itself`() {
        val failure = FirebaseAuthErrors.forCode("ERROR_SOMETHING_NEW_FOR_2026")

        assertEquals(FirebaseAuthMessages.UNKNOWN, failure.message)
        assertEquals(AuthFailureKind.UNKNOWN, failure.kind)
        assertFalse(failure.message.contains("ERROR_"))
    }

    @Test
    fun `a missing Play Services message is recognised without a code`() {
        val failure = FirebaseAuthErrors.forCode(null, "Google Play services not available")

        assertEquals(FirebaseAuthMessages.PLAY_SERVICES_UNAVAILABLE, failure.message)
    }

    @Test
    fun `every mapped message avoids internal vocabulary`() {
        val codes = listOf(
            "ERROR_INVALID_VERIFICATION_CODE",
            "ERROR_SESSION_EXPIRED",
            "ERROR_TOO_MANY_REQUESTS",
            "ERROR_QUOTA_EXCEEDED",
            "ERROR_BILLING_NOT_ENABLED",
            "ERROR_INVALID_PHONE_NUMBER",
            "ERROR_MISSING_VERIFICATION_CODE",
            "ERROR_MISSING_ACTIVITY",
            "ERROR_CAPTCHA_CHECK_FAILED",
            "ERROR_REQUIRES_RECENT_LOGIN",
            "ERROR_USER_DISABLED",
            "ERROR_USER_NOT_FOUND",
            "ERROR_OPERATION_NOT_ALLOWED",
            "ERROR_NETWORK_REQUEST_FAILED",
            "ERROR_INVALID_CREDENTIAL",
            "ERROR_INTERNAL_ERROR",
        )
        codes.forEach { code ->
            val message = FirebaseAuthErrors.forCode(code).message
            assertTrue("$code should have a message", message.isNotBlank())
            assertTrue(message.endsWith("."))
            assertFalse(message.contains("ERROR_"))
            assertFalse(message.contains("FirebaseAuthException"))
        }
    }
}
