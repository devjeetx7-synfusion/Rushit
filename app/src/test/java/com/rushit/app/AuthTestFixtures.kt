package com.rushit.app

import com.rushit.app.data.auth.AuthGateway
import com.rushit.app.data.auth.AuthProviderId
import com.rushit.app.data.auth.DeleteAccountOutcome
import com.rushit.app.data.auth.SessionRestoreResult
import com.rushit.app.data.auth.SessionSnapshot
import com.rushit.app.data.model.Profile
import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.exceptions.NotFoundRestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import io.github.jan.supabase.exceptions.UnknownRestException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.util.Base64

/**
 * An [AuthGateway] that behaves like the Supabase schema and its Row Level Security rules.
 *
 * The important modelling choice is `provisionProfile`: like `ON CONFLICT DO NOTHING`, it never
 * overwrites an existing row. That is what lets the tests prove a returning user keeps their
 * customized name and avatar.
 */
class FakeAuthGateway : AuthGateway {
    override var isConfigured: Boolean = true

    val profiles = mutableMapOf<String, Profile>()
    val settings = mutableSetOf<String>()
    val activityLog = mutableListOf<ActivityRecord>()
    val provisionedUsers = mutableListOf<String>()
    val savedProfiles = mutableListOf<Profile>()
    val checkedUsernames = mutableListOf<String>()

    var session: SessionSnapshot? = null
    var restoreResult: SessionRestoreResult = SessionRestoreResult.NoSession
    var restoreError: Throwable? = null
    var signInError: Throwable? = null
    var fetchProfileError: Throwable? = null
    var saveError: Throwable? = null
    var saveGate: kotlinx.coroutines.CompletableDeferred<Unit>? = null
    var activityError: Throwable? = null
    var usernameCheckError: Throwable? = null
    var deleteError: Throwable? = null
    var signOutRevokesRemotely: Boolean = true
    var deleteOutcome: DeleteAccountOutcome = DeleteAccountOutcome.Deleted
    var takenUsernames: MutableSet<String> = mutableSetOf()
    var signIns = 0
    var deleteRequests = 0
    var signOutRequests = 0
    var refreshes = 0
    var refreshSucceeds = true

    data class ActivityRecord(val userId: String, val eventType: String, val metadata: Map<String, String>)

    override suspend fun restoreSession(): SessionRestoreResult {
        restoreError?.let { throw it }
        return restoreResult
    }

    override suspend fun refreshSession(): Boolean {
        refreshes++
        return refreshSucceeds
    }

    override suspend fun signInWithIdToken(
        provider: AuthProviderId,
        idToken: String,
        rawNonce: String?,
    ): SessionSnapshot {
        signIns++
        signInError?.let { throw it }
        val signedIn = session ?: error("test gateway needs a session to return")
        return signedIn
    }

    override suspend fun currentSession(): SessionSnapshot? = session

    override suspend fun signOut(): Boolean {
        signOutRequests++
        // Mirrors the real gateway: local state is cleared even when revocation fails.
        session = null
        return signOutRevokesRemotely
    }

    override suspend fun deleteAccount(): DeleteAccountOutcome {
        deleteRequests++
        deleteError?.let { throw it }
        if (deleteOutcome == DeleteAccountOutcome.Deleted) {
            val userId = session?.userId
            profiles.remove(userId)
            settings.remove(userId)
            session = null
        }
        return deleteOutcome
    }

    override suspend fun fetchProfile(userId: String): Profile? {
        fetchProfileError?.let { throw it }
        return profiles[userId]
    }

    override suspend fun provisionProfile(
        userId: String,
        fullName: String?,
        avatarUrl: String?,
    ): Profile? {
        provisionedUsers += userId
        // ON CONFLICT (id) DO NOTHING — an existing row is returned untouched.
        return profiles.getOrPut(userId) {
            Profile(id = userId, fullName = fullName, avatarUrl = avatarUrl, onboardingCompleted = false)
        }
    }

    override suspend fun ensureSettings(userId: String) {
        settings += userId
    }

    override suspend fun saveProfile(
        userId: String,
        fullName: String,
        username: String?,
        avatarUrl: String?,
    ): Profile {
        saveError?.let { throw it }
        saveGate?.await()
        val existing = profiles[userId] ?: Profile(id = userId)
        val saved = existing.copy(
            fullName = fullName,
            username = username,
            avatarUrl = avatarUrl ?: existing.avatarUrl,
            onboardingCompleted = true,
        )
        profiles[userId] = saved
        savedProfiles += saved
        return saved
    }

    override suspend fun usernameTaken(username: String): Boolean {
        checkedUsernames += username
        usernameCheckError?.let { throw it }
        return username.trim().lowercase() in takenUsernames
    }

