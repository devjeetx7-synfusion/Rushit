package com.rushit.app.ui.screens.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rushit.app.BuildConfig
import com.rushit.app.R
import com.rushit.app.domain.isValidIndianMobile
import com.rushit.app.ui.components.RushButton
import com.rushit.app.ui.components.RushIcon
import com.rushit.app.ui.components.RushWordmark
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.Cream
import com.rushit.app.ui.theme.DeepRed
import com.rushit.app.ui.theme.FlagBlue
import com.rushit.app.ui.theme.FlagGreen
import com.rushit.app.ui.theme.FlagSaffron
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.Muted
import com.rushit.app.ui.theme.Peach
import com.rushit.app.ui.theme.SoftRed
import com.rushit.app.ui.viewmodel.AuthUiState
import java.net.URI

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    state: AuthUiState,
    onPhoneChange: (String) -> Unit,
    onContinue: () -> Unit,
    onBrowseSample: () -> Unit,
) {
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var dialogTitle by rememberSaveable { mutableStateOf("How can we help?") }
    val uriHandler = LocalUriHandler.current
    val largeText = LocalConfiguration.current.fontScale > 1.2f
    val validPhone = isValidIndianMobile(state.phone)

    fun openPolicy(url: String, title: String) {
        dialogTitle = title
        val valid = runCatching { URI(url).let { it.scheme == "https" && !it.host.isNullOrBlank() } }.getOrDefault(false)
        if (!valid) {
            dialog = "$title is not configured in this build. Please contact the RushIt team for the current policy."
        } else {
            try { uriHandler.openUri(url) } catch (_: Exception) { dialog = "Couldn't open $title. Check that a browser is installed and try again." }
        }
    }

    Column(
        Modifier.fillMaxSize().background(Cream).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                RushWordmark(Modifier.width(165.dp).height(67.dp))
                Text("Fresh Finds. Fast Delivery.", style = MaterialTheme.typography.labelSmall, color = Ink, modifier = Modifier.padding(start = 3.dp))
            }
            Row(
                Modifier.heightIn(min = 48.dp).clip(CircleShape).background(Peach)
                    .clickable(role = Role.Button, onClickLabel = "Open help") { dialogTitle = "How can we help?"; dialog = "Enter your Indian mobile number to receive a one-time SMS code. If a code doesn't arrive, check your connection and try again. Phone sign-in requires the store's SMS provider to be configured. You can also browse the sample store without signing in." }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RushIcon(R.drawable.ic_help_circle, null, Modifier.size(20.dp), Coral)
                Spacer(Modifier.width(7.dp))
                Text("Help", color = DeepRed, style = MaterialTheme.typography.labelMedium)
            }
        }
        Box(Modifier.fillMaxWidth().heightIn(min = if (largeText) 255.dp else 218.dp)) {
            Image(
                painterResource(R.drawable.login_rider), contentDescription = null,
                modifier = Modifier.align(Alignment.TopEnd).fillMaxWidth(if (largeText) 0.55f else 0.68f).height(160.dp).alpha(if (largeText) .25f else 1f),
                contentScale = ContentScale.Fit,
            )
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth(if (largeText) 1f else .80f).padding(bottom = 2.dp)) {
                Text("Groceries,\njust a tap away!", style = MaterialTheme.typography.headlineLarge, color = Ink)
                Spacer(Modifier.height(10.dp))
                Text("Login to get your daily essentials delivered to your doorstep.", style = MaterialTheme.typography.bodyLarge, color = Muted)
            }
        }
        Spacer(Modifier.height(20.dp))
        Surface(shape = RoundedCornerShape(24.dp), color = CardWhite, shadowElevation = 1.dp, border = BorderStroke(1.dp, Border.copy(alpha = .45f)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Enter your mobile number", style = MaterialTheme.typography.titleMedium, color = Ink)
                Spacer(Modifier.height(10.dp))
                PhoneInput(value = state.phone, onValueChange = onPhoneChange, onDone = { if (validPhone) onContinue() }, error = state.phone.length == 10 && !validPhone, enabled = !state.busy)
                if (state.phone.isNotEmpty() && !validPhone) {
                    Spacer(Modifier.height(6.dp))
                    val guidance = if (state.phone.length < 10) "Enter all 10 digits of your mobile number." else "Enter a valid Indian mobile number starting with 6–9."
                    Text(guidance, color = DeepRed, style = MaterialTheme.typography.bodySmall)
                }
                state.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = DeepRed, style = MaterialTheme.typography.bodySmall)
                }
                state.notice?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(16.dp))
                RushButton("Continue", onContinue, enabled = validPhone, loading = state.busy)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(Modifier.weight(1f), color = Border)
                    Text("or", color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 12.dp))
                    HorizontalDivider(Modifier.weight(1f), color = Border)
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 54.dp).clip(CircleShape).background(Peach.copy(alpha = .72f))
                        .clickable(enabled = !state.busy, role = Role.Button, onClick = onBrowseSample).padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    RushIcon(R.drawable.ic_shopping_bag_01, null, Modifier.size(20.dp), Coral)
                    Spacer(Modifier.width(9.dp))
                    Text("Browse the sample store", style = MaterialTheme.typography.labelMedium, color = Ink, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(6.dp))
                Text("No account needed · Not a live shop", style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.Center) {
            RushIcon(R.drawable.ic_lock_keyhole, null, Modifier.padding(top = 2.dp).size(19.dp), Muted)
            Spacer(Modifier.width(8.dp))
            FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("By continuing, you agree to our", style = MaterialTheme.typography.bodySmall, color = Muted)
                Text("Terms of Service", style = MaterialTheme.typography.bodySmall, color = DeepRed, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable(role = Role.Button) { openPolicy(BuildConfig.TERMS_URL, "Terms of Service") })
                Text("and", style = MaterialTheme.typography.bodySmall, color = Muted)
                Text("Privacy Policy.", style = MaterialTheme.typography.bodySmall, color = DeepRed, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable(role = Role.Button) { openPolicy(BuildConfig.PRIVACY_URL, "Privacy Policy") })
            }
        }
        Spacer(Modifier.height(32.dp))
    }
    if (dialog != null) {
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(dialogTitle) },
            text = { Text(dialog.orEmpty()) },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("Got it") } },
            containerColor = CardWhite,
        )
    }
}

