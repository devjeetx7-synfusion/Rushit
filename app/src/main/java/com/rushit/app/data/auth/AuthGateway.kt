package com.rushit.app.data.auth

enum class AuthProviderId(val wireValue: String) {
    GOOGLE("google"),
    PHONE("phone"),
    ;

    companion object {
        fun fromWireValue(value: String?): AuthProviderId? =
            values().firstOrNull { it.wireValue.equals(value, ignoreCase = true) }
    }
}

data class AuthIdentity(
    val uid: String,
    val email: String?,
    val phoneNumber: String?,
    val provider: AuthProviderId,
    val memberSinceEpochSeconds: Long?,
    val suggestedFullName: String?,
    val suggestedUsername: String?,
    val suggestedAvatarUrl: String?,
)
