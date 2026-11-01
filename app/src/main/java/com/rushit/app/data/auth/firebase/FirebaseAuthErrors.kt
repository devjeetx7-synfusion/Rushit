package com.rushit.app.data.auth.firebase

import com.rushit.app.data.auth.AuthFailure
import com.rushit.app.data.auth.AuthFailureKind

/** User-facing Firebase messages. Never include a token, OTP, verification id or stack trace. */
object FirebaseAuthMessages {
    const val UNCONFIGURED =
        "Sign-in is not configured in this build. RushIt needs a google-services.json for its " +
            "Firebase project. See docs/AUTH_SETUP.md."

    const val OFFLINE = "RushIt could not reach Google. Check your connection and try again."
    const val TIMEOUT = "Firebase did not answer in time. Check your connection and try again."
    const val UNKNOWN = "Sign-in did not complete. Please try again."

    const val NO_ACTIVITY = "RushIt could not open the verification screen. Please try again."
    const val INVALID_PHONE = "That phone number was not accepted. Check it and try again."
    const val INVALID_CODE = "That code is not correct. Check the SMS and try again."
    const val EXPIRED_CODE = "That code has expired. Send a new one and try again."
    const val CODE_REQUIRED = "Enter the code RushIt texted you."
    const val CODE_NOT_REQUESTED = "Request a code before entering one."
    const val TOO_MANY_REQUESTS = "Too many codes requested. Please wait a few minutes and try again."
    const val QUOTA_EXCEEDED =
        "RushIt cannot send more verification texts right now. Please try again later."
    const val BILLING =
        "SMS verification is not available for this RushIt project yet. Please try again later."
    const val APP_VERIFICATION_FAILED =
        "RushIt could not verify this app with Firebase. Reinstall the app from the Play Store and try again."
    const val PLAY_SERVICES_UNAVAILABLE =
        "Phone verification needs Google Play services, which is unavailable on this device."
    const val REQUIRES_RECENT_LOGIN =
        "For your security, please sign in again before deleting your account."
    const val USER_DISABLED = "This account is not allowed to sign in to RushIt."
    const val OPERATION_NOT_ALLOWED =
        "This sign-in method is not enabled for the RushIt Firebase project yet."
    const val MISSING_TOKEN = "Google did not return a sign-in token. Please try again."
    const val INVALID_CREDENTIAL = "That sign-in was not accepted. Please try again."
}

/**
 * Maps a Firebase error code onto a safe message and a coarse [AuthFailureKind].
 *
 * Kept as pure `String -> AuthFailure` logic, with no Firebase type in the signature, so the whole
 * error surface is unit-tested on the JVM without the SDK.
 */
object FirebaseAuthErrors {

