package com.rushit.app.data.auth.google

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse

/**
 * Real Google sign-in through Jetpack Credential Manager.
 *
 * Each stage resolves the current foreground Activity again, so a configuration change cannot reuse
 * a destroyed host. The returned ID token goes straight to Firebase Authentication.
 */
class CredentialManagerGoogleSignIn(
    private val activityProvider: () -> Activity?,
    private val webClientId: String,
    private val getCredential: suspend (Activity, GetCredentialRequest) -> GetCredentialResponse =
        { activity, request -> CredentialManager.create(activity).getCredential(activity, request) },
) : GoogleSignInClient {

    override suspend fun requestGoogleIdToken(): GoogleSignInOutcome {
        if (webClientId.isBlank()) return GoogleSignInOutcome.Unconfigured
        return GoogleCredentialRequest(webClientId) { spec ->
            val activity = activeActivity(activityProvider)
                ?: return@GoogleCredentialRequest noActivity()
            val request = googleCredentialRequest(spec)
            val response = getCredential(activity, request)
            if (activity.isFinishing || activity.isDestroyed) {
                return@GoogleCredentialRequest noActivity()
            }
            response
        }.execute()
    }

    private fun noActivity(): GetCredentialResponse = throw NoForegroundActivityException()

    internal class NoForegroundActivityException : Exception(GoogleSignInMessages.NO_ACTIVITY)
}

/** Resolves the current visible or started host, or null when none can legitimately receive a result. */
internal fun activeActivity(provider: () -> Activity?): Activity? =
    provider()?.takeIf {
        !it.isFinishing && !it.isDestroyed &&
            (it as? androidx.lifecycle.LifecycleOwner)?.lifecycle?.currentState
                ?.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED) == true
    }
