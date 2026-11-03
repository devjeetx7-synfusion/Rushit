package com.rushit.app.data.auth.google

import android.app.Activity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex

/**
 * Application-scoped single-flight owner for Google sign-in.
 *
 * Activity recreation neither duplicates a request nor leaves a stale in-flight flag behind, and a
 * second tap while the picker is open returns [GoogleSignInMessages.ALREADY_SIGNING_IN] instead of
 * starting a second credential request.
 */
internal class GoogleSignInCoordinator internal constructor(
    private val nativeSignIn: GoogleSignInClient,
) : GoogleSignInClient {

    private val requestMutex = Mutex()

    constructor(activityProvider: () -> Activity?, webClientId: String) : this(
        nativeSignIn = CredentialManagerGoogleSignIn(activityProvider, webClientId),
    )

    override suspend fun requestGoogleIdToken(): GoogleSignInOutcome {
        if (!requestMutex.tryLock()) {
            return GoogleSignInOutcome.Failed(GoogleSignInMessages.ALREADY_SIGNING_IN)
        }
        return try {
            nativeSignIn.requestGoogleIdToken()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            GoogleSignInOutcome.Failed(GoogleSignInMessages.UNKNOWN)
        } finally {
            requestMutex.unlock()
        }
    }
}
