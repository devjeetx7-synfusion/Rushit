package com.rushit.app.data.auth

import io.github.jan.supabase.gotrue.SessionManager
import io.github.jan.supabase.gotrue.user.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Bridges the Supabase SDK's session lifecycle to encrypted local storage.
 *
 * The SDK stays the single owner of the session: it loads, refreshes, rotates and expires tokens,
 * and coalesces concurrent refreshes through its own auto-refresh job. This class only decides where
 * the serialized `UserSession` lives, replacing the SDK default of plaintext SharedPreferences.
 *
 * `encodeDefaults` must stay true: `UserSession.expiresAt` is a defaulted property, and dropping it
 * would make every restored session look like it expires immediately.
 */
internal class EncryptedSessionManager(
    private val store: SessionStore,
    private val json: Json = sessionJson,
) : SessionManager {

    override suspend fun saveSession(session: UserSession) = withContext(Dispatchers.IO) {
        store.write(json.encodeToString(UserSession.serializer(), session))
    }

    override suspend fun loadSession(): UserSession? = withContext(Dispatchers.IO) {
        val payload = store.read() ?: return@withContext null
        // An undecodable payload is treated as "no session" so start-up falls back to login.
        runCatching { json.decodeFromString(UserSession.serializer(), payload) }.getOrNull()
    }

    override suspend fun deleteSession() = withContext(Dispatchers.IO) {
        store.clear()
    }

    companion object {
        val sessionJson: Json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
