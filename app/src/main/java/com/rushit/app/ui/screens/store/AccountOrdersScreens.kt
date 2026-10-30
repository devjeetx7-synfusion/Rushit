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
import com.rushit.app.domain.maskedIndianMobile
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
import com.rushit.app.ui.viewmodel.AuthUiState
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
fun AccountScreen(auth: AuthUiState, shop: ShopUiState, onWishlist: () -> Unit, onAddresses: () -> Unit, onOrders: () -> Unit, onSignInOrOut: () -> Unit, onHelp: () -> Unit, onNotifications: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var soundEnabled by remember { mutableStateOf(OrderSound.isEnabled(context)) }
    var info by rememberSaveable { mutableStateOf<String?>(null) }
    fun openPolicy(url: String, title: String) {
        if (!url.startsWith("https://")) info = "$title is not configured in this build."
        else try { uriHandler.openUri(url) } catch (_: Exception) { info = "Couldn't open $title. Check your browser and try again." }
    }
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Cream).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Account", style = MaterialTheme.typography.headlineMedium, color = Ink, modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 20.dp))
        Surface(shape = RoundedCornerShape(22.dp), color = CardWhite, border = BorderStroke(1.dp, Border)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(58.dp).clip(CircleShape).background(Peach), contentAlignment = Alignment.Center) { RushIcon(R.drawable.ic_user_02, null, Modifier.size(29.dp), Coral) }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(if (auth.authenticated) "You're signed in" else "Browsing the sample store", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Spacer(Modifier.height(4.dp))
                    Text(if (auth.authenticated && auth.phone.isNotBlank()) maskedIndianMobile(auth.phone) else if (auth.authenticated) "Phone authentication is active on this device." else "Sign in when phone authentication is configured.", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        AccountRow(R.drawable.ic_package_01, "Your orders", "Order history and tracking", onOrders)
        Spacer(Modifier.height(8.dp))
        AccountRow(R.drawable.ic_heart, "Wishlist", "${shop.wishlist.size} saved across modes", onWishlist)
        Spacer(Modifier.height(8.dp))
        AccountRow(R.drawable.ic_location_01, "Delivery addresses", shop.address ?: "Add an address", onAddresses)
        Spacer(Modifier.height(8.dp))
        AccountRow(R.drawable.ic_user_02, "Profile", "Phone account information", { info = "Profile editing is not available. Sign-in uses your verified phone number." })
        Spacer(Modifier.height(8.dp))
        AccountRow(R.drawable.ic_notification_01, "Notifications", "Updates and notification availability", onNotifications)
        Spacer(Modifier.height(8.dp))
        AccountRow(R.drawable.ic_help_circle, "Help & Support", "Orders, account and common questions", onHelp)
        Spacer(Modifier.height(8.dp))
        AccountRow(R.drawable.ic_lock_keyhole, "Privacy policy", "View policy", { openPolicy(BuildConfig.PRIVACY_URL, "Privacy policy") })
        Spacer(Modifier.height(8.dp))
        AccountRow(R.drawable.ic_package_01, "Terms and conditions", "View terms", { openPolicy(BuildConfig.TERMS_URL, "Terms and conditions") })
        Spacer(Modifier.height(8.dp))
        AccountRow(R.drawable.ic_help_circle, "App information", "RushIt ${BuildConfig.VERSION_NAME}", { info = "RushIt ${BuildConfig.VERSION_NAME} · Demo shopping and device-local orders." })
        Spacer(Modifier.height(8.dp))
        Surface(shape = RoundedCornerShape(16.dp), color = CardWhite, border = BorderStroke(1.dp, Border)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Order confirmation sound", style = MaterialTheme.typography.labelLarge, color = Ink)
                    Text("Also respects your device's sound settings", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it; OrderSound.setEnabled(context, it) })
            }
        }
        Spacer(Modifier.height(8.dp))
        AccountRow(R.drawable.ic_logout_01, if (auth.authenticated) "Sign out" else "Sign in", if (auth.busy) "Please wait…" else "",
            { if (auth.authenticated) confirmSignOut = true else onSignInOrOut() }, enabled = !auth.busy)
        auth.error?.let { Text(it, color = Coral, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) }
        Spacer(Modifier.height(24.dp))
        Text(if (shop.isSample) "Sample catalog · Demo checkout only; no live inventory or delivery" else "Orders and checkout are not connected yet", style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.padding(horizontal = 4.dp))
    }
    if (confirmSignOut) AlertDialog(onDismissRequest = { confirmSignOut = false }, title = { Text("Sign out?") },
        text = { Text("You'll need to verify your phone again to sign back in.") },
        confirmButton = { TextButton(onClick = { confirmSignOut = false; onSignInOrOut() }) { Text("Sign out") } },
        dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } }, containerColor = CardWhite)
    info?.let { message -> AlertDialog(onDismissRequest = { info = null }, title = { Text("Account") },
        text = { Text(message) }, confirmButton = { TextButton(onClick = { info = null }) { Text("Got it") } }, containerColor = CardWhite) }

}

@Composable
private fun AccountRow(icon: Int, title: String, subtitle: String, onClick: () -> Unit, enabled: Boolean = true) {
    Surface(shape = RoundedCornerShape(16.dp), color = CardWhite, border = BorderStroke(1.dp, Border.copy(alpha = .6f))) {
        Row(Modifier.fillMaxWidth().alpha(if (enabled) 1f else .5f).clip(RoundedCornerShape(16.dp)).clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            RushIcon(icon, null, Modifier.size(23.dp), Coral)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = Ink)
                if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            RushIcon(R.drawable.ic_arrow_right_01, null, Modifier.size(19.dp), Muted)
        }
    }
}
