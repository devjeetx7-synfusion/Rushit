package com.rushit.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rushit.app.ui.theme.Amber
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.DeepRed
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.Leaf
import com.rushit.app.ui.theme.Peach
import com.rushit.app.ui.theme.Success

/**
 * All delivery artwork is drawn with Compose so it scales to any density, needs no network and
 * costs a handful of draw calls. Each piece is a small reusable composable rather than one large
 * hand-drawn scene.
 */

private val SkyTop = Color(0xFFEAF3F7)
private val SkyBottom = Color(0xFFFDF3E3)
private val BuildingA = Color(0xFFF3D9C6)
private val BuildingB = Color(0xFFE7D5EA)
private val BuildingC = Color(0xFFD9E7DA)
private val Road = Color(0xFF6B6B6B)
private val RoadLine = Color(0xFFF6F1E7)
private val Helmet = Color(0xFFF6C445)
private val Skin = Color(0xFFE8B98F)
private val Jeans = Color(0xFF2F3E52)

/** Warm illustrated city backdrop: sky gradient, distant buildings, park and a road. */
@Composable
fun CityBackdrop(
    modifier: Modifier = Modifier,
    /** Horizontal travel offset in pixels; pass a rising value while the scooter moves. */
    travel: Float = 0f,
    showRoad: Boolean = true,
) {
    Canvas(modifier.fillMaxSize().semantics { contentDescription = "Illustrated city street" }) {
        drawRect(Brush.verticalGradient(listOf(SkyTop, SkyBottom)))
        drawSun()
        drawSkyline(travel)
        drawParkStrip(travel)
        if (showRoad) drawRoad(travel)
    }
}

private fun DrawScope.drawSun() {
    drawCircle(Amber.copy(alpha = 0.35f), radius = size.minDimension * 0.16f, center = Offset(size.width * 0.82f, size.height * 0.16f))
    drawCircle(Amber.copy(alpha = 0.55f), radius = size.minDimension * 0.09f, center = Offset(size.width * 0.82f, size.height * 0.16f))
}

private fun DrawScope.drawSkyline(travel: Float) {
    val baseY = size.height * 0.62f
    val blockWidth = size.width * 0.16f
    val shift = (travel * 0.35f) % (blockWidth * 2)
    val palette = listOf(BuildingA, BuildingB, BuildingC)
    var x = -blockWidth * 2 - shift
    var index = 0
    while (x < size.width + blockWidth) {
        val height = size.height * (0.22f + 0.06f * ((index % 3)))
        drawRoundRect(
            color = palette[index % palette.size],
            topLeft = Offset(x, baseY - height),
            size = Size(blockWidth * 0.86f, height),
            cornerRadius = CornerRadius(blockWidth * 0.08f),
        )
        // windows
        val windowSize = blockWidth * 0.14f
        var wy = baseY - height + windowSize
        while (wy < baseY - windowSize) {
            drawRoundRect(
                color = CardWhite.copy(alpha = 0.75f),
                topLeft = Offset(x + blockWidth * 0.14f, wy),
                size = Size(windowSize, windowSize),
                cornerRadius = CornerRadius(windowSize * 0.25f),
            )
            drawRoundRect(
                color = CardWhite.copy(alpha = 0.55f),
                topLeft = Offset(x + blockWidth * 0.46f, wy),
                size = Size(windowSize, windowSize),
                cornerRadius = CornerRadius(windowSize * 0.25f),
            )
            wy += windowSize * 2.4f
        }
        x += blockWidth
        index++
    }
}

