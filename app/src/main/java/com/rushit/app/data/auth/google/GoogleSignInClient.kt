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

/** Every message the Google sign-in path can show. Centralized so tests can assert on them. */
object GoogleSignInMessages {
    const val UNCONFIGURED =
        "Google Sign-In is not configured in this build. Set GOOGLE_WEB_CLIENT_ID to the Web OAuth " +
            "client ID used by the RushIt Supabase Google provider, then rebuild."
    const val NO_ACTIVITY = "RushIt could not open the Google account picker. Please try again."
    const val NO_ACCOUNT =
        "No Google account was found on this device. Add a Google account in Android settings and try again."
    const val UNEXPECTED_CREDENTIAL =
        "Google returned a sign-in RushIt did not ask for. Please try again."
    const val MISSING_TOKEN = "Google did not return a sign-in token. Please try again."
    const val UNREADABLE_TOKEN = "RushIt could not read the Google sign-in token. Please try again."
    const val NOT_GOOGLE_TOKEN =
        "That token was not issued by Google. Please sign in again."
    const val AUDIENCE_MISMATCH =
        "Google Sign-In is misconfigured: this build's GOOGLE_WEB_CLIENT_ID does not match the token " +
            "Google issued. Use the Web OAuth client ID from the RushIt Supabase Google provider."
    const val EXPIRED_TOKEN =
        "That Google sign-in expired before it finished. Check your device clock and try again."
    const val NONCE_MISMATCH =
        "RushIt could not verify that sign-in request. Please try again."
    const val PLAY_SERVICES =
        "This device cannot show the Google account picker. Update Google Play services and try again."
    const val OFFLINE = "RushIt could not reach Google. Check your connection and try again."
    const val UNKNOWN = "Google Sign-In did not complete. Please try again."
}

/**
 * Outcome of one Credential Manager request.
 *
 * A person dismissing the account picker is [Cancelled], which is never reported as an error.
 */
sealed interface GoogleSignInOutcome {
    /**
     * @property idToken the Google OIDC ID token, whose audience is the Web OAuth Client ID
     * @property rawNonce the un-hashed nonce; Supabase re-hashes it to defeat replay
     * @property displayName profile hint only — never treated as authoritative
     */
    data class Success(
        val idToken: String,
        val rawNonce: String,
        val displayName: String?,
        val email: String?,
        val photoUri: String?,
    ) : GoogleSignInOutcome

    data object Cancelled : GoogleSignInOutcome
    data object NoAccount : GoogleSignInOutcome
    data object Unconfigured : GoogleSignInOutcome
    data class Failed(val message: String, val offline: Boolean = false) : GoogleSignInOutcome
}

/**
 * Abstraction over Credential Manager so the sign-in orchestration can be unit-tested.
 *
 * Implementations must run on the main dispatcher: Credential Manager needs a foreground Activity.
 */
interface GoogleSignInClient {
    suspend fun requestGoogleIdToken(): GoogleSignInOutcome
}

/** Reads the Google ID token out of a Credential Manager response. */
internal fun googleOutcomeFrom(
    response: GetCredentialResponse,
    rawNonce: String,
): GoogleSignInOutcome {
    val credential = response.credential
    if (credential !is CustomCredential ||
        credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        return GoogleSignInOutcome.Failed(GoogleSignInMessages.UNEXPECTED_CREDENTIAL)
    }
    val googleCredential = runCatching { GoogleIdTokenCredential.createFrom(credential.data) }
        .getOrElse { return GoogleSignInOutcome.Failed(GoogleSignInMessages.UNREADABLE_TOKEN) }

    val idToken = googleCredential.idToken
    if (idToken.isBlank()) {
        return GoogleSignInOutcome.Failed(GoogleSignInMessages.MISSING_TOKEN)
    }
    return GoogleSignInOutcome.Success(
        idToken = idToken,
        rawNonce = rawNonce,
        displayName = googleCredential.displayName,
        email = googleCredential.email,
        photoUri = googleCredential.photoUri?.toString(),
    )
}

/**
 * Maps a Credential Manager failure onto an outcome.
 *
 * Separated from the Android call site so the classification itself is unit-testable. Exception
 * messages are never shown verbatim: they can contain the OAuth client ID and internal state.
 */
internal fun classifyGoogleSignInFailure(error: Throwable): GoogleSignInOutcome = when (error) {
    // The person backed out of the account picker. Not an error, and never reported as one.
    is GetCredentialCancellationException -> GoogleSignInOutcome.Cancelled

    is NoCredentialException -> GoogleSignInOutcome.NoAccount

    is GetCredentialUnsupportedException,
    is GetCredentialProviderConfigurationException,
    -> GoogleSignInOutcome.Failed(GoogleSignInMessages.PLAY_SERVICES)

    is GetCredentialUnknownException -> GoogleSignInOutcome.Failed(GoogleSignInMessages.UNKNOWN)

    is IOException -> GoogleSignInOutcome.Failed(GoogleSignInMessages.OFFLINE, offline = true)

    is GetCredentialException -> GoogleSignInOutcome.Failed(GoogleSignInMessages.UNKNOWN)

    else -> GoogleSignInOutcome.Failed(GoogleSignInMessages.UNKNOWN)
}
