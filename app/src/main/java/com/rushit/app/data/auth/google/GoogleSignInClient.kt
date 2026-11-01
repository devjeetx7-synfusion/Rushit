package com.rushit.app.data.auth.google

import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.io.IOException

/** User-facing messages. Never include a token, nonce, or provider exception. */
object GoogleSignInMessages {
    const val UNCONFIGURED =
        "Google Sign-In is not configured in this build. Set GOOGLE_WEB_CLIENT_ID to the Web OAuth " +
            "client ID of the RushIt Firebase project, then rebuild."
    const val NO_ACTIVITY = "RushIt could not open the Google account picker. Please try again."
    const val UNEXPECTED_CREDENTIAL = "Google returned a sign-in RushIt did not ask for. Please try again."
    const val MISSING_TOKEN = "Google did not return a sign-in token. Please try again."
    const val UNREADABLE_TOKEN = "RushIt could not read the Google sign-in token. Please try again."
    const val PLAY_SERVICES = "The Google account picker is unavailable on this device."
    const val NO_GOOGLE_ACCOUNT =
        "No Google account is signed in on this device. Add one in Android settings, or continue " +
            "with your phone number."
    const val OFFLINE = "RushIt could not reach Google. Check your connection and try again."
    const val UNKNOWN = "Google Sign-In did not complete. Please try again."
    const val ALREADY_SIGNING_IN = "A Google sign-in is already in progress. Finish or cancel it first."
}

/**
 * Outcome of one Google account-picker attempt.
 *
 * [Success] carries a Google OIDC ID token, which is handed to Firebase Authentication and nowhere
 * else: no Google credential is ever sent to Supabase, and no browser OAuth flow exists any more.
 */
sealed interface GoogleSignInOutcome {
    data class Success(val idToken: String) : GoogleSignInOutcome

    /** The person dismissed the account picker. A decision, never an error. */
    data object Cancelled : GoogleSignInOutcome

    data object Unconfigured : GoogleSignInOutcome

    /**
     * No credential matched this request. This is NOT evidence that the device has no account:
     * an authorized-accounts-only request normally returns this for an account that has not used
     * RushIt before, which is why the client retries without that filter.
     */
    data object NoCredentialAvailable : GoogleSignInOutcome

    data object Unsupported : GoogleSignInOutcome

    data class Failed(val message: String, val offline: Boolean = false) : GoogleSignInOutcome
}

interface GoogleSignInClient {
    /** Open the Google account picker and return an ID token for the selected account. */
    suspend fun requestGoogleIdToken(): GoogleSignInOutcome
}

/** Reads a Google ID token from Credential Manager without trusting a different credential type. */
internal fun googleOutcomeFrom(response: GetCredentialResponse): GoogleSignInOutcome {
    val credential = response.credential
    if (credential !is CustomCredential ||
        credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        return GoogleSignInOutcome.Failed(GoogleSignInMessages.UNEXPECTED_CREDENTIAL)
    }
    val rawIdToken = credential.data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN")
    if (rawIdToken.isNullOrBlank()) return GoogleSignInOutcome.Failed(GoogleSignInMessages.MISSING_TOKEN)
    val googleCredential = runCatching { GoogleIdTokenCredential.createFrom(credential.data) }
        .getOrElse { return GoogleSignInOutcome.Failed(GoogleSignInMessages.UNREADABLE_TOKEN) }
    val idToken = googleCredential.idToken
    if (idToken.isBlank()) return GoogleSignInOutcome.Failed(GoogleSignInMessages.MISSING_TOKEN)
    // The token's signature, audience and expiry are verified by Firebase Authentication, so there is
    // no local claim inspection to get wrong (or to log).
    return GoogleSignInOutcome.Success(idToken)
}

/** Keep transport internals out of UI messages. */
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
