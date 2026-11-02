package com.rushit.app.ui.screens.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rushit.app.R
import com.rushit.app.data.auth.AuthState
import com.rushit.app.data.auth.RecoveryAction
import com.rushit.app.data.auth.phone.CountryCode
import com.rushit.app.data.auth.phone.PhoneNumbers
import com.rushit.app.ui.components.GoogleSignInButton
import com.rushit.app.ui.components.RushButton
import com.rushit.app.ui.components.RushIcon
import com.rushit.app.ui.components.RushWordmark
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.Cream
import com.rushit.app.ui.theme.DeepRed
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.Muted
import com.rushit.app.ui.theme.Peach
import com.rushit.app.ui.theme.SoftRed
import com.rushit.app.ui.viewmodel.AuthUiState
import com.rushit.app.ui.viewmodel.PhoneLoginUiState

/**
 * RushIt's sign-in welcome screen.
 *
 * The phone number input is displayed directly inside the welcome card.
 * Users can enter their mobile number and tap "Continue →" to initiate Firebase Phone OTP verification,
 * or tap "Continue with Google" for Google Credential Manager sign-in.
 */
@Composable
fun LoginScreen(
    authState: AuthState,
    uiState: AuthUiState,
    phoneUiState: PhoneLoginUiState,
    errorMessage: String?,
    recoveryAction: RecoveryAction?,
    onGoogleSignIn: () -> Unit,
    onSendCode: (CountryCode, String) -> Unit,
    onRetry: () -> Unit,
    onClearError: () -> Unit,
) {
    var countryIso by rememberSaveable { mutableStateOf(PhoneNumbers.default.iso) }
    var number by rememberSaveable { mutableStateOf("") }
    val selectedCountry = PhoneNumbers.byIso(countryIso) ?: PhoneNumbers.default
    var pickerOpen by remember { mutableStateOf(false) }

    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var dialogTitle by rememberSaveable { mutableStateOf("How can we help?") }
    val focusManager = LocalFocusManager.current
    val largeText = LocalConfiguration.current.fontScale > 1.2f
    val compactWidth = LocalConfiguration.current.screenWidthDp < 360

    val isBusy = uiState.signingIn ||
        phoneUiState.busy ||
        authState == AuthState.Authenticating ||
        authState == AuthState.Loading

    val digits = PhoneNumbers.digitsOf(number)
    val problem = if (digits.isEmpty()) null else PhoneNumbers.errorFor(selectedCountry, number)
    val showProblem = problem != null && digits.length >= selectedCountry.nationalDigits.first
    val canSend = PhoneNumbers.normalize(selectedCountry, number) != null && !isBusy

    fun openHelp() {
        dialogTitle = "How can we help?"
        dialog = "Sign in with your phone number or your Google account to shop, place orders and " +
            "track deliveries. RushIt never sees your Google password or the SMS code, and you can " +
            "delete your RushIt account at any time from Account settings."
    }

    fun submitPhone() {
        focusManager.clearFocus()
        if (canSend) {
            onSendCode(selectedCountry, number)
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Cream),
    ) {
        val heroSpacing = (maxHeight * .022f).coerceIn(8.dp, 18.dp)
        val heroHeight = (maxHeight * .30f).coerceIn(218.dp, 260.dp)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (compactWidth) 14.dp else 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        RushWordmark(Modifier.width(165.dp).height(67.dp))
                        Text(
                            "Fresh Finds. Fast Delivery.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Ink,
                            modifier = Modifier.padding(start = 3.dp),
                        )
                    }
                    Row(
                        Modifier
                            .heightIn(min = 48.dp)
                            .clip(CircleShape)
                            .background(Peach)
                            .clickable(role = Role.Button, onClickLabel = "Open help", onClick = ::openHelp)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RushIcon(R.drawable.ic_help_circle, null, Modifier.size(20.dp), Coral)
                        Spacer(Modifier.width(7.dp))
                        Text("Help", color = DeepRed, style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(Modifier.height(heroSpacing))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = if (largeText) heroHeight + 36.dp else heroHeight),
                ) {
                    Image(
                        painterResource(R.drawable.login_rider),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .fillMaxWidth(if (largeText) 0.55f else 0.68f)
                            .height(160.dp)
                            .alpha(if (largeText) .25f else 1f),
                        contentScale = ContentScale.Fit,
                    )
                    Column(
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(if (largeText) 1f else .80f)
                            .padding(bottom = 2.dp),
                    ) {
                        Text(
                            "Groceries,\njust a tap away!",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Ink,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Login to get your daily essentials delivered to your doorstep.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Muted,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = CardWhite,
                    shadowElevation = 1.dp,
                    border = BorderStroke(1.dp, Border.copy(alpha = .45f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(if (compactWidth) 14.dp else 18.dp)) {
                        Text("Welcome", style = MaterialTheme.typography.titleMedium, color = Ink)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Continue with Google or a phone number to create your RushIt account " +
                                "or pick up where you left off.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                        )
                        Spacer(Modifier.height(18.dp))

                        if (errorMessage != null) {
                            AuthErrorBanner(errorMessage, onClearError)
                            Spacer(Modifier.height(14.dp))
                        }

                        // Inline Phone Number Input Field directly on welcome card
                        Box {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 40.dp)
                                    .clickable(enabled = !isBusy, role = Role.Button) { pickerOpen = true }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                val flagPrefix = if (selectedCountry.flag.isNotBlank()) "${selectedCountry.flag} " else ""
                                Text(
                                    "$flagPrefix${selectedCountry.name} (${selectedCountry.dialCode})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Ink,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    "Change",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Coral,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                            DropdownMenu(
                                expanded = pickerOpen,
                                onDismissRequest = { pickerOpen = false },
                            ) {
                                PhoneNumbers.countries.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            val flag = if (option.flag.isNotBlank()) "${option.flag} " else ""
                                            Text("$flag${option.name} (${option.dialCode})")
                                        },
                                        onClick = {
                                            pickerOpen = false
                                            countryIso = option.iso
                                            onClearError()
                                        },
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = number,
                            onValueChange = { typed ->
                                onClearError()
                                number = PhoneNumbers.nationalDigitsFor(selectedCountry, typed)
                                    .take(PhoneNumbers.maxDigits(selectedCountry))
                            },
                            placeholder = { Text("Enter mobile number", color = Muted) },
                            prefix = {
                                val flag = if (selectedCountry.flag.isNotBlank()) "${selectedCountry.flag} " else ""
                                Text(
                                    "$flag${selectedCountry.dialCode} ",
                                    color = Ink,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            },
                            supportingText = {
                                Text(
                                    if (showProblem) {
                                        problem.orEmpty()
                                    } else {
                                        "${selectedCountry.nationalDigits.last}-digit number without the country code."
                                    },
                                    color = if (showProblem) DeepRed else Muted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            },
                            isError = showProblem,
                            singleLine = true,
                            enabled = !isBusy,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Coral,
                                unfocusedBorderColor = Border,
                                focusedLabelColor = Coral,
                                cursorColor = Coral,
                                errorBorderColor = DeepRed,
                                errorLabelColor = DeepRed,
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { submitPhone() },
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(Modifier.height(14.dp))
                        RushButton(
                            text = "Continue →",
                            onClick = { submitPhone() },
                            enabled = canSend,
                            loading = phoneUiState.requesting,
                        )

                        if (phoneUiState.requesting) {
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    Modifier.size(16.dp),
                                    color = Coral,
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Asking Firebase to send a code…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Muted,
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        OrDivider()
                        Spacer(Modifier.height(16.dp))
                        GoogleSignInButton(
                            onClick = onGoogleSignIn,
                            enabled = !isBusy,
                            loading = uiState.signingIn,
                        )

                        if (recoveryAction == RecoveryAction.RETRY_SESSION) {
                            Spacer(Modifier.height(10.dp))
                            TextButton(
                                onClick = onRetry,
                                enabled = !isBusy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Retry", color = DeepRed, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                AuthPolicyFooter()
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    dialog?.let { message ->
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(dialogTitle) },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("Got it") } },
            containerColor = CardWhite,
        )
    }
}

/** The quiet "or" rule between the primary phone action and the Google alternative. */
@Composable
internal fun OrDivider(modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HorizontalDivider(Modifier.weight(1f), color = Border.copy(alpha = .8f))
        Text("or", style = MaterialTheme.typography.bodySmall, color = Muted)
        HorizontalDivider(Modifier.weight(1f), color = Border.copy(alpha = .8f))
    }
}

/** Inline, dismissible error. Announced politely so screen readers pick it up without stealing focus. */
@Composable
internal fun AuthErrorBanner(message: String, onDismiss: () -> Unit) {
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
                message,
                style = MaterialTheme.typography.bodySmall,
                color = DeepRed,
                textAlign = TextAlign.Start,
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
                    .clickable(role = Role.Button, onClickLabel = "Dismiss error", onClick = onDismiss)
                    .padding(horizontal = 4.dp, vertical = 6.dp),
            )
        }
    }
}
