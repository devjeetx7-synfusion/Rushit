package com.rushit.app.domain

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Order timestamps use [java.text] rather than `java.time`, which needs API 26 or desugaring while
 * this app supports API 24. Storage format is stable and human-inspectable: `2026-10-29T10:15:00+0530`.
 */
object OrderTime {
    private const val STORAGE_PATTERN = "yyyy-MM-dd'T'HH:mm:ssZ"

    fun now(): String = SimpleDateFormat(STORAGE_PATTERN, Locale.US).format(Date())

    /** Formats a stored timestamp for display, falling back to the raw value if it cannot parse. */
    fun display(raw: String, pattern: String = "d MMM, h:mm a"): String {
        val parsed = parse(raw) ?: return raw.take(19).replace('T', ' ')
        return SimpleDateFormat(pattern, Locale.getDefault()).format(parsed)
    }

    fun displayWithYear(raw: String): String = display(raw, "d MMM yyyy, h:mm a")

    private fun parse(raw: String): Date? {
        if (raw.isBlank()) return null
        val trimmed = raw.trim()
        // Accept both `+0530` and `+05:30`, and timestamps written with milliseconds.
        val candidates = listOf(
            STORAGE_PATTERN,
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        )
        candidates.forEach { pattern ->
            val date = runCatching {
                SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }.parse(trimmed)
            }.getOrNull()
            if (date != null) return date
        }
        return null
    }
}
