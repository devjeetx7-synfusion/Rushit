package com.rushit.app.data.auth.phone

/**
 * One selectable dialling country.
 *
 * @property iso ISO 3166-1 alpha-2 code, also used as the stable UI key
 * @property dialCode international dialling prefix, e.g. `+91`
 * @property nationalDigits the lengths (in digits) RushIt accepts for the national part. It is a
 * range because several countries issue more than one length; it is enforced before an SMS is
 * requested so a typo never costs a Firebase SMS quota unit
 */
data class CountryCode(
    val iso: String,
    val dialCode: String,
    val name: String,
    val nationalDigits: IntRange,
)

/**
 * A phone number that has been normalized to E.164: `+` followed by the dialling code and the
 * national number, with no spaces, punctuation or trunk prefix.
 *
 * Firebase expects E.164, and Supabase's `profiles.phone_number` is compared as a plain string, so
 * normalizing once here keeps both consistent.
 */
data class PhoneNumber(
    val country: CountryCode,
    val nationalNumber: String,
) {
    /** `+919876543210` */
    val e164: String get() = country.dialCode + nationalNumber

    /** `+91 9876543210` — display only, never sent anywhere. */
    val display: String get() = "${country.dialCode} $nationalNumber"

    /**
     * `+91 XXXXX X3210` — the OTP screen's confirmation line.
     *
     * The last four digits stay readable so the person can confirm which number received the code;
     * everything else is masked, so a screenshot of the OTP screen does not disclose the full number.
     */
    val masked: String get() = "${country.dialCode} ${PhoneNumbers.maskNational(nationalNumber)}"

    override fun toString(): String = display
}

/**
 * Pure phone-number rules: country list, normalization and validation.
 *
 * Deliberately dependency-free (no libphonenumber) so it runs in JVM unit tests, and deliberately
 * conservative: a number that does not look plausible for the selected country is rejected before
 * Firebase is asked to send an SMS.
 */
object PhoneNumbers {

    /** How many trailing digits stay readable when a number is masked for display. */
    const val MASK_VISIBLE_DIGITS = 4

    val countries: List<CountryCode> = listOf(
        CountryCode("IN", "+91", "India", 10..10),
        CountryCode("US", "+1", "United States", 10..10),
        CountryCode("CA", "+1", "Canada", 10..10),
        CountryCode("GB", "+44", "United Kingdom", 10..10),
        CountryCode("AE", "+971", "United Arab Emirates", 9..9),
        CountryCode("SA", "+966", "Saudi Arabia", 9..9),
        CountryCode("QA", "+974", "Qatar", 8..8),
        CountryCode("KW", "+965", "Kuwait", 8..8),
        CountryCode("OM", "+968", "Oman", 8..8),
        CountryCode("BH", "+973", "Bahrain", 8..8),
        CountryCode("SG", "+65", "Singapore", 8..8),
        CountryCode("MY", "+60", "Malaysia", 9..10),
        CountryCode("ID", "+62", "Indonesia", 9..12),
        CountryCode("PH", "+63", "Philippines", 10..10),
        CountryCode("TH", "+66", "Thailand", 9..9),
        CountryCode("VN", "+84", "Vietnam", 9..9),
        CountryCode("LK", "+94", "Sri Lanka", 9..9),
        CountryCode("NP", "+977", "Nepal", 10..10),
        CountryCode("BD", "+880", "Bangladesh", 10..10),
        CountryCode("PK", "+92", "Pakistan", 10..10),
        CountryCode("AU", "+61", "Australia", 9..9),
        CountryCode("NZ", "+64", "New Zealand", 8..10),
        CountryCode("ZA", "+27", "South Africa", 9..9),
        CountryCode("NG", "+234", "Nigeria", 10..10),
        CountryCode("KE", "+254", "Kenya", 9..9),
        CountryCode("EG", "+20", "Egypt", 10..10),
        CountryCode("TR", "+90", "Türkiye", 10..10),
        CountryCode("DE", "+49", "Germany", 10..11),
        CountryCode("FR", "+33", "France", 9..9),
        CountryCode("IT", "+39", "Italy", 9..10),
        CountryCode("ES", "+34", "Spain", 9..9),
        CountryCode("NL", "+31", "Netherlands", 9..9),
        CountryCode("IE", "+353", "Ireland", 9..9),
        CountryCode("BR", "+55", "Brazil", 10..11),
        CountryCode("JP", "+81", "Japan", 10..10),
        CountryCode("KR", "+82", "South Korea", 9..10),
        CountryCode("CN", "+86", "China", 11..11),
    )

