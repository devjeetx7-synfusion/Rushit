package com.rushit.app.ui.screens.store

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import com.rushit.app.BuildConfig
import com.rushit.app.ui.sound.OrderSound
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rushit.app.R
import com.rushit.app.ui.components.MessageCard
import com.rushit.app.ui.components.RushIcon
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.Cream
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.Muted
import com.rushit.app.ui.theme.Peach
import com.rushit.app.domain.formatPrice
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import com.rushit.app.data.auth.AuthState
import com.rushit.app.ui.components.GoogleSignInButton
import com.rushit.app.ui.theme.DeepRed
import com.rushit.app.ui.theme.SoftRed
import com.rushit.app.ui.viewmodel.AuthUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.rushit.app.ui.viewmodel.OrderUiState
import com.rushit.app.ui.viewmodel.ShopUiState

@Composable
fun OrdersScreen(
    state: OrderUiState,
    onHome: () -> Unit,
    onOpenOrder: (String) -> Unit,
    onTracking: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Cream).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Orders", style = MaterialTheme.typography.headlineMedium, color = Ink, modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 18.dp))
        if (state.ordersLoading) {
            androidx.compose.material3.CircularProgressIndicator(color = Coral)
        } else if (state.ordersError != null) {
            MessageCard("Orders unavailable", state.ordersError, action = "Try again", onAction = onRetry)
        } else if (state.orders.isEmpty()) {
            MessageCard(
                "Your orders, all in one place",
                "No orders have been placed from this device yet. Orders created here are demo orders, not live purchases.",
                action = "Explore the catalog",
                onAction = onHome,
            )
        } else {
            Text(
                "Stored on this device · demo orders",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
            state.orders.forEach { order ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = CardWhite,
                    border = BorderStroke(1.dp, Border),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth().clickable(role = Role.Button) { onOpenOrder(order.id) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(42.dp).clip(CircleShape).background(Peach), contentAlignment = Alignment.Center) {
                            RushIcon(R.drawable.ic_package_01, null, Modifier.size(22.dp), Coral)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Order #${order.id}", style = MaterialTheme.typography.titleMedium, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${order.status.label} · ${order.itemCount} ${if (order.itemCount == 1) "item" else "items"} · ${formatPrice(order.totalPaise)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted,
                            )
                        }
                        RushIcon(R.drawable.ic_arrow_right_01, null, Modifier.size(20.dp), Coral)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        AccountRow(R.drawable.ic_location_01, "Order tracking", "Follow your latest order", onTracking)
    }
}