private fun DrawScope.drawParkStrip(travel: Float) {
    val groundY = size.height * 0.62f
    drawRect(Leaf.copy(alpha = 0.22f), topLeft = Offset(0f, groundY), size = Size(size.width, size.height * 0.10f))
    val treeSpacing = size.width * 0.22f
    val shift = (travel * 0.6f) % treeSpacing
    var x = -treeSpacing + shift
    var index = 0
    while (x < size.width + treeSpacing) {
        val trunkHeight = size.height * 0.08f
        drawRoundRect(
            color = Color(0xFFB08968),
            topLeft = Offset(x + treeSpacing * 0.18f, groundY - trunkHeight),
            size = Size(size.width * 0.012f, trunkHeight),
            cornerRadius = CornerRadius(size.width * 0.006f),
        )
        val canopyRadius = size.width * 0.045f
        drawCircle(Leaf.copy(alpha = if (index % 2 == 0) 0.85f else 0.65f), canopyRadius, Offset(x + treeSpacing * 0.19f, groundY - trunkHeight - canopyRadius * 0.4f))
        x += treeSpacing
        index++
    }
}

private fun DrawScope.drawRoad(travel: Float) {
    val roadTop = size.height * 0.72f
    drawRect(Road.copy(alpha = 0.92f), topLeft = Offset(0f, roadTop), size = Size(size.width, size.height - roadTop))
    drawRect(Color(0xFFCFC7BA), topLeft = Offset(0f, roadTop - size.height * 0.02f), size = Size(size.width, size.height * 0.02f))
    val dashWidth = size.width * 0.09f
    val gap = size.width * 0.06f
    val shift = (travel) % (dashWidth + gap)
    var x = -dashWidth - shift
    val y = roadTop + (size.height - roadTop) * 0.5f
    while (x < size.width + dashWidth) {
        drawRoundRect(
            color = RoadLine,
            topLeft = Offset(x, y - size.height * 0.006f),
            size = Size(dashWidth, size.height * 0.012f),
            cornerRadius = CornerRadius(size.height * 0.006f),
        )
        x += dashWidth + gap
    }
}

/**
 * Reusable coral delivery scooter with a helmeted rider and a RushIt insulated bag.
 *
 * [wheelRotation] is driven by the caller (degrees), [lean] tilts the scooter during acceleration,
 * and the bag always stays attached as part of the same drawing.
 */
@Composable
fun DeliveryScooter(
    modifier: Modifier = Modifier,
    wheelRotation: Float = 0f,
    lean: Float = 0f,
    label: String = "RushIt delivery scooter",
) {
    Canvas(modifier.fillMaxSize().semantics { contentDescription = label }) {
        withTransform({ rotate(lean, pivot = Offset(size.width * 0.5f, size.height * 0.82f)) }) {
            drawScooter(wheelRotation)
        }
    }
}

