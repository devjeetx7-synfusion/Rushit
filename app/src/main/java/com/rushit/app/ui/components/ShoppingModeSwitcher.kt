package com.rushit.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rushit.app.R
import com.rushit.app.data.model.ShoppingMode
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.LocalReducedMotion
import com.rushit.app.ui.theme.Motion
import com.rushit.app.ui.theme.Muted
import com.rushit.app.ui.theme.Peach

private const val SEGMENT_HEIGHT_DP = 64

@Composable
private fun modeImage(mode: ShoppingMode): Int = when (mode) {
    ShoppingMode.GROCERY -> R.drawable.product_tomatoes
    ShoppingMode.ECOMMERCE -> R.drawable.ecommerce_headphones
    ShoppingMode.GIFTS -> R.drawable.gift_bouquet
}

/**
 * Segmented shopping-mode selector with one sliding indicator.
 *
 * The selection itself lives in [com.rushit.app.ui.viewmodel.ShopUiState]; this composable only
 * animates towards it, so no two screens can disagree about the active mode.
 */
@Composable
fun ShoppingModeSwitcher(
    selectedMode: ShoppingMode,
    onModeSelected: (ShoppingMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduced = LocalReducedMotion.current
    val modes = remember { ShoppingMode.entries.toList() }
    val selectedIndex = modes.indexOf(selectedMode).coerceAtLeast(0)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Peach.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, Border),
    ) {
        BoxWithConstraints(Modifier.padding(4.dp)) {
            val segmentWidth = maxWidth / modes.size
            val indicatorOffset by animateDpAsState(
                targetValue = segmentWidth * selectedIndex,
                animationSpec = if (reduced) snap() else tween(Motion.MODE_INDICATOR, easing = FastOutSlowInEasing),
                label = "mode indicator",
            )
            // A single animated fraction drives icon, label weight and colour together.
            val position = if (segmentWidth.value > 0f) indicatorOffset.value / segmentWidth.value else selectedIndex.toFloat()

            // The indicator tracks the row height instead of a fixed size, so large font scales
            // grow the control rather than clipping the two-line labels.
            Box(Modifier.matchParentSize()) {
                Box(
                    Modifier
                        .offset(x = indicatorOffset)
                        .width(segmentWidth)
                        .fillMaxHeight()
                        .shadow(if (reduced) 0.dp else 2.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardWhite)
                        .border(1.dp, Border.copy(alpha = 0.7f), RoundedCornerShape(20.dp)),
                )
            }
            Row(Modifier.fillMaxWidth().heightIn(min = SEGMENT_HEIGHT_DP.dp).selectableGroup()) {
                modes.forEachIndexed { index, mode ->
                    val fraction = (1f - kotlin.math.abs(position - index)).coerceIn(0f, 1f)
                    ModeSegment(
                        mode = mode,
                        selected = mode == selectedMode,
                        fraction = fraction,
                        onClick = { onModeSelected(mode) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeSegment(
    mode: ShoppingMode,
    selected: Boolean,
    fraction: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Weight moves through the font's medium/semibold/bold cuts instead of snapping.
    val weight = FontWeight((500 + 200 * fraction).toInt().coerceIn(1, 1000))
    Column(
        modifier
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .graphicsLayer {
                    val scale = 0.92f + 0.08f * fraction
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(Peach),
            contentAlignment = Alignment.Center,
        ) {
            CatalogImage(null, modeImage(mode), null, Modifier.size(24.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = mode.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 13.sp, fontWeight = weight),
            color = lerp(Muted, Ink, fraction),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
