package com.rushit.app

import com.rushit.app.data.auth.google.SupabaseOAuthRedirect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseOAuthRedirectTest {
    private val code = "abcdefghijklmnopqrstuvwxyz123456"

    @Test
    fun `only the exact registered callback with a code is accepted`() {
        assertEquals(
            SupabaseOAuthRedirect.Result.Code(code),
            SupabaseOAuthRedirect.parse("rushit://auth/callback?code=$code"),
        )
    }

    @Test
    fun `provider cancellation is distinct from rejection`() {
        assertEquals(
            SupabaseOAuthRedirect.Result.Cancelled,
            SupabaseOAuthRedirect.parse("rushit://auth/callback?error=access_denied&error_code=otp_disabled"),
        )
        assertTrue(SupabaseOAuthRedirect.parse("rushit://auth/callback?error=server_error") is SupabaseOAuthRedirect.Result.Rejected)
    }

    @Test
    fun `unrelated links are ignored`() {
        for (uri in listOf(null, "", "not a uri", "https://example.com", "rushit://auth/other?code=$code", "rushit://other/callback?code=$code")) {
            assertEquals(SupabaseOAuthRedirect.Result.Ignored, SupabaseOAuthRedirect.parse(uri))
        }
    }

    @Test
    fun `host path case credentials port fragments and malformed URLs are rejected`() {
        for (uri in listOf(
            "rushit://AUTH/callback?code=$code",
            "rushit://auth/callback/extra?code=$code",
            "rushit://attacker@auth/callback?code=$code",
            "rushit://auth:443/callback?code=$code",
            "rushit://auth/callback?code=$code#access_token=never",
            "rushit://auth/callback?code=$code&code=another",
            "rushit://auth/callback?code=$code&unexpected=value",
            "rushit://auth/callback?code=short",
            "rushit://auth/callback?code=$code&error=server_error",
            "rushit://auth/callback",
            "rushit://auth/callback?code=%zz",
        )) {
            assertTrue(uri, SupabaseOAuthRedirect.parse(uri) is SupabaseOAuthRedirect.Result.Rejected)
        }
    }

    @Test
    fun `implicit tokens are never accepted as a sign-in code`() {
        assertTrue(
            SupabaseOAuthRedirect.parse("rushit://auth/callback#access_token=secret&refresh_token=secret")
                is SupabaseOAuthRedirect.Result.Rejected,
        )
    }
}
