package com.rushit.app.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Profile(
    @SerialName("id") val id: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("username") val username: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("phone_verified") val phoneVerified: Boolean = false,
    @SerialName("onboarding_completed") val onboardingCompleted: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class UserSettings(
    @SerialName("user_id") val userId: String,
    @SerialName("theme") val theme: String = "system",
    @SerialName("language") val language: String = "en",
    @SerialName("notifications_enabled") val notificationsEnabled: Boolean = true,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class UserDevice(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("device_name") val deviceName: String? = null,
    @SerialName("platform") val platform: String? = null,
    @SerialName("app_version") val appVersion: String? = null,
    @SerialName("last_active_at") val lastActiveAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class UserActivityLog(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("event_type") val eventType: String,
    @SerialName("created_at") val createdAt: String? = null
)
