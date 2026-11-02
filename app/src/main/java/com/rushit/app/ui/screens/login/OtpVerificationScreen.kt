package com.rushit.app.ui.screens.login

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rushit.app.R
import com.rushit.app.data.auth.phone.VerificationCode
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
 * OTP Verification Screen.
 *
 * This is the only additional screen opened after the Welcome screen during phone authentication.
 *
 * @param maskedPhoneNumber the masked destination phone number (e.g. "+91 XXXXX X3210")
 * @param uiState phone login UI state including in-flight flags and resend countdown
 * @param errorMessage translated error message to display
 * @param onVerify called when user taps "Verify & Continue"
 * @param onResend called when user taps "Resend OTP" after countdown expires
 * @param onChangeNumber called when user taps "Change number" or presses Back to return to welcome screen
 * @param onClearError called to clear active error state
 */
@Composable
fun OtpVerificationScreen(
    maskedPhoneNumber: String,
    uiState: PhoneLoginUiState,
    errorMessage: String?,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    onChangeNumber: () -> Unit,
    onClearError: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    val complete = VerificationCode.isComplete(code)
    val countingDown = uiState.resendSecondsRemaining > 0

    // System Back button returns to the welcome screen, discarding pending challenge
    BackHandler(enabled = !uiState.verifying) {
        onChangeNumber()
    }

    var wasVerifying by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.verifying) {
        if (uiState.verifying) {
            wasVerifying = true
        } else if (wasVerifying) {
            wasVerifying = false
            if (errorMessage != null) code = ""
        }
    }

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
                onClick = onChangeNumber,
                enabled = !uiState.busy,
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
        ) {
            Text(
                "Enter verification code",
                style = MaterialTheme.typography.headlineMedium,
                color = Ink,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "We've sent a 6-digit OTP to $maskedPhoneNumber.",
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
                    OtpCodeInput(
                        value = code,
                        onValueChange = {
                            onClearError()
                            code = it
                        },
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
                        onClick = { onVerify(code) },
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
            }

            Spacer(Modifier.height(24.dp))
            AuthPolicyFooter(horizontalPadding = 0)
            Spacer(Modifier.height(28.dp))
        }
    }
}
