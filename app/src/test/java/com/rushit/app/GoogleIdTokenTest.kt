package com.rushit.app

import com.rushit.app.data.auth.AuthFailureKind
import com.rushit.app.data.auth.google.Base64Url
import com.rushit.app.data.auth.google.GoogleIdTokenClaims
import com.rushit.app.data.auth.google.GoogleIdTokenValidator
import com.rushit.app.data.auth.google.GoogleSignInMessages
import com.rushit.app.data.auth.google.IdTokenCheck
import com.rushit.app.data.auth.google.SignInNonce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Local ID-token checks: the audience must be this build's Web OAuth Client ID, the issuer must be
 * Google, the token must be unexpired, and the nonce must match what Credential Manager was given.
 */
class GoogleIdTokenTest {

    private val now = 1_700_000_100L

    @Test
    fun `nonce hashing matches what supabase recomputes from the raw nonce`() {
        // sha256("") — a fixed vector, so a change in encoding cannot slip through unnoticed.
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            SignInNonce.sha256Hex(""),
        )
        // GoTrue compares hex(sha256(rawNonce)) with the ID token's nonce claim, so the value handed
        // to Google must be the hex digest and lowercase.
        val hash = SignInNonce.hash("raw-nonce")
        assertEquals(64, hash.length)
        assertEquals(hash, hash.lowercase())
        assertEquals("2c5d107938053a2275f022c153c9a71f65ee07754b8bca543ee97a0c3cc66990", hash)
    }

    @Test
    fun `a raw nonce is never reused`() {
        assertTrue(SignInNonce.newRawNonce() != SignInNonce.newRawNonce())
    }

    @Test
    fun `base64url decoding handles unpadded segments`() {
        assertEquals("Hello", Base64Url.decodeToString("SGVsbG8"))
        assertEquals("any carnal pleasure.", Base64Url.decodeToString("YW55IGNhcm5hbCBwbGVhc3VyZS4"))
        assertNull(Base64Url.decodeToString("not*base64"))
        assertEquals("", Base64Url.decodeToString(""))
    }

    @Test
    fun `claims are read from the token payload`() {
        val token = buildIdToken(
            audience = TestConfig.WEB_CLIENT_ID,
            nonce = "hashed-nonce",
            email = "shopper@example.com",
        )

        val claims = GoogleIdTokenClaims.parse(token)

        assertNotNull(claims)
        assertEquals("https://accounts.google.com", claims!!.issuer)
        assertEquals(listOf(TestConfig.WEB_CLIENT_ID), claims.audiences)
        assertEquals("google-subject-1", claims.subject)
        assertEquals("hashed-nonce", claims.nonce)
        assertEquals("shopper@example.com", claims.email)
        assertEquals(1_700_003_600L, claims.expiresAtEpochSeconds)
    }

    @Test
    fun `a malformed token cannot be parsed`() {
        assertNull(GoogleIdTokenClaims.parse("not-a-jwt"))
        assertNull(GoogleIdTokenClaims.parse("header.payload"))
        assertNull(GoogleIdTokenClaims.parse("a.b.c"))
    }

    @Test
    fun `a correct token is accepted`() {
        val token = buildIdToken(nonce = SignInNonce.hash("raw-nonce"), expiresAt = now + 3_000)

        val check = GoogleIdTokenValidator.validate(token, TestConfig.WEB_CLIENT_ID, "raw-nonce", now)

        assertEquals(IdTokenCheck.Accepted, check)
    }

    @Test
    fun `the unhashed issuer form google also uses is accepted`() {
        val token = buildIdToken(issuer = "accounts.google.com", nonce = SignInNonce.hash("n"))

        assertEquals(
            IdTokenCheck.Accepted,
            GoogleIdTokenValidator.validate(token, TestConfig.WEB_CLIENT_ID, "n", now),
        )
    }

    @Test
    fun `a token issued for a different web client is a configuration error`() {
        val token = buildIdToken(audience = TestConfig.OTHER_CLIENT_ID, nonce = SignInNonce.hash("n"))

        val check = GoogleIdTokenValidator.validate(token, TestConfig.WEB_CLIENT_ID, "n", now)
            as IdTokenCheck.Rejected

        assertEquals(GoogleSignInMessages.AUDIENCE_MISMATCH, check.message)
        assertEquals(AuthFailureKind.UNCONFIGURED, check.kind)
    }

    @Test
    fun `a token from another issuer is rejected`() {
        val token = buildIdToken(issuer = "https://login.example.test", nonce = SignInNonce.hash("n"))

        val check = GoogleIdTokenValidator.validate(token, TestConfig.WEB_CLIENT_ID, "n", now)
            as IdTokenCheck.Rejected

        assertEquals(GoogleSignInMessages.NOT_GOOGLE_TOKEN, check.message)
        assertEquals(AuthFailureKind.INVALID_TOKEN, check.kind)
    }

    @Test
    fun `an expired token is rejected, with tolerance for clock skew`() {
        val expired = buildIdToken(expiresAt = now - 600, nonce = SignInNonce.hash("n"))
        val check = GoogleIdTokenValidator.validate(expired, TestConfig.WEB_CLIENT_ID, "n", now)
            as IdTokenCheck.Rejected
        assertEquals(GoogleSignInMessages.EXPIRED_TOKEN, check.message)

        // Just inside the skew window: let Supabase make the final call rather than failing here.
        val borderline = buildIdToken(expiresAt = now - 30, nonce = SignInNonce.hash("n"))
        assertEquals(
            IdTokenCheck.Accepted,
            GoogleIdTokenValidator.validate(borderline, TestConfig.WEB_CLIENT_ID, "n", now),
        )
    }

    @Test
    fun `a nonce mismatch is rejected`() {
        val token = buildIdToken(nonce = SignInNonce.hash("some-other-nonce"))

        val check = GoogleIdTokenValidator.validate(token, TestConfig.WEB_CLIENT_ID, "raw-nonce", now)
            as IdTokenCheck.Rejected

        assertEquals(GoogleSignInMessages.NONCE_MISMATCH, check.message)
    }

    @Test
    fun `a token with a nonce cannot be paired with a request that had none`() {
        val token = buildIdToken(nonce = SignInNonce.hash("raw-nonce"))

        val check = GoogleIdTokenValidator.validate(token, TestConfig.WEB_CLIENT_ID, null, now)
            as IdTokenCheck.Rejected

        assertEquals(GoogleSignInMessages.NONCE_MISMATCH, check.message)
    }

    @Test
    fun `a token without a nonce is accepted when the request had none`() {
        val token = buildIdToken(nonce = null)

        assertEquals(
            IdTokenCheck.Accepted,
            GoogleIdTokenValidator.validate(token, TestConfig.WEB_CLIENT_ID, null, now),
        )
    }

    @Test
    fun `blank and unreadable tokens are rejected without echoing their contents`() {
        val blank = GoogleIdTokenValidator.validate("  ", TestConfig.WEB_CLIENT_ID, null, now)
            as IdTokenCheck.Rejected
        assertEquals(GoogleSignInMessages.MISSING_TOKEN, blank.message)

        val unreadable = GoogleIdTokenValidator.validate("a.b.c", TestConfig.WEB_CLIENT_ID, null, now)
            as IdTokenCheck.Rejected
        assertEquals(GoogleSignInMessages.UNREADABLE_TOKEN, unreadable.message)
        assertFalse(unreadable.message.contains("a.b.c"))
    }

    @Test
    fun `a missing subject is rejected`() {
        val token = buildIdToken(subject = "", nonce = SignInNonce.hash("n"))

        val check = GoogleIdTokenValidator.validate(token, TestConfig.WEB_CLIENT_ID, "n", now)
            as IdTokenCheck.Rejected

        assertEquals(GoogleSignInMessages.UNREADABLE_TOKEN, check.message)
    }
}
