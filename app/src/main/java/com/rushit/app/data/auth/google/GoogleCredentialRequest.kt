package com.rushit.app.data.auth.google

import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException

/** Pure request values, inspectable in JVM tests without an Android Bundle implementation. */
internal data class GoogleIdRequestSpec(
    val webClientId: String,
    val authorizedAccountsOnly: Boolean,
)

/**
 * Convert the tested request values into the real, non-deprecated Credential Manager request.
 *
 * `setServerClientId` takes the **Web** OAuth client ID: it is the audience Google puts into the ID
 * token, and Firebase Authentication verifies that audience. The matching Android client is found by
 * Google through the package name plus the APK signing fingerprint.
 */
internal fun googleCredentialRequest(spec: GoogleIdRequestSpec): GetCredentialRequest {
    require(spec.webClientId.isNotBlank())
    val builder = GetCredentialRequest.Builder()
    if (spec.authorizedAccountsOnly) {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(spec.webClientId)
            .setFilterByAuthorizedAccounts(true)
            .setAutoSelectEnabled(true)
            .build()
        builder.addCredentialOption(option)
    } else {
        // Stage 2 (button flow / unrestricted account chooser):
        // According to official Google Identity documentation and issue tracker 341690734,
        // GetSignInWithGoogleOption prevents TransactionTooLargeException on Android 14+ / Android 16
        // when multiple Google accounts are present on the device.
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(spec.webClientId).build()
        val unrestrictedGoogleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(spec.webClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        builder.addCredentialOption(signInWithGoogleOption)
        builder.addCredentialOption(unrestrictedGoogleIdOption)
    }
    return builder.build()
}

/**
 * Two-stage orchestration.
 *
 * The first request only offers Google accounts that already authorized RushIt, which keeps the
 * picker short for returning people. `NoCredentialException` there is a retry signal, not evidence
 * that the device has no Google account, so the second request drops that filter to open the
 * unrestricted account picker.
 */
internal class GoogleCredentialRequest(
    private val webClientId: String,
    private val transport: suspend (GoogleIdRequestSpec) -> GetCredentialResponse,
) {
    suspend fun execute(): GoogleSignInOutcome {
        val clientId = webClientId.trim()
        if (clientId.isBlank()) return GoogleSignInOutcome.Unconfigured
        return try {
            val first = try {
                googleOutcomeFrom(transport(GoogleIdRequestSpec(clientId, authorizedAccountsOnly = true)))
            } catch (error: NoCredentialException) {
                null
            }
            if (first != null) first
            else try {
                googleOutcomeFrom(transport(GoogleIdRequestSpec(clientId, authorizedAccountsOnly = false)))
            } catch (error: NoCredentialException) {
                GoogleSignInOutcome.NoCredentialAvailable
            }
        } catch (error: CancellationException) {
            if (error !is TimeoutCancellationException) throw error
            GoogleSignInOutcome.Failed(GoogleSignInMessages.OFFLINE, offline = true)
        } catch (error: Throwable) {
            classifyGoogleSignInFailure(error)
        }
    }
}
