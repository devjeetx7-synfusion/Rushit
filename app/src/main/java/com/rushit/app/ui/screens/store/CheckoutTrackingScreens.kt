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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
    onChangeQuantity: (String, String?, Int) -> Unit,
    onPlaceOrder: (String) -> Unit,
) {
    val cartItems = remember(state.products, state.cart) {
        state.cart.mapNotNull { (key, quantity) ->
            if (quantity <= 0) return@mapNotNull null
            val (productId, variantId) = com.rushit.app.domain.parseCartKey(key)
            val product = state.products.firstOrNull { it.id == productId } ?: return@mapNotNull null
            Triple(product, variantId, quantity)
        }
    }
    val itemsPaise = state.totals.totalPaise
    var selectedPaymentMethod by remember { androidx.compose.runtime.mutableStateOf("Cash on Delivery") }
    val paymentMethods = listOf("Cash on Delivery", "UPI", "Credit Card", "Net Banking", "Wallet")
    var showPaymentDemo by remember { androidx.compose.runtime.mutableStateOf(false) }
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
            if (cartItems.isEmpty()) {
                MessageCard("Your cart is empty", "Add items before checking out.", icon = R.drawable.ic_shopping_cart_01)
            } else {
                Surface(shape = RoundedCornerShape(18.dp), color = CardWhite, border = BorderStroke(1.dp, Border)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("${cartItems.size} ${if (cartItems.size == 1) "item" else "items"}", style = MaterialTheme.typography.titleMedium, color = Ink)
                        cartItems.forEach { (product, variantId, quantity) ->
                            CheckoutLine(
                                product = product,
                                variantId = variantId,
                                quantity = quantity,
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

            Spacer(Modifier.height(12.dp))
            
            // Payment Method
            Surface(shape = RoundedCornerShape(18.dp), color = CardWhite, border = BorderStroke(1.dp, Border)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Payment Method", style = MaterialTheme.typography.titleMedium, color = Ink)
                    paymentMethods.forEach { method ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPaymentMethod = method }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = (selectedPaymentMethod == method),
                                onClick = { selectedPaymentMethod = method },
                                colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = Coral)
                            )
                            Text(
                                text = method,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Ink,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    if (selectedPaymentMethod != "Cash on Delivery") {
                        Surface(shape = RoundedCornerShape(8.dp), color = Peach, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Text(
                                text = "Demo Mode: No real payment will be collected.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Coral,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
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
                    text = if (orderState.placing) "Placing demo order…" else if (selectedPaymentMethod == "Cash on Delivery") "Place Order · ${formatPrice(payable)}" else "Proceed to Payment · ${formatPrice(payable)}",
                    onClick = {
                        if (selectedPaymentMethod == "Cash on Delivery") {
                            onPlaceOrder(selectedPaymentMethod)
                        } else {
                            showPaymentDemo = true
                        }
                    },
                    enabled = cartItems.isNotEmpty() && !state.address.isNullOrBlank() && !orderState.placing,
                    loading = orderState.placing,
                )
                if (showPaymentDemo) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showPaymentDemo = false },
                        title = { Text("Simulate Payment") },
                        text = { Text("Simulate a successful payment for ${formatPrice(payable)} via $selectedPaymentMethod?") },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                showPaymentDemo = false
                                onPlaceOrder(selectedPaymentMethod)
                            }) {
                                Text("Simulate Success", color = Coral)
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                showPaymentDemo = false
                                // Simulate failure doesn't place order
                            }) {
                                Text("Simulate Failure")
                            }
                        }
                    )
                }
                Text(
                    if (state.address.isNullOrBlank()) "Add a delivery address to create a demo order." else "Creates a local demo order · no payment, no real delivery",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun CheckoutLine(product: Product, variantId: String?, quantity: Int, onChangeQuantity: (String, String?, Int) -> Unit) {
    val variant = product.variants.find { it.id == variantId }
    val price = variant?.pricePaise ?: product.pricePaise
    val stock = variant?.stockQuantity ?: product.stockQuantity
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(Peach.copy(alpha = .5f)), contentAlignment = Alignment.Center) {
            CatalogImage(product.imageUrl, product.imageRes, product.name, Modifier.size(48.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(product.name, style = MaterialTheme.typography.labelLarge, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (variant != null) {
                Text(variant.attributes.entries.joinToString(", ") { "${it.key}: ${it.value}" }, style = MaterialTheme.typography.bodySmall, color = Muted)
            } else {
                Text(product.unit, style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            Spacer(Modifier.height(2.dp))
            Text(formatPrice(price), style = MaterialTheme.typography.labelLarge, color = Ink)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            QuantityControl(R.drawable.ic_minus_sign, "Remove one ${product.name}") { onChangeQuantity(product.id, variantId, -1) }
            Text("$quantity", style = MaterialTheme.typography.labelMedium, color = Ink, modifier = Modifier.padding(horizontal = 8.dp))
            QuantityControl(
                R.drawable.ic_add_01, "Add one ${product.name}",
                enabled = !product.previewOnly && product.isAvailable && quantity < (stock?.coerceAtMost(99) ?: 99)
            ) { onChangeQuantity(product.id, variantId, 1) }
        }
    }
}

@Composable
private fun QuantityControl(icon: Int, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape)
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
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onOpenOrder: (String) -> Unit,
    onShop: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Cream)) {
        ScreenHeader("Order tracking", onBack)
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Coral) }
        } else if (error != null) {
            MessageCard("Orders unavailable", error, Modifier.padding(16.dp), action = "Try again", onAction = onRetry)
        } else if (orders.isEmpty()) {
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
