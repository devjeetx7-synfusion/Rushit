package com.rushit.app

import com.rushit.app.data.auth.phone.PhoneNumbers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumbersTest {

    private val india = PhoneNumbers.india

    @Test
    fun `a plain national number becomes E164`() {
        val number = PhoneNumbers.normalize(india, "9876543210")

        assertNotNull(number)
        assertEquals("+919876543210", number!!.e164)
        assertEquals("+91 9876543210", number.display)
    }

    @Test
    fun `spaces, dashes and brackets are ignored`() {
        assertEquals(
            "+919876543210",
            PhoneNumbers.normalize(india, "98765 43210")!!.e164,
        )
        assertEquals(
            "+919876543210",
            PhoneNumbers.normalize(india, "98765-43210")!!.e164,
        )
    }

    @Test
    fun `a national trunk zero is dropped`() {
        assertEquals("+919876543210", PhoneNumbers.normalize(india, "09876543210")!!.e164)
    }

    @Test
    fun `a pasted international number is not doubled`() {
        assertEquals("+919876543210", PhoneNumbers.normalize(india, "+91 98765 43210")!!.e164)
        assertEquals("+919876543210", PhoneNumbers.normalize(india, "919876543210")!!.e164)
    }

    @Test
    fun `too short and too long numbers are rejected with a reason`() {
        assertNull(PhoneNumbers.normalize(india, "98765"))
        assertNull(PhoneNumbers.normalize(india, "987654321012"))

        assertTrue(PhoneNumbers.errorFor(india, "98765")!!.contains("too short"))
        assertTrue(PhoneNumbers.errorFor(india, "987654321012")!!.contains("too long"))
        assertTrue(PhoneNumbers.errorFor(india, "")!!.contains("Enter your phone number"))
    }

    @Test
    fun `an acceptable number has no error`() {
        assertNull(PhoneNumbers.errorFor(india, "9876543210"))
        assertNull(PhoneNumbers.errorFor(india, "+91 98765 43210"))
        assertNull(PhoneNumbers.errorFor(india, "919876543210"))
        assertNull(PhoneNumbers.errorFor(india, "09876543210"))
    }

    @Test
    fun `only India is in the countries list`() {
        assertEquals(1, PhoneNumbers.countries.size)
        assertEquals("IN", PhoneNumbers.countries.first().iso)
        assertEquals("+91", PhoneNumbers.countries.first().dialCode)
    }

    @Test
    fun `the default country is India`() {
        assertEquals("IN", PhoneNumbers.default.iso)
        assertEquals("+91", PhoneNumbers.default.dialCode)
    }

    @Test
    fun `non-India ISO falls back to null`() {
        assertNull(PhoneNumbers.byIso("US"))
        assertNull(PhoneNumbers.byIso("SG"))
        assertNull(PhoneNumbers.byIso(null))
        assertNotNull(PhoneNumbers.byIso("IN"))
    }

    @Test
    fun `a number is masked for the code screen but keeps the last four digits`() {
        val number = PhoneNumbers.normalize(india, "9876543210")!!

        assertEquals("+91 XXXXX X3210", number.masked)
        assertTrue(number.masked.count { it.isDigit() } < number.nationalNumber.length)
    }

    @Test
    fun `masking handles short, empty and unformatted input`() {
        assertEquals("XXXX 4567", PhoneNumbers.maskNational("91234567"))
        assertEquals("X234", PhoneNumbers.maskNational("1234"))
        assertEquals("", PhoneNumbers.maskNational(""))
        assertEquals("XXXXX X3210", PhoneNumbers.maskNational("98-76 5-43210"))
    }

    @Test
    fun `a pasted international number is reduced to its national part for the field`() {
        assertEquals("9876543210", PhoneNumbers.nationalDigitsFor(india, "+91 98765 43210"))
        assertEquals("9876543210", PhoneNumbers.nationalDigitsFor(india, "919876543210"))
        assertEquals("98765", PhoneNumbers.nationalDigitsFor(india, "98765"))
        assertEquals("987654321012", PhoneNumbers.nationalDigitsFor(india, "987654321012"))
    }

    @Test
    fun `the editable field is capped at 10 digits for India`() {
        assertEquals(10, PhoneNumbers.maxDigits(india))
    }
}
