package com.rushit.app

import com.rushit.app.data.auth.AuthProviderId
import com.rushit.app.data.auth.ProfileGateway
import com.rushit.app.data.auth.AuthIdentity
import com.rushit.app.data.model.Profile

object TestConfig {
    const val USER_A = "localUserA0000000000000001"
    const val USER_B = "localUserB0000000000000002"
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
    email = null,
    phoneNumber = phoneNumber,
    provider = AuthProviderId.PHONE,
    memberSinceEpochSeconds = 1_700_000_100L,
    suggestedFullName = fullName,
    suggestedUsername = null,
    suggestedAvatarUrl = null,
)

fun completedProfile(
    userId: String = TestConfig.USER_A,
    fullName: String? = "Rush It",
    username: String? = "rushit",
    avatarUrl: String? = null,
    onboardingCompleted: Boolean = true,
) = Profile(
    id = userId,
    fullName = fullName,
    username = username,
    avatarUrl = avatarUrl,
    onboardingCompleted = onboardingCompleted,
)

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

    override suspend fun findProfile(userId: String): Profile? {
        findError?.let { throw it }
        return profiles[userId]
    }

    override suspend fun provisionProfile(
        userId: String,
        fullName: String?,
        avatarUrl: String?,
    ): Profile? {
        provisionError?.let { throw it }
        provisioned += userId
        val existing = profiles[userId]
        if (existing != null) return existing
        val created = Profile(
            id = userId,
            fullName = fullName,
            avatarUrl = avatarUrl,
            onboardingCompleted = false,
        )
        profiles[userId] = created
        return created
    }

    override suspend fun ensureSettings(userId: String) {
        profiles[userId] ?: return
        settings += userId
    }

    override suspend fun saveProfile(
        userId: String,
        fullName: String,
        username: String?,
        avatarUrl: String?,
    ): Profile {
        saveError?.let { throw it }
        val existing = profiles[userId]
            ?: provisionProfile(userId, fullName, avatarUrl)
            ?: error("provisioning did not create a row")
        val saved = existing.copy(
            fullName = fullName,
            username = username,
            avatarUrl = avatarUrl,
            onboardingCompleted = true,
        )
        profiles[userId] = saved
        savedProfiles += saved
        return saved
    }

    override suspend fun usernameTaken(username: String): Boolean {
        usernameCheckError?.let { throw it }
        return username.lowercase() in takenUsernames
    }

    override suspend fun recordActivity(
        userId: String,
        eventType: String,
        metadata: Map<String, String>,
    ) {
        activityLog += Triple(userId, eventType, metadata)
    }

    override suspend fun deleteProfile(userId: String) {
        deleteError?.let { throw it }
        deleted += userId
        profiles.remove(userId)
    }
}
