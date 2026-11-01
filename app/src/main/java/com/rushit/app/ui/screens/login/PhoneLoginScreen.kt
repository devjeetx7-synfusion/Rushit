package com.rushit.app.ui.screens.login

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rushit.app.R
import com.rushit.app.data.auth.phone.CountryCode
import com.rushit.app.data.auth.phone.PhoneNumbers
import com.rushit.app.data.auth.phone.VerificationCode
import com.rushit.app.ui.components.GoogleSignInButton
import com.rushit.app.ui.components.OtpCodeInput
import com.rushit.app.ui.components.RushButton
import com.rushit.app.ui.components.RushIcon
import com.rushit.app.ui.components.RushWordmark
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.DeepRed
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.Muted
import com.rushit.app.ui.theme.Peach
import com.rushit.app.ui.viewmodel.PhoneLoginUiState

/**
 * Phone sign-in with a Firebase one-time code.
 *
 * One navigation route, two steps: the number entry screen, then — only after Firebase confirms it
 * accepted the request — the OTP screen. Keeping both steps here means the number the code went to is
 * still available for the "sent to +91 XXXXX X3210" line, and the system back button can undo the
 * OTP step instead of abandoning a verification the person already paid an SMS for.
 *
 * The screen owns presentation state only. The Firebase verification id, the resend token and the
 * code never leave the repository, so nothing here is worth logging or persisting.
 *
 * @param uiState in-flight flags and the resend countdown, from [com.rushit.app.ui.viewmodel.AuthViewModel]
 * @param errorMessage a person-readable failure, already translated from a Firebase error code
 * @param onGoogleSignIn the Google alternative, offered on the number step only
 */
@Composable
fun PhoneLoginScreen(
    uiState: PhoneLoginUiState,
    errorMessage: String?,
    onSendCode: (CountryCode, String) -> Unit,
    onResendCode: () -> Unit,
    onVerifyCode: (String) -> Unit,
    onChangeNumber: () -> Unit,
    onBack: () -> Unit,
    onClearError: () -> Unit,
    onGoogleSignIn: () -> Unit,
    googleLoading: Boolean,
) {
    var countryIso by rememberSaveable { mutableStateOf(PhoneNumbers.default.iso) }
    var number by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    val selected = PhoneNumbers.byIso(countryIso) ?: PhoneNumbers.default

    // Back out of the OTP step, not out of the flow: the pending Firebase verification is discarded
    // here so the number can be corrected without tripping the "already requested" guard.
    BackHandler(enabled = uiState.codeSent) {
        code = ""
        onChangeNumber()
    }

    if (uiState.codeSent) {
        OtpVerificationScreen(
            maskedPhoneNumber = PhoneNumbers.normalize(selected, number)?.masked
                ?: "${selected.dialCode} ${PhoneNumbers.maskNational(PhoneNumbers.digitsOf(number))}",
            uiState = uiState,
            code = code,
            onCodeChange = {
                onClearError()
                code = it
            },
            onClearCode = { code = "" },
            errorMessage = errorMessage,
            onVerify = { onVerifyCode(code) },
            onResend = onResendCode,
            onChangeNumber = {
                code = ""
                onChangeNumber()
            },
            onBack = onBack,
            onClearError = onClearError,
        )
    } else {
        PhoneNumberScreen(
            selected = selected,
            number = number,
            onNumberChange = {
                onClearError()
                number = it
            },
            onCountryChange = {
                countryIso = it.iso
                onClearError()
            },
            uiState = uiState,
            errorMessage = errorMessage,
            onSendCode = { onSendCode(selected, number) },
            onBack = onBack,
            onClearError = onClearError,
            onGoogleSignIn = onGoogleSignIn,
            googleLoading = googleLoading,
        )
    }
}

