package com.rushit.app

import com.rushit.app.data.auth.AccessToken
import com.rushit.app.data.auth.AuthGateway
import com.rushit.app.data.auth.AuthGatewayException
import com.rushit.app.data.auth.AuthIdentity
import com.rushit.app.data.auth.AuthProviderId
import com.rushit.app.data.auth.PhoneChallenge
import com.rushit.app.data.auth.PhoneChallengeStart
import com.rushit.app.data.model.Profile

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
    email = null,
    phoneNumber = phoneNumber,
    provider = AuthProviderId.PHONE,
    memberSinceEpochSeconds = 1_700_000_100L,
    suggestedFullName = fullName,
    suggestedUsername = null,
    suggestedAvatarUrl = null,
)

fun completedProfile(
    uid: String = TestConfig.USER_A,
    fullName: String? = "Rush It",
    username: String? = "rushit",
    avatarUrl: String? = null,
    onboardingCompleted: Boolean = true,
) = Profile(
    id = uid,
    fullName = fullName,
    username = username,
    avatarUrl = avatarUrl,
    onboardingCompleted = onboardingCompleted,
)

class FakeAuthGateway : AuthGateway {
    override var isConfigured: Boolean = true

    var identity: AuthIdentity? = null

    var googleError: Throwable? = null
    var tokenError: Throwable? = null
    var startPhoneError: Throwable? = null
    var resendError: Throwable? = null
    var verifyError: Throwable? = null
    var signOutError: Throwable? = null
    var deleteError: Throwable? = null

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
        val id = identity ?: googleIdentity()
        identity = id
        return id
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
        val id = identity ?: phoneIdentity()
        identity = id
        return id
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
