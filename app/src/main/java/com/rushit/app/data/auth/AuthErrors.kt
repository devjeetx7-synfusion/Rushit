package com.rushit.app.data.auth

import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.NotFoundRestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
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
 * Thrown by a gateway when it has already produced a failure that is safe to show.
 *
 * Keeping the classification inside the adapter is what allows [AuthErrors] and every unit test to
 * work without the Firebase SDK on the classpath.
 */
class AuthGatewayException(val failure: AuthFailure) : Exception(failure.message)

/**
 * Translates transport and SDK failures into messages that are safe to show.
 *
 * `RestException.message` is deliberately never surfaced: supabase-kt builds it from the request
 * URL *and headers*, which include the publishable key and the bearer token. Only the `error` and
 * `description` fields are inspected, and only to choose a curated message.
 */
object AuthErrors {

    const val OFFLINE = "RushIt could not reach its servers. Check your connection and try again."
    const val SLOW = "That took too long. Check your connection and try again."
    const val SESSION_GONE = "Your session has expired. Please sign in again."
    const val POLICY = "RushIt could not change that. Your account permissions did not allow it."
    const val UNKNOWN = "Something went wrong. Please try again."
    const val RATE_LIMITED = "Too many attempts just now. Please wait a minute and try again."
    const val USERNAME_TAKEN = "That username is already taken. Please choose another one."

    /**
     * Shown when Supabase refuses an authenticated request.
     *
     * With Firebase as the identity provider this is almost always a project configuration gap —
     * the third-party Firebase integration, or the `role: "authenticated"` custom claim, is missing —
     * so the message names that instead of blaming the person's account.
     */
    const val BACKEND_NOT_AUTHORIZED =
        "RushIt could not authorize your account with its database. The Supabase project is not " +
            "configured for Firebase sign-in yet (Third-party Auth → Firebase, plus the " +
            "\"role\": \"authenticated\" custom claim). Please contact RushIt support."

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

        // An adapter already classified this failure; trust it verbatim.
        error is AuthGatewayException -> error.failure

        error is TimeoutCancellationException || error is HttpRequestTimeoutException ||
            error is SocketTimeoutException -> AuthFailure(SLOW, AuthFailureKind.TIMEOUT)

        error is HttpRequestException || error is UnknownHostException ||
            error is ConnectException || error is IOException ->
            AuthFailure(OFFLINE, AuthFailureKind.NETWORK)

        error is UnauthorizedRestException -> AuthFailure(BACKEND_NOT_AUTHORIZED, AuthFailureKind.POLICY_DENIED)

        error is NotFoundRestException ->
            AuthFailure("RushIt could not find that account data. Please sign in again.", AuthFailureKind.NOT_FOUND)

        error is BadRequestRestException -> fromBadRequest(error)

        error is RestException -> fromRestText(error)

        else -> AuthFailure(UNKNOWN, AuthFailureKind.UNKNOWN)
    }

    private fun fromBadRequest(error: BadRequestRestException): AuthFailure {
        val text = "${error.error} ${error.description.orEmpty()}".lowercase()
        return when {
            text.contains("row-level security") || text.contains("row level security") ||
                text.contains("42501") || text.contains("permission denied") ->
                AuthFailure(BACKEND_NOT_AUTHORIZED, AuthFailureKind.POLICY_DENIED)

            text.contains("duplicate key") || text.contains("unique") ->
                AuthFailure(USERNAME_TAKEN, AuthFailureKind.CONFLICT)

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
                AuthFailure(BACKEND_NOT_AUTHORIZED, AuthFailureKind.POLICY_DENIED)

            text.contains("duplicate key") || text.contains("unique constraint") ||
                text.contains("23505") ->
                AuthFailure(USERNAME_TAKEN, AuthFailureKind.CONFLICT)

            text.contains("jwt expired") || text.contains("pgrst301") ||
                text.contains("token has expired") || text.contains("jwt") ->
                AuthFailure(SESSION_GONE, AuthFailureKind.SESSION_EXPIRED)

            text.contains("rate limit") || text.contains("429") ->
                AuthFailure(RATE_LIMITED, AuthFailureKind.REJECTED)

            else -> AuthFailure(UNKNOWN, AuthFailureKind.UNKNOWN)
        }
    }
}
