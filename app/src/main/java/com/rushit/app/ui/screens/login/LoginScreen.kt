package com.rushit.app.ui.screens.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rushit.app.BuildConfig
import com.rushit.app.R
import com.rushit.app.data.auth.AuthState
import com.rushit.app.data.auth.RecoveryAction
import com.rushit.app.ui.components.GoogleSignInButton
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
import java.net.URI

/**
 * RushIt's sign-in screen.
 *
 * Two Firebase Authentication methods are offered — Google and a phone one-time code — so there is no
 * guest or sample-store shortcut: every shopping screen sits behind a real account. The screen keeps
 * the RushIt wordmark, the delivery rider, the cream and coral palette and the policy links.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    authState: AuthState,
    uiState: AuthUiState,
    errorMessage: String?,
    recoveryAction: RecoveryAction?,
    onGoogleSignIn: () -> Unit,
    onPhoneSignIn: () -> Unit,
    onRetry: () -> Unit,
    onClearError: () -> Unit,
) {
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var dialogTitle by rememberSaveable { mutableStateOf("How can we help?") }
    val uriHandler = LocalUriHandler.current
    val largeText = LocalConfiguration.current.fontScale > 1.2f
    val compactWidth = LocalConfiguration.current.screenWidthDp < 360

    // Both sources count as busy: the repository's own state and the button's in-flight flag.
    val isBusy = uiState.signingIn ||
        authState == AuthState.Authenticating ||
        authState == AuthState.Loading

    fun openPolicy(url: String, title: String) {
        dialogTitle = title
        val valid = runCatching { URI(url).let { it.scheme == "https" && !it.host.isNullOrBlank() } }
            .getOrDefault(false)
        dialog = if (!valid) {
            "$title is not configured in this build. Please contact the RushIt team for the current policy."
        } else {
            try {
                uriHandler.openUri(url)
                null
            } catch (error: Exception) {
                "Couldn't open $title. Check that a browser is installed and try again."
            }
        }
    }

    fun openHelp() {
        dialogTitle = "How can we help?"
        dialog = "Sign in with your Google account to shop, place orders and track deliveries. " +
            "Your Google password is never asked for or stored, and you can delete your RushIt " +
            "account at any time from Account settings."
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

                        GoogleSignInButton(
                            onClick = onGoogleSignIn,
                            enabled = !isBusy,
                            loading = uiState.signingIn,
                        )
                        Spacer(Modifier.height(10.dp))
                        PhoneSignInButton(onClick = onPhoneSignIn, enabled = !isBusy)

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
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    RushIcon(
                        R.drawable.ic_lock_keyhole,
                        null,
                        Modifier
                            .padding(top = 2.dp)
                            .size(19.dp),
                        Muted,
                    )
                    Spacer(Modifier.width(8.dp))
                    FlowRow(
                        Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            "By continuing, you agree to our",
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                        )
                        Text(
                            "Terms of Service",
                            style = MaterialTheme.typography.bodySmall,
                            color = DeepRed,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(role = Role.Button) {
                                    openPolicy(BuildConfig.TERMS_URL, "Terms of Service")
                                },
                        )
                        Text("and", style = MaterialTheme.typography.bodySmall, color = Muted)
                        Text(
                            "Privacy Policy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DeepRed,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(role = Role.Button) {
                                    openPolicy(BuildConfig.PRIVACY_URL, "Privacy Policy")
                                },
                        )
                    }
                }
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

/**
 * Secondary sign-in entry point.
 *
 * Deliberately quieter than the Google button: Google stays the primary action, and a phone sign-in
 * is one tap away without competing with it.
 */
@Composable
private fun PhoneSignInButton(onClick: () -> Unit, enabled: Boolean = true) {
    val active = enabled
    Surface(
        onClick = onClick,
        enabled = active,
        shape = CircleShape,
        color = Cream,
        contentColor = DeepRed,
        border = BorderStroke(1.dp, if (active) Coral.copy(alpha = .55f) else Border.copy(alpha = .5f)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .alpha(if (active) 1f else .62f),
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                "Continue with phone number",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = DeepRed,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
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
