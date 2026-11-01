package com.rushit.app

import com.rushit.app.data.auth.AccessToken
import com.rushit.app.data.auth.AuthGateway
import com.rushit.app.data.auth.AuthGatewayException
import com.rushit.app.data.auth.AuthIdentity
import com.rushit.app.data.auth.AuthProviderId
import com.rushit.app.data.auth.BackendTokenBridge
import com.rushit.app.data.auth.PhoneChallenge
import com.rushit.app.data.auth.PhoneChallengeStart
import com.rushit.app.data.auth.ProfileGateway
import com.rushit.app.data.model.Profile

/** Shared constants: Firebase UIDs are opaque strings, not UUIDs. */
object TestConfig {
    const val USER_A = "firebaseUidUserA0000000000000001"
    const val USER_B = "firebaseUidUserB0000000000000002"
    const val WEB_CLIENT_ID = "1234567890-abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com"
    const val TOKEN = "firebase-id-token-for-tests"
}

fun googleIdentity(
    uid: String = TestConfig.USER_A,
    email: String? = "shopper@example.com",
    fullName: String? = "Rush It",
    username: String? = null,
    avatarUrl: String? = "https://lh3.googleusercontent.com/a/photo",
    phoneNumber: String? = null,
) = AuthIdentity(
    uid = uid,
    email = email,
    phoneNumber = phoneNumber,
    provider = AuthProviderId.GOOGLE,
    memberSinceEpochSeconds = 1_700_000_000L,
    suggestedFullName = fullName,
    suggestedUsername = username,
    suggestedAvatarUrl = avatarUrl,
)

fun phoneIdentity(
    uid: String = TestConfig.USER_B,
    phoneNumber: String = "+919876543210",
    fullName: String? = null,
) = AuthIdentity(
    uid = uid,
    // Phone-only accounts have no email; nothing in the app may require one.
    email = null,
    phoneNumber = phoneNumber,
    provider = AuthProviderId.PHONE,
    memberSinceEpochSeconds = 1_700_000_100L,
    suggestedFullName = fullName,
    suggestedUsername = null,
    suggestedAvatarUrl = null,
)

fun completedProfile(
    firebaseUid: String = TestConfig.USER_A,
    fullName: String? = "Rush It",
    username: String? = "rushit",
    avatarUrl: String? = null,
    onboardingCompleted: Boolean = true,
) = Profile(
    id = "internal-uuid-for-$firebaseUid",
    firebaseUid = firebaseUid,
    fullName = fullName,
    username = username,
    avatarUrl = avatarUrl,
    onboardingCompleted = onboardingCompleted,
)

/**
 * An [AuthGateway] that behaves like Firebase Authentication.
 *
 * Every failure hook throws an [AuthGatewayException], exactly like the real adapter, so the tests
 * exercise the same classification path the production code uses.
 */
class FakeAuthGateway : AuthGateway {
    override var isConfigured: Boolean = true

    /** Returned by [awaitInitialIdentity] and [currentIdentity]. */
    var identity: AuthIdentity? = null

    var googleError: Throwable? = null
    var tokenError: Throwable? = null
    var startPhoneError: Throwable? = null
    var resendError: Throwable? = null
    var verifyError: Throwable? = null
    var signOutError: Throwable? = null
    var deleteError: Throwable? = null

    /** True when Firebase completes phone verification on its own (instant/auto-retrieval). */
    var autoVerifyOnStart: Boolean = false

    var accessToken: AccessToken? = AccessToken(TestConfig.TOKEN, 4_000_000_000L)

    val tokenRequests = mutableListOf<Boolean>()
    val discardedChallenges = mutableListOf<PhoneChallenge>()
    var signOutCount = 0
    var deleteCount = 0
    var googleSignInCount = 0
    var startPhoneCount = 0
    var resendCount = 0
    var verifyCount = 0

    var lastPhoneNumber: String? = null
    var lastCode: String? = null
    private var challengeCounter = 0

    override suspend fun awaitInitialIdentity(): AuthIdentity? = identity

    override suspend fun currentIdentity(): AuthIdentity? = identity

    override suspend fun signInWithGoogleIdToken(idToken: String): AuthIdentity {
        googleSignInCount++
        googleError?.let { throw it }
        return identity ?: googleIdentity()
    }

