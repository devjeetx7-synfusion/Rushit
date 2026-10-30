package com.rushit.app.ui.screens.order

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rushit.app.R
import com.rushit.app.data.model.Order
import com.rushit.app.data.model.OrderStatus
import com.rushit.app.data.model.ShoppingMode
import com.rushit.app.domain.JourneyScene
import com.rushit.app.domain.RiderGeometry
import com.rushit.app.domain.TravelParallax
import com.rushit.app.domain.travelSpeedFactor
import com.rushit.app.domain.wheelDegrees
import com.rushit.app.ui.components.RushIcon
import com.rushit.app.ui.components.SuccessCheck
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.LocalReducedMotion
import com.rushit.app.ui.theme.Muted
import com.rushit.app.ui.theme.Peach
import com.rushit.app.ui.theme.Success
import kotlinx.coroutines.isActive

/** Hoisted so On the Way → Almost There keeps the same wheel/road phase. */
class TravelMotionState {
    val distance = Animatable(0f)
    val speed = Animatable(1f)
}

@Composable
fun rememberTravelMotion(orderId: String, active: Boolean, almostThere: Boolean): TravelMotionState {
    val reduced = LocalReducedMotion.current
    val motion = remember(orderId) { TravelMotionState() }
    LaunchedEffect(orderId, almostThere, reduced) {
        if (reduced) motion.speed.snapTo(1f)
        else motion.speed.animateTo(travelSpeedFactor(almostThere), tween(1_200, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(orderId, active, reduced) {
        if (!active || reduced) return@LaunchedEffect
        var last = 0L
        while (isActive) {
            val now = withFrameNanos { it }
            if (last != 0L) {
                val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                motion.distance.snapTo((motion.distance.value + dt * 0.35f * motion.speed.value) % 1_000f)
            }
            last = now
        }
    }
    return motion
}

@Composable
fun OrderConfirmedScene(order: Order) {
    val reduced = LocalReducedMotion.current
    val enter = remember(order.id) { Animatable(if (reduced) 1f else 0f) }
    val tick = remember(order.id) { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(order.id, reduced) {
        if (reduced) { enter.snapTo(1f); tick.snapTo(1f); return@LaunchedEffect }
        enter.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        tick.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
    }
    Column(Modifier.fillMaxWidth()) {
        OrderSceneContainer {
            BoxWithConstraints(Modifier.fillMaxSize().graphicsLayer {
                alpha = enter.value
                val s = 0.94f + 0.06f * enter.value
                scaleX = s; scaleY = s
            }) {
                val density = LocalDensity.current
                val scale = with(density) { maxWidth.toPx() } / 344f
                Image(painterResource(R.drawable.order_confirmation_bg), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                GroceryGroup(R.drawable.order_confirmation_groceries_left, 28f, 88f, scale, enter.value, 0f)
                GroceryGroup(R.drawable.order_confirmation_groceries_right, 225f, 80f, scale, enter.value, 0.18f)
                val checkSize = with(density) { (54f * scale).toDp() }
                SuccessCheck(
                    modifier = Modifier.offset(x = with(density) { (148f * scale).toDp() }, y = with(density) { (86f * scale).toDp() }).size(checkSize),
                    progress = tick.value,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        SceneTitle("Order confirmed!", "Your order has been received and the store will start preparing it.")
    }
}

@Composable
private fun GroceryGroup(res: Int, x: Float, y: Float, scale: Float, progress: Float, delay: Float) {
    val density = LocalDensity.current
    val local = ((progress - delay) / (1f - delay)).coerceIn(0f, 1f)
    val eased = FastOutSlowInEasing.transform(local)
    val width = if (res == R.drawable.order_confirmation_groceries_left) 103f else 110f
    val height = if (res == R.drawable.order_confirmation_groceries_left) 95f else 101f
    Image(
        painterResource(res), null,
        Modifier.offset(x = with(density) { (x * scale).toDp() }, y = with(density) { ((y + 8f * (1f - eased)) * scale).toDp() })
            .size(with(density) { (width * scale).toDp() }, with(density) { (height * scale).toDp() })
            .graphicsLayer { alpha = eased },
    )
}

@Composable
fun PackingAnimationScene(order: Order, status: OrderStatus) {
    if (order.mode != ShoppingMode.GROCERY) {
        GenericPackingScene(status)
        return
    }
    val reduced = LocalReducedMotion.current
    val progress = remember(order.id) { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(order.id, reduced) {
        if (reduced) progress.snapTo(1f) else progress.animateTo(1f, tween(1_800, easing = LinearEasing))
    }
    val idle = rememberInfiniteTransition(label = "bag idle")
    val bob by idle.animateFloat(0f, 1f, infiniteRepeatable(tween(2_400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bag bob")
    Column(Modifier.fillMaxWidth()) {
        OrderSceneContainer {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val scale = with(density) { maxWidth.toPx() } / 344f
                Image(painterResource(R.drawable.order_packing), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                // Bag settles with a very small idle movement after the items arrive.
                val bagY = 62f + if (reduced) 0f else (bob - 0.5f) * 2f
                Image(
                    painterResource(R.drawable.order_packing_bag), null,
                    Modifier.offset(x = with(density) { (118f * scale).toDp() }, y = with(density) { (bagY * scale).toDp() })
                        .size(with(density) { (137f * scale).toDp() }, with(density) { (122f * scale).toDp() }),
                )
                PackingItem(R.drawable.order_packing_item_tomato, 100f, 43f, 150f, 78f, scale, progress.value, 0f)
                PackingItem(R.drawable.order_packing_item_milk, 180f, 42f, 175f, 74f, scale, progress.value, 0.16f)
                PackingItem(R.drawable.order_packing_item_banana, 260f, 43f, 205f, 78f, scale, progress.value, 0.32f)
            }
        }
        Spacer(Modifier.height(16.dp))
        SceneTitle(
            if (status == OrderStatus.PENDING) "Waiting for confirmation" else "Packing your order…",
            if (status == OrderStatus.PENDING) "This order is waiting for confirmation." else "Your items are being carefully packed with love.",
        )
    }
}

@Composable
private fun PackingItem(res: Int, fromX: Float, fromY: Float, toX: Float, toY: Float, scale: Float, progress: Float, delay: Float) {
    val density = LocalDensity.current
    val local = ((progress - delay) / 0.55f).coerceIn(0f, 1f)
    val eased = FastOutSlowInEasing.transform(local)
    val x = fromX + (toX - fromX) * eased
    val y = fromY + (toY - fromY) * eased
    val alpha = if (local < 0.75f) 1f else 1f - (local - 0.75f) / 0.25f
    val s = 1f - 0.35f * eased
    Image(
        painterResource(res), null,
        Modifier.offset(x = with(density) { ((x - 27f) * scale).toDp() }, y = with(density) { ((y - 27f) * scale).toDp() })
            .size(with(density) { (55f * scale).toDp() })
            .graphicsLayer { scaleX = s; scaleY = s; this.alpha = alpha },
    )
}

@Composable
private fun GenericPackingScene(status: OrderStatus) {
    Column(Modifier.fillMaxWidth()) {
        OrderSceneContainer {
            Box(Modifier.fillMaxSize().background(Peach.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                RushIcon(R.drawable.ic_shopping_bag_01, null, Modifier.size(72.dp), Coral)
            }
        }
        Spacer(Modifier.height(16.dp))
        SceneTitle("Packing your order…", if (status == OrderStatus.PENDING) "This order is waiting for confirmation." else "Your items are being carefully packed with love.")
    }
}

@Composable
fun FindingRiderScene() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        OrderSceneContainer {
            Box(Modifier.fillMaxSize().background(Peach.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(96.dp).clip(CircleShape).background(CardWhite), contentAlignment = Alignment.Center) {
                    RushIcon(R.drawable.ic_user_02, null, Modifier.size(48.dp), Coral)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        SceneTitle("Waiting for a rider", "No delivery partner has been assigned yet.")
    }
}

@Composable
fun RiderReadyScene(order: Order) {
    Column(Modifier.fillMaxWidth()) {
        OrderSceneContainer {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                Image(painterResource(R.drawable.order_rider_ready), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                val density = LocalDensity.current
                val h = with(density) { maxHeight.toPx() }
                val w = with(density) { maxWidth.toPx() }
                val riderH = h * 0.80f
                val riderW = riderH * RiderGeometry.SOURCE_WIDTH / RiderGeometry.SOURCE_HEIGHT
                Image(
                    painterResource(R.drawable.order_rider_static),
                    "RushIt rider waiting at the store",
                    Modifier.offset(x = with(density) { (w * 0.22f).toDp() }, y = with(density) { (h - riderH).toDp() })
                        .size(with(density) { riderW.toDp() }, with(density) { riderH.toDp() }),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        SceneTitle("Your rider is ready!", "Your delivery partner is preparing to pick up your order.")
    }
}

@Composable
fun OrderPickedUpScene(order: Order) {
    val reduced = LocalReducedMotion.current
    val enter = remember(order.id) { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(order.id, reduced) {
        if (reduced) enter.snapTo(1f) else enter.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
    }
    Column(Modifier.fillMaxWidth()) {
        OrderSceneContainer {
            Image(
                painterResource(R.drawable.order_pickup),
                "Store employee handing the parcel to the RushIt rider",
                Modifier.fillMaxSize().graphicsLayer { alpha = enter.value },
                contentScale = ContentScale.Crop,
            )
        }
        Spacer(Modifier.height(16.dp))
        SceneTitle("Order picked up!", "Your delivery partner has collected your order and is on the way to you.")
    }
}

@Composable
fun RiderTravelScene(order: Order, scene: JourneyScene, motion: TravelMotionState) {
    val almostThere = scene == JourneyScene.ALMOST_THERE
    Column(Modifier.fillMaxWidth()) {
        OrderSceneContainer {
            BoxWithConstraints(Modifier.fillMaxSize().semantics {
                contentDescription = if (almostThere) "RushIt rider approaching the destination" else "RushIt rider on the way"
            }) {
                val density = LocalDensity.current
                val w = with(density) { maxWidth.toPx() }
                val h = with(density) { maxHeight.toPx() }
                val scale = h / 183f
                val stripW = 464f * scale
                val distance = motion.distance.value * w
                Box(Modifier.fillMaxSize().background(Color(0xFFEAF3F7)))
                ParallaxStrip(R.drawable.order_travel_skyline, stripW, 0f, 120f * scale, distance * TravelParallax.SKYLINE)
                ParallaxStrip(R.drawable.order_travel_trees, stripW, 70f * scale, 80f * scale, distance * TravelParallax.TREES)
                ParallaxStrip(R.drawable.order_travel_road, stripW, 140f * scale, 43f * scale, distance * TravelParallax.ROAD)

                val riderH = h * 0.78f
                val riderW = riderH * RiderGeometry.SOURCE_WIDTH / RiderGeometry.SOURCE_HEIGHT
                val rx = w * 0.27f
                val ry = h - riderH - h * 0.07f
                val wheelR = RiderGeometry.wheelRadius(riderW)
                val degrees = wheelDegrees(distance, wheelR)
                val (rearX, rearY) = RiderGeometry.rearHub(riderW, riderH)
                val (frontX, frontY) = RiderGeometry.frontHub(riderW, riderH)
                listOf(rearX to rearY, frontX to frontY).forEach { (hx, hy) ->
                    val cx = rx + hx
                    val cy = ry + hy
                    Image(
                        painterResource(R.drawable.order_wheel), null,
                        Modifier.offset(x = with(density) { (cx - wheelR).toDp() }, y = with(density) { (cy - wheelR).toDp() })
                            .size(with(density) { (wheelR * 2f).toDp() })
                            .graphicsLayer { rotationZ = degrees },
                    )
                }
                Image(
                    painterResource(R.drawable.order_rider_travel), null,
                    Modifier.offset(x = with(density) { rx.toDp() }, y = with(density) { ry.toDp() })
                        .size(with(density) { riderW.toDp() }, with(density) { riderH.toDp() }),
                )
                if (almostThere) {
                    Box(
                        Modifier.offset(x = with(density) { (w * 0.83f).toDp() }, y = with(density) { (h * 0.26f).toDp() })
                            .size(with(density) { (w * 0.07f).toDp() }).clip(CircleShape).background(Coral),
                        contentAlignment = Alignment.Center,
                    ) { Box(Modifier.size(with(density) { (w * 0.03f).toDp() }).clip(CircleShape).background(CardWhite)) }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        SceneTitle(
            if (almostThere) "Almost there!" else "On the way!",
            if (almostThere) "Your delivery partner is very close to your location." else "Your order is on the way to your location.",
        )
    }
}

@Composable
private fun ParallaxStrip(res: Int, stripW: Float, y: Float, height: Float, offset: Float) {
    val density = LocalDensity.current
    val start = -(offset % stripW)
    val widthDp = with(density) { stripW.toDp() }
    val heightDp = with(density) { height.toDp() }
    val yDp = with(density) { y.toDp() }
    Image(
        painterResource(res), null,
        Modifier.offset(x = with(density) { start.toDp() }, y = yDp).size(widthDp, heightDp),
        contentScale = ContentScale.FillBounds,
    )
    Image(
        painterResource(res), null,
        Modifier.offset(x = with(density) { (start + stripW).toDp() }, y = yDp).size(widthDp, heightDp),
        contentScale = ContentScale.FillBounds,
    )
}

@Composable
fun DeliveryHandoverScene(order: Order, celebrate: Boolean) {
    val reduced = LocalReducedMotion.current
    val enter = remember(order.id, celebrate) { Animatable(if (reduced || !celebrate) 1f else 0f) }
    val tick = remember(order.id, celebrate) { Animatable(if (reduced || !celebrate) 1f else 0f) }
    LaunchedEffect(order.id, celebrate, reduced) {
        if (reduced || !celebrate) { enter.snapTo(1f); tick.snapTo(1f); return@LaunchedEffect }
        enter.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        tick.animateTo(1f, tween(480, delayMillis = 260, easing = FastOutSlowInEasing))
    }
    Column(Modifier.fillMaxWidth()) {
        OrderSceneContainer {
            Box(Modifier.fillMaxSize()) {
                Image(
                    painterResource(R.drawable.order_delivered),
                    "RushIt rider handing the groceries to the customer at the door",
                    Modifier.fillMaxSize().graphicsLayer {
                        alpha = enter.value
                        val s = 0.96f + 0.04f * enter.value
                        scaleX = s; scaleY = s
                    },
                    contentScale = ContentScale.Crop,
                )
                SuccessCheck(Modifier.align(Alignment.TopEnd).padding(8.dp).size(40.dp), progress = tick.value)
            }
        }
        Spacer(Modifier.height(16.dp))
        SceneTitle("Delivered!", "Your order has been successfully delivered.")
    }
}

@Composable
fun TerminalOrderScene(title: String, message: String) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        OrderSceneContainer {
            Box(Modifier.fillMaxSize().background(Peach.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(96.dp).clip(CircleShape).background(CardWhite), contentAlignment = Alignment.Center) {
                    RushIcon(R.drawable.ic_cancel_01, null, Modifier.size(44.dp), Muted)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        SceneTitle(title, message)
    }
}
