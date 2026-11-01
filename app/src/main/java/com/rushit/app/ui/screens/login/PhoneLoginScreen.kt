package com.rushit.app.ui.screens.login

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rushit.app.data.auth.phone.CountryCode
import com.rushit.app.data.auth.phone.PhoneNumbers
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
import com.rushit.app.R
import com.rushit.app.ui.viewmodel.PhoneLoginUiState

/**
 * Phone-number sign-in with a Firebase OTP.
 *
 * The screen owns only presentation state — the country, the typed digits and the typed code. The
 * verification id, the resend token and the code itself never leave the repository, so nothing here
 * is worth logging or persisting.
 *
 * @param uiState in-flight flags and the resend countdown, from [com.rushit.app.ui.viewmodel.AuthViewModel]
 * @param errorMessage a person-readable failure, already translated from a Firebase error code
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
) {
    var country by rememberSaveable { mutableStateOf(PhoneNumbers.default.iso) }
    var number by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var pickerOpen by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val selected = PhoneNumbers.byIso(country) ?: PhoneNumbers.default
    val digits = PhoneNumbers.digitsOf(number)
    val canSend = digits.isNotEmpty() && !uiState.busy

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                onClick = onBack,
                enabled = !uiState.busy,
                shape = CircleShape,
                color = Peach,
                modifier = Modifier.heightIn(min = 44.dp),
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RushIcon(R.drawable.ic_arrow_left_01, "Back", Modifier.size(18.dp), DeepRed)
                    Spacer(Modifier.width(6.dp))
                    Text("Back", color = DeepRed, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        Column(Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
            Text("Sign in with your number", style = MaterialTheme.typography.headlineSmall, color = Ink)
            Spacer(Modifier.height(8.dp))
            Text(
                if (uiState.codeSent) {
                    "Enter the 6-digit code RushIt texted to ${selected.dialCode} $digits."
                } else {
                    "RushIt will text you a one-time code. Your number is only used to sign you in."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
            )
            Spacer(Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = CardWhite,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, Border.copy(alpha = .45f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(18.dp)) {
                    if (!uiState.codeSent) {
                        Box {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .clickable(enabled = !uiState.busy, role = Role.Button) { pickerOpen = true }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "${selected.name} (${selected.dialCode})",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Ink,
                                )
                                Text("Change", style = MaterialTheme.typography.labelMedium, color = Coral)
                            }
                            DropdownMenu(expanded = pickerOpen, onDismissRequest = { pickerOpen = false }) {
                                PhoneNumbers.countries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text("${option.name} (${option.dialCode})") },
                                        onClick = {
                                            country = option.iso
                                            pickerOpen = false
                                            onClearError()
                                        },
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = number,
                            onValueChange = { onClearError(); number = it.filter(Char::isDigit).take(15) },
                            label = { Text("Phone number") },
                            prefix = {
                                Text(
                                    selected.dialCode,
                                    color = Muted,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            },
                            supportingText = {
                                Text(
                                    "Digits only — RushIt adds ${selected.dialCode} automatically.",
                                    color = Muted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            },
                            singleLine = true,
                            enabled = !uiState.busy,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Coral,
                                unfocusedBorderColor = Border,
                                focusedLabelColor = Coral,
                                cursorColor = Coral,
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { onClearError(); code = it.filter(Char::isDigit).take(6) },
                            label = { Text("6-digit code") },
                            supportingText = {
                                Text(
                                    "Firebase may fill this in automatically.",
                                    color = Muted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            },
                            singleLine = true,
                            enabled = !uiState.verifying,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Coral,
                                unfocusedBorderColor = Border,
                                focusedLabelColor = Coral,
                                cursorColor = Coral,
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (errorMessage != null) {
                        Spacer(Modifier.height(14.dp))
                        // Announced politely so a screen reader reports a rejected code without
                        // stealing focus from the field the person is correcting.
                        AuthErrorBanner(errorMessage, onClearError)
                    }

                    Spacer(Modifier.height(18.dp))
                    if (!uiState.codeSent) {
                        RushButton(
                            text = "Send code",
                            onClick = { focusManager.clearFocus(); onSendCode(selected, number) },
                            enabled = canSend,
                            loading = uiState.requesting,
                        )
                    } else {
                        RushButton(
                            text = "Verify and continue",
                            onClick = { focusManager.clearFocus(); onVerifyCode(code) },
                            enabled = !uiState.busy && code.isNotBlank(),
                            loading = uiState.verifying,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            if (uiState.resendSecondsRemaining > 0) {
                                Text(
                                    "Resend in ${uiState.resendSecondsRemaining}s",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Muted,
                                )
                            } else {
                                TextButton(
                                    onClick = onResendCode,
                                    enabled = !uiState.busy,
                                ) {
                                    Text("Resend code", color = DeepRed, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            TextButton(onClick = onChangeNumber, enabled = !uiState.busy) {
                                Text("Change number", color = Muted)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (uiState.requesting || uiState.verifying) {
                    CircularProgressIndicator(Modifier.size(16.dp), color = Coral, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (uiState.verifying) "Checking your code…" else "Asking Firebase to send a code…",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                } else {
                    Text(
                        "Message and data rates may apply. RushIt never sees your code.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
