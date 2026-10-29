package com.rushit.app.data.auth

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL

internal fun interface AuthTransport {
    fun request(method: String, path: String, body: JSONObject?, token: String?): AuthResponse
}

internal data class AuthResponse(val code: Int, val body: String) {
    fun sessionOrNull(): StoredSession? = runCatching {
        val json = JSONObject(body)
        val access = json.getString("access_token")
        val refresh = json.getString("refresh_token")
        require(access.isNotBlank() && refresh.isNotBlank())
        StoredSession(access, refresh)
    }.getOrNull()

    fun toFailure(isVerification: Boolean): AuthResult.Failure = AuthResult.Failure(when (code) {
        400, 422 -> if (isVerification) "That code is invalid or expired. Check it or request another." else "This number could not receive a code. Check it and try again, or contact support."
        401, 403 -> "Phone sign-in is unavailable. Check the Supabase project and public key configuration."
        429 -> "Too many attempts. Please wait a little before trying again."
        in 500..599 -> "The sign-in service is temporarily unavailable. Please try again."
        else -> "We couldn't ${if (isVerification) "verify the code" else "send a code"}. Please try again."
    })
}

/**
 * Supabase Auth's documented /otp, /verify, /user, /token and /logout endpoints.
 * Only a public anon/publishable key belongs in the client. RLS still applies to all data.
 */
class SupabaseAuthRepository internal constructor(
    private val store: SessionStore,
    private val projectUrl: String,
    private val publicKey: String,
    private val transport: AuthTransport? = null,
) : AuthRepository {
    constructor(context: Context, projectUrl: String, publicKey: String) :
        this(SecureSessionStore(context.applicationContext), projectUrl, publicKey)

    private val baseUrl: String? = runCatching {
        val uri = URI(projectUrl.trim())
        require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null && uri.query == null && uri.fragment == null)
        projectUrl.trim().trimEnd('/')
    }.getOrNull()
    private val configured get() = baseUrl != null && publicKey.isNotBlank()

    override suspend fun restoreSession(): SessionResult = withContext(Dispatchers.IO) {
        val session = try { store.read() } catch (_: Exception) {
            store.clear()
            return@withContext SessionResult.Unauthenticated("Your saved session could not be restored. Please sign in again.")
        } ?: return@withContext SessionResult.Unauthenticated()
        if (!configured) return@withContext SessionResult.Unauthenticated("Sign-in is not configured on this build.")
        try {
            val user = call("GET", "/user", token = session.accessToken)
            when {
                user.code in 200..299 -> SessionResult.Authenticated
                user.code == 401 || user.code == 403 -> {
                    val refreshed = call("POST", "/token?grant_type=refresh_token", JSONObject().put("refresh_token", session.refreshToken))
                    val newSession = refreshed.sessionOrNull()
                    if (refreshed.code in 200..299 && newSession != null) {
                        store.write(newSession)
                        SessionResult.Authenticated
                    } else if (refreshed.code == 400 || refreshed.code == 401 || refreshed.code == 403) {
                        store.clear()
                        SessionResult.Unauthenticated("Your session has expired. Please sign in again.")
                    } else {
                        SessionResult.Unauthenticated("Could not restore your session. Check your connection and try again.")
                    }
                }
                else -> SessionResult.Unauthenticated("Could not restore your session. Please try again.")
            }
        } catch (_: Exception) {
            // Keep the encrypted session so a transient network failure does not destroy it.
            SessionResult.Unauthenticated("Could not restore your session. Check your connection and try again.")
        }
    }

    override suspend fun requestOtp(phone: String): AuthResult = withContext(Dispatchers.IO) {
        if (!configured) return@withContext missingConfiguration()
        perform {
            val response = call("POST", "/otp", JSONObject().put("phone", phone).put("create_user", true))
            if (response.code in 200..299) AuthResult.Success else response.toFailure(isVerification = false)
        }
    }

    override suspend fun verifyOtp(phone: String, token: String): AuthResult = withContext(Dispatchers.IO) {
        if (!configured) return@withContext missingConfiguration()
        perform {
            val response = call("POST", "/verify", JSONObject().put("phone", phone).put("token", token).put("type", "sms"))
            if (response.code !in 200..299) return@perform response.toFailure(isVerification = true)
            val session = response.sessionOrNull()
                ?: return@perform AuthResult.Failure("Sign-in could not be completed. Please request a new code.")
            try {
                store.write(session)
                AuthResult.Success
            } catch (_: Exception) {
                AuthResult.Failure("We couldn't save your session securely. Please try again.")
            }
        }
    }

    override suspend fun signOut(): AuthResult = withContext(Dispatchers.IO) {
        var readFailed = false
        val session = try { store.read() } catch (_: Exception) { readFailed = true; null }
        val revoked = when {
            readFailed -> false
            session == null -> true
            !configured -> false
            else -> try { call("POST", "/logout", token = session.accessToken).code in 200..299 } catch (_: Exception) { false }
        }
        store.clear()
        if (revoked) AuthResult.Success else AuthResult.Failure("Signed out on this device, but the server session could not be revoked. Check your connection.")
    }

    private fun missingConfiguration() = AuthResult.Failure(
        "Phone sign-in isn't configured yet. Ask the app administrator to configure Supabase and SMS delivery."
    )

    private inline fun perform(block: () -> AuthResult): AuthResult = try {
        block()
    } catch (_: SocketTimeoutException) {
        AuthResult.Failure("The request timed out. Check your connection and try again.")
    } catch (_: IOException) {
        AuthResult.Failure("Couldn't connect. Check your internet connection and try again.")
    } catch (_: Exception) {
        AuthResult.Failure("Something went wrong. Please try again.")
    }

    private fun call(method: String, path: String, body: JSONObject? = null, token: String? = null): AuthResponse {
        if (transport != null) return transport.request(method, path, body, token)
        val connection = URL("${requireNotNull(baseUrl)}/auth/v1$path").openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.requestMethod = method
            connection.connectTimeout = 4_000
            connection.readTimeout = 4_000
            connection.setRequestProperty("apikey", publicKey)
            connection.setRequestProperty("Authorization", "Bearer ${token ?: publicKey}")
            connection.setRequestProperty("Accept", "application/json")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            return AuthResponse(code, responseBody)
        } finally {
            connection.disconnect()
        }
    }
}
