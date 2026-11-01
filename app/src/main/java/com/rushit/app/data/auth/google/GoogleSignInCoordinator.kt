package com.rushit.app.data.auth.google

import android.app.Activity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex

/**
 * Application-scoped single-flight owner for native and browser Google sign-in.
 * Activity recreation neither duplicates a request nor invalidates the SDK's persisted PKCE flow.
 */
internal class GoogleSignInCoordinator internal constructor(
    private val nativeSignIn: GoogleSignInClient,
    private val browserSignIn: SupabaseBrowserSignIn,
    private val browserTransport: SupabaseOAuthTransport,
    private val browserConfigured: Boolean,
) : GoogleSignInClient {
    private val requestMutex = Mutex()
    private val callbackMutex = Mutex()
    private var browserPending = false
    private var lastConsumedCodeHash: String? = null

    constructor(
        activityProvider: () -> Activity?,
        webClientId: String,
        browserTransport: SupabaseOAuthTransport,
        browserConfigured: Boolean,
    ) : this(
        nativeSignIn = CredentialManagerGoogleSignIn(activityProvider, webClientId),
        browserSignIn = SupabaseBrowserSignIn(activityProvider, browserTransport),
        browserTransport = browserTransport,
        browserConfigured = browserConfigured,
    )

    override suspend fun requestGoogleIdToken(): GoogleSignInOutcome {
        if (!requestMutex.tryLock()) return GoogleSignInOutcome.Failed(GoogleSignInMessages.ALREADY_SIGNING_IN)
        try {
            if (browserPending || browserTransport.hasPendingCode()) {
                return GoogleSignInOutcome.Failed(GoogleSignInMessages.ALREADY_SIGNING_IN)
            }
            val outcome = nativeSignIn.requestGoogleIdToken()
            if (outcome != GoogleSignInOutcome.NoCredentialAvailable && outcome != GoogleSignInOutcome.Unsupported) {
                return outcome
            }
            if (!browserConfigured) {
                return GoogleSignInOutcome.Failed(
                    if (outcome == GoogleSignInOutcome.Unsupported) GoogleSignInMessages.PLAY_SERVICES
                    else GoogleSignInMessages.UNKNOWN,
                )
            }
            return browserSignIn.start().also {
                browserPending = it == GoogleSignInOutcome.BrowserPending
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            return GoogleSignInOutcome.Failed(GoogleSignInMessages.UNKNOWN)
        } finally {
            requestMutex.unlock()
        }
    }

    override suspend fun completeBrowserRedirect(uri: String?): GoogleSignInOutcome? {
        val callback = SupabaseOAuthRedirect.parse(uri)
        if (callback == SupabaseOAuthRedirect.Result.Ignored) return null
        if (!callbackMutex.tryLock()) return GoogleSignInOutcome.BrowserPending
        try {
            if (callback is SupabaseOAuthRedirect.Result.Code &&
                lastConsumedCodeHash == SignInNonce.sha256Hex(callback.value)
            ) return null

            // The SDK verifier binds a callback to a request initiated by this app. It survives
            // process death; an unsolicited code without that verifier is never exchanged.
            if (!browserPending && !browserTransport.hasPendingCode()) {
                return GoogleSignInOutcome.Failed(GoogleSignInMessages.BROWSER_EXPIRED)
            }
            browserPending = false
            return when (callback) {
                SupabaseOAuthRedirect.Result.Cancelled -> {
                    browserTransport.clearPendingCode()
                    GoogleSignInOutcome.Cancelled
                }
                SupabaseOAuthRedirect.Result.Rejected -> {
                    browserTransport.clearPendingCode()
                    GoogleSignInOutcome.Failed(GoogleSignInMessages.INVALID_REDIRECT)
                }
                is SupabaseOAuthRedirect.Result.Code -> {
                    lastConsumedCodeHash = SignInNonce.sha256Hex(callback.value)
                    try {
                        browserSignIn.exchange(callback.value)
                    } finally {
                        // Codes are single-use. Failed exchanges require a fresh browser attempt.
                        browserTransport.clearPendingCode()
                    }
                }
                SupabaseOAuthRedirect.Result.Ignored -> null
            }
        } catch (error: CancellationException) {
            browserPending = false
            runCatching { browserTransport.clearPendingCode() }
            throw error
        } finally {
            callbackMutex.unlock()
        }
    }

    /** Called on foreground return; active native/exchange work is deliberately left alone. */
    override suspend fun cancelBrowserSignIn(): GoogleSignInOutcome? {
        if (requestMutex.isLocked || callbackMutex.isLocked) return null
        if (!browserPending && !browserTransport.hasPendingCode()) return null
        if (!callbackMutex.tryLock()) return null
        try {
            browserPending = false
            browserTransport.clearPendingCode()
            return GoogleSignInOutcome.Cancelled
        } finally {
            callbackMutex.unlock()
        }
    }
}
