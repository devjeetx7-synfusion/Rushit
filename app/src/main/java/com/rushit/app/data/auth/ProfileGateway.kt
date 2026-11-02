package com.rushit.app.data.auth

import com.rushit.app.data.model.Profile

interface ProfileGateway {
    val isConfigured: Boolean

    suspend fun findProfile(userId: String): Profile?

    suspend fun provisionProfile(userId: String, fullName: String?, avatarUrl: String?): Profile?

    suspend fun ensureSettings(userId: String)

    suspend fun saveProfile(
        userId: String,
        fullName: String,
        username: String?,
        avatarUrl: String?,
    ): Profile

    suspend fun usernameTaken(username: String): Boolean

    suspend fun recordActivity(userId: String, eventType: String, metadata: Map<String, String>)

    suspend fun deleteProfile(userId: String)
}

object ProfileMessages {
    const val BACKEND_REMOVED =
        "RushIt backend service has been removed from this build. Online profile storage is not connected."
    const val PROFILE_MISSING =
        "RushIt could not read your RushIt profile. Please sign in again."
}
