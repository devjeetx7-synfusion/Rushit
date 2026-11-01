package com.rushit.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.rushit.app.data.auth.google.SupabaseOAuthRedirect
import com.rushit.app.ui.navigation.RushItApp
import com.rushit.app.ui.theme.Cream
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.RushItTheme
import com.rushit.app.ui.viewmodel.AuthViewModel
import com.rushit.app.ui.viewmodel.OrderViewModel
import com.rushit.app.ui.viewmodel.ShopViewModel
import androidx.compose.ui.graphics.toArgb

class MainActivity : ComponentActivity() {
    private val app get() = application as RushItApplication

    // The ViewModel survives configuration changes; its sign-in client belongs to the Application.
    // Each Activity rebinds the weak foreground host through lifecycle callbacks, never a stale ref.
    private val auth: AuthViewModel by viewModels {
        AuthViewModel.factory(app.authRepository, app.googleSignIn)
    }
    private val shop: ShopViewModel by viewModels { ShopViewModel.factory(applicationContext, app.catalogRepository) }
    private val orders: OrderViewModel by viewModels { OrderViewModel.factory(app.orderRepository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.foregroundActivityTracker.bind(this)
        consumeAuthIntent(intent)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeAuthIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Returning from the browser without a callback abandons that attempt. A callback already
        // being exchanged is protected by the client's single-flight lock and is not cancelled.
        auth.onForeground()
    }

    private fun consumeAuthIntent(intent: Intent) {
        val uri = intent.dataString ?: return
        if (SupabaseOAuthRedirect.parse(uri) == SupabaseOAuthRedirect.Result.Ignored) return
        auth.handleAuthRedirect(uri)
        // Prevent relaunch/rotation from replaying an authorization code. The client also keeps a
        // one-way consumed-code marker for duplicate deliveries across Activity recreation.
        intent.data = null
    }
}
