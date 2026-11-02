package com.rushit.app.data.auth.google

object GoogleSignInMessages {
    const val UNCONFIGURED = "Google Sign-In is unavailable because the backend has been removed."
    const val NO_GOOGLE_ACCOUNT = "No Google account was found on this device."
    const val PLAY_SERVICES = "Google Play services is unavailable."
    const val NO_ACTIVITY = "Cannot open sign-in without a foreground screen."
}

sealed interface GoogleSignInOutcome {
    data class Success(val idToken: String) : GoogleSignInOutcome
    data object Cancelled : GoogleSignInOutcome
    data object Unconfigured : GoogleSignInOutcome
    data object NoCredentialAvailable : GoogleSignInOutcome
    data object Unsupported : GoogleSignInOutcome
    data class Failed(val message: String) : GoogleSignInOutcome
}

interface GoogleSignInClient {
    suspend fun requestGoogleIdToken(): GoogleSignInOutcome
}

class LocalGoogleSignInClient : GoogleSignInClient {
    override suspend fun requestGoogleIdToken(): GoogleSignInOutcome {
        return GoogleSignInOutcome.Failed(GoogleSignInMessages.UNCONFIGURED)
    }
}
