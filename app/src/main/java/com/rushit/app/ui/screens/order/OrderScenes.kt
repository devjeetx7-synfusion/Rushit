package com.rushit.app.ui.screens.order

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rushit.app.R
import com.rushit.app.data.model.Order
import com.rushit.app.data.model.OrderStatus
import com.rushit.app.data.model.ShoppingMode
import com.rushit.app.domain.OrderTime
import com.rushit.app.ui.components.CatalogImage
import com.rushit.app.ui.components.CityBackdrop
import com.rushit.app.ui.components.Confetti
import com.rushit.app.ui.components.DeliveryRouteMap
import com.rushit.app.ui.components.DeliveryScooter
import com.rushit.app.ui.components.RushButton
import com.rushit.app.ui.components.RushIcon
import com.rushit.app.ui.components.RushItBag
import com.rushit.app.ui.components.StatusPill
import com.rushit.app.ui.components.SuccessCheck
import com.rushit.app.ui.theme.Amber
import com.rushit.app.ui.theme.BannerPeach
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.DeepRed
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.LocalReducedMotion
import com.rushit.app.ui.theme.Muted
import com.rushit.app.ui.theme.Peach
import com.rushit.app.ui.theme.Success

/** Illustrations used per shopping mode, so gift orders never look like grocery orders. */
private fun productsForMode(mode: ShoppingMode): List<Int> = when (mode) {
    ShoppingMode.GROCERY -> listOf(R.drawable.product_tomatoes, R.drawable.product_milk, R.drawable.product_bananas)
    ShoppingMode.ECOMMERCE -> listOf(R.drawable.ecommerce_headphones, R.drawable.ecommerce_lamp)
    ShoppingMode.GIFTS -> listOf(R.drawable.gift_bouquet, R.drawable.category_care)
}

@Composable
private fun SceneTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = Ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Muted, textAlign = TextAlign.Center)
    }
}

/**
 * Order confirmation: bag scales in, produce appears, the tick draws, a little confetti, then the
 * details. One-shot animations keyed on the order id so a recomposition never replays them.
 */
@Composable
fun OrderConfirmedScene(order: Order, onTrack: () -> Unit, onContinueShopping: () -> Unit) {
    val reduced = LocalReducedMotion.current
    val bagScale = remember(order.id) { Animatable(if (reduced) 1f else 0.72f) }
    val tick = remember(order.id) { Animatable(if (reduced) 1f else 0f) }
    val contentAlpha = remember(order.id) { Animatable(if (reduced) 1f else 0f) }
    val confetti = remember(order.id) { Animatable(0f) }

    LaunchedEffect(order.id, reduced) {
        if (reduced) {
            bagScale.snapTo(1f); tick.snapTo(1f); contentAlpha.snapTo(1f)
            return@LaunchedEffect
        }
        bagScale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 260f))
        tick.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
        confetti.animateTo(1f, tween(1_100, easing = LinearEasing))
        contentAlpha.animateTo(1f, tween(280))
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
            Confetti(Modifier.fillMaxSize(), progress = confetti.value, seed = order.id.hashCode())
            Box(
                Modifier.size(168.dp).graphicsLayer {
                    scaleX = bagScale.value
                    scaleY = bagScale.value
                    alpha = bagScale.value.coerceIn(0f, 1f)
                },
                contentAlignment = Alignment.Center,
            ) { RushItBag(Modifier.fillMaxSize()) }
            SuccessCheck(
                modifier = Modifier.align(Alignment.TopCenter).size(64.dp),
                progress = tick.value,
                scale = if (reduced) 1f else (0.7f + 0.3f * tick.value),
            )
        }
        Spacer(Modifier.height(10.dp))
        Column(Modifier.graphicsLayer { alpha = contentAlpha.value }, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Order Confirmed!", style = MaterialTheme.typography.headlineLarge, color = Ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text("We're getting your order ready.", style = MaterialTheme.typography.bodyLarge, color = Muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = CardWhite, border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
                Column(Modifier.padding(horizontal = 22.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Order #${order.id}", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Text(OrderTime.display(order.createdAt), style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
            Spacer(Modifier.height(18.dp))
            RushButton("Track order", onTrack, arrow = true)
            if (order.isDemo) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Demo order · no payment was taken",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(4.dp))
            androidx.compose.material3.TextButton(onClick = onContinueShopping) {
                Text("Continue shopping", color = DeepRed, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/** Packing scene: warm studio, conveyor, products drifting into the RushIt bag. */
@Composable
fun PackingScene(order: Order, status: OrderStatus) {
    val reduced = LocalReducedMotion.current
    val products = remember(order.mode) { productsForMode(order.mode) }
    val transition = rememberInfiniteTransition(label = "packing")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "packing drift",
    )
    val settle = if (reduced) 1f else 0.96f + drift * 0.04f

    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(24.dp)).background(BannerPeach.copy(alpha = 0.55f)),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                // back shelf
                drawRoundRect(Color(0xFFF6E7D2), Offset(0f, size.height * 0.62f), Size(size.width, size.height * 0.38f), CornerRadius(size.height * 0.04f))
                // conveyor belt
                drawRoundRect(Color(0xFFB9C4C9), Offset(0f, size.height * 0.72f), Size(size.width, size.height * 0.10f), CornerRadius(size.height * 0.04f))
                var x = 0f
                while (x < size.width) {
                    drawRoundRect(Color(0xFF97A5AB), Offset(x, size.height * 0.72f), Size(size.width * 0.02f, size.height * 0.10f))
                    x += size.width * 0.06f
                }
                // gentle sparkle dots
                repeat(6) { index ->
                    drawCircle(Amber.copy(alpha = 0.35f), size.minDimension * 0.012f, Offset(size.width * (0.1f + index * 0.15f), size.height * 0.14f))
                }
            }
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp).size(130.dp)
                    .graphicsLayer { scaleX = settle; scaleY = settle },
            ) { RushItBag(Modifier.fillMaxSize()) }
            // products floating towards the bag
            Box(Modifier.align(Alignment.TopCenter).padding(top = 16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    products.forEachIndexed { index, image ->
                        val offset = if (reduced) 0f else (drift * (6f + index * 4f))
                        Box(
                            Modifier.size(54.dp).graphicsLayer { translationY = -offset }
                                .clip(CircleShape).background(CardWhite.copy(alpha = 0.92f)),
                            contentAlignment = Alignment.Center,
                        ) { CatalogImage(null, image, null, Modifier.size(44.dp)) }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        SceneTitle(
            "Packing your order…",
            if (status == OrderStatus.CONFIRMED) "Your items are being carefully packed with love." else "All items packed. Waiting for a delivery partner.",
        )
    }
}

/** Finding a rider: honest pending state, used only while no partner is assigned. */
@Composable
fun FindingRiderScene() {
    val reduced = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "finding rider")
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "rider pulse",
    )
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(24.dp)).background(Peach.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(120.dp).graphicsLayer { scaleX = if (reduced) 1f else pulse; scaleY = if (reduced) 1f else pulse }
                    .clip(CircleShape).background(CardWhite),
                contentAlignment = Alignment.Center,
            ) {
                RushIcon(R.drawable.ic_user_02, null, Modifier.size(52.dp), Coral)
            }
        }
        Spacer(Modifier.height(18.dp))
        SceneTitle("Finding your rider…", "We're matching you with the nearest delivery partner.")
        Spacer(Modifier.height(10.dp))
        StatusPill("No partner assigned yet", color = Amber)
    }
}

