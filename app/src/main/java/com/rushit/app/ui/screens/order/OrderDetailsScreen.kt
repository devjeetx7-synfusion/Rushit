package com.rushit.app.ui.screens.order

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rushit.app.R
import com.rushit.app.data.model.Order
import com.rushit.app.data.model.OrderStatus
import com.rushit.app.domain.OrderTime
import com.rushit.app.domain.formatPrice
import com.rushit.app.domain.stageIndex
import com.rushit.app.ui.components.CatalogImage
import com.rushit.app.ui.components.DeliveryTimeline
import com.rushit.app.ui.components.MessageCard
import com.rushit.app.ui.components.RushIcon
import com.rushit.app.ui.components.ScreenHeader
import com.rushit.app.ui.components.StatusPill
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.Cream
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.Muted
import com.rushit.app.ui.theme.Peach
import com.rushit.app.ui.theme.Success
import com.rushit.app.ui.viewmodel.OrderUiState

@Composable
fun OrderDetailsScreen(
    state: OrderUiState,
    onBack: () -> Unit,
    onTrack: (String) -> Unit,
    onHelp: () -> Unit,
) {
    val order = state.order
    Column(Modifier.fillMaxSize().background(Cream)) {
        ScreenHeader("Order details", onBack)
        if (order == null) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                MessageCard(
                    title = "Order unavailable",
                    message = state.error ?: "This order could not be loaded.",
                    icon = R.drawable.ic_package_01,
                )
            }
            return@Column
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp)) {
            HeaderCard(order, onHelp, onTrack)
            Spacer(Modifier.height(12.dp))
            if (order.status != OrderStatus.CANCELLED && order.status != OrderStatus.FAILED) {
                Surface(shape = RoundedCornerShape(18.dp), color = CardWhite, border = BorderStroke(1.dp, Border)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Delivery progress", style = MaterialTheme.typography.titleMedium, color = Ink)
                        Spacer(Modifier.height(14.dp))
                        DeliveryTimeline(activeStageIndex = stageIndex(order.status).coerceAtLeast(0))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            SectionCard("Delivery address") {
                Text(
                    order.addressSummary ?: "No address was saved with this order.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (order.addressSummary == null) Muted else Ink,
                )
            }
            Spacer(Modifier.height(12.dp))
            SectionCard("Items (${order.itemCount})") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    order.items.forEach { line ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Peach), contentAlignment = Alignment.Center) {
                                CatalogImage(line.imageUrl, line.imageRes, line.name, Modifier.size(40.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(line.name, style = MaterialTheme.typography.labelMedium, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${line.unit} · ${formatPrice(line.unitPricePaise)}", style = MaterialTheme.typography.bodySmall, color = Muted)
                            }
                            Text("×${line.quantity}", style = MaterialTheme.typography.labelMedium, color = Muted)
                            Spacer(Modifier.width(10.dp))
                            Text(formatPrice(line.lineTotalPaise), style = MaterialTheme.typography.labelMedium, color = Ink)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            SectionCard("Bill details") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BillRow("Items total", formatPrice(order.subtotalPaise))
                    BillRow("Delivery fee", if (order.deliveryFeePaise == 0L) "FREE" else formatPrice(order.deliveryFeePaise))
                    BillRow("Platform fee", formatPrice(order.platformFeePaise))
                    if (order.discountPaise > 0) BillRow("Discount", "−" + formatPrice(order.discountPaise))
                    HorizontalDivider(color = Border)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("To pay", style = MaterialTheme.typography.titleMedium, color = Ink)
                        Text(formatPrice(order.totalPaise), style = MaterialTheme.typography.titleMedium, color = Ink)
                    }
                    Text(
                        if (order.isDemo) "Demo order · illustrative prices, no payment was taken."
                        else "Payment status is provided by the order source.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            SectionCard("Delivery partner") {
                if (order.isDemo) {
                    Text(
                        "A real delivery partner has not been assigned. RushIt does not show a fabricated rider, rating or phone number.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                } else {
                    Text("Partner details are provided by your delivery provider.", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onHelp, modifier = Modifier.weight(1f)) {
                    Text("Contact help", color = Coral, style = MaterialTheme.typography.labelMedium)
                }
                TextButton(onClick = { onTrack(order.id) }, modifier = Modifier.weight(1f)) {
                    Text("View tracking", color = Coral, style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeaderCard(order: Order, onHelp: () -> Unit, onTrack: (String) -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = CardWhite, border = BorderStroke(1.dp, Border)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(if (order.status == OrderStatus.DELIVERED) Success.copy(alpha = 0.15f) else Peach), contentAlignment = Alignment.Center) {
                    RushIcon(
                        if (order.status == OrderStatus.DELIVERED) R.drawable.ic_heart_filled else R.drawable.ic_package_01,
                        null,
                        Modifier.size(22.dp),
                        if (order.status == OrderStatus.DELIVERED) Success else Coral,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(order.status.label, style = MaterialTheme.typography.titleLarge, color = Ink)
                    Text("Order #${order.id}", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                StatusPill(if (order.isDemo) "Demo" else "Live", color = if (order.isDemo) Coral else Success)
            }
            Spacer(Modifier.height(8.dp))
            Text("Placed ${OrderTime.displayWithYear(order.createdAt)}", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = CardWhite, border = BorderStroke(1.dp, Border)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun BillRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Muted)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Ink)
    }
}
