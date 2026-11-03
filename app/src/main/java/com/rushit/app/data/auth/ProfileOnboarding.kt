package com.rushit.app.data.auth

import com.rushit.app.data.model.Profile

data class ProfileSubmission(
    val fullName: String,
    val username: String?,
    val avatarUrl: String?,
)

object ProfileOnboarding {

    fun requiresOnboarding(profile: Profile?): Boolean = profile?.onboardingCompleted != true

    fun draftFor(identity: AuthIdentity, existing: Profile?): ProfileDraft = ProfileDraft(
        id = identity.uid,
        suggestedFullName = identity.suggestedFullName.clean(),
        suggestedUsername = identity.suggestedUsername.clean(),
        suggestedAvatarUrl = identity.suggestedAvatarUrl.clean(),
        savedFullName = existing?.fullName.clean(),
        savedUsername = existing?.username.clean(),
        savedAvatarUrl = existing?.avatarUrl.clean(),
        email = identity.email.clean(),
    )

    fun newProfileValues(identity: AuthIdentity): NewProfileValues = NewProfileValues(
        userId = identity.uid,
        fullName = identity.suggestedFullName.clean(),
        avatarUrl = identity.suggestedAvatarUrl.clean(),
    )

    fun submissionFor(
        fullNameInput: String,
        usernameInput: String?,
        avatarUrlInput: String? = null,
    ): SubmissionResult {
        val fullName = FullNameRules.normalize(fullNameInput)
        val nameCheck = FullNameRules.validate(fullName)
        if (nameCheck != null) return SubmissionResult.Rejected(nameCheck)

        val username = UsernameRules.normalize(usernameInput.orEmpty()).ifBlank { null }
        if (username != null) {
            val usernameCheck = UsernameRules.validate(username)
            if (usernameCheck != null) return SubmissionResult.Rejected(usernameCheck)
        }

        val avatarUrl = avatarUrlInput?.trim()?.takeIf { it.startsWith("https://") && it.length <= 2048 }

        return SubmissionResult.Accepted(
            ProfileSubmission(
                fullName = fullName,
                username = username,
                avatarUrl = avatarUrl,
            ),
        )
    }

    private fun String?.clean(): String? = this?.trim()?.ifBlank { null }
}

data class NewProfileValues(val userId: String, val fullName: String?, val avatarUrl: String?)

sealed interface SubmissionResult {
    data class Accepted(val submission: ProfileSubmission) : SubmissionResult
    data class Rejected(val message: String) : SubmissionResult
}

object FullNameRules {
    const val MIN_LENGTH = 2
    const val MAX_LENGTH = 60

    fun normalize(input: String): String =
        input.replace('\u00A0', ' ')
            .split(' ', '\t', '\n', '\r')
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .trim()
            .take(MAX_LENGTH)

    fun validate(normalized: String): String? = when {
        normalized.isEmpty() -> "Please enter your name so RushIt knows what to call you."
        normalized.length < MIN_LENGTH -> "Your name needs at least $MIN_LENGTH characters."
        normalized.any { it.isISOControl() } -> "Your name contains characters RushIt cannot save."
        normalized.none { it.isLetterOrDigit() } -> "Please enter your name using letters or numbers."
        else -> null
    }
}

object UsernameRules {
    const val MIN_LENGTH = 3
    const val MAX_LENGTH = 20

    private val ALLOWED = Regex("^[a-z0-9._]+$")
    private val BOUNDARY = Regex("^[a-z0-9].*[a-z0-9]$")
    private val REPEATED_SEPARATORS = Regex("[._]{2,}")

    fun normalize(input: String): String =
        input.trim()
            .lowercase()
            .replace(' ', '.')
            .replace(Regex("[^a-z0-9._]"), "")
            .replace(REPEATED_SEPARATORS) { it.value.first().toString() }
            .trim('.', '_')

    fun validate(normalized: String): String? = when {
        normalized.isEmpty() -> null
        normalized.length < MIN_LENGTH ->
            "Usernames need at least $MIN_LENGTH characters."
        normalized.length > MAX_LENGTH ->
            "Usernames can be at most $MAX_LENGTH characters."
        !ALLOWED.matches(normalized) ->
            "Use only lowercase letters, numbers, dots and underscores."
        !BOUNDARY.matches(normalized) ->
            "Usernames must start and end with a letter or number."
        else -> null
    }
}
