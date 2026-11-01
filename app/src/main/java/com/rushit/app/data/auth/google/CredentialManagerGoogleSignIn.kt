package com.rushit.app.data.auth.google

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse

/**
 * Real Google sign-in through Jetpack Credential Manager, not the deprecated GoogleSignIn API.
 * Each stage resolves the current foreground Activity again so rotation cannot reuse a dead host.
 */
class CredentialManagerGoogleSignIn(
    private val activityProvider: () -> Activity?,
    private val webClientId: String,
    private val getCredential: suspend (Activity, GetCredentialRequest) -> GetCredentialResponse = { activity, request ->
        CredentialManager.create(activity).getCredential(activity, request)
    },
) : GoogleSignInClient {
    override suspend fun requestGoogleIdToken(): GoogleSignInOutcome {
        if (webClientId.isBlank()) return GoogleSignInOutcome.Unconfigured
        return GoogleCredentialRequest(webClientId) { spec ->
            val activity = foregroundActivity(activityProvider) ?: return@GoogleCredentialRequest noActivity()
            googleCredentialRequest(spec).let { request ->
                getCredential(activity, request).also {
                    // Do not accept a late response through a destroyed host after recreation.
                    if (foregroundActivity(activityProvider) !== activity) return@GoogleCredentialRequest noActivity()
                }
            }
        }.execute()
    }

    private fun noActivity(): GetCredentialResponse = throw NoForegroundActivityException()

    internal class NoForegroundActivityException : Exception(GoogleSignInMessages.NO_ACTIVITY)
}
