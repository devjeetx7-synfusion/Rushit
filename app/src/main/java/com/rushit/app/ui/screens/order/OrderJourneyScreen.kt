package com.rushit.app.ui.screens.order

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.rushit.app.R
import com.rushit.app.domain.JourneyScene
import com.rushit.app.ui.components.MessageCard
import com.rushit.app.ui.components.ScreenHeader
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.Cream
import com.rushit.app.ui.theme.LocalReducedMotion
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
    onCancel: () -> Unit,
    onViewOrder: () -> Unit,
    onContinueShopping: () -> Unit,
    onCelebrationShown: () -> Unit,
    onRetry: () -> Unit,
) {
    val reduced = LocalReducedMotion.current
    val order = state.order
    val scene = state.scene ?: JourneyScene.PACKING

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumed = true
            if (event == Lifecycle.Event.ON_PAUSE) resumed = false
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val travelling = scene == JourneyScene.ON_THE_WAY || scene == JourneyScene.ALMOST_THERE
    val travelMotion = rememberTravelMotion(
        orderId = order?.id.orEmpty(),
        active = resumed && travelling && order != null,
        almostThere = scene == JourneyScene.ALMOST_THERE,
    )

    Column(Modifier.fillMaxSize().background(Cream)) {
        ScreenHeader(if (order != null) "Order #${order.id}" else "Order tracking", onBack)
        when {
            state.loading -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
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
                val announceCelebration = remember(order.id, scene == JourneyScene.DELIVERED) { state.showCelebration }
                LaunchedEffect(scene, order.id, announceCelebration) {
                    if (scene == JourneyScene.DELIVERED && announceCelebration) onCelebrationShown()
                }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                    if (order.isDemo) DemoOrderNotice(Modifier.padding(vertical = 6.dp))
                    AnimatedContent(
                        targetState = scene,
                        transitionSpec = { fadeIn(tween(if (reduced) 0 else 260)) togetherWith fadeOut(tween(if (reduced) 0 else 180)) },
                        label = "journey scene",
                    ) { current ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            when (current) {
                                JourneyScene.AWAITING_CONFIRMATION -> PackingAnimationScene(order, order.status)
                                JourneyScene.CONFIRMED -> OrderConfirmedScene(order)
                                JourneyScene.PACKING -> PackingAnimationScene(order, order.status)
                                JourneyScene.FINDING_RIDER -> FindingRiderScene()
                                JourneyScene.RIDER_READY -> RiderReadyScene(order)
                                JourneyScene.PICKED_UP -> OrderPickedUpScene(order)
                                JourneyScene.ON_THE_WAY, JourneyScene.ALMOST_THERE -> RiderTravelScene(order, current, travelMotion)
                                JourneyScene.DELIVERED -> DeliveryHandoverScene(order, celebrate = announceCelebration)
                                JourneyScene.CANCELLED -> TerminalOrderScene("Order cancelled", "This order was cancelled. No payment was taken and nothing will be delivered.")
                                JourneyScene.FAILED -> TerminalOrderScene("Order failed", "This order could not be completed. Please try again from your cart.")
                            }
                        }
                    }
                    if (order.status != com.rushit.app.data.model.OrderStatus.CANCELLED && order.status != com.rushit.app.data.model.OrderStatus.FAILED) {
                        OrderProgressIndicator(order.status, Modifier.padding(vertical = 18.dp))
                    }
                    OrderStatusCard(order, state.transitionError)
                    Spacer(Modifier.height(12.dp))
                    OrderTrackingActions(
                        order = order,
                        transitioning = state.transitioning,
                        onAdvance = onAdvance,
                        onCancel = onCancel,
                        onViewOrder = onViewOrder,
                        onHome = onContinueShopping,
                    )
                }
            }
        }
    }
}
