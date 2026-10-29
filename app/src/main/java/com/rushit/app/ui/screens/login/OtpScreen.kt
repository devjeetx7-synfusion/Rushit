package com.rushit.app.ui.screens.login

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rushit.app.R
import com.rushit.app.domain.maskedIndianMobile
import com.rushit.app.ui.components.RoundIconButton
import com.rushit.app.ui.components.RushButton
import com.rushit.app.ui.components.RushWordmark
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.Cream
import com.rushit.app.ui.theme.DeepRed
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.Muted
import com.rushit.app.ui.theme.Peach
import com.rushit.app.ui.viewmodel.AuthUiState
import kotlinx.coroutines.delay

@Composable
fun OtpScreen(state: AuthUiState, onBack: () -> Unit, onOtpChange: (String) -> Unit, onVerify: () -> Unit, onResend: () -> Unit) {
    BackHandler { if (!state.busy) onBack() }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(state.resendAfterElapsedMillis) {
        while (now < state.resendAfterElapsedMillis) {
            now = SystemClock.elapsedRealtime()
            delay(1_000)
        }
    }
    val seconds = ((state.resendAfterElapsedMillis - now + 999) / 1_000).coerceAtLeast(0)
    Column(Modifier.fillMaxSize().background(Cream).imePadding().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(R.drawable.ic_arrow_left_01, "Go back", onBack, background = Peach, enabled = !state.busy)
            Spacer(Modifier.width(14.dp))
            RushWordmark(Modifier.width(125.dp).height(50.dp))
        }
        Spacer(Modifier.height(52.dp))
        Text("Verify your number", style = MaterialTheme.typography.headlineMedium, color = Ink)
        Spacer(Modifier.height(10.dp))
        Text("Enter the 6-digit code sent to ${maskedIndianMobile(state.phone)}.", style = MaterialTheme.typography.bodyLarge, color = Muted)
        Text("Change phone number", style = MaterialTheme.typography.labelMedium, color = Coral,
            modifier = Modifier.clip(CircleShape).clickable(enabled = !state.busy, role = Role.Button, onClick = onBack)
                .padding(vertical = 10.dp, horizontal = 4.dp))
        Spacer(Modifier.height(18.dp))
        Text("One-time code", style = MaterialTheme.typography.titleMedium, color = Ink)
        Spacer(Modifier.height(10.dp))
        BasicTextField(
            value = state.otp, onValueChange = onOtpChange, singleLine = true, enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardWhite)
                .border(1.5.dp, Border, RoundedCornerShape(18.dp)).padding(horizontal = 18.dp, vertical = 20.dp)
                .semantics { contentDescription = "Six-digit one-time code" },
            textStyle = MaterialTheme.typography.titleLarge.copy(color = Ink, letterSpacing = 5.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (state.otp.length == 6) onVerify() }),
            cursorBrush = SolidColor(Coral),
            decorationBox = { inner -> androidx.compose.foundation.layout.Box { if (state.otp.isEmpty()) Text("000000", color = Muted.copy(alpha = .5f), style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 5.sp)); inner() } },
        )
        state.error?.let { Spacer(Modifier.height(10.dp)); Text(it, color = DeepRed, style = MaterialTheme.typography.bodyMedium) }
        state.notice?.let { Spacer(Modifier.height(10.dp)); Text(it, color = Muted, style = MaterialTheme.typography.bodyMedium) }
        Spacer(Modifier.height(20.dp))
        RushButton("Verify & continue", onVerify, enabled = state.otp.length == 6, loading = state.busy)
        Spacer(Modifier.height(18.dp))
        if (seconds > 0) {
            Text("Resend code in ${seconds}s", style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Text("Resend code", style = MaterialTheme.typography.labelLarge, color = Coral,
                modifier = Modifier.align(Alignment.CenterHorizontally).clip(CircleShape)
                    .clickable(enabled = !state.busy, role = Role.Button, onClick = onResend).padding(horizontal = 16.dp, vertical = 12.dp))
        }
    }
}
