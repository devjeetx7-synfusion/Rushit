package com.rushit.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rushit.app.R
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.Ink

/**
 * The single sign-in entry point: the official Google "G" mark with "Continue with Google".
 *
 * Deliberately the only button on the login card. It is disabled and shows a spinner while a request
 * is in flight, which — together with the single-flight guard in `AuthViewModel` — makes duplicate
 * sign-ins impossible.
 *
 * @param loading replaces the Google mark with a spinner; the label is kept so the button does not
 * jump while a request runs
 */
@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    label: String = "Continue with Google",
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, label = "google button press")
    val active = enabled && !loading

    Surface(
        onClick = onClick,
        enabled = active,
        interactionSource = interaction,
        shape = CircleShape,
        color = CardWhite,
        contentColor = Ink,
        shadowElevation = if (pressed) 0.5.dp else 2.dp,
        border = BorderStroke(1.dp, if (active) Border else Border.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .alpha(if (active) 1f else 0.62f)
            .semantics { if (loading) stateDescription = "Signing in" },
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    Modifier
                        .size(22.dp)
                        .semantics { contentDescription = "Signing in with Google" },
                    color = Coral,
                    strokeWidth = 2.4.dp,
                )
            } else {
                // Decorative: the adjacent label already says "Continue with Google".
                Image(
                    painterResource(R.drawable.ic_google_g),
                    contentDescription = null,
                    Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
