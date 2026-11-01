package com.rushit.app.ui.screens.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import coil.compose.AsyncImage
import com.rushit.app.R
import com.rushit.app.data.auth.AuthAccount
import com.rushit.app.data.auth.AuthState
import com.rushit.app.data.auth.ProfileDraft
import com.rushit.app.data.auth.UsernameAvailability
import com.rushit.app.data.auth.UsernameRules
import com.rushit.app.ui.components.RushButton
import com.rushit.app.ui.components.RushIcon
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.Cream
import com.rushit.app.ui.theme.DeepRed
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.Muted
import com.rushit.app.ui.theme.Peach
import com.rushit.app.ui.theme.SoftRed
import com.rushit.app.ui.theme.Success
import com.rushit.app.ui.viewmodel.AuthUiState
import kotlinx.coroutines.delay

/** Whether the profile form is a required first-run step or an optional edit of an existing profile. */
enum class ProfileFormMode { ONBOARDING, EDITING }

private const val USERNAME_CHECK_DEBOUNCE_MILLIS = 450L

/**
 * Profile onboarding and profile editing.
 *
 * RushIt asks only for what it actually uses: a display name (required) and an optional username.
 * Google's name and photo arrive as *suggestions*; anything already saved in `profiles` wins, so
 * signing in again never overwrites a customization.
 *
 * Interrupted onboarding is safe: `onboarding_completed` stays false in Supabase, so the next launch
 * routes back here with whatever was already saved pre-filled.
 */