@Composable
private fun PhoneInput(value: String, onValueChange: (String) -> Unit, onDone: () -> Unit, error: Boolean, enabled: Boolean) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 62.dp).clip(RoundedCornerShape(18.dp))
            .border(1.5.dp, if (error) Coral else Border, RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IndiaFlag()
        Spacer(Modifier.width(9.dp))
        Text("+91", style = MaterialTheme.typography.titleMedium, color = Ink)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.width(1.dp).height(24.dp).background(Border))
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = value, onValueChange = onValueChange, singleLine = true, enabled = enabled,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink, fontWeight = FontWeight.SemiBold),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }), cursorBrush = SolidColor(Coral),
            modifier = Modifier.weight(1f).semantics { contentDescription = "Mobile number" },
            decorationBox = { inner -> Box { if (value.isEmpty()) Text("98765 43210", style = MaterialTheme.typography.bodyLarge, color = Muted.copy(alpha = .65f)); inner() } },
        )
    }
}

@Composable
private fun IndiaFlag() {
    Canvas(Modifier.size(width = 28.dp, height = 19.dp).clip(RoundedCornerShape(3.dp)).semantics { contentDescription = "India" }) {
        drawRect(FlagSaffron, size = Size(size.width, size.height / 3))
        drawRect(CardWhite, topLeft = Offset(0f, size.height / 3), size = Size(size.width, size.height / 3))
        drawRect(FlagGreen, topLeft = Offset(0f, size.height * 2 / 3), size = Size(size.width, size.height / 3))
        drawCircle(FlagBlue, radius = size.height / 10, center = Offset(size.width / 2, size.height / 2))
    }
}
