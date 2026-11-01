package com.rushit.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rushit.app.domain.DeliveryStage
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.LocalReducedMotion
import com.rushit.app.ui.theme.Muted
import com.rushit.app.ui.theme.Success

/**
 * Four-stage delivery timeline. The active stage comes from the order status only; this component
 * never advances anything.
 */
@Composable
fun DeliveryTimeline(
    activeStageIndex: Int,
    modifier: Modifier = Modifier,
    failed: Boolean = false,
) {
    val reduced = LocalReducedMotion.current
    val stages = DeliveryStage.entries.toList()
    val progressTarget = if (failed) 0f else (activeStageIndex.coerceAtLeast(0).toFloat() / (stages.size - 1))
    val progress by animateFloatAsStateCompat(progressTarget, reduced, label = "timeline progress")

    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
            Box(
                Modifier.fillMaxWidth().height(3.dp).align(Alignment.Center)
                    .background(Border, CircleShape),
            )
            Box(
                Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(3.dp).align(Alignment.CenterStart)
                    .background(if (failed) Border else Success, CircleShape),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                stages.forEachIndexed { index, _ ->
                    val done = !failed && index <= activeStageIndex
                    val current = !failed && index == activeStageIndex
                    StageDot(done = done, current = current, reduced = reduced)
                    if (index != stages.lastIndex) Spacer4()
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            stages.forEachIndexed { index, stage ->
                val done = !failed && index <= activeStageIndex
                Text(
                    text = stage.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 12.sp),
                    color = if (done) Ink else Muted,
                    fontWeight = if (done) FontWeight.Bold else FontWeight.Medium,
                    textAlign = if (index == 0) TextAlign.Start else if (index == stages.lastIndex) TextAlign.End else TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Spacer4() = Box(Modifier.width(0.dp))

@Composable
private fun StageDot(done: Boolean, current: Boolean, reduced: Boolean) {
    val size by animateDpAsState(if (current) 20.dp else 16.dp, animationSpec = if (reduced) snap() else tween(240), label = "stage size")
    val color by animateColorAsState(if (done) Success else Border, tween(240), label = "stage color")
    Box(
        Modifier.size(size).clip(CircleShape).background(CardWhite),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(size - 4.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
            if (done) {
                Text("✓", color = CardWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clearAndSetSemantics { })
            } else if (current) {
                PulseDot(reduced)
            }
        }
    }
}

@Composable
private fun PulseDot(reduced: Boolean) {
    if (reduced) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(Coral))
        return
    }
    val transition = rememberInfiniteTransition(label = "stage pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "stage pulse alpha",
    )
    Box(Modifier.size(6.dp).clip(CircleShape).background(Coral.copy(alpha = alpha)))
}

@Composable
private fun animateFloatAsStateCompat(target: Float, reduced: Boolean, label: String): androidx.compose.runtime.State<Float> =
    androidx.compose.animation.core.animateFloatAsState(
        targetValue = target,
        animationSpec = if (reduced) snap() else tween(320, easing = FastOutSlowInEasing),
        label = label,
    )

/** Small status pill used on journey screens. */
@Composable
fun StatusPill(text: String, color: Color = Success, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(CircleShape).background(color.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Box(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
