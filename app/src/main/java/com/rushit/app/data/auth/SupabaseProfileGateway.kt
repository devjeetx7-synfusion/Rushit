package com.rushit.app.data.auth

import com.rushit.app.data.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * [ProfileGateway] over a real Supabase client.
 *
 * Supabase is used for the application database only. No Supabase Auth call is made here (or
 * anywhere else in the app): every request is authorized by the Firebase ID token that
 * [BackendTokenBridge] installed on the client, and Row Level Security compares
 * `auth.jwt() ->> 'sub'` with `profiles.firebase_uid`.
 *
 * @param supabase the configured client, or null when the build has no Supabase project. A null
 * client makes every call fail with an actionable configuration message instead of a crash
 */
class SupabaseProfileGateway(private val supabase: SupabaseClient?) : ProfileGateway {

    private val postgrest get() = supabase!!.postgrest

    override val isConfigured: Boolean = supabase != null

    override suspend fun findProfile(firebaseUid: String): Profile? = configured().let {
        postgrest[PROFILES]
            .select(columns = Columns.type<Profile>()) { filter { eq(FIREBASE_UID_COLUMN, firebaseUid) } }
            .decodeSingleOrNull()
    }

    override suspend fun provisionProfile(
        firebaseUid: String,
        fullName: String?,
        avatarUrl: String?,
    ): Profile? {
        configured()
        // ignoreDuplicates makes this safe under concurrent sign-ins and on every later login: the
        // row is created only if it is absent, so saved values are never overwritten here.
        postgrest[PROFILES].upsert(
            ProfileInsert(firebaseUid = firebaseUid, fullName = fullName, avatarUrl = avatarUrl),
            onConflict = FIREBASE_UID_COLUMN,
            ignoreDuplicates = true,
        )
        return findProfile(firebaseUid)
    }

    override suspend fun ensureSettings(firebaseUid: String) {
        configured()
        val profileId = findProfile(firebaseUid)?.id ?: throw AuthGatewayException(
            AuthFailure(ProfileMessages.PROFILE_MISSING, AuthFailureKind.NOT_FOUND),
        )
        postgrest[USER_SETTINGS].upsert(
            SettingsInsert(userId = profileId),
            onConflict = USER_ID_COLUMN,
            ignoreDuplicates = true,
        )
    }

    override suspend fun saveProfile(
        firebaseUid: String,
        fullName: String,
        username: String?,
        avatarUrl: String?,
    ): Profile {
        configured()
        // Provision first so an update can never be silently applied to zero rows.
        if (findProfile(firebaseUid) == null) provisionProfile(firebaseUid, fullName, avatarUrl)

        postgrest[PROFILES].update({
            set("full_name", fullName)
            // A blank username is stored as NULL so the unique index ignores it.
            set("username", username)
            set("avatar_url", avatarUrl)
            set("onboarding_completed", true)
        }) {
            filter { eq(FIREBASE_UID_COLUMN, firebaseUid) }
        }
        return findProfile(firebaseUid)
            ?: throw IllegalStateException("RushIt saved the profile but could not read it back")
    }

    override suspend fun usernameTaken(username: String): Boolean {
        configured()
        val normalized = username.trim().lowercase()
        if (normalized.isEmpty()) return false
        // RLS restricts profiles to their owner, so availability is answered by a SECURITY DEFINER
        // function that returns a boolean and nothing else. A plain SELECT would always say "free".
        return !postgrest.rpc(USERNAME_AVAILABLE_RPC, mapOf("p_username" to normalized))
            .decodeAs<Boolean>()
    }

    override suspend fun recordActivity(
        firebaseUid: String,
        eventType: String,
        metadata: Map<String, String>,
    ) {
        configured()
        val profileId = findProfile(firebaseUid)?.id ?: return
        postgrest[USER_ACTIVITY_LOGS].insert(
            ActivityInsert(userId = profileId, eventType = eventType, metadata = metadata),
        )
    }

    override suspend fun deleteProfile(firebaseUid: String) {
        configured()
        postgrest[PROFILES].delete { filter { eq(FIREBASE_UID_COLUMN, firebaseUid) } }
    }

    private fun configured(): Unit = if (!isConfigured) {
        throw AuthGatewayException(
            AuthFailure(ProfileMessages.SUPABASE_UNCONFIGURED, AuthFailureKind.UNCONFIGURED),
        )
    } else {
        Unit
    }

    // `id` is deliberately absent: the database default (gen_random_uuid()) owns it, so a client can
    // never choose a profile id that collides with somebody else's row.
    @Serializable
    private data class ProfileInsert(
        @SerialName("firebase_uid") val firebaseUid: String,
        @SerialName("full_name") val fullName: String? = null,
        @SerialName("avatar_url") val avatarUrl: String? = null,
    )

    @Serializable
    private data class SettingsInsert(
        @SerialName("user_id") val userId: String,
    )

    @Serializable
    private data class ActivityInsert(
        @SerialName("user_id") val userId: String,
        @SerialName("event_type") val eventType: String,
        val metadata: Map<String, String> = emptyMap(),
    )

    companion object {
        const val PROFILES = "profiles"
        const val USER_SETTINGS = "user_settings"
        const val USER_ACTIVITY_LOGS = "user_activity_logs"
        const val FIREBASE_UID_COLUMN = "firebase_uid"
        const val USER_ID_COLUMN = "user_id"
        const val USERNAME_AVAILABLE_RPC = "username_available"

        /**
         * Serializer used for every RushIt Supabase client.
         *
         * `ignoreUnknownKeys` keeps the app working when the database gains a column before a client
         * release; `encodeDefaults = false` keeps `created_at`/`updated_at` out of insert payloads so
         * the database defaults and triggers stay in charge.
         */
        @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
        val serializer: KotlinXSerializer = KotlinXSerializer(
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = false
                explicitNulls = false
            },
        )
    }
}
