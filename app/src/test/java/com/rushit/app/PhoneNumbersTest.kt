package com.rushit.app

import com.rushit.app.data.auth.phone.PhoneNumbers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phone-number normalization.
 *
 * Firebase expects E164 and rejects anything else, so these rules are what keep a typo from
 * becoming a wasted SMS or an unhelpful Firebase error.
 */
class PhoneNumbersTest {

    private val india = PhoneNumbers.byIso("IN")!!

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
        // Validation and normalization must agree about a pasted international number, otherwise the
        // screen rejects a number the gateway would happily send an SMS to.
        assertNull(PhoneNumbers.errorFor(india, "+91 98765 43210"))
        assertNull(PhoneNumbers.errorFor(india, "919876543210"))
        assertNull(PhoneNumbers.errorFor(india, "09876543210"))
    }

    @Test
    fun `country codes are unique per ISO and formatted with a plus`() {
        val isos = PhoneNumbers.countries.map { it.iso }
        assertEquals(isos.size, isos.toSet().size)
        PhoneNumbers.countries.forEach { country ->
            assertTrue(country.dialCode.startsWith("+"))
            assertTrue(country.nationalDigits.first > 0)
            assertTrue(country.nationalDigits.last >= country.nationalDigits.first)
        }
    }

    @Test
    fun `the default country is India`() {
        assertEquals("IN", PhoneNumbers.default.iso)
        assertEquals("+91", PhoneNumbers.default.dialCode)
    }

    @Test
    fun `an unknown ISO falls back to null rather than throwing`() {
        assertNull(PhoneNumbers.byIso("ZZ"))
        assertNull(PhoneNumbers.byIso(null))
    }

    @Test
    fun `a number is masked for the code screen but keeps the last four digits`() {
        val number = PhoneNumbers.normalize(india, "9876543210")!!

        assertEquals("+91 XXXXX X3210", number.masked)
        // The masked form never contains every digit of the real number.
        assertTrue(number.masked.count { it.isDigit() } < number.nationalNumber.length)
    }

    @Test
    fun `masking handles short, empty and unformatted input`() {
        assertEquals("XXXX 5678", PhoneNumbers.maskNational("91234567"))
        assertEquals("X234", PhoneNumbers.maskNational("1234"))
        assertEquals("", PhoneNumbers.maskNational(""))
        assertEquals("XXXXX X3210", PhoneNumbers.maskNational("98-76 5-43210"))
    }

    @Test
    fun `a pasted international number is reduced to its national part for the field`() {
        assertEquals("9876543210", PhoneNumbers.nationalDigitsFor(india, "+91 98765 43210"))
        assertEquals("9876543210", PhoneNumbers.nationalDigitsFor(india, "919876543210"))
        // Partial input is left alone so typing is never rewritten mid-number.
        assertEquals("98765", PhoneNumbers.nationalDigitsFor(india, "98765"))
        assertEquals("987654321012", PhoneNumbers.nationalDigitsFor(india, "987654321012"))
    }

    @Test
    fun `the editable field is capped at the country's longest national number`() {
        assertEquals(10, PhoneNumbers.maxDigits(india))
        assertEquals(8, PhoneNumbers.maxDigits(PhoneNumbers.byIso("SG")!!))
        assertEquals(12, PhoneNumbers.maxDigits(PhoneNumbers.byIso("ID")!!))
    }

    @Test
    fun `a shorter national number is accepted for a country that allows it`() {
        val singapore = PhoneNumbers.byIso("SG")!!
        assertEquals("+6591234567", PhoneNumbers.normalize(singapore, "91234567")!!.e164)

        val germany = PhoneNumbers.byIso("DE")!!
        // Germany issues 10- and 11-digit national numbers.
        assertEquals("+491511234567", PhoneNumbers.normalize(germany, "1511234567")!!.e164)
        assertEquals("+4915112345678", PhoneNumbers.normalize(germany, "15112345678")!!.e164)
    }
}
