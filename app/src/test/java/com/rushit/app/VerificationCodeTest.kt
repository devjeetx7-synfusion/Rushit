package com.rushit.app

import com.rushit.app.data.auth.phone.VerificationCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The one-time-code rules the OTP screen depends on.
 *
 * Firebase only ever issues six-digit codes, so the UI refuses to submit anything else: a partial
 * code must never produce a "wrong code" from Firebase that the person cannot understand.
 */
class VerificationCodeTest {

    @Test
    fun `only digits survive and the length is capped`() {
        assertEquals("123456", VerificationCode.normalize("123456"))
        assertEquals("123456", VerificationCode.normalize("12a34b56"))
        assertEquals("123456", VerificationCode.normalize("1234567"))
        assertEquals("", VerificationCode.normalize("abc"))
        assertEquals("", VerificationCode.normalize(""))
    }

    @Test
    fun `a pasted code with separators is normalized`() {
        assertEquals("123456", VerificationCode.normalize(" 123 456 "))
        assertEquals("123456", VerificationCode.normalize("123-456"))
        assertEquals("123456", VerificationCode.normalize("OTP: 123456"))
    }

    @Test
    fun `completeness requires exactly six digits`() {
        assertFalse(VerificationCode.isComplete(""))
        assertFalse(VerificationCode.isComplete("12345"))
        assertTrue(VerificationCode.isComplete("123456"))
        // Extra digits are truncated rather than rejected, so a pasted "1234567" still works.
        assertTrue(VerificationCode.isComplete("1234567"))
    }

    @Test
    fun `an empty or partial code is explained without repeating it`() {
        val empty = VerificationCode.validationMessage("")
        val partial = VerificationCode.validationMessage("12345")

        assertTrue(empty!!.contains("6-digit"))
        assertTrue(partial!!.contains("6 digits"))
        // The entered digits must never appear in a message: it would end up in a log or a screenshot.
        assertFalse(partial.contains("12345"))
        assertNull(VerificationCode.validationMessage("123456"))
        assertNull(VerificationCode.validationMessage("123 456"))
    }

    @Test
    fun `countdown is formatted as minutes and seconds`() {
        assertEquals("00:30", VerificationCode.formatCountdown(30))
        assertEquals("00:09", VerificationCode.formatCountdown(9))
        assertEquals("01:05", VerificationCode.formatCountdown(65))
        assertEquals("00:00", VerificationCode.formatCountdown(0))
        // A clock that ticks past zero must not print a negative timer.
        assertEquals("00:00", VerificationCode.formatCountdown(-4))
    }
}
