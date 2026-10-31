package com.rushit.app.data.auth

import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.NotFoundRestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import io.github.jan.supabase.gotrue.exception.AuthErrorCode
import io.github.jan.supabase.gotrue.exception.AuthRestException
import io.github.jan.supabase.gotrue.exception.AuthSessionMissingException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** A person-readable explanation of a failure plus the class it belongs to. */
data class AuthFailure(val message: String, val kind: AuthFailureKind)

/**
 * Translates transport and SDK failures into messages that are safe to show.
 *
 * `RestException.message` is deliberately never surfaced: supabase-kt builds it from the request
 * URL *and headers*, which include the anon key and the bearer token. Only the `error` and
 * `description` fields are inspected, and only to choose a curated message.
 */
object AuthErrors {

    private const val OFFLINE = "RushIt could not reach its servers. Check your connection and try again."
    private const val SLOW = "That took too long. Check your connection and try again."
    private const val REJECTED = "Google Sign-In was not accepted. Please try again."
    private const val SESSION_GONE = "Your session has expired. Please sign in again."
    private const val POLICY = "RushIt could not change that. Your account permissions did not allow it."
    private const val UNKNOWN = "Something went wrong. Please try again."
    private const val PROVIDER_OFF =
        "Google Sign-In is not enabled for this RushIt project yet. Enable the Google provider in Supabase and try again."
    private const val RATE_LIMITED = "Too many attempts just now. Please wait a minute and try again."

    /**
     * Classify [error], rethrowing genuine coroutine cancellation so structured concurrency is not
     * broken by an error handler.
     */
    fun describeOrRethrow(error: Throwable): AuthFailure {
        if (error is CancellationException && error !is TimeoutCancellationException) throw error
        return describe(error)
    }

    fun describe(error: Throwable?): AuthFailure = when {
        error == null -> AuthFailure(UNKNOWN, AuthFailureKind.UNKNOWN)

        error is TimeoutCancellationException || error is HttpRequestTimeoutException ||
            error is SocketTimeoutException -> AuthFailure(SLOW, AuthFailureKind.TIMEOUT)

        error is AuthSessionMissingException ->
            AuthFailure(SESSION_GONE, AuthFailureKind.SESSION_EXPIRED)

        error is AuthRestException -> fromAuthErrorCode(error)

        error is HttpRequestException || error is UnknownHostException ||
            error is ConnectException || error is IOException ->
            AuthFailure(OFFLINE, AuthFailureKind.NETWORK)

        error is UnauthorizedRestException -> AuthFailure(SESSION_GONE, AuthFailureKind.SESSION_EXPIRED)

        error is NotFoundRestException -> AuthFailure(
            "RushIt could not find that account data. Please sign in again.",
            AuthFailureKind.NOT_FOUND,
        )

        error is BadRequestRestException -> fromBadRequest(error)

        error is RestException -> fromRestText(error)

        else -> AuthFailure(UNKNOWN, AuthFailureKind.UNKNOWN)
    }

    private fun fromAuthErrorCode(error: AuthRestException): AuthFailure = when (error.errorCode) {
        AuthErrorCode.SessionNotFound,
        AuthErrorCode.UserNotFound,
        AuthErrorCode.ReauthenticationNeeded,
        -> AuthFailure(SESSION_GONE, AuthFailureKind.SESSION_EXPIRED)

        AuthErrorCode.BadJwt,
        AuthErrorCode.UnexpectedAudience,
        AuthErrorCode.ValidationFailed,
        AuthErrorCode.BadJson,
        AuthErrorCode.BadOauthState,
        AuthErrorCode.BadOauthCallback,
        AuthErrorCode.ReauthenticationNotValid,
        -> AuthFailure(
            "Google rejected that sign-in token. Please sign in again.",
            AuthFailureKind.INVALID_TOKEN,
        )

        AuthErrorCode.ProviderDisabled,
        AuthErrorCode.OauthProviderNotSupported,
        AuthErrorCode.EmailProviderDisabled,
        -> AuthFailure(PROVIDER_OFF, AuthFailureKind.UNCONFIGURED)

        AuthErrorCode.OverRequestRateLimit,
        AuthErrorCode.OverEmailSendRateLimit,
        AuthErrorCode.OverSmsSendRateLimit,
        -> AuthFailure(RATE_LIMITED, AuthFailureKind.REJECTED)

        AuthErrorCode.NoAuthorization,
        AuthErrorCode.NotAdmin,
        -> AuthFailure(POLICY, AuthFailureKind.POLICY_DENIED)

        AuthErrorCode.UserBanned -> AuthFailure(
            "This account is not allowed to sign in to RushIt.",
            AuthFailureKind.REJECTED,
        )

        AuthErrorCode.UnexpectedFailure -> AuthFailure(REJECTED, AuthFailureKind.REJECTED)

        else -> fromRestText(error)
    }

    private fun fromBadRequest(error: BadRequestRestException): AuthFailure {
        val text = "${error.error} ${error.description.orEmpty()}".lowercase()
        return when {
            text.contains("row-level security") || text.contains("row level security") ->
                AuthFailure(POLICY, AuthFailureKind.POLICY_DENIED)

            text.contains("duplicate key") || text.contains("unique") ->
                AuthFailure("That username is already taken. Please choose another one.", AuthFailureKind.CONFLICT)

            text.contains("invalid login credentials") -> AuthFailure(REJECTED, AuthFailureKind.REJECTED)

            else -> fromRestText(error)
        }
    }

    /**
     * Last resort for REST failures: classify from the short `error`/`description` text only.
     *
     * The exception's own message is never used, because it embeds request headers.
     */
    private fun fromRestText(error: RestException): AuthFailure {
        val text = "${error.error} ${error.description.orEmpty()}".lowercase()
        return when {
            text.contains("row-level security") || text.contains("row level security") ||
                text.contains("permission denied") || text.contains("42501") ->
                AuthFailure(POLICY, AuthFailureKind.POLICY_DENIED)

            text.contains("duplicate key") || text.contains("unique constraint") ||
                text.contains("23505") ->
                AuthFailure("That username is already taken. Please choose another one.", AuthFailureKind.CONFLICT)

            text.contains("jwt expired") || text.contains("pgrst301") ||
                text.contains("session_not_found") || text.contains("token has expired") ->
                AuthFailure(SESSION_GONE, AuthFailureKind.SESSION_EXPIRED)

            text.contains("rate limit") || text.contains("429") ->
                AuthFailure(RATE_LIMITED, AuthFailureKind.REJECTED)

            text.contains("google") && text.contains("provider") ->
                AuthFailure(PROVIDER_OFF, AuthFailureKind.UNCONFIGURED)

            else -> AuthFailure(REJECTED, AuthFailureKind.REJECTED)
        }
    }
}
