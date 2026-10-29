package com.rushit.app.ui.screens.order

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rushit.app.R
import com.rushit.app.data.model.OrderStatus
import com.rushit.app.domain.JourneyScene
import com.rushit.app.domain.canCancel
import com.rushit.app.domain.stageIndex
import com.rushit.app.ui.components.DeliveryTimeline
import com.rushit.app.ui.components.MessageCard
import com.rushit.app.ui.components.RushButton
import com.rushit.app.ui.components.RushIcon
import com.rushit.app.ui.components.ScreenHeader
import com.rushit.app.ui.components.StatusPill
import com.rushit.app.ui.theme.Amber
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.Cream
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.LocalReducedMotion
import com.rushit.app.ui.theme.Muted
import com.rushit.app.ui.theme.Peach
import com.rushit.app.ui.theme.SoftAmber
import com.rushit.app.ui.theme.Success
import com.rushit.app.ui.viewmodel.OrderUiState

/**
 * The delivery journey. The scene is derived from the stored order status; the only way a status
 * changes here is an explicit demo action, never an animation finishing.
 */
@Composable
fun OrderJourneyScreen(
    state: OrderUiState,
    onBack: () -> Unit,
    onAdvance: () -> Unit,
    onAssignRider: () -> Unit,
    onCancel: () -> Unit,
    onToggleAutoPlay: (Boolean) -> Unit,
    onViewOrder: () -> Unit,
    onContinueShopping: () -> Unit,
    onCelebrationShown: () -> Unit,
    onRetry: () -> Unit,
) {
    val reduced = LocalReducedMotion.current
    val order = state.order

    Column(Modifier.fillMaxSize().background(Cream)) {
        ScreenHeader(if (order != null) "Order #${order.id}" else "Order tracking", onBack)

        when {
            state.loading -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator(color = Coral)
            }
            order == null -> Column(Modifier.fillMaxSize().padding(16.dp)) {
                MessageCard(
                    title = "No order to show",
                    message = state.transitionError ?: state.error ?: "Place a demo order from checkout to preview the delivery journey.",
                    action = "Try again",
                    onAction = onRetry,
                    icon = R.drawable.ic_package_01,
                )
            }
            else -> {
                val scene = state.scene ?: JourneyScene.PACKING
                // Persist "celebrated" as soon as the delivered scene is actually shown.
                LaunchedEffect(scene, order.id, state.showCelebration) {
                    if (scene == JourneyScene.DELIVERED && state.showCelebration) onCelebrationShown()
                }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                    if (order.isDemo) DemoOrderBanner(Modifier.padding(horizontal = 16.dp, vertical = 6.dp))

                    AnimatedContent(
                        targetState = scene,
                        transitionSpec = { fadeIn(tween(if (reduced) 0 else 260)) togetherWith fadeOut(tween(if (reduced) 0 else 180)) },
                        label = "journey scene",
                    ) { current ->
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                            when (current) {
                                JourneyScene.AWAITING_CONFIRMATION -> PackingScene(order, order.status)
                                JourneyScene.PACKING -> PackingScene(order, order.status)
                                JourneyScene.FINDING_RIDER -> FindingRiderScene()
                                JourneyScene.RIDER_READY -> RiderReadyScene(order, null)
                                JourneyScene.ON_THE_WAY -> OnTheWayScene(order)
                                JourneyScene.ALMOST_THERE -> AlmostThereScene(order)
                                JourneyScene.DELIVERED -> DeliveredScene(
                                    order = order,
                                    celebrate = state.showCelebration,
                                    onViewOrder = onViewOrder,
                                    onContinueShopping = onContinueShopping,
                                )
                                JourneyScene.CANCELLED -> TerminalOrderScene(
                                    title = "Order cancelled",
                                    message = "This order was cancelled. No payment was taken and nothing will be delivered.",
                                    onContinueShopping = onContinueShopping,
                                    onViewOrder = onViewOrder,
                                )
                                JourneyScene.FAILED -> TerminalOrderScene(
                                    title = "Order failed",
                                    message = "This order could not be completed. Please try again from your cart.",
                                    onContinueShopping = onContinueShopping,
                                    onViewOrder = onViewOrder,
                                )
                            }
                        }
                    }

                    if (order.status != OrderStatus.CANCELLED && order.status != OrderStatus.FAILED) {
                        Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
                            DeliveryTimeline(activeStageIndex = stageIndex(order.status).coerceAtLeast(0))
                        }
                    }

                    StatusCard(state)

                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (order.isDemo && !order.status.isTerminal) {
                            RushButton("Preview next stage", onAdvance, modifier = Modifier.weight(1f), arrow = false)
                        }
                        if (canCancel(order)) {
                            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                                Text("Cancel order", color = Muted, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    if (order.isDemo && !order.status.isTerminal) {
                        DemoControls(
                            autoPlay = state.demoAutoPlay,
                            order = order,
                            onToggleAutoPlay = onToggleAutoPlay,
                            onAssignRider = onAssignRider,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoOrderBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SoftAmber,
        border = androidx.compose.foundation.BorderStroke(1.dp, Amber.copy(alpha = 0.5f)),
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
private fun StatusCard(state: OrderUiState) {
    val order = state.order ?: return
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Status", style = MaterialTheme.typography.titleMedium, color = Ink)
                Spacer(Modifier.weight(1f))
                StatusPill(
                    text = order.status.label,
                    color = when (order.status) {
                        OrderStatus.DELIVERED -> Success
                        OrderStatus.CANCELLED, OrderStatus.FAILED -> Muted
                        OrderStatus.ARRIVING -> Coral
                        else -> Coral
                    },
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = statusMessage(order.status),
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
            )
            state.transitionError?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = Coral)
            }
        }
    }
}

private fun statusMessage(status: OrderStatus): String = when (status) {
    OrderStatus.PENDING -> "Waiting for the store to confirm this order."
    OrderStatus.CONFIRMED -> "The store accepted this order and will start packing it."
    OrderStatus.PREPARING -> "Your items are being packed."
    OrderStatus.READY_FOR_PICKUP -> "Packed and waiting for a delivery partner."
    OrderStatus.PICKED_UP -> "A delivery partner has picked up your order."
    OrderStatus.OUT_FOR_DELIVERY -> "Your order is on the way. Live location isn't available yet."
    OrderStatus.ARRIVING -> "Your delivery partner is approaching your location."
    OrderStatus.DELIVERED -> "This order was marked delivered."
    OrderStatus.CANCELLED -> "This order was cancelled before delivery."
    OrderStatus.FAILED -> "This order could not be completed."
}

@Composable
private fun DemoControls(
    autoPlay: Boolean,
    order: com.rushit.app.data.model.Order,
    onToggleAutoPlay: (Boolean) -> Unit,
    onAssignRider: () -> Unit,
) {
    Surface(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Peach.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("Demo playback", style = MaterialTheme.typography.titleMedium, color = Ink)
            Text(
                "These controls only move this device-local demo order. They never change a real order status.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                modifier = Modifier.padding(top = 2.dp),
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Auto-advance stages", style = MaterialTheme.typography.bodyMedium, color = Ink, modifier = Modifier.weight(1f))
                Switch(checked = autoPlay, onCheckedChange = onToggleAutoPlay, enabled = !order.status.isTerminal)
            }
            if (order.status == OrderStatus.READY_FOR_PICKUP) {
                TextButton(onClick = onAssignRider) { Text("Assign demo rider", color = Coral, style = MaterialTheme.typography.labelMedium) }
            }
        }
    }
}
