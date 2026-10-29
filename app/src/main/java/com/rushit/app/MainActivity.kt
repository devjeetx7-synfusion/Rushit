package com.rushit.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.rushit.app.ui.navigation.RushItApp
import com.rushit.app.ui.theme.Cream
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.RushItTheme
import com.rushit.app.ui.viewmodel.AuthViewModel
import com.rushit.app.ui.viewmodel.ShopViewModel
import androidx.compose.ui.graphics.toArgb

class MainActivity : ComponentActivity() {
    private val app get() = application as RushItApplication
    private val auth: AuthViewModel by viewModels { AuthViewModel.factory(app.authRepository) }
    private val shop: ShopViewModel by viewModels { ShopViewModel.factory(applicationContext, app.catalogRepository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Cream.toArgb()
        // Android 7 cannot draw dark navigation icons, so use a dark bar there.
        window.navigationBarColor = if (Build.VERSION.SDK_INT >= 26) Cream.toArgb() else Ink.toArgb()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent { RushItTheme { RushItApp(auth, shop) } }
    }
}
