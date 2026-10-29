package com.rushit.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rushit.app.ui.components.CartSummary
import com.rushit.app.ui.components.RushBottomBar
import com.rushit.app.ui.components.mainDestinations
import com.rushit.app.ui.screens.home.HomeScreen
import com.rushit.app.ui.screens.login.LoginScreen
import com.rushit.app.ui.screens.login.OtpScreen
import com.rushit.app.ui.screens.splash.SplashScreen
import com.rushit.app.ui.screens.store.AccountScreen
import com.rushit.app.ui.screens.store.CartScreen
import com.rushit.app.ui.screens.store.CategoriesScreen
import com.rushit.app.ui.screens.store.OrdersScreen
import com.rushit.app.ui.screens.store.ProductListingScreen
import com.rushit.app.ui.screens.store.SearchScreen
import com.rushit.app.ui.theme.Cream
import com.rushit.app.ui.viewmodel.AuthEvent
import com.rushit.app.ui.viewmodel.AuthViewModel
import com.rushit.app.ui.viewmodel.ShopViewModel
import kotlinx.coroutines.flow.collect

@Composable
fun RushItApp(auth: AuthViewModel, shop: ShopViewModel) {
    val authState by auth.state.collectAsState()
    val shopState by shop.state.collectAsState()
    val nav = rememberNavController()
    var needsStartupRoute by remember(auth) { mutableStateOf(!auth.startupRouteHandled) }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showTabs = !needsStartupRoute && mainDestinations.any { it.route == route }
    val shoppingRoute = !needsStartupRoute && (showTabs || route == "search" || route == "all" || route == "bestsellers" || route == "wishlist" || route == "category/{id}")
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val showSummary = shoppingRoute && shopState.totals.itemCount > 0 && !imeVisible

    fun selectMain(destination: String) {
        nav.navigate(destination) {
            popUpTo("home") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(auth, nav) {
        auth.events.collect { event ->
            when (event) {
                AuthEvent.OpenOtp -> nav.navigate("otp") { launchSingleTop = true }
                AuthEvent.OpenHome -> nav.navigate("home") {
                    popUpTo("login") { inclusive = true }
                    launchSingleTop = true
                }
                AuthEvent.OpenLogin -> nav.navigate("login") {
                    popUpTo(nav.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Cream,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (showTabs || showSummary) {
                Column(Modifier.fillMaxWidth().background(Cream).navigationBarsPadding()) {
                    if (showSummary) CartSummary(shopState.totals, onClick = { nav.navigate("cart") { launchSingleTop = true } })
                    if (showTabs && route != null) RushBottomBar(route, onSelect = ::selectMain)
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            NavHost(navController = nav, startDestination = "splash", modifier = Modifier.fillMaxSize()) {
                // Keep a restored stack inert until a fresh process has checked its session.
                composable("splash") { }
                composable("login") {
                    if (!needsStartupRoute) LoginScreen(authState, auth::updatePhone, onContinue = { auth.requestOtp() }, onBrowseSample = auth::browseSampleStore)
                }
                composable("otp") {
                    if (!needsStartupRoute) OtpScreen(authState, onBack = { auth.clearMessage(); nav.popBackStack() }, onOtpChange = auth::updateOtp,
                        onVerify = auth::verifyOtp, onResend = { auth.requestOtp(resend = true) })
                }
                composable("home") {
                    if (!needsStartupRoute) HomeScreen(
                        state = shopState,
                        onSearch = { shop.updateSearch(""); nav.navigate("search") },
                        onCategory = { nav.navigate("category/$it") },
                        onSeeAll = { nav.navigate("bestsellers") },
                        onShopNow = { nav.navigate("all") },
                        onAccount = { selectMain("account") },
                        onAdd = { shop.changeQuantity(it, 1) },
                        onWishlist = shop::toggleWishlist,
                        onSaveAddress = shop::saveAddress,
                        onRetry = shop::loadCatalog,
                    )
                }
                composable("categories") {
                    if (!needsStartupRoute) CategoriesScreen(shopState, onCategory = { nav.navigate("category/$it") }, onRetry = shop::loadCatalog)
                }
                composable("orders") { if (!needsStartupRoute) OrdersScreen(onHome = { selectMain("home") }) }
                composable("account") {
                    if (!needsStartupRoute) AccountScreen(authState, shopState, onWishlist = { nav.navigate("wishlist") }, onSaveAddress = shop::saveAddress, onSignInOrOut = auth::signOutOrSignIn)
                }
                composable("search") {
                    if (!needsStartupRoute) SearchScreen(shopState, onBack = { nav.popBackStack() }, onQuery = shop::updateSearch,
                        onAdd = { shop.changeQuantity(it, 1) }, onWishlist = shop::toggleWishlist, onRetry = shop::loadCatalog)
                }
                composable("all") {
                    if (!needsStartupRoute) ProductListingScreen("All groceries", shopState.products, shopState, onBack = { nav.popBackStack() },
                        onAdd = { shop.changeQuantity(it, 1) }, onWishlist = shop::toggleWishlist, onRetry = shop::loadCatalog)
                }
                composable("bestsellers") {
                    if (!needsStartupRoute) ProductListingScreen("Bestsellers", shopState.products.filter { it.isBestseller }, shopState, onBack = { nav.popBackStack() },
                        onAdd = { shop.changeQuantity(it, 1) }, onWishlist = shop::toggleWishlist, onRetry = shop::loadCatalog)
                }
                composable("wishlist") {
                    if (!needsStartupRoute) ProductListingScreen("Wishlist", shopState.products.filter { it.id in shopState.wishlist }, shopState,
                        onBack = { nav.popBackStack() }, onAdd = { shop.changeQuantity(it, 1) }, onWishlist = shop::toggleWishlist,
                        onRetry = shop::loadCatalog, emptyTitle = "Your wishlist is empty", emptyMessage = "Tap a heart on a product to save it here.")
                }
                composable("category/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { backStackEntry ->
                    if (!needsStartupRoute) {
                        val id = backStackEntry.arguments?.getString("id").orEmpty()
                        val title = shopState.categories.firstOrNull { it.id == id }?.name ?: "Category"
                        ProductListingScreen(title, shopState.products.filter { it.categoryId == id }, shopState, onBack = { nav.popBackStack() },
                            onAdd = { shop.changeQuantity(it, 1) }, onWishlist = shop::toggleWishlist, onRetry = shop::loadCatalog)
                    }
                }
                composable("cart") {
                    if (!needsStartupRoute) CartScreen(shopState, onBack = { nav.popBackStack() }, onChangeQuantity = shop::changeQuantity)
                }
            }
            if (needsStartupRoute) {
                SplashScreen(authState) { authenticated ->
                    nav.navigate(if (authenticated) "home" else "login") {
                        popUpTo(nav.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                    auth.markStartupRouted()
                    needsStartupRoute = false
                }
            }
        }
    }
}
