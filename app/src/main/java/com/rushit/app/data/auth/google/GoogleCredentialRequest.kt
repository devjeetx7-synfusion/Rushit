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
    val hashedNonce: String,
    val authorizedAccountsOnly: Boolean,
)

/** Convert the tested request values into the real, non-deprecated Credential Manager request. */
internal fun googleCredentialRequest(spec: GoogleIdRequestSpec): GetCredentialRequest {
    require(spec.webClientId.isNotBlank())
    require(spec.hashedNonce.isNotBlank())
    val option = GetGoogleIdOption.Builder()
        .setServerClientId(spec.webClientId)
        .setFilterByAuthorizedAccounts(spec.authorizedAccountsOnly)
        .setAutoSelectEnabled(spec.authorizedAccountsOnly)
        .setNonce(spec.hashedNonce)
        .build()
    return GetCredentialRequest.Builder().addCredentialOption(option).build()
}

/**
 * Two-stage orchestration. NoCredentialException is a retry signal, never evidence that the
 * device has no Google account. The coordinator decides whether to try browser OAuth afterward.
 */
internal class GoogleCredentialRequest(
    private val webClientId: String,
    private val nonceFactory: () -> String = SignInNonce::newRawNonce,
    private val transport: suspend (GoogleIdRequestSpec) -> GetCredentialResponse,
) {
    suspend fun execute(): GoogleSignInOutcome {
        val clientId = webClientId.trim()
        if (clientId.isBlank()) return GoogleSignInOutcome.Unconfigured
        // Both stages belong to one attempt and use the same fresh nonce. A later tap gets a new one.
        val rawNonce = nonceFactory()
        val hashedNonce = SignInNonce.hash(rawNonce)
        return try {
            val first = try {
                googleOutcomeFrom(transport(GoogleIdRequestSpec(clientId, hashedNonce, true)), rawNonce)
            } catch (error: NoCredentialException) {
                null
            }
            if (first != null) first
            else try {
                googleOutcomeFrom(transport(GoogleIdRequestSpec(clientId, hashedNonce, false)), rawNonce)
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
