package com.rushit.app.data.auth.google

/**
 * Minimal Base64 URL decoder.
 *
 * `java.util.Base64` needs API 26 and `android.util.Base64` cannot run in JVM unit tests, while
 * RushIt supports API 24. Decoding an ID token payload is the only use, so a small pure-Kotlin
 * implementation keeps the token checks testable and available on every supported device.
 */
internal object Base64Url {

    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    /** Returns null when [input] is not valid unpadded Base64 URL content. */
    fun decode(input: String): ByteArray? {
        val clean = input.trim().trimEnd('=')
        if (clean.isEmpty()) return ByteArray(0)
        if (clean.any { ALPHABET.indexOf(it) < 0 }) return null

        val output = ByteArray(clean.length * 3 / 4)
        var buffer = 0
        var bits = 0
        var index = 0
        for (character in clean) {
            buffer = (buffer shl 6) or ALPHABET.indexOf(character)
            bits += 6
            if (bits >= 8) {
                bits -= 8
                output[index++] = (buffer shr bits).toByte()
            }
        }
        return if (index == output.size) output else output.copyOf(index)
    }

    fun decodeToString(input: String): String? = decode(input)?.toString(Charsets.UTF_8)
}