/** Rider ready / assigned: scooter idling in the city. */
@Composable
fun RiderReadyScene(order: Order) {
    val reduced = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "idle wheels")
    val wheel by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2_600, easing = LinearEasing)),
        label = "idle wheel spin",
    )
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(24.dp))) {
            CityBackdrop(Modifier.fillMaxSize(), travel = 0f)
            DeliveryScooter(
                modifier = Modifier.fillMaxWidth(0.74f).height(150.dp).align(Alignment.BottomCenter).padding(bottom = 18.dp),
                wheelRotation = if (reduced) 0f else wheel,
                lean = -1.5f,
            )
        }
        Spacer(Modifier.height(16.dp))
        SceneTitle(
            "Your rider is ready!",
            if (order.status == OrderStatus.PICKED_UP) "Your order has been picked up." else "Your delivery partner is preparing to pick up your order.",
        )
        Spacer(Modifier.height(14.dp))
        if (order.isDemo) {
            DemoRiderCard()
        } else {
            StatusPill("Partner details not shared yet", color = Amber)
        }
    }
}

/** The app has no real rider directory, so this card never pretends to be one. */
@Composable
fun DemoRiderCard() {
    Surface(shape = RoundedCornerShape(18.dp), color = CardWhite, border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(Peach), contentAlignment = Alignment.Center) {
                RushIcon(R.drawable.ic_user_02, null, Modifier.size(26.dp), Coral)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Demo delivery partner", style = MaterialTheme.typography.titleMedium, color = Ink)
                Text(
                    "Illustration only · RushIt hasn't assigned a real rider, so no name, rating or phone number is shown.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
        }
    }
}

/** Illustrated route screen. Never claims live GPS or an invented ETA. */
@Composable
fun OnTheWayScene(order: Order) {
    val reduced = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "route")
    val travel by transition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(tween(6_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "route progress",
    )
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(24.dp))) {
            DeliveryRouteMap(Modifier.fillMaxSize(), progress = if (reduced) 0.55f else travel)
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusPill("Illustrated route", color = Coral)
            Spacer(Modifier.width(8.dp))
            StatusPill("Not live GPS", color = Amber)
        }
        Spacer(Modifier.height(12.dp))
        SceneTitle("Your order is on the way!", "Your delivery partner is on the way to you.")
        Spacer(Modifier.height(10.dp))
        Text(
            "Live location and arrival estimates aren't connected yet, so this screen shows the order status instead of a countdown.",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
            textAlign = TextAlign.Center,
        )
    }
}