@Composable
fun ProfileSetupScreen(
    authState: AuthState,
    uiState: AuthUiState,
    errorMessage: String?,
    usernameAvailability: UsernameAvailability,
    onSave: (fullName: String, username: String?, avatarUrl: String?) -> Unit,
    onCheckUsername: (String) -> Unit,
    onClearError: () -> Unit,
    mode: ProfileFormMode = ProfileFormMode.ONBOARDING,
    onBack: (() -> Unit)? = null,
) {
    val draft = profileDraftFrom(authState)
    if (draft == null) {
        // No signed-in user to edit; the navigation layer sends this person back to login.
        SignedOutNotice()
        return
    }

    var fullName by rememberSaveable(draft.id) { mutableStateOf(draft.initialFullName) }
    var username by rememberSaveable(draft.id) { mutableStateOf(draft.initialUsername) }
    val avatarUrl = draft.initialAvatarUrl
    val focusManager = LocalFocusManager.current
    val compactWidth = LocalConfiguration.current.screenWidthDp < 360

    val normalizedUsername = UsernameRules.normalize(username)
    val usernameError = UsernameRules.validate(normalizedUsername)
    val nameError = if (fullName.isBlank()) "Please enter your name so RushIt knows what to call you." else null
    val saving = uiState.savingProfile
    val canSave = nameError == null && usernameError == null && !saving && !uiState.busy

    // Debounced availability hint. The database unique index is still the authority, so a stale
    // "available" can never create a duplicate.
    LaunchedEffect(normalizedUsername, mode) {
        if (normalizedUsername.isBlank() || usernameError != null) return@LaunchedEffect
        delay(USERNAME_CHECK_DEBOUNCE_MILLIS)
        onCheckUsername(normalizedUsername)
    }

    Box(Modifier.fillMaxSize().background(Cream), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = if (compactWidth) 16.dp else 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (onBack != null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier
                            .heightIn(min = 48.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button, onClickLabel = "Back", onClick = onBack)
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RushIcon(R.drawable.ic_arrow_left_01, null, Modifier.size(20.dp), Ink)
                        Spacer(Modifier.width(6.dp))
                        Text("Back", style = MaterialTheme.typography.labelMedium, color = Ink)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.height(8.dp))
            AvatarPreview(avatarUrl, fullName)
            Spacer(Modifier.height(16.dp))
            Text(
                if (mode == ProfileFormMode.ONBOARDING) "Complete your profile" else "Edit profile",
                style = MaterialTheme.typography.headlineSmall,
                color = Ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (mode == ProfileFormMode.ONBOARDING) {
                    "This is how RushIt will greet you. Everything else comes from how you signed in."
                } else {
                    "Update how your name and username appear across RushIt."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = CardWhite,
                border = BorderStroke(1.dp, Border),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(if (compactWidth) 14.dp else 18.dp)) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it.take(80) },
                        label = { Text("Display name") },
                        supportingText = {
                            Text(
                                nameError ?: "Shown on your orders and deliveries",
                                color = if (nameError != null) DeepRed else Muted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        isError = nameError != null,
                        singleLine = true,
                        enabled = !saving,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Coral,
                            unfocusedBorderColor = Border,
                            focusedLabelColor = Coral,
                            cursorColor = Coral,
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it.take(30) },
                        label = { Text("Username (optional)") },
                        prefix = { Text("@", color = Muted, style = MaterialTheme.typography.bodyLarge) },
                        supportingText = {
                            UsernameSupport(
                                error = usernameError,
                                availability = if (usernameAvailability is UsernameAvailability.Unknown || uiState.checkingUsername) {
                                    null
                                } else {
                                    usernameAvailability
                                },
                                hasValue = normalizedUsername.isNotBlank(),
                            )
                        },
                        isError = usernameError != null,
                        singleLine = true,
                        enabled = !saving,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Coral,
                            unfocusedBorderColor = Border,
                            focusedLabelColor = Coral,
                            cursorColor = Coral,
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (errorMessage != null) {
                        Spacer(Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SoftRed,
                            border = BorderStroke(1.dp, Coral.copy(alpha = .28f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { liveRegion = LiveRegionMode.Polite },
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RushIcon(R.drawable.ic_cancel_01, null, Modifier.size(18.dp), DeepRed)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DeepRed,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Dismiss",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DeepRed,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable(role = Role.Button, onClickLabel = "Dismiss error", onClick = onClearError)
                                        .padding(horizontal = 4.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    RushButton(
                        text = if (mode == ProfileFormMode.ONBOARDING) "Save and continue" else "Save changes",
                        onClick = {
                            focusManager.clearFocus()
                            onSave(fullName, normalizedUsername.ifBlank { null }, avatarUrl)
                        },
                        enabled = canSave,
                        loading = saving,
                        arrow = mode == ProfileFormMode.ONBOARDING,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RushIcon(R.drawable.ic_lock_keyhole, null, Modifier.size(16.dp), Muted)
                Spacer(Modifier.width(8.dp))
                Text(
                    "RushIt never asks for your Google password.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

/** Reuses the same form for the Account screen's "Edit profile" entry. */
@Composable
fun EditProfileScreen(
    authState: AuthState,
    uiState: AuthUiState,
    errorMessage: String?,
    usernameAvailability: UsernameAvailability,
    onSave: (fullName: String, username: String?, avatarUrl: String?) -> Unit,
    onCheckUsername: (String) -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit,
) {
    ProfileSetupScreen(
        authState = authState,
        uiState = uiState,
        errorMessage = errorMessage,
        usernameAvailability = usernameAvailability,
        onSave = onSave,
        onCheckUsername = onCheckUsername,
        onClearError = onClearError,
        mode = ProfileFormMode.EDITING,
        onBack = onBack,
    )
}

@Composable
private fun UsernameSupport(
    error: String?,
    availability: UsernameAvailability?,
    hasValue: Boolean,
) {
    val text = when {
        error != null -> error
        !hasValue -> "Lowercase letters, numbers, dots and underscores · " +
            "${UsernameRules.MIN_LENGTH}–${UsernameRules.MAX_LENGTH} characters"
        availability == UsernameAvailability.Taken -> "That username is already taken."
        availability == UsernameAvailability.Available -> "Looks good, that username is available."
        else -> "Checking availability…"
    }
    val color = when {
        error != null -> DeepRed
        availability == UsernameAvailability.Taken -> DeepRed
        availability == UsernameAvailability.Available && hasValue -> Success
        else -> Muted
    }
    Text(text, color = color, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun AvatarPreview(avatarUrl: String?, fullName: String) {
    Box(
        Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(Peach),
        contentAlignment = Alignment.Center,
    ) {
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Your profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        } else {
            val initials = fullName.trim()
                .split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.take(1).uppercase() }
            if (initials.isNotBlank()) {
                Text(
                    initials,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Coral,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                RushIcon(R.drawable.ic_user_02, null, Modifier.size(40.dp), Coral)
            }
        }
    }
}

@Composable
private fun SignedOutNotice() {
    Box(Modifier.fillMaxSize().background(Cream).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(Modifier.size(28.dp), color = Coral, strokeWidth = 2.5.dp)
            Spacer(Modifier.height(14.dp))
            Text(
                "You are signed out. Returning to the login screen…",
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Builds the form's starting values from whichever signed-in state is present. */
internal fun profileDraftFrom(authState: AuthState): ProfileDraft? = when (authState) {
    is AuthState.ProfileIncomplete -> authState.draft
    is AuthState.Authenticated -> authState.account.toDraft()
    else -> null
}

private fun AuthAccount.toDraft(): ProfileDraft = ProfileDraft(
    id = id,
    suggestedFullName = null,
    suggestedUsername = null,
    suggestedAvatarUrl = null,
    savedFullName = profile.fullName,
    savedUsername = profile.username,
    savedAvatarUrl = profile.avatarUrl,
    email = email,
)


