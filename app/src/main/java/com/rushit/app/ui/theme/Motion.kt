package com.rushit.app.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Coordinated, deliberately short timings for the shopping surfaces. Keep them in one place so a
 * mode change, its indicator and the content swap feel like one gesture rather than three effects.
 */
object Motion {
    const val MODE_INDICATOR = 280
    const val CATEGORY_SELECTION = 240
    const val CONTENT_TRANSITION = 210
    const val WISHLIST = 200
    const val ADD_TO_CART = 240
    const val BOTTOM_NAVIGATION = 210
    const val IMAGE_CROSSFADE = 180
    const val BANNER_AUTO_ADVANCE_MS = 5_500L
}

/** Provided once at the app root; components read it instead of touching Settings themselves. */
val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * Honours the system "remove animations" accessibility setting (Developer options / accessibility).
 * Motion is decorative here, so collapsing it never hides information.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}

/** A duration that collapses to zero when the user asked for reduced motion. */
fun motionDuration(baseMillis: Int, reduced: Boolean): Int = if (reduced) 0 else baseMillis