    override suspend fun startPhoneVerification(phoneNumberE164: String): PhoneChallengeStart {
        startPhoneCount++
        lastPhoneNumber = phoneNumberE164
        startPhoneError?.let { throw it }
        if (autoVerifyOnStart) {
            identity = phoneIdentity()
            return PhoneChallengeStart.AutoVerified
        }
        challengeCounter++
        return PhoneChallengeStart.CodeSent(PhoneChallenge("challenge-$challengeCounter"))
    }

    override suspend fun resendPhoneCode(challenge: PhoneChallenge): PhoneChallengeStart {
        resendCount++
        resendError?.let { throw it }
        return PhoneChallengeStart.CodeSent(challenge)
    }

    override suspend fun verifyPhoneCode(challenge: PhoneChallenge, code: String): AuthIdentity {
        verifyCount++
        lastCode = code
        verifyError?.let { throw it }
        return identity ?: phoneIdentity()
    }

    override suspend fun discardPhoneChallenge(challenge: PhoneChallenge) {
        discardedChallenges += challenge
    }

    override suspend fun accessToken(forceRefresh: Boolean): AccessToken? {
        tokenRequests += forceRefresh
        tokenError?.let { throw it }
        return accessToken
    }

    override suspend fun signOut() {
        signOutCount++
        signOutError?.let { throw it }
        identity = null
    }

    override suspend fun deleteAccount() {
        deleteCount++
        deleteError?.let { throw it }
        identity = null
    }
}

/** An in-memory stand-in for the Supabase profile tables and their RLS rules. */
class FakeProfileGateway : ProfileGateway {
    override var isConfigured: Boolean = true

    val profiles = mutableMapOf<String, Profile>()
    val settings = mutableSetOf<String>()
    val activityLog = mutableListOf<Triple<String, String, Map<String, String>>>()
    val deleted = mutableListOf<String>()
    val provisioned = mutableListOf<String>()
    val savedProfiles = mutableListOf<Profile>()
    var takenUsernames = mutableSetOf<String>()

    var findError: Throwable? = null
    var provisionError: Throwable? = null
    var saveError: Throwable? = null
    var deleteError: Throwable? = null
    var usernameCheckError: Throwable? = null

    override suspend fun findProfile(firebaseUid: String): Profile? {
        findError?.let { throw it }
        return profiles[firebaseUid]
    }

    override suspend fun provisionProfile(
        firebaseUid: String,
        fullName: String?,
        avatarUrl: String?,
    ): Profile? {
        provisionError?.let { throw it }
        provisioned += firebaseUid
        // Mirrors `on conflict (firebase_uid) do nothing`: a saved row is never replaced.
        val existing = profiles[firebaseUid]
        if (existing != null) return existing
        val created = Profile(
            id = "internal-uuid-for-$firebaseUid",
            firebaseUid = firebaseUid,
            fullName = fullName,
            avatarUrl = avatarUrl,
            onboardingCompleted = false,
        )
        profiles[firebaseUid] = created
        return created
    }

    override suspend fun ensureSettings(firebaseUid: String) {
        profiles[firebaseUid] ?: return
        settings += firebaseUid
    }

    override suspend fun saveProfile(
        firebaseUid: String,
        fullName: String,
        username: String?,
        avatarUrl: String?,
    ): Profile {
        saveError?.let { throw it }
        val existing = profiles[firebaseUid]
            ?: provisionProfile(firebaseUid, fullName, avatarUrl)
            ?: error("provisioning did not create a row")
        val saved = existing.copy(
            fullName = fullName,
            username = username,
            avatarUrl = avatarUrl,
            onboardingCompleted = true,
        )
        profiles[firebaseUid] = saved
        savedProfiles += saved
        return saved
    }

    override suspend fun usernameTaken(username: String): Boolean {
        usernameCheckError?.let { throw it }
        return username.lowercase() in takenUsernames
    }

    override suspend fun recordActivity(
        firebaseUid: String,
        eventType: String,
        metadata: Map<String, String>,
    ) {
        activityLog += Triple(firebaseUid, eventType, metadata)
    }

    override suspend fun deleteProfile(firebaseUid: String) {
        deleteError?.let { throw it }
        deleted += firebaseUid
        profiles.remove(firebaseUid)
    }
}

/** Records exactly what was handed to the Supabase client, and when it was dropped. */
class RecordingTokenBridge : BackendTokenBridge {
    var token: AccessToken? = null
    var clears = 0
    val installed = mutableListOf<AccessToken>()

    override val hasToken: Boolean
        get() = token != null

    override suspend fun use(token: AccessToken) {
        this.token = token
        installed += token
    }

    override suspend fun clear() {
        clears++
        token = null
    }
}
