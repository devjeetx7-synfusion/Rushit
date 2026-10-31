package com.rushit.app.ui.screens.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rushit.app.R
import com.rushit.app.ui.components.RushWordmark
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.Cream
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.Peach
import com.rushit.app.data.auth.AuthState

@Composable
fun SplashScreen(state: AuthState, onFinished: (String) -> Unit) {
    var entered by remember { mutableStateOf(false) }
    var routed by remember { mutableStateOf(false) }
    val finish by rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) { entered = true }
    LaunchedEffect(state) {
        val route = startupRouteFor(state)
        if (route != null && !routed) {
            // Let the first visible frame and NavHost attach without adding a startup timer.
            withFrameNanos { }
            routed = true
            finish(route)
        }
    }
    // A fast session check can finish before the animation starts: content must never start invisible.
    val logoAlpha by animateFloatAsState(if (entered) 1f else 0.78f, tween(450), label = "logo fade")
    val logoScale by animateFloatAsState(if (entered) 1f else 0.96f, tween(500), label = "logo scale")
    val imageAlpha by animateFloatAsState(if (entered) 1f else 0.72f, tween(550), label = "illustration entrance")
    val imageScale by animateFloatAsState(if (entered) 1f else 0.98f, tween(550), label = "illustration scale")

    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Cream, Cream, Peach.copy(alpha = 0.65f))))
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.8f))
        RushWordmark(Modifier.fillMaxWidth(0.72f).height(92.dp).graphicsLayer { alpha = logoAlpha; scaleX = logoScale; scaleY = logoScale })
        Spacer(Modifier.height(4.dp))
        Text("Fresh Finds. Fast Delivery.", color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.alpha(logoAlpha))
        Spacer(Modifier.weight(0.25f))
        Box(Modifier.fillMaxWidth().weight(2.6f), contentAlignment = Alignment.Center) {
            Image(
                painterResource(R.drawable.splash_groceries), contentDescription = "A bag filled with fresh groceries",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = imageAlpha; scaleX = imageScale; scaleY = imageScale },
            )
        }
        Spacer(Modifier.weight(0.2f))
        SplashProgress()
        Spacer(Modifier.height(16.dp))
        Text("Getting things fresh for you...", color = Ink, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SplashProgress() {
    val transition = rememberInfiniteTransition(label = "loading")
    val offset by transition.animateFloat(
        initialValue = 0f, targetValue = 0.52f,
        animationSpec = infiniteRepeatable(tween(1050, easing = LinearEasing), RepeatMode.Reverse), label = "progress",
    )
    Canvas(Modifier.width(170.dp).height(8.dp).semantics {
        contentDescription = "Initializing RushIt"
        progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
    }) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(Peach, cornerRadius = radius)
        drawRoundRect(Coral, topLeft = Offset(size.width * offset, 0f), size = Size(size.width * .48f, size.height), cornerRadius = radius)
    }
}

/**
 * The start destination for a resolved authentication state, or null while the state is still
 * transient. Kept pure and internal so the routing decision is unit-testable: showing the login
 * screen for a moment while a valid session is being restored is exactly the bug this prevents.
 */
internal fun startupRouteFor(state: AuthState): String? = when (state) {
    is AuthState.Authenticated -> "home"
    is AuthState.ProfileIncomplete -> "profile-setup"
    AuthState.Unauthenticated -> "login"
    is AuthState.RecoverableError -> "login"
    AuthState.Loading, AuthState.Authenticating -> null
}
