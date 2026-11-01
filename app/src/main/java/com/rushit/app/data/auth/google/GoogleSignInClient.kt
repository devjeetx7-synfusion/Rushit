package com.rushit.app.data.auth.google

import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import java.io.IOException

/** User-facing messages. Never include a token, nonce, authorization code or provider exception. */
object GoogleSignInMessages {
    const val UNCONFIGURED =
        "Google Sign-In is not configured in this build. Set GOOGLE_WEB_CLIENT_ID to the Web OAuth " +
            "client ID used by the RushIt Supabase Google provider, then rebuild."
    const val NO_ACTIVITY = "RushIt could not open the Google account picker. Please try again."
    const val UNEXPECTED_CREDENTIAL = "Google returned a sign-in RushIt did not ask for. Please try again."
    const val MISSING_TOKEN = "Google did not return a sign-in token. Please try again."
    const val UNREADABLE_TOKEN = "RushIt could not read the Google sign-in token. Please try again."
    const val NOT_GOOGLE_TOKEN = "That token was not issued by Google. Please sign in again."
    const val AUDIENCE_MISMATCH =
        "Google Sign-In is misconfigured: this build's GOOGLE_WEB_CLIENT_ID does not match the token " +
            "Google issued. Use the Web OAuth client ID from the RushIt Supabase Google provider."
    const val EXPIRED_TOKEN = "That Google sign-in expired before it finished. Check your device clock and try again."
    const val NONCE_MISMATCH = "RushIt could not verify that sign-in request. Please try again."
    const val PLAY_SERVICES = "The Google account picker is unavailable on this device."
    const val OFFLINE = "RushIt could not reach Google. Check your connection and try again."
    const val UNKNOWN = "Google Sign-In did not complete. Please try again."
    const val ALREADY_SIGNING_IN = "A Google sign-in is already in progress. Finish or cancel it first."
    const val BROWSER_UNAVAILABLE =
        "RushIt could not open a browser for Google Sign-In. Install or enable a browser and try again."
    const val BROWSER_FAILED = "Browser Google Sign-In did not complete. Please try again."
    const val BROWSER_EXPIRED = "That browser sign-in expired or was already used. Please start again."
    const val BROWSER_REJECTED = "Google or Supabase could not complete browser sign-in. Please try again."
    const val INVALID_REDIRECT = "RushIt could not verify the browser sign-in callback. Please start again."
}

/** Outcomes of an entire sign-in attempt, including the official Supabase browser fallback. */
sealed interface GoogleSignInOutcome {
    /** A native Google ID token, to be validated again by the ViewModel before Supabase exchange. */
    data class Success(
        val idToken: String,
        val rawNonce: String,
        val displayName: String?,
        val email: String?,
        val photoUri: String?,
    ) : GoogleSignInOutcome

    /** Supabase SDK imported a session after verifying the browser OAuth code and PKCE verifier. */
    data object BrowserSuccess : GoogleSignInOutcome
    data object BrowserPending : GoogleSignInOutcome
    data object Cancelled : GoogleSignInOutcome
    data object Unconfigured : GoogleSignInOutcome

    /**
     * No credential matched this request. This is NOT evidence that the device has no account:
     * an authorized-only request normally returns this for an account that has not authorized RushIt.
     */
    data object NoCredentialAvailable : GoogleSignInOutcome
    data object Unsupported : GoogleSignInOutcome
    data class Failed(val message: String, val offline: Boolean = false) : GoogleSignInOutcome
}

interface GoogleSignInClient {
    suspend fun requestGoogleIdToken(): GoogleSignInOutcome

    /** Returns null for a URI outside RushIt's registered authentication callback namespace. */
    suspend fun completeBrowserRedirect(uri: String?): GoogleSignInOutcome? = null

    /** Returns Cancelled only if a pending browser attempt was actually abandoned. */
    suspend fun cancelBrowserSignIn(): GoogleSignInOutcome? = null
}

/** Reads a Google ID token from Credential Manager without trusting a different credential type. */
internal fun googleOutcomeFrom(response: GetCredentialResponse, rawNonce: String): GoogleSignInOutcome {
    val credential = response.credential
    if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        return GoogleSignInOutcome.Failed(GoogleSignInMessages.UNEXPECTED_CREDENTIAL)
    }
    val googleCredential = runCatching { GoogleIdTokenCredential.createFrom(credential.data) }
        .getOrElse { return GoogleSignInOutcome.Failed(GoogleSignInMessages.UNREADABLE_TOKEN) }
    val idToken = googleCredential.idToken
    if (idToken.isBlank()) return GoogleSignInOutcome.Failed(GoogleSignInMessages.MISSING_TOKEN)
    val claims = GoogleIdTokenClaims.parse(idToken)
    return GoogleSignInOutcome.Success(
        idToken = idToken,
        rawNonce = rawNonce,
        displayName = googleCredential.displayName ?: claims?.name,
        email = claims?.email,
        photoUri = claims?.picture,
    )
}

/** Keep transport internals out of UI messages; only the orchestrator decides whether to fallback. */
internal fun classifyGoogleSignInFailure(error: Throwable): GoogleSignInOutcome = when (error) {
    is CredentialManagerGoogleSignIn.NoForegroundActivityException ->
        GoogleSignInOutcome.Failed(GoogleSignInMessages.NO_ACTIVITY)
    is GetCredentialCancellationException -> GoogleSignInOutcome.Cancelled
    is NoCredentialException -> GoogleSignInOutcome.NoCredentialAvailable
    is GetCredentialUnsupportedException,
    is GetCredentialProviderConfigurationException,
    -> GoogleSignInOutcome.Unsupported
    is IOException -> GoogleSignInOutcome.Failed(GoogleSignInMessages.OFFLINE, offline = true)
    is GetCredentialUnknownException,
    is GetCredentialException,
    -> GoogleSignInOutcome.Failed(GoogleSignInMessages.UNKNOWN)
    else -> GoogleSignInOutcome.Failed(GoogleSignInMessages.UNKNOWN)
}
