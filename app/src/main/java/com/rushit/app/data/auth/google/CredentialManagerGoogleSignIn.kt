package com.rushit.app.data.auth.google

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException

/**
 * Real Google sign-in through Jetpack Credential Manager.
 *
 * This is the only place in RushIt that touches Credential Manager, and it never fabricates a
 * successful result: [GoogleSignInOutcome.Success] is only produced from a token Google returned.
 *
 * @param activityProvider supplies the foreground Activity Credential Manager must attach its UI to;
 * returning null means there is nothing to attach to, which is reported rather than simulated
 * @param webClientId the **Web** OAuth client ID, used as the ID token audience. The Android OAuth
 * client is matched by Google using the package name and signing fingerprint, not by this value.
 */
class CredentialManagerGoogleSignIn(
    private val activityProvider: () -> Activity?,
    private val webClientId: String,
) : GoogleSignInClient {

    override suspend fun requestGoogleIdToken(): GoogleSignInOutcome {
        val clientId = webClientId.trim()
        if (clientId.isBlank()) return GoogleSignInOutcome.Unconfigured

        val activity = activityProvider()
            ?: return GoogleSignInOutcome.Failed(GoogleSignInMessages.NO_ACTIVITY)

        // The raw nonce stays here and goes to Supabase; Google receives only its SHA-256 hash.
        val rawNonce = SignInNonce.newRawNonce()
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(false)
            .setNonce(SignInNonce.hash(rawNonce))
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val response = CredentialManager.create(activity).getCredential(activity, request)
            googleOutcomeFrom(response, rawNonce)
        } catch (error: CancellationException) {
            // Respect structured concurrency; a timeout is a real failure worth reporting.
            if (error !is TimeoutCancellationException) throw error
            GoogleSignInOutcome.Failed(GoogleSignInMessages.OFFLINE, offline = true)
        } catch (error: Throwable) {
            classifyGoogleSignInFailure(error)
        }
    }
}
