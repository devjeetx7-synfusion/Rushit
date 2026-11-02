package com.rushit.app.data.auth.phone

data class CountryCode(
    val iso: String,
    val dialCode: String,
    val name: String,
    val nationalDigits: IntRange,
    val flag: String = "",
)

data class PhoneNumber(
    val country: CountryCode,
    val nationalNumber: String,
) {
    val e164: String get() = country.dialCode + nationalNumber
    val display: String get() = "${country.dialCode} $nationalNumber"
    val masked: String get() = "${country.dialCode} ${PhoneNumbers.maskNational(nationalNumber)}"
    override fun toString(): String = display
}

object PhoneNumbers {
    const val MASK_VISIBLE_DIGITS = 4
    val india: CountryCode = CountryCode("IN", "+91", "India", 10..10, "🇮🇳")
    val countries: List<CountryCode> = listOf(india)
    val default: CountryCode = india

    fun byIso(iso: String?): CountryCode? =
        if (iso.equals("IN", ignoreCase = true)) india else null

    fun digitsOf(raw: String): String {
        val digits = raw.filter(Char::isDigit)
        return when {
            digits.isEmpty() -> ""
            digits.startsWith("0") && digits.length > 1 -> digits.trimStart('0').ifEmpty { digits }
            else -> digits
        }
    }

    fun normalize(country: CountryCode = india, raw: String): PhoneNumber? {
        val national = nationalDigitsOf(country, digitsOf(raw))
        if (national.isEmpty() || national.length !in country.nationalDigits) return null
        return PhoneNumber(country, national)
    }

    fun maxDigits(country: CountryCode = india): Int = country.nationalDigits.last

    fun maskNational(nationalDigits: String): String {
        val digits = nationalDigits.filter(Char::isDigit)
        if (digits.isEmpty()) return ""
        val visible = if (digits.length <= 1) 0 else minOf(MASK_VISIBLE_DIGITS, digits.length - 1)
        val masked = "X".repeat(digits.length - visible) + digits.takeLast(visible)
        if (masked.length < 8) return masked
        val split = masked.length / 2
        return masked.substring(0, split) + " " + masked.substring(split)
    }

    fun errorFor(country: CountryCode = india, raw: String): String? {
        val digits = digitsOf(raw)
        if (digits.isEmpty()) return "Enter your phone number so RushIt can text you a code."
        val national = nationalDigitsOf(country, digits)
        return when {
            national.length < country.nationalDigits.first ->
                "That number looks too short for India (10 digits required)."
            national.length > country.nationalDigits.last ->
                "That number looks too long for India (10 digits required)."
            else -> null
        }
    }

    fun nationalDigitsFor(country: CountryCode = india, raw: String): String =
        nationalDigitsOf(country, digitsOf(raw))

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
