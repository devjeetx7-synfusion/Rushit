package com.rushit.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import com.rushit.app.ui.navigation.RushItApp
import com.rushit.app.ui.theme.Cream
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.RushItTheme
import com.rushit.app.ui.viewmodel.AuthViewModel
import com.rushit.app.ui.viewmodel.OrderViewModel
import com.rushit.app.ui.viewmodel.ShopViewModel

/**
 * The only Activity.
 *
 * There is no authentication Intent to consume any more: Google sign-in is a Credential Manager
 * request that completes in-process, and phone verification is a Firebase callback. The app has no
 * OAuth redirect URI, no Custom Tab and no deep link.
 */
class MainActivity : ComponentActivity() {
    private val app get() = application as RushItApplication

    // The ViewModel survives configuration changes; its sign-in client belongs to the Application.
    // Each Activity rebinds the weak foreground host through lifecycle callbacks, never a stale ref.
    private val auth: AuthViewModel by viewModels {
        AuthViewModel.factory(app.authRepository, app.googleSignIn)
    }
    private val shop: ShopViewModel by viewModels {
        ShopViewModel.factory(applicationContext, app.catalogRepository)
    }
    private val orders: OrderViewModel by viewModels { OrderViewModel.factory(app.orderRepository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.foregroundActivityTracker.bind(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Cream.toArgb()
        // Android 7 cannot draw dark navigation icons, so use a dark bar there.
        window.navigationBarColor = if (Build.VERSION.SDK_INT >= 26) Cream.toArgb() else Ink.toArgb()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent { RushItTheme { RushItApp(auth, shop, orders) } }
    }
}