    override suspend fun recordActivity(
        userId: String,
        eventType: String,
        metadata: Map<String, String>,
    ) {
        activityError?.let { throw it }
        activityLog += ActivityRecord(userId, eventType, metadata)
    }
}

/** A session as Supabase would return it after a Google sign-in. */
fun googleSession(
    userId: String = "11111111-2222-3333-4444-555555555555",
    email: String? = "shopper@example.com",
    fullName: String? = "Google Name",
    username: String? = null,
    avatarUrl: String? = "https://lh3.googleusercontent.com/a/photo",
    memberSince: Long? = 1_700_000_000L,
) = SessionSnapshot(
    userId = userId,
    email = email,
    provider = AuthProviderId.GOOGLE,
    memberSinceEpochSeconds = memberSince,
    suggestedFullName = fullName,
    suggestedUsername = username,
    suggestedAvatarUrl = avatarUrl,
)

fun completedProfile(
    id: String,
    fullName: String = "Custom Name",
    username: String? = "shopper",
    avatarUrl: String? = "https://example.test/custom.png",
) = Profile(
    id = id,
    fullName = fullName,
    username = username,
    avatarUrl = avatarUrl,
    onboardingCompleted = true,
    createdAt = "2026-01-01T00:00:00Z",
    updatedAt = "2026-01-01T00:00:00Z",
)

/**
 * Builds an unsigned JWT with the given claims.
 *
 * RushIt only ever reads the payload locally — Supabase and Google verify the signature — so an
 * unsigned token is exactly what the client-side checks see.
 */
fun buildIdToken(
    audience: String = TestConfig.WEB_CLIENT_ID,
    issuer: String = "https://accounts.google.com",
    subject: String = "google-subject-1",
    issuedAt: Long = 1_700_000_000L,
    expiresAt: Long = 1_700_003_600L,
    nonce: String? = null,
    email: String? = "shopper@example.com",
    name: String? = "Google Name",
    picture: String? = "https://lh3.googleusercontent.com/a/photo",
): String {
    val claims = buildList {
        add("\"iss\":\"$issuer\"")
        add("\"aud\":\"$audience\"")
        add("\"sub\":\"$subject\"")
        add("\"iat\":$issuedAt")
        add("\"exp\":$expiresAt")
        nonce?.let { add("\"nonce\":\"$it\"") }
        email?.let { add("\"email\":\"$it\"") }
        name?.let { add("\"name\":\"$it\"") }
        picture?.let { add("\"picture\":\"$it\"") }
    }.joinToString(",")
    val header = encodeSegment("""{"alg":"RS256","typ":"JWT"}""")
    val payload = encodeSegment("{$claims}")
    return "$header.$payload.${encodeSegment("signature")}"
}

private fun encodeSegment(value: String): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

object TestConfig {
    /** Shaped like a real Web OAuth client ID, but not a real credential. */
    const val WEB_CLIENT_ID = "000000000000-testwebclientid.apps.googleusercontent.com"
    const val OTHER_CLIENT_ID = "000000000000-differentclient.apps.googleusercontent.com"
    const val USER_A = "11111111-2222-3333-4444-555555555555"
    const val USER_B = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
}

/**
 * Real supabase-kt REST exceptions need an [io.ktor.client.statement.HttpResponse], whose `message`
 * embeds the request URL and headers. Building them through a mock engine is what lets the tests
 * assert that those internals never reach a user-facing string.
 */
class RestExceptionFactory {
    private val client = HttpClient(MockEngine { respond("{}", HttpStatusCode.OK, headersOf("Content-Type", "application/json")) })

    /** The `message` of these exceptions contains request headers, including the apikey and JWT. */
    private suspend fun response() = client.get("https://example.supabase.co/rest/v1/profiles?select=id") {
        headers.appendAll(headersOf("apikey", SECRET_ANON_KEY))
        headers.appendAll(headersOf("Authorization", "Bearer $SECRET_JWT"))
    }

    suspend fun rowLevelSecurity() = BadRequestRestException(
        "new row violates row-level security policy for table \"profiles\"",
        response(),
        "Failing row contains ...",
    )

    suspend fun duplicateKey() = BadRequestRestException(
        "duplicate key value violates unique constraint \"profiles_username_unique\"",
        response(),
        "Key (lower(username))=(rushit) already exists.",
    )

    suspend fun jwtExpired() = UnknownRestException("JWT expired", response(), "PGRST301")

    suspend fun unauthorized() = UnauthorizedRestException("JWT expired", response())

    suspend fun notFound() = NotFoundRestException("Could not find the function public.delete_user_account", response())

    fun close() = client.close()

    companion object {
        const val SECRET_ANON_KEY = "super-secret-anon-key"
        const val SECRET_JWT = "super-secret-jwt"
    }
}