    /** India first: RushIt's launch market. */
    val default: CountryCode = countries.first()

    fun byIso(iso: String?): CountryCode? =
        countries.firstOrNull { it.iso.equals(iso, ignoreCase = true) }

    /** Digits only, with a single leading trunk zero removed when one was typed. */
    fun digitsOf(raw: String): String {
        val digits = raw.filter(Char::isDigit)
        return when {
            digits.isEmpty() -> ""
            // A single national trunk prefix is never part of E.164. Leading zeros are only stripped
            // when the rest of the number still looks like a national number.
            digits.startsWith("0") && digits.length > 1 -> digits.trimStart('0').ifEmpty { digits }
            else -> digits
        }
    }

    /**
     * Normalize [raw] for [country].
     *
     * @return the E.164 form, or null when the input is not a plausible number for that country.
     * Null is returned for an empty input too: callers show [errorFor] to explain why
     */
    fun normalize(country: CountryCode, raw: String): PhoneNumber? {
        val national = nationalDigitsOf(country, digitsOf(raw))
        if (national.isEmpty() || national.length !in country.nationalDigits) return null
        return PhoneNumber(country, national)
    }

    /** The number of digits the editable field may hold for [country]. */
    fun maxDigits(country: CountryCode): Int = country.nationalDigits.last

    /**
     * Mask [nationalDigits] for display: every digit except the last [MASK_VISIBLE_DIGITS] becomes
     * `X`, and long numbers keep a readable gap in the middle.
     *
     * `9876543210` → `XXXXX X3210`, `91234567` → `XXXX 5678`, `1234` → `X234`.
     */
    fun maskNational(nationalDigits: String): String {
        val digits = nationalDigits.filter(Char::isDigit)
        if (digits.isEmpty()) return ""
        // At least one digit is always masked, so an unusually short number is not printed in full.
        val visible = if (digits.length <= 1) 0 else minOf(MASK_VISIBLE_DIGITS, digits.length - 1)
        val masked = "X".repeat(digits.length - visible) + digits.takeLast(visible)
        if (masked.length < 8) return masked
        val split = masked.length / 2
        return masked.substring(0, split) + " " + masked.substring(split)
    }

    /** A person-readable reason [raw] cannot be used, or null when it is acceptable. */
    fun errorFor(country: CountryCode, raw: String): String? {
        val digits = digitsOf(raw)
        if (digits.isEmpty()) return "Enter your phone number so RushIt can text you a code."
        val national = nationalDigitsOf(country, digits)
        return when {
            national.length < country.nationalDigits.first ->
                "That number looks too short for ${country.name}."
            national.length > country.nationalDigits.last ->
                "That number looks too long for ${country.name}."
            else -> null
        }
    }

    /**
     * The national part of [raw], with a pasted international dialling code removed.
     *
     * Used by the editable field itself: pasting `+91 98765 43210` (or `919876543210`) must leave the
     * ten national digits in the field instead of being truncated to a different, wrong number.
     */
    fun nationalDigitsFor(country: CountryCode, raw: String): String =
        nationalDigitsOf(country, digitsOf(raw))

    /**
     * The national part of [digits], with a pasted international dialling code removed.
     *
     * A number that already starts with the country's own code (and still fits afterwards) is treated
     * as fully international, so `+91 98765 43210`, `91 98765 43210` and `098765 43210` all normalize
     * to the same E.164 value instead of being doubled or rejected as too long.
     */
    private fun nationalDigitsOf(country: CountryCode, digits: String): String {
        val dialDigits = country.dialCode.removePrefix("+")
        return if (dialDigits.isNotEmpty() &&
            digits.startsWith(dialDigits) &&
            digits.length - dialDigits.length in country.nationalDigits
        ) {
            digits.removePrefix(dialDigits)
        } else {
            digits
        }
    }
}
