package com.rushit.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Immutable
data class AnalyticsDataPoint(
    val label: String,
    val detailLabel: String,
    val amountRupees: Int,
    val orderCount: Int,
)

enum class AnalyticsTimeRange(val code: String, val label: String) {
    DAYS_7("7D", "7 Days"),
    DAYS_30("30D", "30 Days"),
    DAYS_90("90D", "90 Days"),
    ALL("All", "All Time"),
}

private val sample7Days = listOf(
    AnalyticsDataPoint("Mon", "24 Oct · Sample", 1840, 6),
    AnalyticsDataPoint("Tue", "25 Oct · Sample", 2310, 8),
    AnalyticsDataPoint("Wed", "26 Oct · Sample", 1980, 7),
    AnalyticsDataPoint("Thu", "27 Oct · Sample", 3200, 11),
    AnalyticsDataPoint("Fri", "28 Oct · Sample", 2790, 9),
    AnalyticsDataPoint("Sat", "29 Oct · Sample", 3650, 13),
    AnalyticsDataPoint("Sun", "30 Oct · Sample", 4120, 15),
)

private val sample30Days = listOf(
    AnalyticsDataPoint("W1", "Week 1 · Sample", 11200, 38),
    AnalyticsDataPoint("W2", "Week 2 · Sample", 14850, 49),
    AnalyticsDataPoint("W3", "Week 3 · Sample", 13400, 44),
    AnalyticsDataPoint("W4", "Week 4 · Sample", 18900, 62),
    AnalyticsDataPoint("Now", "Current · Sample", 19890, 69),
)

private val sample90Days = listOf(
    AnalyticsDataPoint("Aug", "Aug 2026 · Sample", 48200, 160),
    AnalyticsDataPoint("Sep", "Sep 2026 · Sample", 61400, 205),
    AnalyticsDataPoint("Oct", "Oct 2026 · Sample", 78240, 262),
)

private val sampleAllTime = listOf(
    AnalyticsDataPoint("May", "May 2026 · Sample", 31500, 104),
    AnalyticsDataPoint("Jun", "Jun 2026 · Sample", 38900, 129),
    AnalyticsDataPoint("Jul", "Jul 2026 · Sample", 44100, 148),
    AnalyticsDataPoint("Aug", "Aug 2026 · Sample", 48200, 160),
    AnalyticsDataPoint("Sep", "Sep 2026 · Sample", 61400, 205),
    AnalyticsDataPoint("Oct", "Oct 2026 · Sample", 78240, 262),
)

fun formatInrCurrency(amount: Int): String {
    val formatter = NumberFormat.getIntegerInstance(Locale("en", "IN"))
    return "₹${formatter.format(amount)}"
}

fun formatAxisInr(amount: Int): String = when {
    amount >= 100000 -> "₹${String.format(Locale.US, "%.1fL", amount / 100000f)}"
    amount >= 1000 -> "₹${(amount / 1000f).let { if (it % 1f == 0f) it.toInt().toString() else String.format(Locale.US, "%.1f", it) }}k"
    else -> "₹$amount"
}

/**
 * Interactive Dashboard Sales & Order Analytics Card.
 * Clearly separates the illustrative Sample Time-Series dataset (for 7D/30D/90D/All chart exploration)
 * from the exact Stored Demo Orders dataset (which has no timestamps).
 */
