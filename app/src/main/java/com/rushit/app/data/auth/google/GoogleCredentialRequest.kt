package com.rushit.app.data.auth.google

import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
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
 * Google through the package name plus the APK signing fingerprint, so nothing else is needed here.
 */
internal fun googleCredentialRequest(spec: GoogleIdRequestSpec): GetCredentialRequest {
    require(spec.webClientId.isNotBlank())
    val option = GetGoogleIdOption.Builder()
        .setServerClientId(spec.webClientId)
        .setFilterByAuthorizedAccounts(spec.authorizedAccountsOnly)
        .setAutoSelectEnabled(spec.authorizedAccountsOnly)
        .build()
    return GetCredentialRequest.Builder().addCredentialOption(option).build()
}

/**
 * Two-stage orchestration.
 *
 * The first request only offers Google accounts that already authorized RushIt, which keeps the
 * picker short for returning people. `NoCredentialException` there is a retry signal, not evidence
 * that the device has no Google account, so the second request drops that filter.
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