/** Approaching: scooter slows down beside the destination. */
@Composable
fun AlmostThereScene(order: Order) {
    val reduced = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "arriving")
    val wheel by transition.animateFloat(
        initialValue = 0f,
        targetValue = 180f,
        animationSpec = infiniteRepeatable(tween(2_200, easing = LinearEasing)),
        label = "arriving wheels",
    )
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(24.dp))) {
            CityBackdrop(Modifier.fillMaxSize(), travel = if (reduced) 0f else 24f)
            // destination house
            Canvas(Modifier.fillMaxSize()) {
                val baseX = size.width * 0.66f
                val baseY = size.height * 0.66f
                drawRoundRect(Color(0xFFF1D9BE), Offset(baseX, baseY - size.height * 0.22f), Size(size.width * 0.24f, size.height * 0.22f), CornerRadius(size.minDimension * 0.02f))
                val roof = androidx.compose.ui.graphics.Path().apply {
                    moveTo(baseX - size.width * 0.02f, baseY - size.height * 0.22f)
                    lineTo(baseX + size.width * 0.12f, baseY - size.height * 0.34f)
                    lineTo(baseX + size.width * 0.26f, baseY - size.height * 0.22f)
                    close()
                }
                drawPath(roof, DeepRed.copy(alpha = 0.85f))
                drawRoundRect(Color(0xFF8B5E3C), Offset(baseX + size.width * 0.09f, baseY - size.height * 0.12f), Size(size.width * 0.06f, size.height * 0.12f), CornerRadius(size.minDimension * 0.01f))
            }
            DeliveryScooter(
                modifier = Modifier.fillMaxWidth(0.66f).height(140.dp).align(Alignment.BottomStart).padding(start = 4.dp, bottom = 16.dp),
                wheelRotation = if (reduced) 0f else wheel,
                lean = 0f,
            )
        }
        Spacer(Modifier.height(16.dp))
        SceneTitle("Almost there!", "Your delivery partner is approaching your location.")
    }
}

/** Delivered celebration: tick, light confetti, then the actions. */
@Composable
fun DeliveredScene(
    order: Order,
    celebrate: Boolean,
    onViewOrder: () -> Unit,
    onContinueShopping: () -> Unit,
) {
    val reduced = LocalReducedMotion.current
    val tick = remember(order.id, celebrate) { Animatable(if (reduced || !celebrate) 1f else 0f) }
    val confetti = remember(order.id, celebrate) { Animatable(0f) }
    val content = remember(order.id, celebrate) { Animatable(if (reduced) 1f else 0f) }

    LaunchedEffect(order.id, celebrate, reduced) {
        if (!celebrate || reduced) {
            tick.snapTo(1f); content.snapTo(1f); return@LaunchedEffect
        }
        tick.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        confetti.animateTo(1f, tween(1_200, easing = LinearEasing))
        content.animateTo(1f, tween(260))
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(232.dp), contentAlignment = Alignment.Center) {
            Confetti(Modifier.fillMaxSize(), progress = confetti.value, seed = order.id.hashCode() + 3)
            Box(Modifier.fillMaxWidth().height(190.dp)) {
                CityBackdrop(Modifier.fillMaxSize(), travel = 0f, showRoad = true)
                // rider handing over the bag
                Box(Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 6.dp).size(width = 132.dp, height = 108.dp)) {
                    DeliveryScooter(Modifier.fillMaxSize(), wheelRotation = 0f)
                }
                Box(Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 10.dp).size(84.dp)) { RushItBag(Modifier.fillMaxSize()) }
            }
            SuccessCheck(
                modifier = Modifier.align(Alignment.TopCenter).size(58.dp),
                progress = tick.value,
                scale = if (reduced) 1f else (0.75f + 0.25f * tick.value),
            )
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.graphicsLayer { alpha = content.value }, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Order delivered!", style = MaterialTheme.typography.headlineMedium, color = Success, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text("Thank you for shopping with RushIt.", style = MaterialTheme.typography.bodyLarge, color = Muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            RushButton("View Order", onViewOrder, arrow = false)
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.TextButton(onClick = onContinueShopping) {
                Text("Continue Shopping", color = DeepRed, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/** Shared terminal state for cancelled and failed orders — deliberately not celebratory. */
@Composable
fun TerminalOrderScene(
    title: String,
    message: String,
    onContinueShopping: () -> Unit,
    onViewOrder: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp)).background(Peach.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(96.dp).clip(CircleShape).background(CardWhite), contentAlignment = Alignment.Center) {
                RushIcon(R.drawable.ic_cancel_01, null, Modifier.size(44.dp), Muted)
            }
        }
        Spacer(Modifier.height(18.dp))
        SceneTitle(title, message)
        Spacer(Modifier.height(18.dp))
        RushButton("View order", onViewOrder, arrow = false)
        Spacer(Modifier.height(6.dp))
        androidx.compose.material3.TextButton(onClick = onContinueShopping) {
            Text("Continue shopping", color = DeepRed, style = MaterialTheme.typography.labelMedium)
        }
    }
}