@Composable
fun AccountScreen(
    authState: AuthState,
    uiState: AuthUiState,
    errorMessage: String?,
    shop: ShopUiState,
    onWishlist: () -> Unit,
    onAddresses: () -> Unit,
    onOrders: () -> Unit,
    onEditProfile: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onClearError: () -> Unit,
    onHelp: () -> Unit,
    onNotifications: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var soundEnabled by remember { mutableStateOf(OrderSound.isEnabled(context)) }
    var info by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    fun openPolicy(url: String, title: String) {
        if (!url.startsWith("https://")) info = "$title is not configured in this build."
        else try { uriHandler.openUri(url) } catch (error: Exception) { info = "Couldn't open $title. Check your browser and try again." }
    }

    val account = (authState as? AuthState.Authenticated)?.account
    val isAuthenticated = account != null
    val isBusy = uiState.busy || authState is AuthState.Authenticating || authState is AuthState.Loading

    Column(Modifier.fillMaxSize().background(Cream).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Account", style = MaterialTheme.typography.headlineMedium, color = Ink, modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 20.dp))

        Surface(shape = RoundedCornerShape(22.dp), color = CardWhite, border = BorderStroke(1.dp, Border)) {
            if (isAuthenticated && account != null) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    AccountAvatar(account.avatarUrl, account.displayName)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(account.displayName ?: "RushIt shopper", style = MaterialTheme.typography.titleMedium, color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        account.username?.let {
                            Spacer(Modifier.height(2.dp))
                            Text("@$it", style = MaterialTheme.typography.bodySmall, color = Coral)
                        }
                        account.email?.let {
                            Spacer(Modifier.height(2.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        account.memberSinceEpochSeconds?.let { seconds ->
                            Spacer(Modifier.height(2.dp))
                            Text("Member since ${formatMemberSince(seconds)}", style = MaterialTheme.typography.bodySmall, color = Muted)
                        }
                    }
                }
            } else {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(58.dp).clip(CircleShape).background(Peach), contentAlignment = Alignment.Center) {
                            RushIcon(R.drawable.ic_user_02, null, Modifier.size(29.dp), Coral)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("You're signed out", style = MaterialTheme.typography.titleMedium, color = Ink)
                            Spacer(Modifier.height(4.dp))
                            Text("Sign in with Google to sync your RushIt account.", style = MaterialTheme.typography.bodySmall, color = Muted)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    GoogleSignInButton(onClick = onSignIn, enabled = !isBusy, loading = uiState.signingIn)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        AccountRow(R.drawable.ic_user_02, "Edit profile", if (isAuthenticated) "Name, username and photo" else "Sign in to edit your profile", onEditProfile, enabled = isAuthenticated && !isBusy)
        Spacer(Modifier.height(8.dp))
        AccountRow(R.drawable.ic_package_01, "Your orders", "Order history and tracking", onOrders)
        Spacer(Modifier.height(8.dp))
        AccountRow(R.drawable.ic_heart, "Wishlist", "${shop.wishlist.size} saved across modes", onWishlist)
        Spacer(Modifier.height(8.dp))
        AccountRow(R.drawable.ic_location_01, "Delivery addresses", shop.address ?: "Add an address", onAddresses)
        Spacer(Modifier.height(8.dp))
        AccountRow(R.drawable.ic_notification_01, "Notifications", "Updates and notification availability", onNotifications)
        Spacer(Modifier.height(8.dp))
        AccountRow(R.drawable.ic_help_circle, "Help & Support", "Orders, account and common questions", onHelp)
        Spacer(Modifier.height(8.dp))
        AccountRow(R.drawable.ic_lock_keyhole, "Privacy policy", "View policy", { openPolicy(BuildConfig.PRIVACY_URL, "Privacy policy") })
        Spacer(Modifier.height(8.dp))
        AccountRow(R.drawable.ic_package_01, "Terms and conditions", "View terms", { openPolicy(BuildConfig.TERMS_URL, "Terms and conditions") })
        Spacer(Modifier.height(8.dp))
        AccountRow(R.drawable.ic_help_circle, "App information", "RushIt ${BuildConfig.VERSION_NAME}", { info = "RushIt ${BuildConfig.VERSION_NAME} · Signed in with Google · demo catalog and device-local orders." })
        Spacer(Modifier.height(8.dp))
        Surface(shape = RoundedCornerShape(16.dp), color = CardWhite, border = BorderStroke(1.dp, Border)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Order confirmation sound", style = MaterialTheme.typography.labelLarge, color = Ink)
                    Text("Also respects your device's sound settings", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it; OrderSound.setEnabled(context, it) }, enabled = !isBusy)
            }
        }

        if (isAuthenticated) {
            Spacer(Modifier.height(8.dp))
            AccountRow(
                R.drawable.ic_logout_01,
                "Sign out",
                when {
                    uiState.signingOut -> "Signing out…"
                    else -> "Sign out of RushIt on this device"
                },
                { confirmSignOut = true },
                enabled = !isBusy,
            )
            Spacer(Modifier.height(8.dp))
            AccountRow(
                R.drawable.ic_cancel_01,
                "Delete account",
                when {
                    uiState.deletingAccount -> "Deleting your account…"
                    else -> "Permanently remove your RushIt account and data"
                },
                { confirmDelete = true },
                enabled = !isBusy,
                destructive = true,
            )
        } else {
            Spacer(Modifier.height(8.dp))
            AccountRow(R.drawable.ic_logout_01, "Sign in", "Continue with Google", onSignIn, enabled = !isBusy)
        }

        if (uiState.signingOut || uiState.deletingAccount) {
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), color = Coral, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(if (uiState.deletingAccount) "Deleting your account…" else "Signing out…", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }

        errorMessage?.let { message ->
            Spacer(Modifier.height(14.dp))
            Surface(shape = RoundedCornerShape(14.dp), color = SoftRed, border = BorderStroke(1.dp, Coral.copy(alpha = .28f)), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    RushIcon(R.drawable.ic_cancel_01, null, Modifier.size(18.dp), DeepRed)
                    Spacer(Modifier.width(10.dp))
                    Text(message, style = MaterialTheme.typography.bodySmall, color = DeepRed, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Text("Dismiss", style = MaterialTheme.typography.labelMedium, color = DeepRed, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button, onClickLabel = "Dismiss error", onClick = onClearError).padding(horizontal = 4.dp, vertical = 6.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(if (shop.isSample) "Sample catalog · Demo checkout only; no live inventory or delivery" else "Orders and checkout are not connected yet", style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.padding(horizontal = 4.dp))
        Spacer(Modifier.height(16.dp))
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { if (!isBusy) confirmSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("You'll need to sign in with Google again to reach your orders and profile.") },
            confirmButton = { TextButton(onClick = { confirmSignOut = false; onSignOut() }, enabled = !isBusy) { Text("Sign out", color = DeepRed) } },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }, enabled = !isBusy) { Text("Cancel", color = Ink) } },
            containerColor = CardWhite,
        )
    }

    if (confirmDelete) {
        DeleteAccountDialog(
            busy = uiState.deletingAccount,
            onDismiss = { if (!uiState.deletingAccount) confirmDelete = false },
            onConfirm = { confirmDelete = false; onDeleteAccount() },
        )
    }

    info?.let { message ->
        AlertDialog(onDismissRequest = { info = null }, title = { Text("Account") },
            text = { Text(message) }, confirmButton = { TextButton(onClick = { info = null }) { Text("Got it") } }, containerColor = CardWhite)
    }
}

/**
 * Account deletion needs an unmistakable confirmation: the person types DELETE before the request
 * goes to the server. Supabase then verifies the caller's own JWT server-side.
 */
@Composable
private fun DeleteAccountDialog(busy: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var confirmation by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete your account?", color = DeepRed) },
        text = {
            Column {
                Text("This permanently deletes your RushIt account, profile, settings and activity history from Supabase. It cannot be undone, and your device-local demo orders will be cleared too.")
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    label = { Text("Type DELETE to confirm") },
                    singleLine = true,
                    enabled = !busy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !busy && confirmation.trim() == DELETE_CONFIRMATION_WORD) {
                if (busy) CircularProgressIndicator(Modifier.size(16.dp), color = DeepRed, strokeWidth = 2.dp)
                else Text("Delete account", color = DeepRed)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Keep my account", color = Ink) } },
        containerColor = CardWhite,
    )
}

@Composable
private fun AccountAvatar(avatarUrl: String?, displayName: String?) {
    Box(Modifier.size(58.dp).clip(CircleShape).background(Peach), contentAlignment = Alignment.Center) {
        if (avatarUrl != null) {
            AsyncImage(model = avatarUrl, contentDescription = "Your profile photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
        } else {
            val initials = displayName.orEmpty().trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }
            if (initials.isNotBlank()) Text(initials, style = MaterialTheme.typography.titleMedium, color = Coral, fontWeight = FontWeight.Bold)
            else RushIcon(R.drawable.ic_user_02, null, Modifier.size(29.dp), Coral)
        }
    }
}

private const val DELETE_CONFIRMATION_WORD = "DELETE"

/** Formats the Supabase account creation time in the device locale. */
internal fun formatMemberSince(epochSeconds: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epochSeconds * 1000L))


@Composable
private fun AccountRow(
    icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    val accent = if (destructive) DeepRed else Coral
    val titleColor = if (destructive) DeepRed else Ink
    Surface(shape = RoundedCornerShape(16.dp), color = CardWhite, border = BorderStroke(1.dp, if (destructive) DeepRed.copy(alpha = .22f) else Border.copy(alpha = .6f))) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).alpha(if (enabled) 1f else .5f).clip(RoundedCornerShape(16.dp)).clickable(enabled = enabled, role = Role.Button, onClickLabel = title, onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            RushIcon(icon, null, Modifier.size(23.dp), accent)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = titleColor)
                if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            RushIcon(R.drawable.ic_arrow_right_01, null, Modifier.size(19.dp), Muted)
        }
    }
}
