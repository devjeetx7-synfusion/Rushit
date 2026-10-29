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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rushit.app.R
import com.rushit.app.ui.components.MessageCard
import com.rushit.app.ui.components.RushIcon
import com.rushit.app.ui.screens.home.AddressDialog
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.Cream
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.Muted
import com.rushit.app.ui.theme.Peach
import com.rushit.app.ui.viewmodel.AuthUiState
import com.rushit.app.ui.viewmodel.ShopUiState

@Composable
fun OrdersScreen(onHome: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Cream).padding(16.dp)) {
        Text("Orders", style = MaterialTheme.typography.headlineMedium, color = Ink, modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 22.dp))
        MessageCard("Your orders, all in one place", "Order history isn't connected yet. You can still explore the sample catalog.", action = "Explore groceries", onAction = onHome)
    }
}

@Composable
fun AccountScreen(auth: AuthUiState, shop: ShopUiState, onWishlist: () -> Unit, onSaveAddress: (String) -> Unit, onSignInOrOut: () -> Unit) {
    var showAddress by rememberSaveable { mutableStateOf(false) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Cream).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Account", style = MaterialTheme.typography.headlineMedium, color = Ink, modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 20.dp))
        Surface(shape = RoundedCornerShape(22.dp), color = CardWhite, border = BorderStroke(1.dp, Border)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(58.dp).clip(CircleShape).background(Peach), contentAlignment = Alignment.Center) { RushIcon(R.drawable.ic_user_02, null, Modifier.size(29.dp), Coral) }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(if (auth.authenticated) "You're signed in" else "Browsing the sample store", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Spacer(Modifier.height(4.dp))
                    Text(if (auth.authenticated) "Phone authentication is active on this device." else "Sign in when phone authentication is configured.", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        AccountRow(R.drawable.ic_heart, "Wishlist", "${shop.wishlist.size} saved", onWishlist)
        Spacer(Modifier.height(8.dp))
        AccountRow(R.drawable.ic_location_01, "Delivery address", shop.address ?: "Add an address", { showAddress = true })
        Spacer(Modifier.height(8.dp))
        AccountRow(R.drawable.ic_help_circle, "Help", "Phone sign-in and sample store", { showHelp = true })
        Spacer(Modifier.height(8.dp))
        AccountRow(R.drawable.ic_logout_01, if (auth.authenticated) "Sign out" else "Sign in", if (auth.busy) "Please wait…" else "", onSignInOrOut, enabled = !auth.busy)
        auth.error?.let { Text(it, color = Coral, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) }
        Spacer(Modifier.height(24.dp))
        Text(if (shop.isSample) "Sample catalog · No live inventory, orders or checkout" else "Orders and checkout are not connected yet", style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.padding(horizontal = 4.dp))
    }
    if (showAddress) AddressDialog(shop.address, onDismiss = { showAddress = false }, onSave = { onSaveAddress(it); showAddress = false })
    if (showHelp) AlertDialog(onDismissRequest = { showHelp = false }, title = { Text("Need a hand?") },
        text = { Text("The sample store is for browsing only. For phone sign-in, your app administrator must configure Supabase Auth and SMS delivery.") },
        confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Got it") } }, containerColor = CardWhite)
}

@Composable
private fun AccountRow(icon: Int, title: String, subtitle: String, onClick: () -> Unit, enabled: Boolean = true) {
    Surface(shape = RoundedCornerShape(16.dp), color = CardWhite, border = BorderStroke(1.dp, Border.copy(alpha = .6f))) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
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