    fun forCode(errorCode: String?, message: String? = null): AuthFailure {
        val code = errorCode?.trim()?.uppercase()
        return when (code) {
            // Phone verification
            "ERROR_INVALID_VERIFICATION_CODE", "ERROR_INVALID_CODE", "INVALID_CODE" ->
                AuthFailure(FirebaseAuthMessages.INVALID_CODE, AuthFailureKind.REJECTED)

            "ERROR_SESSION_EXPIRED", "ERROR_VERIFICATION_ID_EXPIRED", "ERROR_CODE_EXPIRED" ->
                AuthFailure(FirebaseAuthMessages.EXPIRED_CODE, AuthFailureKind.REJECTED)

            "ERROR_TOO_MANY_REQUESTS", "ERROR_QUOTA_EXCEEDED" ->
                AuthFailure(FirebaseAuthMessages.TOO_MANY_REQUESTS, AuthFailureKind.REJECTED)

            "ERROR_BILLING_NOT_ENABLED" ->
                AuthFailure(FirebaseAuthMessages.BILLING, AuthFailureKind.UNAVAILABLE)

            "ERROR_INVALID_PHONE_NUMBER", "ERROR_MISSING_PHONE_NUMBER", "ERROR_INVALID_NUMBER" ->
                AuthFailure(FirebaseAuthMessages.INVALID_PHONE, AuthFailureKind.REJECTED)

            "ERROR_MISSING_VERIFICATION_CODE", "ERROR_MISSING_VERIFICATION_ID" ->
                AuthFailure(FirebaseAuthMessages.CODE_REQUIRED, AuthFailureKind.REJECTED)

            "ERROR_MISSING_ACTIVITY", "ERROR_MISSING_APP_TOKEN", "ERROR_MISSING_APP_CREDENTIAL" ->
                AuthFailure(FirebaseAuthMessages.NO_ACTIVITY, AuthFailureKind.UNKNOWN)

            "ERROR_CAPTCHA_CHECK_FAILED", "ERROR_APP_NOT_AUTHORIZED",
            "ERROR_INVALID_APP_CREDENTIAL", "ERROR_INVALID_CERT_HASH", "ERROR_INVALID_RECAPTCHA_TOKEN",
            "ERROR_RECAPTCHA_TOKEN_EXPIRED", "ERROR_MISSING_RECAPTCHA_TOKEN",
            "ERROR_MISSING_CLIENT_IDENTIFIER", "ERROR_INVALID_CLIENT_IDENTIFIER",
            ->
                AuthFailure(FirebaseAuthMessages.APP_VERIFICATION_FAILED, AuthFailureKind.UNCONFIGURED)

            "ERROR_FAILED_TO_GET_SAFETY_NET_TOKEN_EXCEEDED_TIMEOUT" ->
                AuthFailure(FirebaseAuthMessages.APP_VERIFICATION_FAILED, AuthFailureKind.TIMEOUT)

            // Session / account state
            "ERROR_REQUIRES_RECENT_LOGIN", "ERROR_NEED_CONFIRMATION" ->
                AuthFailure(FirebaseAuthMessages.REQUIRES_RECENT_LOGIN, AuthFailureKind.SESSION_EXPIRED)

            "ERROR_USER_DISABLED", "ERROR_USER_TOKEN_EXPIRED", "ERROR_USER_MISMATCH" ->
                AuthFailure(FirebaseAuthMessages.USER_DISABLED, AuthFailureKind.REJECTED)

            "ERROR_USER_NOT_FOUND" ->
                AuthFailure(FirebaseAuthMessages.INVALID_CREDENTIAL, AuthFailureKind.NOT_FOUND)

            "ERROR_OPERATION_NOT_ALLOWED", "ERROR_ADMIN_RESTRICTED_OPERATION",
            "ERROR_INVALID_API_KEY", "ERROR_API_KEY_NOT_VALID", "ERROR_INVALID_APIKEY",
            "ERROR_APP_NOT_INSTALLED",
            ->
                AuthFailure(FirebaseAuthMessages.OPERATION_NOT_ALLOWED, AuthFailureKind.UNCONFIGURED)

            "ERROR_NETWORK_REQUEST_FAILED", "ERROR_WEB_NETWORK_REQUEST_FAILED" ->
                AuthFailure(FirebaseAuthMessages.OFFLINE, AuthFailureKind.NETWORK)

            "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_IDP_RESPONSE", "ERROR_INVALID_PROVIDER_ID",
            "ERROR_INVALID_OAUTH_ACCESS_TOKEN", "ERROR_MISSING_ID_TOKEN",
            ->
                AuthFailure(FirebaseAuthMessages.INVALID_CREDENTIAL, AuthFailureKind.INVALID_TOKEN)

            "ERROR_INTERNAL_ERROR", "ERROR_INTERNAL_SUCCESS_AND_ERROR" ->
                AuthFailure(FirebaseAuthMessages.UNKNOWN, AuthFailureKind.UNKNOWN)

            // Older SDKs surface a Play-Services message without a code we can branch on.
            else -> if (message?.contains("Play services", ignoreCase = true) == true) {
                AuthFailure(FirebaseAuthMessages.PLAY_SERVICES_UNAVAILABLE, AuthFailureKind.UNCONFIGURED)
            } else {
                AuthFailure(FirebaseAuthMessages.UNKNOWN, AuthFailureKind.UNKNOWN)
            }
        }
    }
}
