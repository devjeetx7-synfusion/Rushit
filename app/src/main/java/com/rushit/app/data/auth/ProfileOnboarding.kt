package com.rushit.app.data.auth

import com.rushit.app.data.model.Profile

/** The normalized values written when onboarding or a profile edit is saved. */
data class ProfileSubmission(
    val fullName: String,
    val username: String?,
    val avatarUrl: String?,
)

/**
 * Pure rules that decide what RushIt stores for a profile.
 *
 * Keeping this out of the repository is what makes "never overwrite a customization" a tested
 * invariant instead of a comment.
 */
object ProfileOnboarding {

    /**
     * Onboarding is required when there is no row yet, or when the row has never been completed.
     *
     * A missing row is treated as incomplete rather than as an error: the row is provisioned
     * immediately afterwards.
     */
    fun requiresOnboarding(profile: Profile?): Boolean = profile?.onboardingCompleted != true

    /**
     * Build the onboarding draft for a signed-in identity.
     *
     * Anything the person already saved wins over provider metadata, so re-authenticating never
     * resets a chosen name, username, or avatar.
     */
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

    /**
     * Values for the first-time profile row.
     *
     * Provider data is copied in as a convenience but `onboarding_completed` stays false, so the
     * person always confirms their own display name.
     */
    fun newProfileValues(identity: AuthIdentity): NewProfileValues = NewProfileValues(
        firebaseUid = identity.uid,
        fullName = identity.suggestedFullName.clean(),
        avatarUrl = identity.suggestedAvatarUrl.clean(),
    )

    /**
     * Normalize and validate onboarding input.
     *
     * @return [SubmissionResult.Rejected] with a message when a required field is unusable
     */
    fun submissionFor(
        fullNameInput: String,
        usernameInput: String?,
        avatarUrlInput: String? = null,
    ): SubmissionResult {
        val fullName = FullNameRules.normalize(fullNameInput)
        val nameCheck = FullNameRules.validate(fullName)
        if (nameCheck != null) return SubmissionResult.Rejected(nameCheck)

        // A blank username is stored as NULL, never as an empty string, so the unique index and
        // the "is this username taken" lookup both behave predictably.
        val username = UsernameRules.normalize(usernameInput.orEmpty()).ifBlank { null }
        if (username != null) {
            val usernameCheck = UsernameRules.validate(username)
            if (usernameCheck != null) return SubmissionResult.Rejected(usernameCheck)
        }

        // The avatar is whatever the form shows: a saved customization or the Google photo. Only
        // https URLs are persisted, so a malformed value cannot be written to the row.
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

/** Values for creating a profile row that does not exist yet. */
data class NewProfileValues(val firebaseUid: String, val fullName: String?, val avatarUrl: String?)

sealed interface SubmissionResult {
    data class Accepted(val submission: ProfileSubmission) : SubmissionResult
    data class Rejected(val message: String) : SubmissionResult
}

/** Display name rules: required, trimmed, single-spaced, no control characters. */
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

    /** Returns null when acceptable, otherwise the message to show. */
    fun validate(normalized: String): String? = when {
        normalized.isEmpty() -> "Please enter your name so RushIt knows what to call you."
        normalized.length < MIN_LENGTH -> "Your name needs at least $MIN_LENGTH characters."
        normalized.any { it.isISOControl() } -> "Your name contains characters RushIt cannot save."
        normalized.none { it.isLetterOrDigit() } -> "Please enter your name using letters or numbers."
        else -> null
    }
}

/**
 * Username rules.
 *
 * Usernames are optional in this release, so [normalize] returning blank is valid input and is
 * stored as NULL. Uniqueness is enforced case-insensitively by the database, and normalization
 * here matches that index exactly.
 */
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

    /** Returns null when acceptable, otherwise the message to show. */
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
