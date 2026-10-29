package com.rushit.app.ui.screens.store

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.rushit.app.R
import com.rushit.app.domain.formatPrice
import com.rushit.app.ui.components.MessageCard
import com.rushit.app.ui.components.RushButton
import com.rushit.app.ui.components.RushIcon
import com.rushit.app.ui.components.ScreenHeader
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.Cream
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.Muted
import com.rushit.app.ui.viewmodel.ShopUiState

@Composable
fun CheckoutScreen(state: ShopUiState, onBack: () -> Unit, onAddress: () -> Unit) {
    val products = remember(state.products, state.cart) { state.products.filter { (state.cart[it.id] ?: 0) > 0 } }
    Column(Modifier.fillMaxSize().background(Cream)) {
        ScreenHeader("Review checkout", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            MessageCard("Checkout isn't connected", "This is a review only. Payment, delivery fees and order placement are not available.")
            Spacer(Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(18.dp), color = CardWhite, border = BorderStroke(1.dp, Border)) {
                Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onAddress).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    RushIcon(R.drawable.ic_location_01, null, Modifier.size(24.dp), Coral)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Delivery address", style = MaterialTheme.typography.titleMedium, color = Ink)
                        Text(state.address ?: "Add an address", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                    RushIcon(R.drawable.ic_arrow_right_01, null, Modifier.size(20.dp), Coral)
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(18.dp), color = CardWhite, border = BorderStroke(1.dp, Border)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${state.selectedMode.label} summary", style = MaterialTheme.typography.titleMedium, color = Ink)
                    if (state.isSample) Text("Sample products · Illustrative prices", style = MaterialTheme.typography.bodySmall, color = Muted)
                    if (products.isEmpty()) Text("Your cart is empty.", style = MaterialTheme.typography.bodyMedium, color = Muted)
                    products.forEach { product ->
                        val quantity = state.cart[product.id] ?: 0
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${product.name} × $quantity", style = MaterialTheme.typography.bodyMedium, color = Ink, modifier = Modifier.weight(1f))
                            Text(formatPrice(product.pricePaise * quantity), style = MaterialTheme.typography.bodyMedium, color = Ink)
                        }
                    }
                    HorizontalDivider(color = Border)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal", style = MaterialTheme.typography.titleMedium, color = Ink)
                        Text(formatPrice(state.totals.totalPaise), style = MaterialTheme.typography.titleMedium, color = Ink)
                    }
                    Text("Payment options and delivery charges are not configured.", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
            Spacer(Modifier.height(18.dp))
        }
        Surface(color = CardWhite, shadowElevation = 8.dp) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                RushButton("Place order unavailable", onClick = {}, enabled = false, arrow = false)
                Text("No payment will be taken and no order will be created.", style = MaterialTheme.typography.bodySmall, color = Muted,
                    modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
fun TrackingScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Cream)) {
        ScreenHeader("Order tracking", onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            MessageCard("Nothing to track yet", "Live order tracking isn't configured. When real orders are supported, their status will appear here.", Modifier.padding(16.dp), icon = R.drawable.ic_package_01)
        }
    }
}
