package com.rushit.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rushit.app.R
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.CtaRed
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.Muted
import com.rushit.app.ui.theme.Peach

@Composable
fun RushIcon(@DrawableRes icon: Int, description: String?, modifier: Modifier = Modifier, tint: Color = Ink) {
    Icon(painter = painterResource(icon), contentDescription = description, modifier = modifier, tint = tint)
}

@Composable
fun RushWordmark(modifier: Modifier = Modifier, contentDescription: String? = "RushIt") {
    Image(painter = painterResource(R.drawable.rushit_wordmark), contentDescription = contentDescription, modifier = modifier)
}

@Composable
fun RushButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    arrow: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "button press")
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        interactionSource = interaction,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = CtaRed,
            contentColor = CardWhite,
            disabledContainerColor = Coral.copy(alpha = 0.38f),
            disabledContentColor = CardWhite,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp).semantics { contentDescription = "Please wait" }, color = CardWhite, strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
        if (arrow && !loading) {
            Spacer(Modifier.width(10.dp))
            RushIcon(R.drawable.ic_arrow_right_01, null, Modifier.size(20.dp), CardWhite)
        }
    }
}

@Composable
fun RoundIconButton(
    @DrawableRes icon: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Ink,
    background: Color = CardWhite,
    enabled: Boolean = true,
    badge: Int = 0,
) {
    Box(
        modifier.size(48.dp).graphicsLayer { alpha = if (enabled) 1f else .45f }.clip(CircleShape).background(background)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        RushIcon(icon, description, Modifier.size(24.dp), tint)
        if (badge > 0) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 2.dp).size(18.dp)
                    .clip(CircleShape).background(CtaRed),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (badge > 9) "9+" else "$badge",
                    color = CardWhite,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = Ink, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Row(
                Modifier.clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onAction)
                    .padding(start = 10.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(action, style = MaterialTheme.typography.labelMedium, color = Coral)
                Spacer(Modifier.width(4.dp))
                RushIcon(R.drawable.ic_arrow_right_01, null, Modifier.size(17.dp), Coral)
            }
        }
    }
}

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        RoundIconButton(R.drawable.ic_arrow_left_01, "Go back", onBack, background = Peach)
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = Ink)
    }
}

@Composable
fun MessageCard(title: String, message: String, modifier: Modifier = Modifier, @DrawableRes icon: Int = R.drawable.ic_shopping_bag_01, action: String? = null, onAction: (() -> Unit)? = null) {
    Surface(modifier = modifier.fillMaxWidth(), color = CardWhite, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Border)) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(62.dp).clip(CircleShape).background(Peach), contentAlignment = Alignment.Center) {
                RushIcon(icon, null, Modifier.size(30.dp), Coral)
            }
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = Ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = Muted, textAlign = TextAlign.Center)
            if (action != null && onAction != null) {
                Spacer(Modifier.height(18.dp))
                Text(action, modifier = Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onAction).padding(horizontal = 18.dp, vertical = 12.dp), color = Coral, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
