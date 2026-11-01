package com.rushit.app.data.auth.google

import android.app.Activity
import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.rushit.app.data.auth.AuthErrors
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

/** The official Supabase SDK operations used by the browser fallback; no hand-built OAuth requests. */
interface SupabaseOAuthTransport {
    suspend fun authorizationUrl(): String
    suspend fun hasPendingCode(): Boolean
    suspend fun exchange(code: String)
    suspend fun clearPendingCode()
}

internal class OfficialSupabaseOAuthTransport(private val client: SupabaseClient) : SupabaseOAuthTransport {
    override suspend fun authorizationUrl(): String {
        // Auth is configured for PKCE. The SDK generates the verifier/challenge and persists the
        // verifier so a callback can still be exchanged after Android recreates the process.
        client.auth.codeVerifierCache.deleteCodeVerifier()
        val url = client.auth.getOAuthUrl(Google, redirectUrl = SupabaseOAuthRedirect.URL)
        // supabase-kt 2.5.0 saves its generated verifier asynchronously. Do not launch the browser
        // until that save is visible, or a quick process death could lose the PKCE verifier.
        withTimeout(5_000L) {
            while (client.auth.codeVerifierCache.loadCodeVerifier().isNullOrBlank()) delay(10L)
        }
        return url
    }

    override suspend fun hasPendingCode(): Boolean =
        !client.auth.codeVerifierCache.loadCodeVerifier().isNullOrBlank()

    override suspend fun exchange(code: String) {
        // The SDK sends the stored PKCE verifier to Supabase and imports the resulting session.
        // No Google token endpoint, client secret, implicit token, or custom token exchange is used.
        client.auth.exchangeCodeForSession(code)
    }

    override suspend fun clearPendingCode() {
        client.auth.codeVerifierCache.deleteCodeVerifier()
    }
}

/** Foreground-only browser launch. Custom Tabs falls back to the user's browser if necessary. */
internal class SupabaseBrowserSignIn(
    private val activityProvider: () -> Activity?,
    private val transport: SupabaseOAuthTransport,
    private val launchBrowser: (Activity, String) -> Unit = ::launchCustomTab,
) {
    suspend fun start(): GoogleSignInOutcome {
        val activity = foregroundActivity(activityProvider)
            ?: return GoogleSignInOutcome.Failed(GoogleSignInMessages.NO_ACTIVITY)
        return try {
            val url = transport.authorizationUrl()
            // Recheck after the suspending SDK call: the original Activity may have been destroyed.
            if (foregroundActivity(activityProvider) !== activity) {
                transport.clearPendingCode()
                return GoogleSignInOutcome.Failed(GoogleSignInMessages.NO_ACTIVITY)
            }
            launchBrowser(activity, url)
            GoogleSignInOutcome.BrowserPending
        } catch (error: CancellationException) {
            transport.clearPendingCode()
            throw error
        } catch (error: ActivityNotFoundException) {
            transport.clearPendingCode()
            GoogleSignInOutcome.Failed(GoogleSignInMessages.BROWSER_UNAVAILABLE)
        } catch (error: Throwable) {
            transport.clearPendingCode()
            GoogleSignInOutcome.Failed(AuthErrors.describeOrRethrow(error).message)
        }
    }

    suspend fun exchange(code: String): GoogleSignInOutcome = try {
        transport.exchange(code)
        GoogleSignInOutcome.BrowserSuccess
    } catch (error: CancellationException) {
        if (error !is TimeoutCancellationException) throw error
        GoogleSignInOutcome.Failed(GoogleSignInMessages.OFFLINE, offline = true)
    } catch (error: Throwable) {
        GoogleSignInOutcome.Failed(AuthErrors.describeOrRethrow(error).message)
    } finally {
        // The SDK deletes the verifier on success. Also clear it after a failed/timed-out exchange
        // so an old single-use code cannot be retried with a stale verifier.
        transport.clearPendingCode()
    }

    suspend fun clear() {
        transport.clearPendingCode()
    }
}

internal fun foregroundActivity(provider: () -> Activity?): Activity? =
    provider()?.takeIf {
        !it.isFinishing && !it.isDestroyed &&
            (it as? androidx.lifecycle.LifecycleOwner)?.lifecycle?.currentState
                ?.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) == true
    }

private fun launchCustomTab(activity: Activity, url: String) {
    val intent = CustomTabsIntent.Builder().setShowTitle(true).build()
    intent.launchUrl(activity, Uri.parse(url))
}
