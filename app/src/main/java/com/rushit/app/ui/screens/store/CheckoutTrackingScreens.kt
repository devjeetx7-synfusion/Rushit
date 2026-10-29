package com.rushit.app.ui.screens.store

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rushit.app.R
import com.rushit.app.data.model.Product
import com.rushit.app.domain.DeliveryPricing
import com.rushit.app.domain.formatPrice
import com.rushit.app.ui.components.CatalogImage
import com.rushit.app.ui.components.MessageCard
import com.rushit.app.ui.components.RushButton
import com.rushit.app.ui.components.RushIcon
import com.rushit.app.ui.components.ScreenHeader
import com.rushit.app.ui.theme.Amber
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.Cream
import com.rushit.app.ui.theme.DeepRed
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.Muted
import com.rushit.app.ui.theme.Peach
import com.rushit.app.ui.theme.SoftAmber
import com.rushit.app.ui.theme.SoftRed
import com.rushit.app.ui.viewmodel.OrderUiState
import com.rushit.app.ui.viewmodel.ShopUiState

/**
 * Checkout mirrors the storyboard bill layout, but stays honest: there is no payment gateway or
 * fulfilment backend, so the primary action creates a clearly-labelled local demo order.
 */
@Composable
fun CheckoutScreen(
    state: ShopUiState,
    orderState: OrderUiState,
    onBack: () -> Unit,
    onAddress: () -> Unit,
    onChangeQuantity: (String, Int) -> Unit,
    onPlaceOrder: () -> Unit,
) {
    val products = remember(state.products, state.cart) { state.products.filter { (state.cart[it.id] ?: 0) > 0 } }
    val itemsPaise = state.totals.totalPaise
    val deliveryFee = DeliveryPricing.deliveryFee(itemsPaise)
    val platformFee = DeliveryPricing.platformFee()
    val payable = DeliveryPricing.total(itemsPaise)

    Column(Modifier.fillMaxSize().background(Cream)) {
        ScreenHeader("Checkout", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            // Delivery address
            Surface(shape = RoundedCornerShape(18.dp), color = CardWhite, border = BorderStroke(1.dp, Border)) {
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Change delivery address", onClick = onAddress).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(SoftRed), contentAlignment = Alignment.Center) {
                        RushIcon(R.drawable.ic_location_01, null, Modifier.size(22.dp), Coral)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Deliver to", style = MaterialTheme.typography.bodySmall, color = Muted)
                        Text(
                            state.address ?: "Add a delivery address",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (state.address == null) Coral else Ink,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text("Change", style = MaterialTheme.typography.labelMedium, color = Coral)
                }
            }
            Spacer(Modifier.height(12.dp))

            // Items with quantity controls
            if (products.isEmpty()) {
                MessageCard("Your cart is empty", "Add items before checking out.", icon = R.drawable.ic_shopping_cart_01)
            } else {
                Surface(shape = RoundedCornerShape(18.dp), color = CardWhite, border = BorderStroke(1.dp, Border)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("${products.size} ${if (products.size == 1) "item" else "items"}", style = MaterialTheme.typography.titleMedium, color = Ink)
                        products.forEach { product ->
                            CheckoutLine(
                                product = product,
                                quantity = state.cart[product.id] ?: 0,
                                onChangeQuantity = onChangeQuantity,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // Bill details
            Surface(shape = RoundedCornerShape(18.dp), color = CardWhite, border = BorderStroke(1.dp, Border)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bill Details", style = MaterialTheme.typography.titleMedium, color = Ink)
                    BillLine("Items Total", formatPrice(itemsPaise))
                    BillLine("Delivery Fee", if (deliveryFee == 0L) "FREE" else formatPrice(deliveryFee), highlight = deliveryFee == 0L)
                    BillLine("Platform Fee", formatPrice(platformFee))
                    HorizontalDivider(color = Border)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("To Pay", style = MaterialTheme.typography.titleLarge, color = Ink)
                        Text(formatPrice(payable), style = MaterialTheme.typography.titleLarge, color = Ink)
                    }
                    Text(
                        "Illustrative fees for the sample store. No payment is taken.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            Surface(shape = RoundedCornerShape(16.dp), color = SoftAmber, border = BorderStroke(1.dp, Amber.copy(alpha = 0.45f))) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RushIcon(R.drawable.ic_help_circle, null, Modifier.size(20.dp), Amber)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Demo checkout: placing an order creates a device-local demo order so you can preview the delivery journey. Nothing is charged or delivered.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Ink,
                    )
                }
            }
            orderState.error?.let { error ->
                Spacer(Modifier.height(10.dp))
                Text(error, style = MaterialTheme.typography.bodySmall, color = DeepRed)
            }
            Spacer(Modifier.height(18.dp))
        }

        Surface(color = CardWhite, shadowElevation = 8.dp) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                RushButton(
                    text = if (orderState.placing) "Placing demo order…" else "Place Order · ${formatPrice(payable)}",
                    onClick = onPlaceOrder,
                    enabled = products.isNotEmpty() && !orderState.placing,
                    loading = orderState.placing,
                )
                Text(
                    "Creates a local demo order · no payment, no real delivery",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun CheckoutLine(product: Product, quantity: Int, onChangeQuantity: (String, Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Peach.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
            CatalogImage(product.imageUrl, product.imageRes, product.name, Modifier.size(46.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(product.name, style = MaterialTheme.typography.labelLarge, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(product.unit, style = MaterialTheme.typography.bodySmall, color = Muted)
            Text(formatPrice(product.pricePaise * quantity), style = MaterialTheme.typography.labelMedium, color = Ink)
        }
        Row(
            Modifier.clip(CircleShape).background(SoftRed).padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuantityControl(R.drawable.ic_minus_sign, "Remove one ${product.name}") { onChangeQuantity(product.id, -1) }
            Text("$quantity", style = MaterialTheme.typography.labelLarge, color = Ink, modifier = Modifier.padding(horizontal = 6.dp))
            QuantityControl(
                R.drawable.ic_add_01,
                "Add one ${product.name}",
                enabled = !product.previewOnly && product.isAvailable && quantity < (product.stockQuantity?.coerceAtMost(99) ?: 99),
            ) { onChangeQuantity(product.id, 1) }
        }
    }
}

@Composable
private fun QuantityControl(icon: Int, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.size(34.dp).clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        RushIcon(icon, description, Modifier.size(16.dp), if (enabled) Coral else Muted)
    }
}

@Composable
private fun BillLine(label: String, value: String, highlight: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Muted)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = if (highlight) Coral else Ink)
    }
}

/**
 * Tracking entry point. Shows the most recent stored order; when none exists it says so instead of
 * inventing a delivery.
 */
@Composable
fun TrackingScreen(
    orders: List<com.rushit.app.data.model.Order>,
    onBack: () -> Unit,
    onOpenOrder: (String) -> Unit,
    onShop: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Cream)) {
        ScreenHeader("Order tracking", onBack)
        if (orders.isEmpty()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                MessageCard(
                    "No orders to track",
                    "Tracking shows the status of your stored orders. Place a demo order from checkout to preview the delivery journey.",
                    Modifier.padding(16.dp),
                    icon = R.drawable.ic_package_01,
                    action = "Back to store",
                    onAction = onShop,
                )
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(orders, key = { it.id }) { order ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = CardWhite,
                        border = BorderStroke(1.dp, Border),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().clickable(role = Role.Button) { onOpenOrder(order.id) }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(42.dp).clip(CircleShape).background(Peach), contentAlignment = Alignment.Center) {
                                RushIcon(R.drawable.ic_package_01, null, Modifier.size(22.dp), Coral)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Order #${order.id}", style = MaterialTheme.typography.titleMedium, color = Ink)
                                Text("${order.status.label} · ${order.itemCount} items", style = MaterialTheme.typography.bodySmall, color = Muted)
                            }
                            RushIcon(R.drawable.ic_arrow_right_01, null, Modifier.size(20.dp), Coral)
                        }
                    }
                }
                item(key = "demo_note") {
                    Text(
                        "These orders are stored on this device only. Live tracking needs an order backend.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

/** Small inline progress used while an order is being created. */
@Composable
fun InlineLoading(text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Coral)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}

/** Kept for callers that only need a simple text action. */
@Composable
fun CheckoutHelpAction(onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text("Need help?", color = Coral, style = MaterialTheme.typography.labelMedium) }
}