@Composable
fun SalesAnalyticsCard(
    orders: List<String>,
    onOpenOrders: () -> Unit,
) {
    val themeConfig = LocalAdminThemeConfig.current
    val brandColor = themeConfig.activePrimary
    val brandSoft = themeConfig.activePrimarySoft

    var dataMode by rememberSaveable { mutableStateOf("sample") } // "sample" or "stored"
    var selectedRangeCode by rememberSaveable { mutableStateOf(AnalyticsTimeRange.DAYS_7.code) }
    val selectedRange = remember(selectedRangeCode) {
        AnalyticsTimeRange.entries.firstOrNull { it.code == selectedRangeCode } ?: AnalyticsTimeRange.DAYS_7
    }

    val storedOrderPoints = remember(orders) {
        orders.mapIndexedNotNull { idx, raw ->
            val clean = raw.substringBefore(" ⟐ ")
            val id = clean.substringBefore(" · ").trim()
            val customer = clean.split(" · ").getOrNull(1)?.trim() ?: "Customer"
            val status = clean.substringAfterLast(" · ").trim()
            val amount = Regex("₹(\\d+)").find(clean)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return@mapIndexedNotNull null
            AnalyticsDataPoint(
                label = "#${idx + 1}",
                detailLabel = "$id · $customer ($status)",
                amountRupees = amount,
                orderCount = 1,
            )
        }
    }

    val activePoints = remember(dataMode, selectedRange, storedOrderPoints) {
        if (dataMode == "stored") {
            storedOrderPoints
        } else {
            when (selectedRange) {
                AnalyticsTimeRange.DAYS_7 -> sample7Days
                AnalyticsTimeRange.DAYS_30 -> sample30Days
                AnalyticsTimeRange.DAYS_90 -> sample90Days
                AnalyticsTimeRange.ALL -> sampleAllTime
            }
        }
    }

    var selectedIndex by remember(activePoints) {
        mutableIntStateOf((activePoints.size / 2).coerceAtMost((activePoints.size - 1).coerceAtLeast(0)))
    }
    val safeSelectedIndex = selectedIndex.coerceIn(0, (activePoints.size - 1).coerceAtLeast(0))
    val selectedPoint = activePoints.getOrNull(safeSelectedIndex)

    val totalAmount = remember(activePoints) { activePoints.sumOf { it.amountRupees } }
    val totalOrdersCount = remember(activePoints) { activePoints.sumOf { it.orderCount } }
    val deliveredStoredTotal = remember(orders) {
        orders.filter { it.substringAfterLast(" · ").trim().equals("Delivered", ignoreCase = true) }
            .mapNotNull { Regex("₹(\\d+)").find(it)?.groupValues?.getOrNull(1)?.toIntOrNull() }
            .sum()
    }

    AdminSurfaceCard {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Sales Overview",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AdminColorScheme.primaryText,
                    )
                    Surface(
                        shape = CircleShape,
                        color = if (dataMode == "sample") AdminColorScheme.warningSoft else brandSoft,
                    ) {
                        Text(
                            text = if (dataMode == "sample") "SAMPLE DATASET" else "STORED ORDERS",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = if (dataMode == "sample") AdminColorScheme.warning else brandColor,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
                Text(
                    text = if (dataMode == "sample") {
                        "Illustrative ${selectedRange.label} time-series · tap or drag chart points"
                    } else {
                        "Exact values from ${storedOrderPoints.size} local demo order records (un-dated)"
                    },
                    color = AdminColorScheme.secondaryText,
                    fontSize = 11.5.sp,
                )
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brandSoft)
                    .clickable(onClick = onOpenOrders),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AdminIcons.TrendingUp,
                    contentDescription = "Open Orders",
                    tint = brandColor,
                    modifier = Modifier.size(19.dp),
                )
            }
        }

        // Data Source Switcher (Sample Time-Series vs Stored Orders)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AdminColorScheme.surfaceSecondary)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(
                "sample" to "Sample Trend (Demo)",
                "stored" to "Stored Orders (${storedOrderPoints.size})",
            ).forEach { (mode, title) ->
                val active = dataMode == mode
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .clickable { dataMode = mode },
                    shape = RoundedCornerShape(9.dp),
                    color = if (active) AdminColorScheme.surface else Color.Transparent,
                    shadowElevation = if (active) 1.dp else 0.dp,
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                        textAlign = TextAlign.Center,
                        color = if (active) brandColor else AdminColorScheme.secondaryText,
                        fontSize = 11.5.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }

        // Time Filter Pills (7D / 30D / 90D / All) when in Sample Trend mode
        if (dataMode == "sample") {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AnalyticsTimeRange.entries.forEach { range ->
                    val selected = range == selectedRange
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .clickable { selectedRangeCode = range.code },
                        shape = CircleShape,
                        color = if (selected) brandColor else AdminColorScheme.surfaceSecondary,
                        border = BorderStroke(
                            1.dp,
                            if (selected) brandColor else AdminColorScheme.divider,
                        ),
                    ) {
                        Text(
                            text = range.code,
                            modifier = Modifier.padding(vertical = 6.dp),
                            textAlign = TextAlign.Center,
                            color = if (selected) Color.White else AdminColorScheme.secondaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        // Summary KPI Strip inside Analytics Card
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AdminColorScheme.surfaceSecondary)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = if (dataMode == "sample") "SAMPLE PERIOD TOTAL" else "LISTED ORDER SUM",
                    color = AdminColorScheme.secondaryText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = formatInrCurrency(totalAmount),
                    color = AdminColorScheme.primaryText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (dataMode == "sample") "SAMPLE ORDERS" else "DELIVERED VALUE",
                    color = AdminColorScheme.secondaryText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (dataMode == "sample") "$totalOrdersCount orders" else formatInrCurrency(deliveredStoredTotal),
                    color = AdminColorScheme.success,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (activePoints.isEmpty()) {
            EmptyState(
                title = "No analytics data points",
                message = "Add demo orders or switch to Sample Trend to preview the analytics chart.",
            )
        } else {
            // Selected Point Tooltip Banner
            selectedPoint?.let { pt ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = AdminColorScheme.primaryText,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = pt.detailLabel,
                                color = Color.White.copy(alpha = 0.82f),
                                fontSize = 11.sp,
                            )
                            Text(
                                text = "${formatInrCurrency(pt.amountRupees)} · ${pt.orderCount} ${if (pt.orderCount == 1) "order" else "orders"}",
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = brandColor,
                        ) {
                            Text(
                                text = pt.label,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            // Smooth Area + Line Chart with Y-Axis Labels, Grid Lines, Touch Interaction, and X-Axis Labels
            val maxAmount = remember(activePoints) {
                (activePoints.maxOfOrNull { it.amountRupees } ?: 1000).coerceAtLeast(500)
            }
            val niceMax = remember(maxAmount) {
                val step = when {
                    maxAmount > 50000 -> 20000
                    maxAmount > 15000 -> 5000
                    maxAmount > 4000 -> 1000
                    else -> 250
                }
                ((maxAmount + step - 1) / step) * step
            }
            val yTicks = remember(niceMax) {
                listOf(niceMax, (niceMax * 2) / 3, niceMax / 3, 0)
            }

            val compactChart = LocalAdminWindow.current.isCompactHeight
            val plotHeight = if (compactChart) 104.dp else 148.dp
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compactChart) 128.dp else 175.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Y-Axis Labels Column
                Column(
                    modifier = Modifier
                        .width(44.dp)
                        .height(plotHeight),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End,
                ) {
                    yTicks.forEach { tick ->
                        Text(
                            text = formatAxisInr(tick),
                            color = AdminColorScheme.secondaryText,
                            fontSize = 10.sp,
                            maxLines = 1,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))

                // Main Chart & X-Axis Column
                Column(modifier = Modifier.weight(1f)) {
                    val chartDescription = remember(activePoints, safeSelectedIndex, dataMode) {
                        val pt = activePoints.getOrNull(safeSelectedIndex)
                        "Analytics chart with ${activePoints.size} points. Selected: ${pt?.detailLabel ?: "none"}, value ${pt?.let { formatInrCurrency(it.amountRupees) } ?: "0"}"
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(plotHeight)
                            .semantics { contentDescription = chartDescription }
                            .pointerInput(activePoints) {
                                detectTapGestures { offset ->
                                    if (activePoints.isNotEmpty() && size.width > 0) {
                                        val stepX = if (activePoints.size > 1) {
                                            size.width.toFloat() / (activePoints.size - 1)
                                        } else {
                                            size.width.toFloat()
                                        }
                                        val nearest = activePoints.indices.minByOrNull { idx ->
                                            val px = if (activePoints.size > 1) idx * stepX else size.width / 2f
                                            abs(offset.x - px)
                                        } ?: 0
                                        selectedIndex = nearest
                                    }
                                }
                            }
                            .pointerInput(activePoints) {
                                detectHorizontalDragGestures { change, _ ->
                                    change.consume()
                                    if (activePoints.isNotEmpty() && size.width > 0) {
                                        val ratio = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                        val nearest = (ratio * (activePoints.size - 1)).roundToInt()
                                            .coerceIn(0, activePoints.size - 1)
                                        selectedIndex = nearest
                                    }
                                }
                            },
                    ) {
                        val w = size.width
                        val h = size.height
                        val topPad = 12.dp.toPx()
                        val bottomPad = 10.dp.toPx()
                        val plotHeight = (h - topPad - bottomPad).coerceAtLeast(10f)

                        // Draw horizontal grid lines
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        for (i in 0..3) {
                            val y = topPad + (plotHeight * i / 3f)
                            drawLine(
                                color = AdminColorScheme.divider,
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = if (i < 3) dashEffect else null,
                            )
                        }

                        val coordinates = activePoints.mapIndexed { idx, pt ->
                            val x = if (activePoints.size > 1) {
                                (w * idx) / (activePoints.size - 1).toFloat()
                            } else {
                                w / 2f
                            }
                            val fraction = (pt.amountRupees.toFloat() / niceMax.toFloat()).coerceIn(0f, 1f)
                            val y = topPad + plotHeight * (1f - fraction)
                            Offset(x, y)
                        }

                        if (coordinates.isNotEmpty()) {
                            val linePath = Path().apply {
                                moveTo(coordinates.first().x, coordinates.first().y)
                                for (i in 0 until coordinates.lastIndex) {
                                    val curr = coordinates[i]
                                    val next = coordinates[i + 1]
                                    val controlX1 = (curr.x + next.x) / 2f
                                    val controlX2 = (curr.x + next.x) / 2f
                                    cubicTo(
                                        controlX1, curr.y,
                                        controlX2, next.y,
                                        next.x, next.y,
                                    )
                                }
                            }

                            val areaPath = Path().apply {
                                addPath(linePath)
                                lineTo(coordinates.last().x, topPad + plotHeight)
                                lineTo(coordinates.first().x, topPad + plotHeight)
                                close()
                            }

                            // Gradient Fill under curve
                            drawPath(
                                path = areaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        brandColor.copy(alpha = 0.30f),
                                        brandColor.copy(alpha = 0.03f),
                                    ),
                                    startY = topPad,
                                    endY = topPad + plotHeight,
                                ),
                            )

                            // Smooth Curve Line
                            drawPath(
                                path = linePath,
                                color = brandColor,
                                style = Stroke(
                                    width = 3.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round,
                                ),
                            )

                            // Vertical highlight line on selected point
                            val selectedOffset = coordinates.getOrNull(safeSelectedIndex)
                            if (selectedOffset != null) {
                                drawLine(
                                    color = brandColor.copy(alpha = 0.45f),
                                    start = Offset(selectedOffset.x, topPad),
                                    end = Offset(selectedOffset.x, topPad + plotHeight),
                                    strokeWidth = 1.5.dp.toPx(),
                                    pathEffect = dashEffect,
                                )
                            }

                            // Data Points
                            coordinates.forEachIndexed { idx, ptOffset ->
                                val isSelected = idx == safeSelectedIndex
                                drawCircle(
                                    color = if (isSelected) brandColor else Color.White,
                                    radius = if (isSelected) 6.5.dp.toPx() else 4.5.dp.toPx(),
                                    center = ptOffset,
                                )
                                drawCircle(
                                    color = if (isSelected) Color.White else brandColor,
                                    radius = if (isSelected) 3.dp.toPx() else 2.5.dp.toPx(),
                                    center = ptOffset,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    // X-Axis Labels Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        activePoints.forEachIndexed { idx, pt ->
                            val isSelected = idx == safeSelectedIndex
                            Text(
                                text = pt.label,
                                color = if (isSelected) brandColor else AdminColorScheme.secondaryText,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { selectedIndex = idx }
                                    .padding(horizontal = 3.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
            }
        }

        // Honest Data Explanation Footer
        Text(
            text = if (dataMode == "sample") {
                "Sample trend is a clearly labelled demo dataset for testing 7D/30D/90D/All filtering. Switch to 'Stored Orders' to inspect the un-dated local order records."
            } else {
                "Stored local orders do not include creation timestamps or payment confirmation. Totals include all listed demo records."
            },
            color = AdminColorScheme.secondaryText,
            fontSize = 11.sp,
            lineHeight = 15.sp,
        )
    }
}

/**
 * Order Status Distribution breakdown card for the Dashboard.
 */
@Composable
fun OrderStatusDistributionCard(
    orders: List<String>,
    onOpenOrders: () -> Unit,
) {
    val statuses = remember(orders) {
        orders.map { it.substringBefore(" ⟐ ").substringAfterLast(" · ").trim() }
    }
    val total = statuses.size.coerceAtLeast(1)
    val delivered = statuses.count { it.equals("Delivered", true) }
    val inTransit = statuses.count { it.equals("On the way", true) || it.equals("Almost there", true) || it.equals("Picked Up", true) || it.equals("Rider Ready", true) }
    val preparing = statuses.count { it.equals("Preparing", true) || it.equals("Confirmed", true) }
    val cancelled = statuses.count { it.equals("Cancelled", true) }

    val buckets = listOf(
        Triple("Delivered", delivered, AdminColorScheme.success),
        Triple("In Transit", inTransit, AdminColorScheme.info),
        Triple("Preparing", preparing, AdminColorScheme.warning),
        Triple("Cancelled", cancelled, AdminColorScheme.error),
    )

    AdminSurfaceCard(
        modifier = Modifier.clickable(onClick = onOpenOrders),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Order Status Distribution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${orders.size} recorded demo orders by current stage",
                    color = AdminColorScheme.secondaryText,
                    fontSize = 12.sp,
                )
            }
            StatusChip("${orders.size} Total")
        }

        Spacer(Modifier.height(6.dp))
        // Proportional segmented bar
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(AdminColorScheme.surfaceSecondary),
        ) {
            var startX = 0f
            buckets.forEach { (_, count, color) ->
                if (count > 0) {
                    val segWidth = (count.toFloat() / total.toFloat()) * size.width
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(startX, 0f),
                        size = Size(segWidth, size.height),
                        cornerRadius = CornerRadius(size.height / 2, size.height / 2),
                    )
                    startX += segWidth
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            buckets.forEach { (label, count, color) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color),
                    )
                    Text(
                        text = "$label ($count)",
                        color = AdminColorScheme.primaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