private fun DrawScope.drawScooter(wheelRotation: Float) {
    val w = size.width
    val h = size.height
    val wheelRadius = w * 0.13f
    val wheelY = h * 0.86f
    val rearX = w * 0.30f
    val frontX = w * 0.78f

    // soft ground shadow
    drawOval(
        color = Color.Black.copy(alpha = 0.10f),
        topLeft = Offset(w * 0.14f, wheelY + wheelRadius * 0.55f),
        size = Size(w * 0.72f, wheelRadius * 0.7f),
    )

    // wheels
    listOf(rearX, frontX).forEach { centerX ->
        drawCircle(Color(0xFF2B2B2B), wheelRadius, Offset(centerX, wheelY))
        drawCircle(Color(0xFF8C8C8C), wheelRadius * 0.55f, Offset(centerX, wheelY))
        rotate(wheelRotation, pivot = Offset(centerX, wheelY)) {
            for (i in 0 until 4) {
                val angle = Math.toRadians((i * 45).toDouble())
                drawLine(
                    color = Color(0xFFD9D9D9),
                    start = Offset(centerX - (wheelRadius * 0.5f * Math.cos(angle)).toFloat(), wheelY - (wheelRadius * 0.5f * Math.sin(angle)).toFloat()),
                    end = Offset(centerX + (wheelRadius * 0.5f * Math.cos(angle)).toFloat(), wheelY + (wheelRadius * 0.5f * Math.sin(angle)).toFloat()),
                    strokeWidth = wheelRadius * 0.12f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }

    // body
    val bodyTop = h * 0.52f
    drawRoundRect(
        color = Coral,
        topLeft = Offset(w * 0.20f, bodyTop),
        size = Size(w * 0.52f, h * 0.22f),
        cornerRadius = CornerRadius(w * 0.09f),
    )
    // footboard + front shield
    drawRoundRect(
        color = DeepRed,
        topLeft = Offset(w * 0.44f, bodyTop + h * 0.06f),
        size = Size(w * 0.30f, h * 0.13f),
        cornerRadius = CornerRadius(w * 0.05f),
    )
    drawRoundRect(
        color = Coral,
        topLeft = Offset(w * 0.66f, h * 0.40f),
        size = Size(w * 0.10f, h * 0.30f),
        cornerRadius = CornerRadius(w * 0.04f),
    )
    // handlebar
    drawLine(
        color = Color(0xFF3B3B3B),
        start = Offset(w * 0.70f, h * 0.40f),
        end = Offset(w * 0.80f, h * 0.34f),
        strokeWidth = w * 0.022f,
        cap = StrokeCap.Round,
    )
    drawCircle(Color(0xFF2B2B2B), w * 0.018f, Offset(w * 0.805f, h * 0.335f))
    // headlight
    drawCircle(Amber, w * 0.035f, Offset(w * 0.775f, h * 0.47f))

    // rider: legs, torso, arms, helmet
    drawRoundRect(
        color = Jeans,
        topLeft = Offset(w * 0.40f, h * 0.50f),
        size = Size(w * 0.16f, h * 0.22f),
        cornerRadius = CornerRadius(w * 0.05f),
    )
    drawRoundRect(
        color = DeepRed,
        topLeft = Offset(w * 0.38f, h * 0.28f),
        size = Size(w * 0.20f, h * 0.26f),
        cornerRadius = CornerRadius(w * 0.07f),
    )
    // arm reaching to the handlebar
    drawLine(
        color = DeepRed,
        start = Offset(w * 0.55f, h * 0.34f),
        end = Offset(w * 0.74f, h * 0.37f),
        strokeWidth = w * 0.05f,
        cap = StrokeCap.Round,
    )
    drawCircle(Skin, w * 0.026f, Offset(w * 0.755f, h * 0.372f))
    drawCircle(Skin, w * 0.062f, Offset(w * 0.475f, h * 0.20f))
    // helmet
    drawArc(
        color = Helmet,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(w * 0.402f, h * 0.125f),
        size = Size(w * 0.146f, h * 0.146f),
    )
    drawRoundRect(
        color = Helmet,
        topLeft = Offset(w * 0.402f, h * 0.198f),
        size = Size(w * 0.146f, h * 0.026f),
        cornerRadius = CornerRadius(h * 0.013f),
    )

    // insulated delivery bag, always attached to the scooter
    val bagLeft = w * 0.16f
    val bagTop = h * 0.42f
    val bagWidth = w * 0.24f
    val bagHeight = h * 0.26f
    drawRoundRect(
        color = DeepRed,
        topLeft = Offset(bagLeft, bagTop),
        size = Size(bagWidth, bagHeight),
        cornerRadius = CornerRadius(w * 0.035f),
    )
    drawRoundRect(
        color = Coral,
        topLeft = Offset(bagLeft, bagTop),
        size = Size(bagWidth, bagHeight * 0.34f),
        cornerRadius = CornerRadius(w * 0.035f),
    )
    drawRoundRect(
        color = CardWhite.copy(alpha = 0.92f),
        topLeft = Offset(bagLeft + bagWidth * 0.12f, bagTop + bagHeight * 0.44f),
        size = Size(bagWidth * 0.76f, bagHeight * 0.26f),
        cornerRadius = CornerRadius(bagWidth * 0.08f),
    )
    drawRoundRect(
        color = Coral,
        topLeft = Offset(bagLeft + bagWidth * 0.2f, bagTop + bagHeight * 0.5f),
        size = Size(bagWidth * 0.6f, bagHeight * 0.09f),
        cornerRadius = CornerRadius(bagHeight * 0.045f),
    )
}

/** The RushIt shopping bag used on the confirmation and packing scenes. */
@Composable
fun RushItBag(modifier: Modifier = Modifier, fill: Boolean = true) {
    Canvas((if (fill) modifier.fillMaxSize() else modifier).semantics { contentDescription = "RushIt shopping bag" }) {
        val w = size.width
        val h = size.height
        // handles
        drawArc(
            color = Color(0xFFD9A57B),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.30f, h * 0.16f),
            size = Size(w * 0.40f, h * 0.28f),
            style = Stroke(width = w * 0.045f, cap = StrokeCap.Round),
        )
        // body
        drawRoundRect(
            color = Color(0xFFE9BE93),
            topLeft = Offset(w * 0.16f, h * 0.34f),
            size = Size(w * 0.68f, h * 0.56f),
            cornerRadius = CornerRadius(w * 0.05f),
        )
        drawRoundRect(
            color = Color(0xFFDBA87C),
            topLeft = Offset(w * 0.16f, h * 0.34f),
            size = Size(w * 0.68f, h * 0.10f),
            cornerRadius = CornerRadius(w * 0.05f),
        )
        // produce peeking out of the bag
        drawCircle(Leaf, w * 0.075f, Offset(w * 0.33f, h * 0.35f))
        drawCircle(Color(0xFFE4572E), w * 0.065f, Offset(w * 0.50f, h * 0.33f))
        drawCircle(Amber, w * 0.055f, Offset(w * 0.64f, h * 0.36f))
        drawRoundRect(
            color = CardWhite,
            topLeft = Offset(w * 0.70f, h * 0.30f),
            size = Size(w * 0.10f, h * 0.14f),
            cornerRadius = CornerRadius(w * 0.03f),
        )
    }
}

/** Animated green success check drawn as two strokes. */
@Composable
fun SuccessCheck(
    modifier: Modifier = Modifier,
    /** 0f..1f draw progress. */
    progress: Float,
    color: Color = Success,
    scale: Float = 1f,
) {
    Canvas(modifier.semantics { contentDescription = "Order confirmed tick" }) {
        val radius = size.minDimension * 0.48f * scale
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color.copy(alpha = 0.16f), radius * 1.12f, center)
        drawCircle(color, radius, center)
        val p = progress.coerceIn(0f, 1f)
        val start = Offset(center.x - radius * 0.42f, center.y + radius * 0.02f)
        val middle = Offset(center.x - radius * 0.10f, center.y + radius * 0.34f)
        val end = Offset(center.x + radius * 0.46f, center.y - radius * 0.34f)
        val path = Path().apply {
            moveTo(start.x, start.y)
            val firstLeg = 0.45f
            if (p <= firstLeg) {
                val t = p / firstLeg
                lineTo(start.x + (middle.x - start.x) * t, start.y + (middle.y - start.y) * t)
            } else {
                lineTo(middle.x, middle.y)
                val t = (p - firstLeg) / (1f - firstLeg)
                lineTo(middle.x + (end.x - middle.x) * t, middle.y + (end.y - middle.y) * t)
            }
        }
        drawPath(path, Color.White, style = Stroke(width = radius * 0.2f, cap = StrokeCap.Round))
    }
}

/** A few lightweight confetti rectangles; the count is fixed so nothing grows over time. */
@Composable
fun Confetti(modifier: Modifier = Modifier, progress: Float, seed: Int = 7) {
    val colors = listOf(Coral, Amber, Leaf, Color(0xFF5B8DEF), Success)
    Canvas(modifier) {
        if (progress <= 0f) return@Canvas
        val random = java.util.Random(seed.toLong())
        val eased = progress.coerceIn(0f, 1f)
        repeat(16) { index ->
            val startX = random.nextFloat() * size.width
            val drift = (random.nextFloat() - 0.5f) * size.width * 0.25f
            val delayFraction = random.nextFloat() * 0.35f
            val local = ((eased - delayFraction) / (1f - delayFraction)).coerceIn(0f, 1f)
            if (local <= 0f) return@repeat
            val y = -size.height * 0.05f + local * size.height * 0.9f
            val x = startX + drift * local
            val alpha = (1f - local * 0.65f).coerceIn(0f, 1f)
            val color = colors[index % colors.size].copy(alpha = alpha)
            withTransform({ rotate(local * 220f + index * 17f, pivot = Offset(x, y)) }) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(size.minDimension * 0.022f, size.minDimension * 0.042f),
                    cornerRadius = CornerRadius(size.minDimension * 0.008f),
                )
            }
        }
    }
}

