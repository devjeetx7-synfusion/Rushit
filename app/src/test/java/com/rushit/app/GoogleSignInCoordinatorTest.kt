package com.rushit.app

import com.rushit.app.data.auth.google.GoogleIdRequestSpec
import com.rushit.app.data.auth.google.GoogleSignInClient
import com.rushit.app.data.auth.google.GoogleSignInCoordinator
import com.rushit.app.data.auth.google.GoogleSignInMessages
import com.rushit.app.data.auth.google.GoogleSignInOutcome
import com.rushit.app.data.auth.google.SupabaseBrowserSignIn
import com.rushit.app.data.auth.google.SupabaseOAuthTransport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleSignInCoordinatorTest {
    private val clientId = "1234567890-abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com"
    private val code = "abcdefghijklmnopqrstuvwxyz123456"
    private val callback = "rushit://auth/callback?code=$code"

    @Test
    fun `unsupported native sign-in falls back to official browser transport`() = runTest {
        val browser = FakeBrowser()
        val client = coordinator(browser) { GoogleSignInOutcome.Unsupported }
        assertEquals(GoogleSignInOutcome.BrowserPending, client.requestGoogleIdToken())
        assertEquals(1, browser.starts)
        assertEquals(GoogleSignInOutcome.BrowserSuccess, client.completeBrowserRedirect(callback))
        assertEquals(listOf(code), browser.exchanged)
        assertEquals(1, browser.clears)
    }

    @Test
    fun `no native credential after both stages also uses browser fallback`() = runTest {
        val browser = FakeBrowser()
        val client = coordinator(browser) { GoogleSignInOutcome.NoCredentialAvailable }
        assertEquals(GoogleSignInOutcome.BrowserPending, client.requestGoogleIdToken())
        assertEquals(1, browser.starts)
    }

    @Test
    fun `native cancellation never opens a browser`() = runTest {
        val browser = FakeBrowser()
        val client = coordinator(browser) { GoogleSignInOutcome.Cancelled }
        assertEquals(GoogleSignInOutcome.Cancelled, client.requestGoogleIdToken())
        assertEquals(0, browser.starts)
    }

    @Test
    fun `duplicate callback is exchanged only once`() = runTest {
        val browser = FakeBrowser()
        val client = coordinator(browser) { GoogleSignInOutcome.Unsupported }
        client.requestGoogleIdToken()
        assertEquals(GoogleSignInOutcome.BrowserSuccess, client.completeBrowserRedirect(callback))
        assertNull(client.completeBrowserRedirect(callback))
        assertEquals(listOf(code), browser.exchanged)
    }

    @Test
    fun `callback without this app's persisted PKCE verifier is rejected`() = runTest {
        val browser = FakeBrowser().apply { pending = false }
        val client = coordinator(browser) { GoogleSignInOutcome.Unsupported }
        val outcome = client.completeBrowserRedirect(callback) as GoogleSignInOutcome.Failed
        assertEquals(GoogleSignInMessages.BROWSER_EXPIRED, outcome.message)
        assertTrue(browser.exchanged.isEmpty())
    }

    @Test
    fun `returning without a callback cancels and clears the pending verifier`() = runTest {
        val browser = FakeBrowser()
        val client = coordinator(browser) { GoogleSignInOutcome.Unsupported }
        client.requestGoogleIdToken()
        assertEquals(GoogleSignInOutcome.Cancelled, client.cancelBrowserSignIn())
        assertEquals(1, browser.clears)
        assertNull(client.cancelBrowserSignIn())
        val outcome = client.completeBrowserRedirect(callback) as GoogleSignInOutcome.Failed
        assertEquals(GoogleSignInMessages.BROWSER_EXPIRED, outcome.message)
    }

    @Test
    fun `concurrent sign-in taps have one owner`() = runTest {
        val gate = CompletableDeferred<GoogleSignInOutcome>()
        var calls = 0
        val client = coordinator(FakeBrowser()) { calls++; gate.await() }
        val first = async { client.requestGoogleIdToken() }
        val second = client.requestGoogleIdToken() as GoogleSignInOutcome.Failed
        assertEquals(GoogleSignInMessages.ALREADY_SIGNING_IN, second.message)
        gate.complete(GoogleSignInOutcome.Cancelled)
        assertEquals(GoogleSignInOutcome.Cancelled, first.await())
        assertEquals(1, calls)
    }

    @Test
    fun `unconfigured Supabase never launches browser OAuth`() = runTest {
        val browser = FakeBrowser()
        val client = GoogleSignInCoordinator(
            nativeSignIn = fakeNative { GoogleSignInOutcome.Unsupported },
            browserSignIn = SupabaseBrowserSignIn({ null }, browser),
            browserTransport = browser, browserConfigured = false,
        )
        assertEquals(GoogleSignInOutcome.Failed(GoogleSignInMessages.PLAY_SERVICES), client.requestGoogleIdToken())
        assertEquals(0, browser.starts)
    }

    private fun coordinator(browser: FakeBrowser, native: suspend (GoogleIdRequestSpec) -> GoogleSignInOutcome) =
        GoogleSignInCoordinator(
            nativeSignIn = fakeNative(native),
            browserSignIn = SupabaseBrowserSignIn({ null }, browser) { _, _ -> },
            browserTransport = browser, browserConfigured = true,
        )

    private fun fakeNative(request: suspend (GoogleIdRequestSpec) -> GoogleSignInOutcome) = object : GoogleSignInClient {
        override suspend fun requestGoogleIdToken() = request(GoogleIdRequestSpec(clientId, "hash", true))
    }

    private class FakeBrowser : SupabaseOAuthTransport {
        var starts = 0
        var pending = false
        var clears = 0
        val exchanged = mutableListOf<String>()
        override suspend fun authorizationUrl(): String {
            starts++
            pending = true
            return "https://project.supabase.co/auth/v1/authorize?provider=google"
        }
        override suspend fun hasPendingCode() = pending
        override suspend fun exchange(code: String) { exchanged += code; pending = false }
        override suspend fun clearPendingCode() { clears++; pending = false }
    }
}
