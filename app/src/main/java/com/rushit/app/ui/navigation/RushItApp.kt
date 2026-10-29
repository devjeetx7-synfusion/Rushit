package com.rushit.app.ui.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.CompositionLocalProvider
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
import com.rushit.app.data.model.ShoppingMode
import com.rushit.app.domain.categoriesForMode
import com.rushit.app.domain.productsForMode
import com.rushit.app.ui.components.CartSummary
import com.rushit.app.ui.components.RushBottomBar
import com.rushit.app.ui.components.mainDestinations
import com.rushit.app.ui.screens.home.HomeScreen
import com.rushit.app.ui.screens.login.LoginScreen
import com.rushit.app.ui.screens.login.OtpScreen
import com.rushit.app.ui.screens.splash.SplashScreen
import com.rushit.app.ui.screens.store.AccountScreen
import com.rushit.app.ui.screens.store.AddressScreen
import com.rushit.app.ui.screens.store.CheckoutScreen
import com.rushit.app.ui.screens.store.ProductDetailsScreen
import com.rushit.app.ui.screens.store.TrackingScreen
import com.rushit.app.ui.screens.store.CartScreen
import com.rushit.app.ui.screens.store.CategoriesScreen
import com.rushit.app.ui.screens.store.OrdersScreen
import com.rushit.app.ui.screens.store.ProductListingScreen
import com.rushit.app.ui.screens.store.SearchScreen
import com.rushit.app.ui.theme.Cream
import com.rushit.app.ui.theme.LocalReducedMotion
import com.rushit.app.ui.theme.rememberReducedMotion
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
    val modeProducts = remember(shopState.products, shopState.selectedMode) { productsForMode(shopState.products, shopState.selectedMode) }
    val modeCategories = remember(shopState.categories, shopState.selectedMode) { categoriesForMode(shopState.categories, shopState.selectedMode) }
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

    fun openProduct(id: String) { nav.navigate("product/${Uri.encode(id)}") { launchSingleTop = true } }
    fun openCategory(id: String) { nav.navigate("category/${Uri.encode(id)}") { launchSingleTop = true } }

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

    CompositionLocalProvider(LocalReducedMotion provides rememberReducedMotion()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Cream,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (showTabs || showSummary) {
                    Column(Modifier.fillMaxWidth().background(Cream).navigationBarsPadding()) {
                        AnimatedVisibility(
                            visible = showSummary,
                            enter = slideInVertically(tween(220)) { height -> height / 2 } + fadeIn(tween(180)),
                            exit = slideOutVertically(tween(180)) { height -> height / 2 } + fadeOut(tween(140)),
                        ) {
                            CartSummary(
                                totals = shopState.totals,
                                onClick = { nav.navigate("cart") { launchSingleTop = true } },
                                isSample = shopState.isSample,
                            )
                        }
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
                            onSearch = { shop.updateSearch(""); shop.selectSearchCategory(null); nav.navigate("search") { launchSingleTop = true } },
                            onSeeAll = {
                                val categoryId = shopState.selectedCategoryId
                                nav.navigate(
                                    when {
                                        categoryId != null -> "category/${Uri.encode(categoryId)}"
                                        shopState.selectedMode == ShoppingMode.GROCERY -> "bestsellers"
                                        else -> "all"
                                    },
                                ) { launchSingleTop = true }
                            },
                            onShopNow = { nav.navigate("all") { launchSingleTop = true } },
                            onAccount = { selectMain("account") },
                            onCart = { nav.navigate("cart") { launchSingleTop = true } },
                            onAddress = { nav.navigate("addresses") { launchSingleTop = true } },
                            onModeSelected = shop::selectMode,
                            onCategorySelected = shop::selectCategory,
                            onProduct = ::openProduct,
                            onAdd = { shop.changeQuantity(it, 1) },
                            onWishlist = shop::toggleWishlist,
                            onRetry = shop::loadCatalog,
                        )
                    }
                    composable("categories") {
                        if (!needsStartupRoute) CategoriesScreen(
                            state = shopState,
                            onModeSelected = shop::selectMode,
                            onCategory = { id -> shop.selectCategory(id); openCategory(id) },
                            onRetry = shop::loadCatalog,
                        )
                    }
                    composable("orders") { if (!needsStartupRoute) OrdersScreen(onHome = { selectMain("home") }, onTracking = { nav.navigate("tracking") { launchSingleTop = true } }) }
                    composable("account") {
                        if (!needsStartupRoute) AccountScreen(authState, shopState, onWishlist = { nav.navigate("wishlist") { launchSingleTop = true } },
                            onAddresses = { nav.navigate("addresses") { launchSingleTop = true } }, onOrders = { selectMain("orders") }, onSignInOrOut = auth::signOutOrSignIn)
                    }
                    composable("search") {
                        if (!needsStartupRoute) SearchScreen(shopState, onBack = { nav.popBackStack() }, onQuery = shop::updateSearch,
                            onSubmit = shop::recordSearch, onRecent = { shop.updateSearch(it); shop.recordSearch() }, onClearRecent = shop::clearRecentSearches,
                            onCategoryFilter = shop::selectSearchCategory,
                            onAdd = { shop.changeQuantity(it, 1) }, onWishlist = shop::toggleWishlist, onProduct = ::openProduct, onRetry = shop::loadCatalog)
                    }
                    composable("all") {
                        if (!needsStartupRoute) ProductListingScreen("All ${shopState.selectedMode.label}", modeProducts, shopState, onBack = { nav.popBackStack() },
                            onAdd = { shop.changeQuantity(it, 1) }, onWishlist = shop::toggleWishlist, onProduct = ::openProduct, onRetry = shop::loadCatalog,
                            emptyMessage = "No products have been configured for this mode yet.")
                    }
                    composable("bestsellers") {
                        if (!needsStartupRoute) ProductListingScreen("Bestsellers", modeProducts.filter { it.isBestseller }, shopState, onBack = { nav.popBackStack() },
                            onAdd = { shop.changeQuantity(it, 1) }, onWishlist = shop::toggleWishlist, onProduct = ::openProduct, onRetry = shop::loadCatalog)
                    }
                    composable("wishlist") {
                        if (!needsStartupRoute) ProductListingScreen("Wishlist", modeProducts.filter { it.id in shopState.wishlist }, shopState,
                            onBack = { nav.popBackStack() }, onAdd = { shop.changeQuantity(it, 1) }, onWishlist = shop::toggleWishlist,
                            onProduct = ::openProduct, onRetry = shop::loadCatalog, emptyTitle = "No saved ${shopState.selectedMode.label.lowercase()} items",
                            emptyMessage = "Tap a heart to save an item. Switch modes to see saved items from another mode.")
                    }
                    composable("category/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { backStackEntry ->
                        if (!needsStartupRoute) {
                            val id = backStackEntry.arguments?.getString("id").orEmpty()
                            val category = modeCategories.firstOrNull { it.id == id }
                            ProductListingScreen(category?.name ?: "Category", modeProducts.filter { it.categoryId == id }, shopState, onBack = { nav.popBackStack() },
                                onAdd = { shop.changeQuantity(it, 1) }, onWishlist = shop::toggleWishlist, onProduct = ::openProduct, onRetry = shop::loadCatalog,
                                category = category,
                                emptyMessage = if (category == null) "This category isn't part of ${shopState.selectedMode.label}. Pick a category from the current mode."
                                else "No products are configured in this category yet.")
                        }
                    }
                    composable("product/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { backStackEntry ->
                        if (!needsStartupRoute) ProductDetailsScreen(shopState, backStackEntry.arguments?.getString("id").orEmpty(),
                            onBack = { nav.popBackStack() }, onChangeQuantity = shop::changeQuantity, onWishlist = shop::toggleWishlist,
                            onCart = { nav.navigate("cart") { launchSingleTop = true } }, onRetry = shop::loadCatalog)
                    }
                    composable("cart") {
                        if (!needsStartupRoute) CartScreen(shopState, onBack = { nav.popBackStack() }, onChangeQuantity = shop::changeQuantity,
                            onCheckout = { nav.navigate("checkout") { launchSingleTop = true } }, onAddress = { nav.navigate("addresses") { launchSingleTop = true } }, onProduct = ::openProduct)
                    }
                    composable("addresses") {
                        if (!needsStartupRoute) AddressScreen(shopState, onBack = { nav.popBackStack() }, onSelect = shop::selectAddress,
                            onDelete = shop::removeAddress, onSave = shop::saveAddress)
                    }
                    composable("checkout") {
                        if (!needsStartupRoute) CheckoutScreen(shopState, onBack = { nav.popBackStack() }, onAddress = { nav.navigate("addresses") { launchSingleTop = true } })
                    }
                    composable("tracking") { if (!needsStartupRoute) TrackingScreen(onBack = { nav.popBackStack() }) }
                }
                AnimatedVisibility(
                    visible = needsStartupRoute,
                    modifier = Modifier.fillMaxSize(),
                    enter = EnterTransition.None,
                    exit = fadeOut(animationSpec = tween(220)),
                ) {
                    SplashScreen(authState) { authenticated ->
                        if (needsStartupRoute) {
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
    }
}