/**
 * Illustrated delivery route: store marker, winding road to the destination, park blocks and the
 * moving scooter. Deliberately labelled as an illustration — this is not GPS tracking.
 */
@Composable
fun DeliveryRouteMap(
    modifier: Modifier = Modifier,
    /** 0f..1f position of the scooter along the route. */
    progress: Float,
    destinationLabel: String = "Your location",
    storeLabel: String = "Store",
) {
    Box(modifier.semantics { contentDescription = "Illustrated delivery route, not live GPS tracking" }) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Color(0xFFF3F6EC))
            // park blocks
            drawRoundRect(Color(0xFFDCEBC8), Offset(size.width * 0.06f, size.height * 0.08f), Size(size.width * 0.24f, size.height * 0.18f), CornerRadius(size.minDimension * 0.03f))
            drawRoundRect(Color(0xFFDCEBC8), Offset(size.width * 0.62f, size.height * 0.60f), Size(size.width * 0.20f, size.height * 0.16f), CornerRadius(size.minDimension * 0.03f))
            // secondary roads
            drawLine(Color(0xFFE4E0D6), Offset(0f, size.height * 0.30f), Offset(size.width, size.height * 0.24f), strokeWidth = size.height * 0.05f)
            drawLine(Color(0xFFE4E0D6), Offset(size.width * 0.30f, 0f), Offset(size.width * 0.38f, size.height), strokeWidth = size.height * 0.045f)
            // route
            val start = Offset(size.width * 0.20f, size.height * 0.22f)
            val end = Offset(size.width * 0.76f, size.height * 0.80f)
            val control = Offset(size.width * 0.24f, size.height * 0.86f)
            val route = Path().apply {
                moveTo(start.x, start.y)
                quadraticBezierTo(control.x, control.y, end.x, end.y)
            }
            drawPath(route, Color(0xFFD8D2C6), style = Stroke(width = size.height * 0.05f, cap = StrokeCap.Round))
            drawPath(route, Coral.copy(alpha = 0.85f), style = Stroke(width = size.height * 0.018f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(size.height * 0.035f, size.height * 0.03f))))
            // markers
            drawCircle(Coral, size.minDimension * 0.05f, start)
            drawCircle(CardWhite, size.minDimension * 0.022f, start)
            drawCircle(Success, size.minDimension * 0.055f, end)
            drawCircle(CardWhite, size.minDimension * 0.024f, end)
            // scooter dot following the quadratic curve
            val t = progress.coerceIn(0f, 1f)
            val x = (1 - t) * (1 - t) * start.x + 2 * (1 - t) * t * control.x + t * t * end.x
            val y = (1 - t) * (1 - t) * start.y + 2 * (1 - t) * t * control.y + t * t * end.y
            drawCircle(CardWhite, size.minDimension * 0.055f, Offset(x, y))
            drawCircle(Coral, size.minDimension * 0.038f, Offset(x, y))
            drawCircle(CardWhite, size.minDimension * 0.014f, Offset(x, y))
        }
        MapChip(storeLabel, Modifier.align(Alignment.TopStart).padding(10.dp))
        MapChip(destinationLabel, Modifier.align(Alignment.BottomEnd).padding(10.dp))
    }
}

@Composable
private fun MapChip(label: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        color = CardWhite,
        shadowElevation = 2.dp,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = Ink,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
        )
    }
}

/** Small decorative leaves used behind illustrations. */
@Composable
fun SoftGlow(modifier: Modifier = Modifier, color: Color = Peach) {
    Canvas(modifier) {
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.55f), Color.Transparent)), radius = size.minDimension * 0.5f)
    }
}