/** Step 1 — the mobile number that will receive the code. */
@Composable
internal fun PhoneNumberScreen(
    selected: CountryCode,
    number: String,
    onNumberChange: (String) -> Unit,
    onCountryChange: (CountryCode) -> Unit,
    uiState: PhoneLoginUiState,
    errorMessage: String?,
    onSendCode: () -> Unit,
    onBack: () -> Unit,
    onClearError: () -> Unit,
    onGoogleSignIn: () -> Unit,
    googleLoading: Boolean,
) {
    var pickerOpen by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val compactWidth = LocalConfiguration.current.screenWidthDp < 360
    val digits = PhoneNumbers.digitsOf(number)
    val problem = if (digits.isEmpty()) null else PhoneNumbers.errorFor(selected, number)
    // While the number is still being typed the helper text stays neutral: turning the field red at
    // the third digit is discouraging, and the disabled button already says it is not complete.
    val showProblem = problem != null && digits.length >= selected.nationalDigits.first
    val canSend = PhoneNumbers.normalize(selected, number) != null && !uiState.busy && !googleLoading

    AuthScreenScaffold(
        backEnabled = !uiState.busy,
        onBack = onBack,
    ) {
        Text(
            "Enter your mobile number",
            style = MaterialTheme.typography.headlineMedium,
            color = Ink,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "We'll send a ${VerificationCode.LENGTH}-digit OTP to verify your number.",
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
        )
        Spacer(Modifier.height(20.dp))

        AuthCard {
            // Country selector: the dial code stays separate from the editable number, which is what
            // keeps E.164 assembly correct and lets a pasted number be normalized safely.
            Box {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clickable(enabled = !uiState.busy, role = Role.Button) { pickerOpen = true }
                        .padding(horizontal = 4.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            "Country",
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${selected.name} (${selected.dialCode})",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Ink,
                        )
                    }
                    Text(
                        "Change",
                        style = MaterialTheme.typography.labelMedium,
                        color = Coral,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                DropdownMenu(expanded = pickerOpen, onDismissRequest = { pickerOpen = false }) {
                    PhoneNumbers.countries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text("${option.name} (${option.dialCode})") },
                            onClick = {
                                pickerOpen = false
                                onCountryChange(option)
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = number,
                onValueChange = { typed ->
                    // Digits only: a phone field must never accept letters. A pasted international
                    // number is reduced to its national part first, then capped at the country's
                    // length, so a paste can never be truncated into a different number.
                    onNumberChange(
                        PhoneNumbers.nationalDigitsFor(selected, typed)
                            .take(PhoneNumbers.maxDigits(selected)),
                    )
                },
                label = { Text("Mobile number") },
                placeholder = { Text("98765 43210", color = Muted) },
                prefix = {
                    Text(
                        selected.dialCode,
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
                            "${selected.nationalDigits.last}-digit number without the country code."
                        },
                        color = if (showProblem) DeepRed else Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                isError = showProblem,
                singleLine = true,
                enabled = !uiState.busy,
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
                    onDone = {
                        focusManager.clearFocus()
                        if (canSend) onSendCode()
                    },
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                AuthErrorBanner(errorMessage, onClearError)
            }

            Spacer(Modifier.height(18.dp))
            RushButton(
                text = "Send OTP",
                onClick = {
                    focusManager.clearFocus()
                    onSendCode()
                },
                enabled = canSend,
                loading = uiState.requesting,
            )

            Spacer(Modifier.height(16.dp))
            OrDivider()
            Spacer(Modifier.height(16.dp))
            GoogleSignInButton(
                onClick = onGoogleSignIn,
                enabled = !uiState.busy && !googleLoading,
                loading = googleLoading,
            )

            if (uiState.requesting) {
                Spacer(Modifier.height(12.dp))
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
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Message and data rates may apply. RushIt never sees your code.",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
            modifier = Modifier.padding(horizontal = if (compactWidth) 2.dp else 8.dp),
        )
        Spacer(Modifier.height(24.dp))
        AuthPolicyFooter(horizontalPadding = 0)
        Spacer(Modifier.height(28.dp))
    }
}

/** Step 2 — the six-digit code Firebase texted to the number from step 1. */
@Composable
internal fun OtpVerificationScreen(
    maskedPhoneNumber: String,
    uiState: PhoneLoginUiState,
    code: String,
    onCodeChange: (String) -> Unit,
    onClearCode: () -> Unit,
    errorMessage: String?,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onChangeNumber: () -> Unit,
    onBack: () -> Unit,
    onClearError: () -> Unit,
) {
    val complete = VerificationCode.isComplete(code)
    val countingDown = uiState.resendSecondsRemaining > 0

    // A rejected code cannot be corrected box by box (the six boxes are one text field), so a failed
    // verification clears it and the field takes focus again. Only a verification attempt does this —
    // a failed resend must never wipe digits the person already typed — and the explanation is kept
    // on screen until the next typed digit dismisses it.
    var wasVerifying by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.verifying) {
        if (uiState.verifying) {
            wasVerifying = true
        } else if (wasVerifying) {
            wasVerifying = false
            if (errorMessage != null) onClearCode()
        }
    }

    AuthScreenScaffold(backEnabled = !uiState.busy, onBack = onBack) {
        Text(
            "Enter verification code",
            style = MaterialTheme.typography.headlineMedium,
            color = Ink,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "We've sent a ${VerificationCode.LENGTH}-digit OTP to $maskedPhoneNumber.",
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
        )
        Spacer(Modifier.height(20.dp))

        AuthCard {
            OtpCodeInput(
                value = code,
                onValueChange = onCodeChange,
                enabled = !uiState.verifying,
                isError = errorMessage != null,
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(14.dp))
                AuthErrorBanner(errorMessage, onClearError)
            }

            Spacer(Modifier.height(18.dp))
            RushButton(
                text = "Verify & Continue",
                onClick = onVerify,
                enabled = complete && !uiState.busy,
                loading = uiState.verifying,
            )

            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (countingDown) {
                    Text(
                        "Resend OTP in ${VerificationCode.formatCountdown(uiState.resendSecondsRemaining)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Muted,
                    )
                } else {
                    TextButton(onClick = onResend, enabled = !uiState.busy) {
                        Text("Resend OTP", color = DeepRed, fontWeight = FontWeight.SemiBold)
                    }
                }
                TextButton(onClick = onChangeNumber, enabled = !uiState.busy) {
                    Text("Change number", color = Muted)
                }
            }

            if (uiState.verifying) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        Modifier.size(16.dp),
                        color = Coral,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Checking your code with Firebase…",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        AuthPolicyFooter(horizontalPadding = 0)
        Spacer(Modifier.height(28.dp))
    }
}

/**
 * Shared shell for the two phone steps: brand mark, back control, scrollable content.
 *
 * Scrollable and inset-aware so a small phone, a landscape orientation or a raised keyboard can never
 * clip the button that moves the flow forward.
 */
@Composable
private fun AuthScreenScaffold(
    backEnabled: Boolean,
    onBack: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                onClick = onBack,
                enabled = backEnabled,
                shape = CircleShape,
                color = Peach,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RushIcon(R.drawable.ic_arrow_left_01, null, Modifier.size(18.dp), DeepRed)
                    Spacer(Modifier.width(6.dp))
                    Text("Back", color = DeepRed, style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.weight(1f))
            RushWordmark(Modifier.width(118.dp).height(48.dp))
        }
        Spacer(Modifier.height(22.dp))
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            content = content,
        )
    }
}

/** The white card every sign-in step uses, so the three screens share one surface treatment. */
@Composable
internal fun AuthCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = CardWhite,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, Border.copy(alpha = .45f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}
