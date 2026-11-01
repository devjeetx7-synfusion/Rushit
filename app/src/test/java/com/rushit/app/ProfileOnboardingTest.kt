package com.rushit.app

import com.rushit.app.data.auth.ProfileDraft
import com.rushit.app.data.auth.ProfileOnboarding
import com.rushit.app.data.auth.SubmissionResult
import com.rushit.app.data.auth.FullNameRules
import com.rushit.app.data.auth.UsernameRules
import com.rushit.app.data.model.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rules that decide what a profile stores.
 *
 * These are the invariants behind "never overwrite a customization" and "handle blank usernames
 * correctly", so they are tested directly rather than through the repository.
 */
class ProfileOnboardingTest {

    @Test
    fun `onboarding is required when there is no row or it was never completed`() {
        assertTrue(ProfileOnboarding.requiresOnboarding(null))
        assertTrue(ProfileOnboarding.requiresOnboarding(Profile(id = "user", onboardingCompleted = false)))
        assertFalse(ProfileOnboarding.requiresOnboarding(Profile(id = "user", onboardingCompleted = true)))
    }

    @Test
    fun `saved values always win over provider suggestions`() {
        val session = googleIdentity(
            uid = TestConfig.USER_A,
            fullName = "Google Name",
            username = "google.handle",
            avatarUrl = "https://lh3.googleusercontent.com/a/photo",
        )
        val existing = Profile(
            id = TestConfig.USER_A,
            fullName = "Name I Chose",
            username = "myhandle",
            avatarUrl = "https://example.test/mine.png",
            onboardingCompleted = false,
        )

        val draft = ProfileOnboarding.draftFor(session, existing)

        assertEquals("Name I Chose", draft.initialFullName)
        assertEquals("myhandle", draft.initialUsername)
        assertEquals("https://example.test/mine.png", draft.initialAvatarUrl)
        assertTrue(draft.hasSavedValues)
    }

    @Test
    fun `provider suggestions fill the form when nothing was saved yet`() {
        val session = googleIdentity(uid = TestConfig.USER_A)

        val draft = ProfileOnboarding.draftFor(session, null)

        assertEquals("Google Name", draft.initialFullName)
        assertEquals("https://lh3.googleusercontent.com/a/photo", draft.initialAvatarUrl)
        assertEquals("", draft.initialUsername)
        assertFalse(draft.hasSavedValues)
        assertEquals("shopper@example.com", draft.email)
    }

    @Test
    fun `partially saved profiles keep the saved part and suggest the rest`() {
        val draft = ProfileOnboarding.draftFor(
            googleIdentity(uid = TestConfig.USER_A),
            Profile(id = TestConfig.USER_A, fullName = "  ", username = null, avatarUrl = null),
        )

        // A whitespace-only saved name is not a customization.
        assertEquals("Google Name", draft.initialFullName)
        assertEquals("https://lh3.googleusercontent.com/a/photo", draft.initialAvatarUrl)
    }

    @Test
    fun `new profile values carry provider hints but never complete onboarding`() {
        val values = ProfileOnboarding.newProfileValues(googleIdentity(uid = TestConfig.USER_A))

        assertEquals(TestConfig.USER_A, values.firebaseUid)
        assertEquals("Google Name", values.fullName)
        assertEquals("https://lh3.googleusercontent.com/a/photo", values.avatarUrl)
    }

    @Test
    fun `a blank username is normalized away instead of being stored empty`() {
        val accepted = ProfileOnboarding.submissionFor("Rush It", "   ", null) as SubmissionResult.Accepted

        assertNull(accepted.submission.username)
        assertEquals("Rush It", accepted.submission.fullName)
    }

    @Test
    fun `usernames are normalized to match the case-insensitive unique index`() {
        val accepted = ProfileOnboarding.submissionFor("Rush It", "  RushIt.Shopper_1 ", null)
            as SubmissionResult.Accepted

        assertEquals("rushit.shopper_1", accepted.submission.username)
    }

    @Test
    fun `an invalid username is rejected with a usable message`() {
        val rejected = ProfileOnboarding.submissionFor("Rush It", "ab", null) as SubmissionResult.Rejected

        assertTrue(rejected.message.contains("at least"))
    }

    @Test
    fun `an invalid display name is rejected before the username is considered`() {
        val rejected = ProfileOnboarding.submissionFor(" ", "rushit", null) as SubmissionResult.Rejected

        assertTrue(rejected.message.contains("name"))
    }

    @Test
    fun `only https avatars are persisted`() {
        val accepted = ProfileOnboarding.submissionFor(
            "Rush It",
            "rushit",
            "http://example.test/insecure.png",
        ) as SubmissionResult.Accepted
        assertNull(accepted.submission.avatarUrl)

        val secure = ProfileOnboarding.submissionFor(
            "Rush It",
            "rushit",
            "https://example.test/photo.png",
        ) as SubmissionResult.Accepted
        assertEquals("https://example.test/photo.png", secure.submission.avatarUrl)
    }

    @Test
    fun `display names are collapsed and trimmed`() {
        assertEquals("Rush It Shopper", FullNameRules.normalize("  Rush\t It\n\nShopper  "))
        assertEquals("Rush It", FullNameRules.normalize("Rush\u00A0It"))
        assertNull(FullNameRules.validate("Rush It"))
    }

    @Test
    fun `display names must contain a letter or a number`() {
        assertNotNull(FullNameRules.validate("..."))
        assertNotNull(FullNameRules.validate(""))
        assertNotNull(FullNameRules.validate("a"))
        assertNull(FullNameRules.validate("a1"))
    }

    @Test
    fun `username rules reject separators at the edges and illegal characters`() {
        assertNotNull(UsernameRules.validate(".rushit"))
        assertNotNull(UsernameRules.validate("rushit_"))
        assertNotNull(UsernameRules.validate("ru-shit"))
        assertNotNull(UsernameRules.validate("a".repeat(UsernameRules.MAX_LENGTH + 1)))
        assertNull(UsernameRules.validate("rushit"))
        assertNull(UsernameRules.validate("rush.it_1"))
        // Blank is allowed: the username is optional in this release.
        assertNull(UsernameRules.validate(""))
    }

    @Test
    fun `username normalization strips characters that cannot be stored`() {
        assertEquals("rush.it", UsernameRules.normalize("  RUSH IT!!  "))
        assertEquals("rushit", UsernameRules.normalize("Rush--It"))
        assertEquals("", UsernameRules.normalize("###"))
    }

    @Test
    fun `a draft built for an authenticated account exposes the saved profile`() {
        val draft = ProfileDraft(
            id = TestConfig.USER_A,
            suggestedFullName = null,
            suggestedUsername = null,
            suggestedAvatarUrl = null,
            savedFullName = "Saved Name",
            savedUsername = "saved",
            savedAvatarUrl = "https://example.test/a.png",
            email = "shopper@example.com",
        )

        assertEquals("Saved Name", draft.initialFullName)
        assertEquals("saved", draft.initialUsername)
        assertEquals("https://example.test/a.png", draft.initialAvatarUrl)
    }
}
