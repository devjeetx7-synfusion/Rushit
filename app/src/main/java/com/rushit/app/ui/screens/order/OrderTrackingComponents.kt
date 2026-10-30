package com.rushit.app.ui.screens.order

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.rushit.app.R
import com.rushit.app.data.model.Order
import com.rushit.app.data.model.OrderStatus
import com.rushit.app.domain.canCancel
import com.rushit.app.domain.stageIndex
import com.rushit.app.ui.components.DeliveryTimeline
import com.rushit.app.ui.components.RushButton
import com.rushit.app.ui.components.RushIcon
import com.rushit.app.ui.components.StatusPill
import com.rushit.app.ui.theme.Amber
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.Muted
import com.rushit.app.ui.theme.Peach
import com.rushit.app.ui.theme.SoftAmber
import com.rushit.app.ui.theme.Success

/** Shared illustration frame. Keeps every stage inside the original rounded container. */
@Composable
fun OrderSceneContainer(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(344f / 183f)
            .clip(RoundedCornerShape(24.dp)),
    ) { content() }
}

@Composable
fun DemoOrderNotice(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SoftAmber,
        border = BorderStroke(1.dp, Amber.copy(alpha = 0.5f)),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RushIcon(R.drawable.ic_help_circle, null, Modifier.size(20.dp), Amber)
            Spacer(Modifier.width(10.dp))
            Text(
                "Demo delivery · this order is stored on your device only. No payment, rider or real tracking is involved.",
                style = MaterialTheme.typography.bodySmall,
                color = Ink,
            )
        }
    }
}

@Composable
fun OrderProgressIndicator(status: OrderStatus, modifier: Modifier = Modifier) {
    DeliveryTimeline(
        activeStageIndex = if (status == OrderStatus.PENDING) -1 else stageIndex(status),
        modifier = modifier,
        failed = status == OrderStatus.CANCELLED || status == OrderStatus.FAILED,
    )
}

@Composable
fun OrderStatusCard(order: Order, transitionError: String?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = CardWhite,
        border = BorderStroke(1.dp, Border),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Status", style = MaterialTheme.typography.titleMedium, color = Ink)
                Spacer(Modifier.weight(1f))
                StatusPill(
                    text = order.status.label,
                    color = when (order.status) {
                        OrderStatus.DELIVERED -> Success
                        OrderStatus.CANCELLED, OrderStatus.FAILED -> Muted
                        else -> Coral
                    },
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(statusMessage(order.status), style = MaterialTheme.typography.bodyMedium, color = Muted)
            transitionError?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = Coral)
            }
        }
    }
}

fun statusMessage(status: OrderStatus): String = when (status) {
    OrderStatus.PENDING -> "Waiting for the store to confirm this order."
    OrderStatus.CONFIRMED -> "The store accepted this order and will start packing it."
    OrderStatus.PREPARING -> "Your items are being packed at the store."
    OrderStatus.READY_FOR_PICKUP -> "Your order is ready and the delivery partner is on the way to the store."
    OrderStatus.PICKED_UP -> "Your order has been picked up by the delivery partner."
    OrderStatus.OUT_FOR_DELIVERY -> "Your delivery partner is heading to your location."
    OrderStatus.ARRIVING -> "Your order is near your location and will arrive shortly."
    OrderStatus.DELIVERED -> "Your order has been delivered. Enjoy your items!"
    OrderStatus.CANCELLED -> "This order was cancelled before delivery."
    OrderStatus.FAILED -> "This order could not be completed."
}

/**
 * The only action row on the tracking screen. It lives outside the animated scene so crossfades
 * never duplicate buttons or intercept taps.
 */
@Composable
fun OrderTrackingActions(
    order: Order,
    transitioning: Boolean,
    onAdvance: () -> Unit,
    onCancel: () -> Unit,
    onViewOrder: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        when {
            order.isDemo && !order.status.isTerminal -> {
                RushButton(
                    "Preview next stage",
                    onAdvance,
                    modifier = Modifier.weight(1f),
                    arrow = false,
                    enabled = !transitioning,
                )
                if (canCancel(order)) {
                    TextButton(onClick = onCancel, modifier = Modifier.weight(1f), enabled = !transitioning) {
                        Text("Cancel order", color = Muted, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            else -> {
                RushButton("View order details", onViewOrder, modifier = Modifier.weight(1f), arrow = false)
                TextButton(onClick = onHome, modifier = Modifier.weight(1f)) {
                    Text("Back to home", color = Muted, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
internal fun SceneTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = Ink, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
