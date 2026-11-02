package com.rushit.app.data.auth

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
 */
class AuthGatewayException(val failure: AuthFailure) : Exception(failure.message)

/**
 * Translates transport and local failures into messages that are safe to show.
 */
object AuthErrors {

    const val OFFLINE = "RushIt could not reach its servers. Check your connection and try again."
    const val SLOW = "That took too long. Check your connection and try again."
    const val SESSION_GONE = "Your session has expired. Please sign in again."
    const val POLICY = "RushIt could not change that. Your account permissions did not allow it."
    const val UNKNOWN = "Something went wrong. Please try again."
    const val RATE_LIMITED = "Too many attempts just now. Please wait a minute and try again."
    const val USERNAME_TAKEN = "That username is already taken. Please choose another one."
    const val AUTH_BACKEND_REMOVED =
        "Cloud authentication backend has been removed from this build. Account access is unavailable."

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

        error is AuthGatewayException -> error.failure

        error is TimeoutCancellationException || error is SocketTimeoutException ->
            AuthFailure(SLOW, AuthFailureKind.TIMEOUT)

        error is UnknownHostException || error is ConnectException || error is IOException ->
            AuthFailure(OFFLINE, AuthFailureKind.NETWORK)

        else -> AuthFailure(UNKNOWN, AuthFailureKind.UNKNOWN)
    }
}
