package com.rushit.app.data.auth.phone

import java.util.Locale

/**
 * Rules for the Firebase SMS one-time code and the resend countdown.
 *
 * Firebase issues six-digit codes, so the UI only enables verification once six digits are present
 * and never asks Firebase to check a partial code. Everything here is pure Kotlin, which keeps the
 * OTP screen's behaviour testable without an emulator, a device or a Firebase project.
 */
object VerificationCode {

    /** Digits in a Firebase SMS code. */
    const val LENGTH = 6

    /** Digits only, capped at [LENGTH]. Pasted codes with spaces, dashes or a leading label survive. */
    fun normalize(raw: String): String = raw.filter(Char::isDigit).take(LENGTH)

    /** True once the person has entered a complete code. */
    fun isComplete(raw: String): Boolean = normalize(raw).length == LENGTH

    /**
     * A person-readable reason [raw] cannot be submitted, or null when it can.
     *
     * The message never repeats the entered code, so a mistyped OTP cannot end up in a log, a crash
     * report or a screenshot of an error banner.
     */
    fun validationMessage(raw: String): String? {
        val digits = normalize(raw)
        return when {
            digits.isEmpty() -> "Enter the $LENGTH-digit code RushIt texted you."
            digits.length < LENGTH -> "The code is $LENGTH digits. Enter all of them."
            else -> null
        }
    }

    /** `00:30` for 30 seconds, `01:05` for 65 — used by the "Resend OTP in 00:30" label. */
    fun formatCountdown(secondsRemaining: Int): String {
        val safe = secondsRemaining.coerceAtLeast(0)
        return String.format(Locale.US, "%02d:%02d", safe / 60, safe % 60)
    }
}
