package com.rushit.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

private val Coral = AppColors.primary
private val CoralSoft = AppColors.secondary
private val Amber = AppColors.accent
private val Cream = AppColors.background
private val Ink = AppColors.text
private val Muted = AppColors.muted
private val Green = AppColors.success
private val Line = AppColors.divider
private val Card = AppColors.surface

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(255, 253, 247)
        window.navigationBarColor = android.graphics.Color.rgb(255, 253, 247)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContent { RushItAdminTheme { AdminRoot() } }
    }
}

private val LocalAdminNavigation = staticCompositionLocalOf<NavHostController?> { null }

private data class Destination(val route: String, val label: String, val glyph: String)
private val tabs = listOf(Destination("home", "Home", "⌂"), Destination("catalog", "Catalog", "▦"), Destination("orders", "Orders", "▤"), Destination("themes", "Themes", "◈"), Destination("more", "More", "•••"))
private val featureRoutes = listOf(
    Destination("designer", "Home Designer", "⌘"), Destination("section/0", "Section Editor", "▧"), Destination("preview", "Home Preview", "◉"),
    Destination("festivals", "Festival Manager", "✦"), Destination("festival_edit/new", "Festival Editor", "✎"), Destination("schedule", "Campaign Scheduler", "◷"),
    Destination("product_edit", "Add / Edit Product", "+"), Destination("categories", "Category Management", "▥"), Destination("order_detail/0", "Order Details", "▣"),
    Destination("customers", "Customer Management", "♙"), Destination("offers", "Offers & Coupons", "%"), Destination("offer_edit/new", "Create Offer", "+"), Destination("notifications", "Notifications Manager", "♧"),
    Destination("settings", "App Settings", "⚙"), Destination("profile", "Admin Profile & Security", "♙")
)

private const val PRODUCT_META_MARKER = " ⟐ "
private data class ProductMetadata(val sku: String = "", val description: String = "", val salePrice: String = "", val stockQuantity: String = "")
private fun visibleProductRecord(value: String): String = value.substringBefore(PRODUCT_META_MARKER)
private fun cleanStoredField(value: String): String = value.replace('|', ' ').replace("##", " ").trim()
private fun formatRupees(value: Double): String = String.format(java.util.Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
private fun productMetadata(value: String): ProductMetadata = runCatching {
    val encoded = value.substringAfter(PRODUCT_META_MARKER)
    val decoded = String(android.util.Base64.decode(encoded, android.util.Base64.DEFAULT), Charsets.UTF_8).split('\u001f')
    ProductMetadata(decoded.getOrElse(0) { "" }, decoded.getOrElse(1) { "" }, decoded.getOrElse(2) { "" }, decoded.getOrElse(3) { "" })
}.getOrDefault(ProductMetadata())
private fun withProductMetadata(record: String, metadata: ProductMetadata): String {
    val packed = listOf(metadata.sku, metadata.description, metadata.salePrice, metadata.stockQuantity).joinToString("\u001f")
    return visibleProductRecord(record) + PRODUCT_META_MARKER + android.util.Base64.encodeToString(packed.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
}

private data class HomeSectionConfig(
    val title: String,
    val type: String,
    val subtitle: String,
    val layout: String,
    val surface: String,
    val motion: String,
    val visibility: String,
) {
    fun encode(): String = listOf(title, type, subtitle, layout, surface, motion, visibility).map(::cleanStoredField).joinToString("|")
}

private fun categoryGlyph(name: String): String = when {
    name.contains("fruit", true) -> "🍎"
    name.contains("veget", true) -> "🥬"
    name.contains("dairy", true) -> "🥛"
    name.contains("snack", true) -> "🍿"
    name.contains("drink", true) || name.contains("beverage", true) -> "🧃"
    else -> "▦"
}
private val homeSectionTypes = listOf("Hero / banner", "Promotion", "Featured products", "Product grid", "Categories", "Festival campaigns", "Offers", "Custom promotion")
private fun parseHomeSection(value: String): HomeSectionConfig {
    val fields = value.split('|')
    if (fields.size >= 7) return HomeSectionConfig(fields[0], fields[1], fields[2], fields[3], fields[4], fields[5], fields[6])
    val title = fields.getOrNull(0).orEmpty()
    val legacyType = when {
        title.contains("banner", true) -> "Hero / banner"
        title.contains("categor", true) -> "Categories"
        title.contains("festival", true) -> "Festival campaigns"
        title.contains("offer", true) || title.contains("deal", true) -> "Offers"
        title.contains("promo", true) -> "Promotion"
        title.contains("grid", true) -> "Product grid"
        else -> "Featured products"
    }
    return HomeSectionConfig(title, legacyType, fields.getOrNull(1).orEmpty(), "Carousel", "Soft", "Fade", fields.lastOrNull()?.takeIf { it in setOf("Visible", "Hidden") } ?: "Visible")
}

private val initialProducts = listOf("Tomato · ₹24/kg · In stock · Vegetables · Visible", "Banana · ₹32/kg · In stock · Fruits · Visible", "Milk · ₹56/L · In stock · Dairy · Visible", "Bread · ₹32/piece · Low stock · Bakery · Visible", "Apple · ₹120/kg · In stock · Fruits · Visible")
private val initialCategories = listOf("Fruits|Visible", "Vegetables|Visible", "Dairy|Visible", "Snacks|Visible", "Beverages|Visible", "Bakery|Visible", "Personal Care|Visible")
private val initialCampaigns = listOf("Diwali|2026-11-10|2026-11-20|1|Enabled", "Holi|2027-03-01|2027-03-10|2|Enabled", "Christmas|2026-12-20|2026-12-26|3|Disabled", "Eid|2027-04-10|2027-04-11|2|Disabled", "Valentine's Day|2027-02-14|2027-02-14|3|Disabled")
private val initialOffers = listOf("Welcome offer|WELCOME10|Percentage|10|499|500|2026-10-01|2026-12-31|Enabled", "First order|FIRSTORDER|Percentage|20|399|1000|2026-10-01|2027-03-31|Enabled", "Flat discount|SOMAI50|Fixed|50|299|500|2026-11-01|2026-12-15|Disabled")
private val initialOrders = listOf("#RI172805456  ·  Ankit Kumar  ·  ₹542  ·  Preparing", "#RI172805455  ·  Priya Singh  ·  ₹386  ·  On the way", "#RI172805454  ·  Rohit Sharma  ·  ₹724  ·  Delivered", "#RI172805453  ·  Sneha Verma  ·  ₹218  ·  Cancelled")
private val defaultHomeSections = listOf("Top Banner|Festival Deals|Visible", "Category Icons|8 categories|Visible", "Flash Deals|Horizontal products|Visible", "Best Selling|Product grid|Visible", "Recommended|Personalized|Visible")

@Composable private fun AdminRoot() {
    val nav = rememberNavController()
    val back by nav.currentBackStackEntryAsState()
    val route = back?.destination?.route ?: "login"
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("rushit_admin_demo", android.content.Context.MODE_PRIVATE) }
    val products = remember(preferences) { mutableStateListOf(*(preferences.getString("products", null)?.split("##")?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() } ?: initialProducts).toTypedArray()) }
    val orders = remember(preferences) { mutableStateListOf(*(preferences.getString("orders", null)?.split("##")?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() } ?: initialOrders).toTypedArray()) }
    val sections = remember(preferences) { mutableStateListOf(*(preferences.getString("sections", null)?.split("##")?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() } ?: defaultHomeSections).toTypedArray()) }
    val publishedSections = remember(preferences) { mutableStateListOf(*(preferences.getString("published_sections", null)?.split("##")?.filter { it.isNotBlank() }?.toTypedArray() ?: emptyArray())) }
    var publishedTheme by remember(preferences) { mutableStateOf(preferences.getString("published_theme", "") ?: "") }
    var publishedBackground by remember(preferences) { mutableStateOf(preferences.getString("published_background", "") ?: "") }
    var publishedRadius by remember(preferences) { mutableFloatStateOf(preferences.getFloat("published_radius", -1f)) }
    var publishedButtonStyle by remember(preferences) { mutableStateOf(preferences.getString("published_button_style", "") ?: "") }
    var publishedTextTone by remember(preferences) { mutableStateOf(preferences.getString("published_text_tone", "") ?: "") }
    val categories = remember(preferences) { mutableStateListOf(*(preferences.getString("categories", null)?.split("##")?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() } ?: initialCategories).toTypedArray()) }
    val campaigns = remember(preferences) { mutableStateListOf(*(preferences.getString("campaigns", null)?.split("##")?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() } ?: initialCampaigns).toTypedArray()) }
    val offers = remember(preferences) { mutableStateListOf(*(preferences.getString("offers", null)?.split("##")?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() } ?: initialOffers).toTypedArray()) }
    var themeName by remember(preferences) { mutableStateOf(preferences.getString("theme_name", "RushIt Coral") ?: "RushIt Coral") }
    var backgroundStyle by remember(preferences) { mutableStateOf(preferences.getString("background_style", "Gradient") ?: "Gradient") }
    var sectionRadius by remember(preferences) { mutableFloatStateOf(preferences.getFloat("section_radius", 16f)) }
    var buttonStyle by remember(preferences) { mutableStateOf(preferences.getString("button_style", "Filled") ?: "Filled") }
    var previewTextTone by remember(preferences) { mutableStateOf(preferences.getString("preview_text_tone", "Ink") ?: "Ink") }
    var settingsVersion by remember { mutableIntStateOf(0) }
    var homeDraftDirty by remember { mutableStateOf(false) }
    var homeDraftRevision by remember { mutableIntStateOf(0) }
    var pendingDesignerDestination by remember { mutableStateOf<String?>(null) }
    var snackbar by remember { mutableStateOf("") }
    val snackbarState = remember { SnackbarHostState() }
    LaunchedEffect(snackbar) { if (snackbar.isNotBlank()) { snackbarState.showSnackbar(snackbar); snackbar = "" } }
    val showTabs = route !in setOf("login", "splash", "product_edit", "product_edit/{index}", "section/{index}", "festival_edit/{index}", "offer_edit/{index}", "order_detail/{index}", "customer_detail/{index}")
    fun navigateToRootTab(destination: String) {
        nav.navigate(destination) {
            popUpTo("home") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val selectedTab = when (route) {
        "home" -> "home"
        "catalog", "product_edit", "product_edit/{index}", "categories" -> "catalog"
        "orders", "order_detail/{index}" -> "orders"
        "themes", "designer", "preview", "festivals", "schedule", "section/{index}", "festival_edit/{index}" -> "themes"
        else -> "more"
    }
    Scaffold(containerColor = Cream, snackbarHost = { SnackbarHost(snackbarState) }, bottomBar = {
        if (showTabs) NavigationBar(containerColor = Color.White, tonalElevation = 6.dp) {
            tabs.forEach { dest ->
                val selected = selectedTab == dest.route
                NavigationBarItem(selected = selected, onClick = {
                    if (route != dest.route) {
                        if (route == "designer" && homeDraftDirty) pendingDesignerDestination = dest.route
                        else navigateToRootTab(dest.route)
                    }
                }, icon = { Text(dest.glyph, fontSize = 20.sp, color = if (selected) Coral else Muted) }, label = { Text(dest.label, fontSize = 10.sp) })
            }
        }
    }) { padding ->
        CompositionLocalProvider(LocalAdminNavigation provides nav) {
        NavHost(navController = nav, startDestination = "splash", modifier = Modifier.padding(padding)) {
            composable("splash") { SplashScreen(onFinish = {
                val destination = if (preferences.getBoolean("demo_session", false)) "home" else "login"
                nav.navigate(destination) { popUpTo("splash") { inclusive = true }; launchSingleTop = true }
            }) }
            composable("login") { LoginScreen(onLogin = { rememberSession ->
                if (rememberSession) preferences.edit().putBoolean("demo_session", true).apply() else preferences.edit().remove("demo_session").apply()
                nav.navigate("home") { popUpTo("login") { inclusive = true }; launchSingleTop = true }
            }) }
            composable("home") { Dashboard(products, orders, demoCustomers.size, preferences.getString("admin_name", "Dharmjeet Singh") ?: "Dharmjeet Singh", onOpen = { nav.navigate(it) }) }
            composable("catalog") { CatalogScreen(products, categories, onAdd = { nav.navigate("product_edit") }, onEdit = { index -> nav.navigate("product_edit/$index") }, onDelete = { index ->
                if (index in products.indices) { products.removeAt(index); preferences.edit().putString("products", products.joinToString("##")).apply(); snackbar = "Product removed from this device's demo catalog" }
            }) }
            composable("orders") { OrdersScreen(orders, onOpen = { index -> nav.navigate("order_detail/$index") }) }
            composable("themes") { ThemeScreen(themeName, backgroundStyle, sectionRadius, buttonStyle, previewTextTone, onSave = { nextTheme, nextBackground, nextRadius, nextButtonStyle, nextTextTone ->
                themeName = nextTheme; backgroundStyle = nextBackground; sectionRadius = nextRadius; buttonStyle = nextButtonStyle; previewTextTone = nextTextTone
                preferences.edit().putString("theme_name", nextTheme).putString("background_style", nextBackground).putFloat("section_radius", nextRadius).putString("button_style", nextButtonStyle).putString("preview_text_tone", nextTextTone).apply()
                snackbar = "Theme preferences saved on this device"
            }, onPreview = { nav.navigate("preview") }) }
            composable("more") { MoreScreen(onOpen = { nav.navigate(it) }) }
            composable("designer") { key(homeDraftRevision) { DesignerScreen(sections, onEdit = { index -> nav.navigate("section/$index") }, onSave = { preferences.edit().putString("sections", sections.joinToString("##")).apply(); homeDraftDirty = false; snackbar = it }, onDirtyChanged = { homeDraftDirty = it }, onLeave = {
                val previous = nav.previousBackStackEntry?.destination?.route ?: "more"
                if (homeDraftDirty) pendingDesignerDestination = previous else nav.popBackStack()
            }, onPreview = { nav.navigate("preview") }) } }
            composable("section/{index}") { entry ->
                val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
                SectionEditorScreen(sections.getOrNull(index).orEmpty(), onSave = { updated ->
                    if (index in sections.indices) sections[index] = updated
                    preferences.edit().putString("sections", sections.joinToString("##")).apply()
                    homeDraftDirty = false
                    homeDraftRevision++
                    snackbar = "Section saved to local draft"
                    nav.popBackStack()
                }, onCancel = { nav.popBackStack() })
            }
            composable("preview") { PreviewScreen(sections = sections, products = products, categories = categories, campaigns = campaigns, offers = offers, publishedSections = publishedSections, themeName = themeName, backgroundStyle = backgroundStyle, sectionRadius = sectionRadius, publishedTheme = publishedTheme, publishedBackground = publishedBackground, publishedRadius = publishedRadius, buttonStyle = buttonStyle, publishedButtonStyle = publishedButtonStyle, textTone = previewTextTone, publishedTextTone = publishedTextTone, animationsEnabled = preferences.getBoolean("animations", true), maintenancePreview = preferences.getBoolean("maintenance", false), storeName = preferences.getString("store_name", "RushIt") ?: "RushIt", onBack = { nav.popBackStack() }, onPublish = {
                publishedSections.clear(); publishedSections.addAll(sections)
                publishedTheme = themeName; publishedBackground = backgroundStyle; publishedRadius = sectionRadius; publishedButtonStyle = buttonStyle; publishedTextTone = previewTextTone
                preferences.edit().putString("sections", sections.joinToString("##")).putString("published_sections", publishedSections.joinToString("##")).putString("published_theme", publishedTheme).putString("published_background", publishedBackground).putFloat("published_radius", publishedRadius).putString("published_button_style", publishedButtonStyle).putString("published_text_tone", publishedTextTone).apply()
                homeDraftDirty = false; homeDraftRevision++
                snackbar = "Published snapshot saved on this device only · User app unchanged"
            }) }
            composable("festivals") { CampaignManagerScreen(campaigns, onAdd = { nav.navigate("festival_edit/new") }, onEdit = { index -> nav.navigate("festival_edit/$index") }, onDelete = { index ->
                if (index in campaigns.indices) { campaigns.removeAt(index); preferences.edit().putString("campaigns", campaigns.joinToString("##")).apply(); snackbar = "Local festival removed" }
            }) }
            composable("festival_edit/{index}") { entry ->
                val key = entry.arguments?.getString("index") ?: "new"
                val index = key.toIntOrNull()
                val fromScheduler = nav.previousBackStackEntry?.destination?.route == "schedule"
                CampaignEditorScreen(campaign = index?.let { campaigns.getOrNull(it) }, scheduler = fromScheduler, onCancel = { nav.popBackStack() }, onSave = { updated ->
                    if (index == null) campaigns.add(updated) else if (index in campaigns.indices) campaigns[index] = updated
                    preferences.edit().putString("campaigns", campaigns.joinToString("##")).apply()
                    snackbar = "Campaign saved locally · not published"
                    nav.popBackStack()
                })
            }
            composable("schedule") { CampaignManagerScreen(campaigns, scheduler = true, onAdd = { nav.navigate("festival_edit/new") }, onEdit = { index -> nav.navigate("festival_edit/$index") }, onDelete = { index ->
                if (index in campaigns.indices) { campaigns.removeAt(index); preferences.edit().putString("campaigns", campaigns.joinToString("##")).apply(); snackbar = "Local campaign removed" }
            }) }
            composable("product_edit") { ProductForm(categories = categories, onCancel = { nav.popBackStack() }, onSave = { products.add(it); preferences.edit().putString("products", products.joinToString("##")).apply(); snackbar = "Product saved locally"; nav.popBackStack() }) }
            composable("product_edit/{index}") { entry ->
                val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
                ProductForm(product = products.getOrNull(index), categories = categories, onCancel = { nav.popBackStack() }, onSave = { updated ->
                    if (index in products.indices) products[index] = updated
                    preferences.edit().putString("products", products.joinToString("##")).apply()
                    snackbar = "Product updated locally"
                    nav.popBackStack()
                })
            }
            composable("categories") { CategoryManagementScreen(categories, products, onSave = { preferences.edit().putString("categories", categories.joinToString("##")).putString("products", products.joinToString("##")).apply(); snackbar = it }) }
            composable("order_detail/{index}") { entry ->
                val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
                val order = orders.getOrNull(index) ?: orders.firstOrNull().orEmpty()
                OrderDetailScreen(order = order, onUpdate = { newStatus ->
                    if (index in orders.indices) {
                        orders[index] = order.substringBeforeLast(" · ") + " · $newStatus"
                        preferences.edit().putString("orders", orders.joinToString("##")).apply()
                    }
                    snackbar = "Order status updated in local demo"
                })
            }
            composable("customers") { CustomersScreen(onOpen = { index -> nav.navigate("customer_detail/$index") }) }
            composable("customer_detail/{index}") { entry ->
                val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
                CustomerDetailScreen(index)
            }
            composable("offers") { OfferManagerScreen(offers, onAdd = { nav.navigate("offer_edit/new") }, onEdit = { index -> nav.navigate("offer_edit/$index") }, onDelete = { index ->
                if (index in offers.indices) { offers.removeAt(index); preferences.edit().putString("offers", offers.joinToString("##")).apply(); snackbar = "Local coupon removed" }
            }) }
            composable("offer_edit/{index}") { entry ->
                val key = entry.arguments?.getString("index") ?: "new"
                val index = key.toIntOrNull()
                OfferEditorScreen(offers.getOrNull(index ?: -1), existingOffers = offers, onCancel = { nav.popBackStack() }, onSave = { offer ->
                    if (index == null) offers.add(offer) else if (index in offers.indices) offers[index] = offer
                    preferences.edit().putString("offers", offers.joinToString("##")).apply()
                    snackbar = "Offer saved locally · not published to customers"
                    nav.popBackStack()
                })
            }
            composable("notifications") { NotificationScreen(preferences, onSend = { snackbar = "Local notification preview saved · not delivered" }) }
            composable("settings") { key(settingsVersion) { SettingsScreen(preferences, themeName, onReset = {
                preferences.edit().clear().apply()
                products.clear(); products.addAll(initialProducts)
                orders.clear(); orders.addAll(initialOrders)
                sections.clear(); sections.addAll(defaultHomeSections)
                publishedSections.clear()
                publishedTheme = ""; publishedBackground = ""; publishedRadius = -1f; publishedButtonStyle = ""; publishedTextTone = ""
                buttonStyle = "Filled"; previewTextTone = "Ink"
                homeDraftDirty = false; homeDraftRevision++
                categories.clear(); categories.addAll(initialCategories)
                campaigns.clear(); campaigns.addAll(initialCampaigns)
                offers.clear(); offers.addAll(initialOffers)
                themeName = "RushIt Coral"
                backgroundStyle = "Gradient"
                sectionRadius = 16f
                settingsVersion++
                snackbar = "Local demo data reset"
            }) } }
            composable("profile") { ProfileScreen(preferences, onLogout = { preferences.edit().remove("demo_session").apply(); nav.navigate("login") { popUpTo("home") { inclusive = true }; launchSingleTop = true } }) }
        }
        pendingDesignerDestination?.let { destination -> AlertDialog(
            onDismissRequest = { pendingDesignerDestination = null },
            title = { Text("Discard Home draft changes?") },
            text = { Text("Your unsaved changes to sections will be discarded before leaving Home Designer.") },
            confirmButton = { TextButton(onClick = {
                val savedSections = preferences.getString("sections", null)?.split("##")?.filter { it.isNotBlank() } ?: defaultHomeSections
                sections.clear(); sections.addAll(savedSections)
                homeDraftDirty = false
                pendingDesignerDestination = null
                navigateToRootTab(destination)
            }) { Text("Discard and leave") } },
            dismissButton = { TextButton(onClick = { pendingDesignerDestination = null }) { Text("Keep editing") } },
        ) }
        }
    }
}

@Composable private fun SplashScreen(onFinish: () -> Unit) {
    var entered by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (entered) 1f else 0.82f, animationSpec = spring(dampingRatio = 0.62f), label = "admin logo entrance")
    LaunchedEffect(Unit) {
        entered = true
        onFinish()
    }
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFFE7E0), Coral, CoralSoft))).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🛒", Modifier.graphicsLayer { scaleX = scale; scaleY = scale }, fontSize = 72.sp)
        Spacer(Modifier.height(14.dp))
        Text("RushIt", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
        Text("ADMIN", color = Color.White.copy(alpha = .9f), fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
        Spacer(Modifier.height(12.dp))
        Text("Manage your store with clarity.", color = Color.White.copy(alpha = .94f), fontSize = 14.sp)
    }
}

@Composable private fun LoginScreen(onLogin: (Boolean) -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var reveal by rememberSaveable { mutableStateOf(false) }
    var rememberAdmin by rememberSaveable { mutableStateOf(true) }
    var error by rememberSaveable { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFFEEE9), Cream, Cream)))
            .imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 26.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(68.dp).clip(RoundedCornerShape(22.dp)).background(Coral), contentAlignment = Alignment.Center) { Text("🛒", fontSize = 31.sp) }
        Spacer(Modifier.height(13.dp))
        Text("RushIt Admin", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Coral)
        Text("Welcome back", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(top = 25.dp))
        Text("Sign in to manage your store", color = Muted, modifier = Modifier.padding(top = 5.dp, bottom = 20.dp))
        AdminField("Email or phone", email, { email = it }, keyboard = KeyboardType.Email)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") }, singleLine = true, shape = AppShapes.input,
            visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { TextButton(onClick = { reveal = !reveal }) { Text(if (reveal) "Hide" else "Show", color = Coral) } },
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).clickable { rememberAdmin = !rememberAdmin }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(rememberAdmin, onCheckedChange = { rememberAdmin = it }, colors = CheckboxDefaults.colors(checkedColor = Coral))
                Text("Keep demo session on this device", fontSize = 12.sp, color = Muted)
            }
            TextButton(onClick = { error = "Password recovery is not connected in this local demo." }) { Text("Forgot?", color = Coral, fontSize = 12.sp) }
        }
        if (error.isNotBlank()) Text(error, Modifier.fillMaxWidth().padding(bottom = 4.dp), color = Coral, fontSize = 12.sp)
        PrimaryButton("Sign in", Modifier.fillMaxWidth()) {
            error = when {
                email.isBlank() -> "Enter an email or phone number."
                password.isBlank() -> "Enter a password to continue."
                else -> ""
            }
            if (error.isBlank()) error = "Authentication is not connected. Use demo access below; entered credentials are not checked."
        }
        TextButton(onClick = { onLogin(rememberAdmin) }, modifier = Modifier.fillMaxWidth().padding(top = 5.dp)) { Text("Continue with demo access", color = Coral) }
        Text("Local demo session only · credentials are not sent or verified.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        Text("Restricted area · Administrators only", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 10.dp))
    }
}

@Composable private fun Dashboard(products: List<String>, orders: List<String>, customerCount: Int, adminName: String, onOpen: (String) -> Unit) {
    val productCount = products.size
    val lowStockProducts = products.filter { visibleProductRecord(it).contains("Low stock", true) || visibleProductRecord(it).contains("Out of stock", true) }
    val lowStockCount = lowStockProducts.size
    val salesValues = orders.mapNotNull { Regex("₹(\\d+)").find(visibleProductRecord(it))?.groupValues?.getOrNull(1)?.toIntOrNull() }
    val salesTotal = salesValues.sum()
    val orderStatuses = orders.map { it.substringAfterLast(" · ").trim() }
    val inProgressCount = orderStatuses.count { it !in setOf("Delivered", "Cancelled") }
    val deliveredCount = orderStatuses.count { it == "Delivered" }
    val chartMaximum = (salesValues.maxOrNull() ?: 1).coerceAtLeast(1)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("RushIt Admin", color = Coral, fontSize = 13.sp, fontWeight = FontWeight.Bold); Text("Good morning, ${adminName.substringBefore(' ')}", color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold); Text("Store overview · Local demo data", color = Muted, fontSize = 13.sp) }; Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFFFF1DF)).clickable { onOpen("notifications") }, contentAlignment = Alignment.Center) { Text("♧", color = Coral, fontSize = 19.sp) }; Spacer(Modifier.width(8.dp)); Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFFFE8E2)).clickable { onOpen("profile") }, contentAlignment = Alignment.Center) { Text(adminName.split(' ').mapNotNull { it.firstOrNull() }.take(2).joinToString(""), color = Coral, fontWeight = FontWeight.Bold, fontSize = 13.sp) } } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCard("Orders", "${orders.size}", "Local demo records", Modifier.weight(1f)) { onOpen("orders") }; StatCard("In progress", "$inProgressCount", "Status in demo records", Modifier.weight(1f)) { onOpen("orders") } } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCard("Products", productCount.toString(), "Local catalog", Modifier.weight(1f)) { onOpen("catalog") }; StatCard("Delivered", "$deliveredCount", "Demo order status", Modifier.weight(1f)) { onOpen("orders") } } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCard("Customers", customerCount.toString(), "Illustrative demo list", Modifier.weight(1f)) { onOpen("customers") }; StatCard("Attention", lowStockCount.toString(), "Low/out of stock", Modifier.weight(1f)) { onOpen("catalog") } } }
        item { Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = Coral) { Column(Modifier.padding(18.dp)) {
            Text("RECORDED ORDER VALUE · DEMO ONLY", color = Color.White.copy(alpha = .9f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("₹$salesTotal", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text("Sum of listed order totals, including any cancelled demo order · not recognized revenue", color = Color.White.copy(alpha = .9f), fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            if (salesValues.isEmpty()) EmptyState("No order totals", "Add demo order records to see a comparison.")
            else Row(Modifier.fillMaxWidth().height(80.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                salesValues.takeLast(7).forEachIndexed { index, amount ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Text("₹$amount", color = Color.White, fontSize = 9.sp, maxLines = 1)
                        Box(Modifier.width(22.dp).height((amount.toFloat() / chartMaximum * 42f).coerceAtLeast(5f).dp).clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)).background(Color.White.copy(alpha = .72f)))
                        Text("#${index + 1}", color = Color.White.copy(alpha = .9f), fontSize = 9.sp)
                    }
                }
            }
            Text("Each bar is one demo order total (left to right), not a date-based sales trend.", color = Color.White.copy(alpha = .85f), fontSize = 10.sp)
        } } }
        item { AdminCard(Modifier.clickable { onOpen("catalog") }) { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Inventory health", fontWeight = FontWeight.Bold); Text("$lowStockCount products need attention", color = Muted, fontSize = 12.sp) }; StatusChip(if (lowStockCount > 0) "Low stock" else "Healthy") } } }
        item { SectionTitle("Low stock alerts", "Manage", onAction = { onOpen("catalog") }) }
        if (lowStockProducts.isEmpty()) item { EmptyState("Stock looks healthy", "No demo product is marked low or out of stock.") }
        items(lowStockProducts.take(3), key = { it }) { product ->
            val record = visibleProductRecord(product)
            DataRow("!", record.substringBefore(" · "), record.split(" · ").getOrNull(2).orEmpty(), { onOpen("catalog") })
        }
        item { SectionTitle("Best-selling products", "Catalog", onAction = { onOpen("catalog") }) }
        item { EmptyState("Sales ranking unavailable", "Demo orders have no line-item quantities, so no best seller can be calculated honestly.") }
        item { SectionTitle("Quick actions", "Shortcuts"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { QuickAction("＋", "Add Product") { onOpen("product_edit") }; QuickAction("⌘", "Home Designer") { onOpen("designer") } }; Spacer(Modifier.height(8.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { QuickAction("▤", "View Orders") { onOpen("orders") }; QuickAction("♧", "Send Notification") { onOpen("notifications") } } }
        item { SectionTitle("Recent orders", "View all", onAction = { onOpen("orders") }) }
        itemsIndexed(orders.take(4)) { index, order -> Surface(Modifier.fillMaxWidth().clickable { onOpen("order_detail/$index") }, shape = RoundedCornerShape(17.dp), color = Card, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Text("🛍️", fontSize = 22.sp); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(order.substringBefore(" · "), fontWeight = FontWeight.Bold, color = Ink); Text(order.substringAfter(" · "), fontSize = 12.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis) }; StatusChip(order.substringAfterLast(" · ")) } } }
    }
}

@Composable private fun StatCard(title: String, value: String, change: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) { Surface(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(19.dp), color = Card, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Column(Modifier.padding(14.dp)) { Text(title, color = Muted, fontSize = 12.sp); Text(value, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp)); Text(change, color = Green, fontSize = 11.sp) } } }
@Composable private fun RowScope.QuickAction(symbol: String, label: String, onClick: () -> Unit) { Surface(Modifier.weight(1f).height(66.dp).clickable(onClick = onClick), shape = RoundedCornerShape(15.dp), color = Card, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) { Text(symbol, color = Coral, fontSize = 21.sp); Spacer(Modifier.width(8.dp)); Text(label, fontSize = 12.sp, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 2) } } }
@Composable private fun SectionTitle(title: String, action: String, onAction: (() -> Unit)? = null) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(title, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink); Text(action, Modifier.clickable(enabled = onAction != null) { onAction?.invoke() }, color = Coral, fontSize = 12.sp) } }

@Composable private fun CatalogScreen(
    products: List<String>,
    categories: List<String>,
    onAdd: () -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
) {
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    var sort by rememberSaveable { mutableStateOf("Name") }
    var deleteTarget by remember { mutableStateOf<Pair<Int, String>?>(null) }
    val filtered = products.withIndex().filter { indexed ->
        val product = visibleProductRecord(indexed.value)
        val metadata = productMetadata(indexed.value)
        (product.contains(search, true) || metadata.sku.contains(search, true) || metadata.description.contains(search, true)) && when (filter) {
            "Low stock" -> product.contains("Low", true)
            "Out of stock" -> product.contains("Out of stock", true)
            "In stock" -> product.contains("In stock", true)
            "All" -> true
            else -> product.split(" · ").getOrNull(3) == filter
        }
    }.let { list ->
        when (sort) {
            "Price ↑" -> list.sortedBy { Regex("₹([0-9]+(?:\\.[0-9]+)?)").find(visibleProductRecord(it.value))?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: Double.MAX_VALUE }
            "Stock ↑" -> list.sortedBy { visibleProductRecord(it.value).split(" · ").getOrNull(2)?.let { value -> Regex("[0-9]+").find(value)?.value?.toIntOrNull() } ?: Int.MAX_VALUE }
            else -> list.sortedBy { visibleProductRecord(it.value).substringBefore(" · ").lowercase() }
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        ScreenHeader("Products", "${products.size} items · local demo catalog", onAdd, "＋ Add")
        OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Search products") }, singleLine = true, shape = AppShapes.input)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(top = 6.dp)) {
            items((listOf("All", "In stock", "Low stock", "Out of stock") + categories.map { it.substringBefore('|') }).distinct(), key = { it }) { chip -> FilterChip(filter == chip, { filter = chip }, { Text(chip) }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
            listOf("Name", "Price ↑", "Stock ↑").forEach { option -> FilterChip(sort == option, { sort = option }, { Text("Sort: $option", fontSize = 11.sp) }) }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (filtered.isEmpty()) item { EmptyState("No matching products", "Try changing your search or filters.", "Clear filters") { search = ""; filter = "All" } }
            items(filtered, key = { it.index }) { indexed ->
                val record = visibleProductRecord(indexed.value)
                val metadata = productMetadata(indexed.value)
                val supplemental = listOfNotNull(metadata.salePrice.takeIf { it.isNotBlank() }?.let { "Sale ₹$it" }, metadata.sku.takeIf { it.isNotBlank() }?.let { "SKU $it" }).joinToString(" · ")
                AdminSurfaceCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(record.substringBefore(" · "), fontWeight = FontWeight.Bold)
                            Text(listOf(record.substringAfter(" · "), supplemental).filter { it.isNotBlank() }.joinToString(" · "), color = AppColors.muted, fontSize = 12.sp)
                            if (metadata.description.isNotBlank()) Text(metadata.description, color = AppColors.muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        StatusChip(record.split(" · ").getOrNull(2).orEmpty())
                    }
                    Row {
                        TextButton(onClick = { onEdit(indexed.index) }) { Text("Edit") }
                        TextButton(onClick = { deleteTarget = indexed.index to record.substringBefore(" · ") }) { Text("Delete", color = AppColors.error) }
                    }
                }
            }
        }
    }
    deleteTarget?.let { (index, name) -> AlertDialog(
        onDismissRequest = { deleteTarget = null },
        title = { Text("Remove product?") },
        text = { Text("Remove $name from this device's local demo catalog? This does not change the RushIt customer app.") },
        confirmButton = { TextButton(onClick = { onDelete(index); deleteTarget = null }) { Text("Remove") } },
        dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Keep") } },
    ) }
}

@Composable private fun OrdersScreen(orders: List<String>, onOpen: (Int) -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    val filtered = orders.withIndex().filter { indexed -> indexed.value.contains(search, true) && (filter == "All" || indexed.value.contains(filter, true)) }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        ScreenHeader("Orders", "${orders.size} local demo orders")
        OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Search order or customer") }, singleLine = true, shape = AppShapes.input)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 6.dp)) {
            items(listOf("All", "Confirmed", "Preparing", "Rider Ready", "On the way", "Almost there", "Delivered", "Cancelled"), key = { it }) { status -> FilterChip(filter == status, { filter = status }, { Text(status, fontSize = 11.sp) }) }
        }
        Text("Date and payment filters are unavailable because these local sample orders contain no timestamps or payment status.", color = Muted, fontSize = 11.sp)
        LazyColumn(contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (filtered.isEmpty()) item { EmptyState("No matching orders", "Try another search or order status.", "Clear filters") { search = ""; filter = "All" } }
            items(filtered) { indexed -> DataRow("🛍️", indexed.value.substringBefore(" · "), indexed.value.substringAfter(" · "), { onOpen(indexed.index) }) }
        }
    }
}

@Composable private fun ThemeScreen(
    themeName: String,
    backgroundStyle: String,
    radius: Float,
    buttonStyle: String,
    textTone: String,
    onSave: (String, String, Float, String, String) -> Unit,
    onPreview: () -> Unit,
) {
    val paletteNames = listOf("RushIt Coral", "Sunrise", "Forest", "Ocean")
    val paletteColors = listOf(Coral, Amber, Green, Color(0xFF4975C6))
    var draftTheme by rememberSaveable(themeName, backgroundStyle, radius, buttonStyle, textTone) { mutableStateOf(themeName) }
    var draftBackground by rememberSaveable(themeName, backgroundStyle, radius, buttonStyle, textTone) { mutableStateOf(backgroundStyle) }
    var draftRadius by rememberSaveable(themeName, backgroundStyle, radius, buttonStyle, textTone) { mutableFloatStateOf(radius) }
    var draftButtonStyle by rememberSaveable(themeName, backgroundStyle, radius, buttonStyle, textTone) { mutableStateOf(buttonStyle) }
    var draftTextTone by rememberSaveable(themeName, backgroundStyle, radius, buttonStyle, textTone) { mutableStateOf(textTone) }
    val hasChanges = draftTheme != themeName || draftBackground != backgroundStyle || draftRadius != radius || draftButtonStyle != buttonStyle || draftTextTone != textTone
    fun resetDraft() { draftTheme = themeName; draftBackground = backgroundStyle; draftRadius = radius; draftButtonStyle = buttonStyle; draftTextTone = textTone }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 22.dp)) {
        item { ScreenHeader("Theme & Colors", "Changes stay local until saved") }
        item { DemoNotice() }
        item { AdminCard {
            Text("Brand palette", fontWeight = FontWeight.Bold)
            Text("Choose a coordinated preview palette", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                paletteNames.forEachIndexed { index, name ->
                    Column(Modifier.clickable { draftTheme = name }.padding(3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(paletteColors[index]), contentAlignment = Alignment.Center) { if (draftTheme == name) Text("✓", color = Color.White, fontWeight = FontWeight.Bold) }
                        Text(name, fontSize = 10.sp, color = if (draftTheme == name) Coral else Muted)
                    }
                }
            }
            Text("Accent and primary actions use the selected palette; Admin shell stays on RushIt brand colors.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        } }
        item { AdminCard {
            Text("Background style", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 8.dp)) { listOf("Default", "Gradient").forEach { FilterChip(draftBackground == it, { draftBackground = it }, { Text(it) }) } }
            Text("Affects the local Home preview only.", color = Muted, fontSize = 12.sp)
        } }
        item { AdminCard {
            Text("Section corner radius · ${draftRadius.toInt()}dp", fontWeight = FontWeight.Bold)
            Slider(value = draftRadius, onValueChange = { draftRadius = it }, valueRange = 8f..28f)
            Text("Affects preview section cards and banners.", color = Muted, fontSize = 12.sp)
        } }
        item { AdminCard {
            Text("Preview text", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Ink", "Soft").forEach { option -> FilterChip(draftTextTone == option, { draftTextTone = option }, { Text(if (option == "Ink") "Charcoal" else "Warm muted") }) }
            }
            Text("Readable text remains fixed in the Admin controls; this setting affects the Home preview sample.", color = Muted, fontSize = 11.sp)
        } }
        item { AdminCard {
            Text("Button style", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Filled", "Soft").forEach { option -> FilterChip(draftButtonStyle == option, { draftButtonStyle = option }, { Text(option) }) }
            }
            Text("Preview CTA only · this does not restyle the separate Admin controls.", color = Muted, fontSize = 11.sp)
        } }
        item { AdminCard {
            Text("Preview", fontWeight = FontWeight.Bold)
            val selectedColor = paletteColors.getOrElse(paletteNames.indexOf(draftTheme)) { Coral }
            Surface(shape = RoundedCornerShape(draftRadius.dp), color = if (draftBackground == "Gradient") Color(0xFFFFEEE9) else Cream) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("RushIt Home", color = if (draftTextTone == "Soft") Muted else Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Fresh picks · local preview", color = if (draftTextTone == "Soft") Muted else Ink, fontSize = 12.sp)
                    Surface(shape = RoundedCornerShape(12.dp), color = if (draftButtonStyle == "Soft") selectedColor.copy(alpha = .13f) else selectedColor, modifier = Modifier.padding(top = 10.dp)) {
                        Text("Browse products", Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = if (draftButtonStyle == "Soft") selectedColor else Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = ::resetDraft, enabled = hasChanges, modifier = Modifier.weight(1f)) { Text("Cancel") }
                TextButton(onClick = { draftTheme = "RushIt Coral"; draftBackground = "Default"; draftRadius = 16f; draftButtonStyle = "Filled"; draftTextTone = "Ink" }, modifier = Modifier.weight(1f)) { Text("Reset defaults") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedButton(onClick = { if (hasChanges) onSave(draftTheme, draftBackground, draftRadius, draftButtonStyle, draftTextTone); onPreview() }, modifier = Modifier.weight(1f)) { Text("Save & preview") }
                Button(onClick = { onSave(draftTheme, draftBackground, draftRadius, draftButtonStyle, draftTextTone) }, enabled = hasChanges, modifier = Modifier.weight(1f)) { Text("Save theme") }
            }
        }
    }
}

@Composable private fun MoreScreen(onOpen: (String) -> Unit) { LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentPadding = PaddingValues(bottom = 20.dp)) { item { ScreenHeader("More tools", "Manage content and store operations") }; items(featureRoutes) { dest -> DataRow(dest.glyph, dest.label, "Local demo workspace", { onOpen(dest.route) }) } } }

@Composable private fun DesignerScreen(
    sections: MutableList<String>,
    onEdit: (Int) -> Unit,
    onSave: (String) -> Unit,
    onDirtyChanged: (Boolean) -> Unit,
    onLeave: () -> Unit,
    onPreview: () -> Unit,
) {
    val baseline = remember { mutableStateListOf<String>().apply { addAll(sections) } }
    var dirty by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Pair<Int, String>?>(null) }
    androidx.activity.compose.BackHandler(enabled = dirty) { onLeave() }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { ScreenHeader("Home Designer", "Configure a local customer Home preview", backAction = onLeave) }
        item { DemoNotice() }
        if (sections.isEmpty()) item { EmptyState("No Home sections", "Add a supported section to start composing your preview.", "Add section") { showAdd = true } }
        itemsIndexed(sections.toList(), key = { index, row -> "$index:${row.hashCode()}" }) { index, row ->
            var visible by remember(row) { mutableStateOf(row.endsWith("|Visible")) }
            AdminCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("☷", color = Coral, fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(row.substringBefore('|'), fontWeight = FontWeight.Bold)
                        Text(row.split('|').getOrNull(1).orEmpty(), color = Muted, fontSize = 12.sp)
                    }
                    Switch(visible, { checked ->
                        visible = checked
                        if (index in sections.indices) sections[index] = row.substringBeforeLast('|') + "|" + if (checked) "Visible" else "Hidden"
                        dirty = true; onDirtyChanged(true)
                    })
                }
                Row {
                    TextButton(onClick = { onEdit(index) }) { Text("Edit", color = Coral) }
                    TextButton(onClick = {
                        if (index > 0 && index in sections.indices) { val moved = sections.removeAt(index); sections.add(index - 1, moved); dirty = true; onDirtyChanged(true) }
                    }) { Text("↑") }
                    TextButton(onClick = {
                        if (index in 0 until sections.lastIndex) { val moved = sections.removeAt(index); sections.add(index + 1, moved); dirty = true; onDirtyChanged(true) }
                    }) { Text("↓") }
                    TextButton(onClick = { deleteTarget = index to row.substringBefore('|') }) { Text("Delete", color = AppColors.error) }
                }
            }
        }
        item {
            OutlinedButton(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) { Text("＋ Add Section") }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPreview, modifier = Modifier.weight(1f)) { Text("Preview") }
                Button(onClick = { onSave("Draft saved on this device"); baseline.clear(); baseline.addAll(sections); dirty = false; onDirtyChanged(false) }, modifier = Modifier.weight(1f)) { Text("Save Draft") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { confirmDiscard = true }, modifier = Modifier.weight(1f)) { Text("Discard changes", color = Muted) }
                OutlinedButton(onClick = onPreview, modifier = Modifier.weight(1f)) { Text("Preview & save snapshot") }
            }
        }
    }
    if (showAdd) {
        val choices = listOf("Category Icons", "Category Selector", "Product Grid", "Product Shelf", "Bestsellers", "Recommended", "Featured Collections", "Promotional Banner", "Festival Collection", "Recently Viewed")
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add a Home section") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { choices.forEach { type -> TextButton(onClick = { sections.add("$type|$type|Visible"); dirty = true; onDirtyChanged(true); showAdd = false }, modifier = Modifier.fillMaxWidth()) { Text(type, modifier = Modifier.fillMaxWidth(), color = Ink) } } } },
            confirmButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } },
        )
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard Home changes?") },
            text = { Text(if (dirty) "Unsaved edits will be replaced with the last saved local draft." else "Return to the last saved local draft?") },
            confirmButton = { TextButton(onClick = { sections.clear(); sections.addAll(baseline); dirty = false; onDirtyChanged(false); confirmDiscard = false }) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } },
        )
    }
    deleteTarget?.let { (index, title) -> AlertDialog(
        onDismissRequest = { deleteTarget = null },
        title = { Text("Remove Home section?") },
        text = { Text("Remove $title from the local draft? Save the draft to keep this change.") },
        confirmButton = { TextButton(onClick = { if (index in sections.indices) sections.removeAt(index); dirty = true; onDirtyChanged(true); deleteTarget = null }) { Text("Remove") } },
        dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Keep") } },
    ) }
}

@Composable private fun SectionEditorScreen(section: String, onSave: (String) -> Unit, onCancel: () -> Unit) {
    val initial = remember(section) { parseHomeSection(section) }
    var title by rememberSaveable(section) { mutableStateOf(initial.title) }
    var type by rememberSaveable(section) { mutableStateOf(initial.type) }
    var subtitle by rememberSaveable(section) { mutableStateOf(initial.subtitle) }
    var layout by rememberSaveable(section) { mutableStateOf(initial.layout) }
    var surface by rememberSaveable(section) { mutableStateOf(initial.surface) }
    var animation by rememberSaveable(section) { mutableStateOf(initial.motion) }
    var visible by rememberSaveable(section) { mutableStateOf(initial.visibility == "Visible") }
    var error by rememberSaveable(section) { mutableStateOf("") }
    var confirmDiscard by remember { mutableStateOf(false) }
    val changed = title != initial.title || type != initial.type || subtitle != initial.subtitle || layout != initial.layout || surface != initial.surface || animation != initial.motion || visible != (initial.visibility == "Visible")
    val cancel = { if (changed) confirmDiscard = true else onCancel() }
    androidx.activity.compose.BackHandler(enabled = changed) { confirmDiscard = true }
    val usesCollectionLayout = type in setOf("Featured products", "Product grid", "Categories")
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item { ScreenHeader("Edit Section", type, backAction = cancel) }
        item { AdminCard {
            Text("Section content", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            AdminField("Section title", title, { title = it.take(48) })
            Text("Section type", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
                items(homeSectionTypes, key = { it }) { option -> FilterChip(type == option, { type = option }, { Text(option, fontSize = 11.sp) }) }
            }
            if (type in setOf("Hero / banner", "Promotion", "Festival campaigns", "Offers", "Custom promotion")) {
                AdminField("Subtitle or content note", subtitle, { subtitle = it.take(100) })
            } else {
                AdminField("Supporting label", subtitle, { subtitle = it.take(60) })
            }
            ToggleRow("Visible in local preview", visible) { visible = it }
        } }
        if (usesCollectionLayout) item { AdminCard {
            Text("Layout", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("Carousel", "Grid").forEach { FilterChip(layout == it, { layout = it }, { Text(it) }) } }
            Text("Card surface", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("Soft", "Glass", "Elevated").forEach { FilterChip(surface == it, { surface = it }, { Text(it) }) } }
        } }
        item { AdminCard {
            Text("Motion preset", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("None", "Fade", "Slide").forEach { FilterChip(animation == it, { animation = it }, { Text(it) }) } }
            Text("Short, lightweight transitions run in the local preview only.", color = Muted, fontSize = 12.sp)
        } }
        item {
            if (error.isNotBlank()) Text(error, color = AppColors.error)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = cancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(onClick = {
                    error = if (title.isBlank()) "Add a title for this section." else ""
                    if (error.isBlank()) onSave(HomeSectionConfig(title.trim(), type, subtitle.trim(), layout, surface, animation, if (visible) "Visible" else "Hidden").encode())
                }, modifier = Modifier.weight(1f)) { Text("Save Changes") }
            }
        }
    }
    if (confirmDiscard) AlertDialog(
        onDismissRequest = { confirmDiscard = false },
        title = { Text("Discard section edits?") },
        text = { Text("Your unsaved changes to this section will be lost.") },
        confirmButton = { TextButton(onClick = { confirmDiscard = false; onCancel() }) { Text("Discard") } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } },
    )
}

@Composable private fun PreviewScreen(sections: List<String>, products: List<String>, categories: List<String>, campaigns: List<String>, offers: List<String>, publishedSections: List<String>, themeName: String, backgroundStyle: String, sectionRadius: Float, publishedTheme: String, publishedBackground: String, publishedRadius: Float, buttonStyle: String, publishedButtonStyle: String, textTone: String, publishedTextTone: String, animationsEnabled: Boolean, maintenancePreview: Boolean, storeName: String, onBack: () -> Unit, onPublish: () -> Unit) {
    val visibleSections = sections.filter { parseHomeSection(it).visibility == "Visible" }
    val visibleCategories = categories.filter { it.endsWith("|Visible") }.map { it.substringBefore('|') }
    val previewProducts = products.filter { visibleProductRecord(it).contains("Visible", true) && !visibleProductRecord(it).contains("Out of stock", true) }
    val hasDraftChanges = sections != publishedSections || themeName != publishedTheme || backgroundStyle != publishedBackground || sectionRadius != publishedRadius || buttonStyle != publishedButtonStyle || textTone != publishedTextTone
    val previewTextColor = if (textTone == "Soft") Muted else Ink
    var confirmPublish by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf("") }
    val previewColor = when (themeName) { "Sunrise" -> Amber; "Forest" -> Green; "Ocean" -> Color(0xFF4975C6); else -> Coral }
    val previewBackground = if (backgroundStyle == "Gradient") {
        Modifier.background(Brush.verticalGradient(listOf(Color(0xFFFFEEE9), Cream)))
    } else {
        Modifier.background(Cream)
    }
    LazyColumn(Modifier.fillMaxSize().then(previewBackground).padding(horizontal = 18.dp), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader("Home Preview", "Local preview · not published to the User app") }
        item { DemoNotice() }
        if (maintenancePreview) item { Text("Maintenance preview · this local banner does not affect the User app or customer traffic.", Modifier.fillMaxWidth().background(Color(0xFFFFF4DC), RoundedCornerShape(12.dp)).padding(12.dp), color = Ink, fontSize = 12.sp) }
        item { StatusChip(if (hasDraftChanges) "Draft · local only" else "Matches saved snapshot") }
        item { AdminCard {
            Text(storeName, color = Coral, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            Text("Good morning · Patna", color = previewTextColor)
            Spacer(Modifier.height(12.dp))
            Text("⌕   Search groceries and products…", Modifier.fillMaxWidth().background(Color(0xFFF6F1EA), RoundedCornerShape(14.dp)).padding(14.dp), color = Muted)
        } }
        if (visibleSections.isEmpty()) item { EmptyState("No visible sections", "Enable or add a section in Home Designer to see it here.") }
        itemsIndexed(visibleSections, key = { index, section -> "$index:${section.hashCode()}" }) { _, section ->
            val config = parseHomeSection(section)
            val title = config.title
            val layout = config.layout
            val cardSurface = when (config.surface) { "Glass" -> Color.White.copy(alpha = .82f); "Elevated" -> Color.White; else -> Color(0xFFFAF7F1) }
            AnimatedVisibility(visible = true, enter = if (!animationsEnabled) EnterTransition.None else when (config.motion) { "Slide" -> slideInVertically { it / 8 }; "Fade" -> fadeIn(); else -> EnterTransition.None }) {
            when (config.type) {
                "Hero / banner", "Promotion", "Festival campaigns", "Offers", "Custom promotion" -> Surface(shape = RoundedCornerShape(sectionRadius.dp), color = previewColor) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Text(config.type, color = Color.White.copy(alpha = .82f), fontSize = 12.sp)
                        Text(title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        val contentLabel = when (config.type) {
                            "Festival campaigns" -> campaigns.firstOrNull()?.split('|')?.firstOrNull()?.let { "Festival preview · $it" } ?: "No local festivals configured"
                            "Offers" -> offers.firstOrNull()?.split('|')?.getOrNull(1)?.let { "Coupon preview · $it" } ?: "No local offers configured"
                            else -> config.subtitle
                        }
                        if (contentLabel.isNotBlank()) Text(contentLabel, color = Color.White.copy(alpha = .9f), fontSize = 12.sp)
                        Text("Local preview only · not published to the User app", color = Color.White.copy(alpha = .84f), fontSize = 11.sp)
                        Surface(shape = RoundedCornerShape(11.dp), color = if (buttonStyle == "Soft") Color.White.copy(alpha = .2f) else Color.White, modifier = Modifier.padding(top = 10.dp)) {
                            Text("Explore", Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = if (buttonStyle == "Soft") Color.White else previewColor, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }
                "Categories" -> AdminCard(containerColor = cardSurface, radius = sectionRadius.dp) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(10.dp))
                    if (visibleCategories.isEmpty()) EmptyState("No visible categories", "Enable a category in Category Management to preview it.")
                    else if (layout == "Grid") Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        visibleCategories.take(6).chunked(2).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { row.forEach { category -> Text("${categoryGlyph(category)}\n$category", Modifier.weight(1f).background(Color(0xFFFFF4EE), RoundedCornerShape(12.dp)).padding(10.dp), color = previewTextColor, fontSize = 12.sp) }; if (row.size == 1) Spacer(Modifier.weight(1f)) } }
                    } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        visibleCategories.take(4).forEach { category -> Text("${categoryGlyph(category)}\n$category", Modifier.weight(1f).background(Color(0xFFFFF4EE), RoundedCornerShape(12.dp)).padding(8.dp), color = previewTextColor, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                    }
                }
                else -> AdminCard(containerColor = cardSurface, radius = sectionRadius.dp) {
                    if (title.contains("best", true)) {
                        EmptyState("Sales ranking unavailable", "No order line quantities are stored in this local demo, so best sellers cannot be ranked.")
                    } else if (previewProducts.isEmpty()) {
                        EmptyState("No visible products", "Add or show products in Product Management to preview this section.")
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text(config.subtitle.ifBlank { "Local demo catalog · $layout" }, color = Muted, fontSize = 12.sp) }
                            Text("Preview only", color = Coral, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        val goods = previewProducts.take(3).map { stored ->
                            val record = visibleProductRecord(stored)
                            val fields = record.split(" · ")
                            val sale = productMetadata(stored).salePrice
                            val price = fields.getOrNull(1).orEmpty() + if (sale.isNotBlank()) " · sale ₹$sale" else ""
                            "${fields.firstOrNull().orEmpty()}\n$price"
                        }
                        if (layout == "Grid") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                goods.chunked(2).forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        row.forEach { PreviewProductCard(it, previewTextColor, Modifier.weight(1f)) }
                                        if (row.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            goods.forEach { PreviewProductCard(it, previewTextColor, Modifier.weight(1f)) }
                        }
                    }
                }
            }
            }
        }
        item {
            if (validationMessage.isNotBlank()) Text(validationMessage, color = AppColors.error, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Return to editor") }
                Button(onClick = {
                    validationMessage = when {
                        visibleSections.isEmpty() -> "Enable at least one section before saving a snapshot."
                        visibleSections.any { it.substringBefore('|').isBlank() } -> "Every visible section needs a title."
                        else -> ""
                    }
                    if (validationMessage.isBlank()) confirmPublish = true
                }, enabled = hasDraftChanges && visibleSections.isNotEmpty(), modifier = Modifier.weight(1f)) { Text("Save local snapshot") }
            }
        }
    }
    if (confirmPublish) AlertDialog(
        onDismissRequest = { confirmPublish = false },
        title = { Text("Save this preview snapshot?") },
        text = { Text("This stores the current preview on this device only. It does not publish to the RushIt User app or any server.") },
        confirmButton = { TextButton(onClick = { confirmPublish = false; onPublish() }) { Text("Save snapshot") } },
        dismissButton = { TextButton(onClick = { confirmPublish = false }) { Text("Cancel") } },
    )
}

@Composable private fun ProductForm(product: String? = null, categories: List<String> = initialCategories, onCancel: () -> Unit, onSave: (String) -> Unit) {
    val original = remember(product) { visibleProductRecord(product.orEmpty()).split(" · ") }
    val metadata = remember(product) { productMetadata(product.orEmpty()) }
    val initialPrice = remember(product) { Regex("[0-9]+(?:\\.[0-9]+)?").find(original.getOrNull(1).orEmpty())?.value.orEmpty() }
    var name by rememberSaveable(product) { mutableStateOf(original.getOrNull(0).orEmpty()) }
    var price by rememberSaveable(product) { mutableStateOf(initialPrice) }
    val initialUnit = remember(product) { original.getOrNull(1)?.substringAfter('/', "unit") ?: "kg" }
    var unit by rememberSaveable(product) { mutableStateOf(initialUnit) }
    val initialStock = remember(product) { metadata.stockQuantity.ifBlank { if (original.getOrNull(2)?.contains("Low", true) == true) "3" else if (original.getOrNull(2)?.contains("Out", true) == true) "0" else "10" } }
    var stock by rememberSaveable(product) { mutableStateOf(initialStock) }
    var description by rememberSaveable(product) { mutableStateOf(metadata.description) }
    var sku by rememberSaveable(product) { mutableStateOf(metadata.sku) }
    var salePrice by rememberSaveable(product) { mutableStateOf(metadata.salePrice) }
    var category by rememberSaveable(product) { mutableStateOf(original.getOrNull(3) ?: "Vegetables") }
    var visible by rememberSaveable(product) { mutableStateOf(original.getOrNull(4) != "Hidden") }
    var saved by rememberSaveable(product) { mutableStateOf(false) }
    var error by rememberSaveable(product) { mutableStateOf("") }
    var confirmDiscard by remember { mutableStateOf(false) }
    val originalStatus = original.getOrNull(2).orEmpty()
    val hasChanges = name != original.getOrNull(0).orEmpty() || price != initialPrice || unit != initialUnit || description != metadata.description || sku != metadata.sku || salePrice != metadata.salePrice || category != (original.getOrNull(3) ?: "Vegetables") || stock != initialStock || visible != (original.getOrNull(4) != "Hidden")
    val cancel = { if (hasChanges) confirmDiscard = true else onCancel() }
    androidx.activity.compose.BackHandler(enabled = hasChanges) { confirmDiscard = true }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenHeader(if (product == null) "Add Product" else "Edit Product", "Saved in the local demo catalog", backAction = cancel) }
        item { AdminCard {
            Text("Product image", fontWeight = FontWeight.Bold)
            Box(Modifier.fillMaxWidth().height(120.dp).padding(top = 8.dp).clip(RoundedCornerShape(15.dp)).background(Color(0xFFFFF0E9)), contentAlignment = Alignment.Center) { Text("🍅\nImage picker not connected in demo", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Muted) }
        } }
        item { AdminField("Product name", name, { name = it }) }
        item { AdminField("Description", description, { description = it }) }
        item { AdminField("SKU · optional", sku, { sku = it.trim().take(40) }) }
        item { AdminField("Price (₹)", price, { price = it }, KeyboardType.Decimal) }
        item { AdminField("Sale price (₹) · optional", salePrice, { salePrice = it }, KeyboardType.Decimal) }
        item { AdminCard {
            Text("Unit", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("kg", "piece", "L", "pack").forEach { FilterChip(unit == it, { unit = it }, { Text(it) }) } }
        } }
        item { AdminCard {
            Text("Category", fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) { items(categories.map { it.substringBefore('|') }.filter { it.isNotBlank() }) { option -> FilterChip(category == option, { category = option }, { Text(option) }) } }
        } }
        item { AdminField("Stock quantity", stock, { stock = it }, KeyboardType.Number) }
        item { AdminCard { ToggleRow("Visible in demo catalog", visible) { visible = it } } }
        item { if (error.isNotBlank()) Text(error, color = Coral, fontSize = 13.sp) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = cancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(onClick = {
                    if (saved) return@Button
                    val parsedPrice = price.toDoubleOrNull()
                    val parsedSalePrice = salePrice.toDoubleOrNull()
                    val count = stock.toIntOrNull()
                    error = when {
                        name.isBlank() -> "Product name is required."
                        parsedPrice == null || parsedPrice < 0.01 -> "Enter a price of at least ₹0.01."
                        salePrice.isNotBlank() && (parsedPrice == null || parsedSalePrice == null || parsedSalePrice < 0.01 || parsedSalePrice >= parsedPrice) -> "Sale price must be greater than zero and lower than the regular price."
                        count == null || count < 0 -> "Stock must be a non-negative whole number."
                        else -> ""
                    }
                    if (error.isBlank()) {
                        val validCount = count ?: return@Button
                        val validPrice = parsedPrice ?: return@Button
                        val availability = if (validCount == 0) "Out of stock" else if (validCount < 5) "Low stock" else "In stock"
                        saved = true
                        onSave(withProductMetadata("${cleanStoredField(name)} · ₹${formatRupees(validPrice)}/$unit · $availability · ${cleanStoredField(category)} · ${if (visible) "Visible" else "Hidden"}", ProductMetadata(cleanStoredField(sku), cleanStoredField(description), salePrice.trim(), validCount.toString())))
                    }
                }, enabled = !saved, modifier = Modifier.weight(1f), shape = AppShapes.button, colors = ButtonDefaults.buttonColors(containerColor = Coral)) { Text(if (product == null) "Save Product" else "Save Changes") }
            }
        }
    }
    if (confirmDiscard) AlertDialog(
        onDismissRequest = { confirmDiscard = false },
        title = { Text("Discard product changes?") },
        text = { Text("Your unsaved product edits will be lost.") },
        confirmButton = { TextButton(onClick = { confirmDiscard = false; onCancel() }) { Text("Discard") } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } },
    )
}

@Composable private fun OrderDetailScreen(order: String, onUpdate: (String) -> Unit) {
    val statuses = listOf("Confirmed", "Preparing", "Rider Ready", "Picked Up", "On the way", "Almost there", "Delivered")
    var status by rememberSaveable(order) { mutableStateOf(order.substringAfterLast(" · ")) }
    var confirm by remember { mutableStateOf(false) }
    var cancelling by remember { mutableStateOf(false) }
    val amount = Regex("₹(\\d+)").find(order)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 136
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader(order.substringBefore(" · "), "Creation date unavailable · demo record") }
        item { DemoNotice() }
        item { AdminCard { Text("Customer", fontWeight = FontWeight.Bold); Text(order.split(" · ").getOrNull(1) ?: "Customer name unavailable", color = Muted); Text("Contact and delivery address are not present in this sample order.", color = Muted, fontSize = 12.sp) } }
        item { AdminCard {
            Text("Order total", fontWeight = FontWeight.Bold)
            Text("₹$amount", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("Line items, quantities, fees and price breakdown are not included in this demo record.", color = Muted, fontSize = 12.sp)
        } }
        item { AdminCard { Text("Payment status", fontWeight = FontWeight.Bold); Text("Not supplied · unknown", color = Muted); Text("No payment confirmation or refund operation is available in this demo.", color = Muted, fontSize = 12.sp) } }
        item { AdminCard {
            Text("Status history", fontWeight = FontWeight.Bold)
            Text("Current recorded status · $status", color = Coral, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Text("The sample order stores only its current status; no event history is available.", color = Muted, fontSize = 12.sp)
            OutlinedButton(onClick = { cancelling = false; confirm = true }, enabled = status !in listOf("Delivered", "Cancelled")) { Text("Update status") }
            if (status in listOf("Confirmed", "Preparing", "Rider Ready")) TextButton(onClick = { cancelling = true; confirm = true }) { Text("Cancel order", color = Coral) }
        } }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text(if (cancelling) "Cancel demo order?" else "Advance demo order?") },
        text = { Text(if (cancelling) "Cancel this local demo order? No real order or payment is affected." else "Update status from $status? No payment, rider, or real order data is involved.") },
        confirmButton = { TextButton(onClick = {
            if (cancelling) status = "Cancelled" else {
                val next = statuses.indexOf(status).let { if (it < 0) 1 else (it + 1).coerceAtMost(statuses.lastIndex) }
                status = statuses[next]
            }
            confirm = false
            onUpdate(status)
        }) { Text(if (cancelling) "Cancel order" else "Update") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } },
    )
}

private val demoCustomers = listOf("Ankit Kumar · Patna · 5 orders", "Priya Singh · Delhi · 12 orders", "Rohit Sharma · Mumbai · 8 orders", "Sneha Verma · Patna · 3 orders", "Aman Gupta · Bengaluru · 6 orders")

@Composable private fun CustomersScreen(onOpen: (Int) -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    val filtered = demoCustomers.withIndex().filter { it.value.contains(search, true) }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        ScreenHeader("Customers", "${demoCustomers.size} demo customers")
        OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Search customers") }, singleLine = true, shape = AppShapes.input)
        LazyColumn(contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (filtered.isEmpty()) item { EmptyState("No customers found", "Try another name or city.") }
            items(filtered) { indexed -> DataRow("♙", indexed.value.substringBefore(" · "), indexed.value.substringAfter(" · "), { onOpen(indexed.index) }) }
        }
    }
}

@Composable private fun CustomerDetailScreen(index: Int) {
    val record = demoCustomers.getOrNull(index) ?: demoCustomers.first()
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader(record.substringBefore(" · "), "Customer profile · local demo") }
        item { DemoNotice() }
        item { AdminCard { Text("Customer information", fontWeight = FontWeight.Bold); Text("Demo ID · CUST-${1001 + index}"); Text("City · ${record.split(" · ").getOrNull(1)}"); Text("Contact details are illustrative only", color = Muted) } }
        item { AdminCard { Text("Order history", fontWeight = FontWeight.Bold); Text("${record.substringAfterLast(" · ")}"); Text("Recent order · ${initialOrders[index % initialOrders.size].substringBefore(" · ")}", color = Muted) } }
        item { AdminCard { Text("Account status", fontWeight = FontWeight.Bold); StatusChip("Active · Demo"); Text("Local status only · no customer account is changed", color = Muted, fontSize = 12.sp) } }
    }
}

@Composable private fun NotificationScreen(prefs: android.content.SharedPreferences, onSend: () -> Unit) {
    var title by rememberSaveable { mutableStateOf(prefs.getString("notification_draft_title", "") ?: "") }
    var message by rememberSaveable { mutableStateOf(prefs.getString("notification_draft_message", "") ?: "") }
    var audience by rememberSaveable { mutableStateOf(prefs.getString("notification_draft_audience", "All demo customers") ?: "All demo customers") }
    var error by rememberSaveable { mutableStateOf("") }
    var feedback by rememberSaveable { mutableStateOf("") }
    val history = remember(prefs) { mutableStateListOf(*(prefs.getString("notifications", null)?.split("##")?.filter { it.isNotBlank() }?.toTypedArray() ?: emptyArray())) }
    val audienceCount = when (audience) {
        "Patna demo customers" -> demoCustomers.count { it.split(" · ").getOrNull(1) == "Patna" }
        "Other demo cities" -> demoCustomers.count { it.split(" · ").getOrNull(1) != "Patna" }
        else -> demoCustomers.size
    }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), contentPadding = PaddingValues(bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenHeader("Notifications", "Local drafts and previews · no push service") }
        item { DemoNotice() }
        item { AdminField("Notification title", title, { title = it.take(60) }) }
        item { OutlinedTextField(message, { message = it.take(240) }, Modifier.fillMaxWidth().heightIn(min = 110.dp), label = { Text("Message · max 240 characters") }, shape = AppShapes.input) }
        item { Text("Demo audience", fontWeight = FontWeight.Bold) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("All demo customers", "Patna demo customers", "Other demo cities").forEach { FilterChip(audience == it, { audience = it }, { Text(it, fontSize = 10.sp) }) }
        } }
        item { Text("Estimated sample audience · $audienceCount demo profiles · no real recipients", color = Muted, fontSize = 12.sp) }
        item { AdminCard {
            Text("Preview", fontWeight = FontWeight.Bold)
            Text(title.ifBlank { "Notification title" }, color = Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            Text(message.ifBlank { "Your message preview will appear here." }, color = Muted)
            Text(audience, color = Coral, fontSize = 11.sp)
        } }
        item { Text("Push notifications are not configured. These actions only save a local draft or preview; no customer is contacted.", color = Muted, fontSize = 12.sp) }
        item {
            if (error.isNotBlank()) Text(error, color = AppColors.error)
            if (feedback.isNotBlank()) Text(feedback, color = AppColors.success)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    prefs.edit().putString("notification_draft_title", title).putString("notification_draft_message", message).putString("notification_draft_audience", audience).apply()
                    feedback = "Draft saved on this device."
                }, modifier = Modifier.weight(1f)) { Text("Save draft") }
                PrimaryButton("Save preview", Modifier.weight(1f)) {
                    error = when { title.isBlank() -> "Add a notification title."; message.isBlank() -> "Add a notification message."; else -> "" }
                    if (error.isBlank()) {
                        val record = "$title · $audience · Preview saved · Not delivered"
                        history.add(0, record)
                        prefs.edit().putString("notifications", history.take(30).joinToString("##")).apply()
                        feedback = "Preview stored locally; no notification was sent."
                        onSend()
                    }
                }
            }
        }
        item { SectionTitle("Saved local previews", "${history.size} records") }
        if (history.isEmpty()) item { EmptyState("No saved previews", "Your locally saved notification previews will appear here.") }
        itemsIndexed(history, key = { index, value -> "$index:${value.hashCode()}" }) { _, record -> DataRow("♧", record, "Not delivered to real devices") }
    }
}

@Composable private fun SettingsScreen(prefs: android.content.SharedPreferences, themeName: String, onReset: () -> Unit) {
    var storeName by remember(prefs) { mutableStateOf(prefs.getString("store_name", "RushIt") ?: "RushIt") }
    var supportEmail by remember(prefs) { mutableStateOf(prefs.getString("support_email", "support@rushit.in") ?: "support@rushit.in") }
    var maintenancePreview by remember(prefs) { mutableStateOf(prefs.getBoolean("maintenance", false)) }
    var animations by remember(prefs) { mutableStateOf(prefs.getBoolean("animations", true)) }
    var error by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    var confirmReset by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 22.dp)) {
        item { ScreenHeader("App Settings", "Preferences stored in this Admin app only") }
        item { DemoNotice() }
        item { AdminCard {
            Text("Store identity · local demo", fontWeight = FontWeight.Bold)
            AdminField("Store name", storeName, { storeName = it.take(50) })
            AdminField("Support email", supportEmail, { supportEmail = it.trim().take(80) }, KeyboardType.Email)
            Text("Currency · Indian Rupee (₹) · fixed for this local India demo", color = Muted, fontSize = 12.sp)
            Text("Current preview palette · $themeName", color = Muted, fontSize = 12.sp)
        } }
        item { AdminCard {
            Text("Local preview controls", fontWeight = FontWeight.Bold)
            ToggleRow("Preview maintenance banner flag", maintenancePreview) { maintenancePreview = it; prefs.edit().putBoolean("maintenance", it).apply() }
            Text("This flag is visible only in Admin settings; it does not change the User app or store availability.", color = Muted, fontSize = 11.sp)
            ToggleRow("Preview motion", animations) { animations = it; prefs.edit().putBoolean("animations", it).apply() }
            Text("Disabling motion removes section entrance transitions in the local Home Preview.", color = Muted, fontSize = 11.sp)
        } }
        item {
            if (error.isNotBlank()) Text(error, color = AppColors.error)
            if (feedback.isNotBlank()) Text(feedback, color = AppColors.success)
            Button(onClick = {
                error = when {
                    storeName.isBlank() -> "Enter a store name."
                    !android.util.Patterns.EMAIL_ADDRESS.matcher(supportEmail).matches() -> "Enter a valid support email address."
                    else -> ""
                }
                if (error.isBlank()) {
                    prefs.edit().putString("store_name", storeName.trim()).putString("support_email", supportEmail.trim()).apply()
                    feedback = "Local store identity saved. The customer app remains unchanged."
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Save local settings") }
        }
        item { AdminCard {
            Text("Information", fontWeight = FontWeight.Bold)
            Text("RushIt Admin · version 1.0 demo", color = Muted, modifier = Modifier.padding(top = 7.dp))
            Text("No admin backend, policy links, payment settings, tax configuration, or delivery controls are connected.", color = Muted, fontSize = 12.sp)
        } }
        item { OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) { Text("Reset local demo data", color = Coral) } }
    }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("Reset local demo data?") },
        text = { Text("Saved catalog, sections, theme, local preferences, drafts, and profile details on this device will be reset.") },
        confirmButton = { TextButton(onClick = { onReset(); confirmReset = false }) { Text("Reset") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
    )
}

@Composable private fun ProfileScreen(prefs: android.content.SharedPreferences, onLogout: () -> Unit) {
    var name by remember(prefs) { mutableStateOf(prefs.getString("admin_name", "Dharmjeet Singh") ?: "Dharmjeet Singh") }
    var email by remember(prefs) { mutableStateOf(prefs.getString("admin_email", "admin@rushit.in") ?: "admin@rushit.in") }
    var role by remember(prefs) { mutableStateOf(prefs.getString("admin_role", "Super Admin") ?: "Super Admin") }
    var edit by remember { mutableStateOf(false) }
    var profileError by remember { mutableStateOf("") }
    var passwordInfo by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenHeader("Admin Profile", "Demo account · local only") }
        item { AdminCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFFFE8E2)), contentAlignment = Alignment.Center) { Text(name.split(' ').mapNotNull { it.firstOrNull() }.take(2).joinToString(""), color = Coral, fontWeight = FontWeight.Bold, fontSize = 19.sp) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { Text(name, fontSize = 19.sp, fontWeight = FontWeight.Bold); Text(email, color = Muted, fontSize = 13.sp); StatusChip(role) }
                TextButton(onClick = { edit = true }) { Text("Edit", color = Coral) }
            }
        } }
        item { AdminCard {
            Text("Role visibility · demo only", fontWeight = FontWeight.Bold)
            Text("Client-side role visibility is not an authorization boundary.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            Column { listOf("Super Admin", "Catalog Manager", "Order Manager", "Content Manager", "Support Agent").forEach { label ->
                Row(Modifier.fillMaxWidth().clickable { role = label; prefs.edit().putString("admin_role", label).apply() }.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = role == label, onClick = { role = label; prefs.edit().putString("admin_role", label).apply() }, colors = RadioButtonDefaults.colors(selectedColor = Coral))
                    Text(label, color = Ink)
                }
            } }
        } }
        item { DataRow("⌑", "Change password", "Demo only · no real password is stored", { passwordInfo = true }) }
        item { AdminCard { Text("Session", fontWeight = FontWeight.Bold); Text("Signed in · Demo session", color = Muted); Text("Login history · Today", color = Muted) } }
        item { Text("This local prototype does not authenticate or authorize production administrators.", color = Muted, fontSize = 12.sp) }
        item { OutlinedButton(onClick = { confirmLogout = true }, modifier = Modifier.fillMaxWidth()) { Text("Log out") } }
    }
    if (confirmLogout) AlertDialog(
        onDismissRequest = { confirmLogout = false },
        title = { Text("End this demo session?") },
        text = { Text("You will return to the local demo sign-in screen. No production account is affected.") },
        confirmButton = { TextButton(onClick = { confirmLogout = false; onLogout() }) { Text("Log out") } },
        dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Stay") } },
    )
    if (edit) {
        var draftName by remember(name) { mutableStateOf(name) }
        var draftEmail by remember(email) { mutableStateOf(email) }
        AlertDialog(onDismissRequest = { edit = false }, title = { Text("Edit demo profile") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminField("Display name", draftName, { draftName = it })
                AdminField("Demo email", draftEmail, { draftEmail = it }, KeyboardType.Email)
                if (profileError.isNotBlank()) Text(profileError, color = AppColors.error, fontSize = 12.sp)
            }
        }, confirmButton = { TextButton(onClick = {
            profileError = when {
                draftName.isBlank() -> "Enter a display name."
                !android.util.Patterns.EMAIL_ADDRESS.matcher(draftEmail).matches() -> "Enter a valid email address."
                else -> ""
            }
            if (profileError.isBlank()) {
                name = draftName.trim(); email = draftEmail.trim()
                prefs.edit().putString("admin_name", name).putString("admin_email", email).apply()
                edit = false
            }
        }) { Text("Save") } }, dismissButton = { TextButton(onClick = { edit = false; profileError = "" }) { Text("Cancel") } })
    }
    if (passwordInfo) AlertDialog(onDismissRequest = { passwordInfo = false }, title = { Text("Password settings") }, text = { Text("Password changes are not available in this offline demo. No password is stored or changed.") }, confirmButton = { TextButton(onClick = { passwordInfo = false }) { Text("Got it") } })
}

@Composable fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f)); Switch(value, onChange) } }
@Composable fun ScreenHeader(title: String, subtitle: String = "", onAction: (() -> Unit)? = null, action: String = "＋", backAction: (() -> Unit)? = null) {
    val nav = LocalAdminNavigation.current
    val route = nav?.currentBackStackEntry?.destination?.route
    val showBack = route != null && route !in setOf("home", "catalog", "orders", "themes", "more", "login", "splash")
    Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (showBack) TextButton(onClick = { if (backAction != null) backAction() else nav?.popBackStack() }, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("‹", color = Coral, fontSize = 27.sp) }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
            if (subtitle.isNotBlank()) Text(subtitle, fontSize = 12.sp, color = Muted)
        }
        if (onAction != null) TextButton(onClick = onAction) { Text(action, color = Coral) }
    }
}
@Composable private fun AdminField(label: String, value: String, onValue: (String) -> Unit, keyboard: KeyboardType = KeyboardType.Text, readOnly: Boolean = false) { OutlinedTextField(value, onValue, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true, readOnly = readOnly, keyboardOptions = KeyboardOptions(keyboardType = keyboard), shape = RoundedCornerShape(14.dp)) }
@Composable private fun PrimaryButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) { Button(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = Coral)) { Text(label, fontWeight = FontWeight.SemiBold) } }
@Composable private fun AdminCard(modifier: Modifier = Modifier, containerColor: Color = Card, radius: androidx.compose.ui.unit.Dp = 18.dp, content: @Composable ColumnScope.() -> Unit) { Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(radius), color = containerColor, border = androidx.compose.foundation.BorderStroke(1.dp, Line), shadowElevation = 1.dp) { Column(Modifier.padding(15.dp), content = content) } }

@Composable private fun PreviewProductCard(label: String, textColor: Color, modifier: Modifier = Modifier) {
    Column(modifier.background(Color.White, RoundedCornerShape(12.dp)).padding(10.dp)) {
        Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFFFFF4EE)), contentAlignment = Alignment.Center) { Text("🛍️", fontSize = 23.sp) }
        Text(label.substringBefore('\n'), color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label.substringAfter('\n'), color = textColor, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
@Composable private fun DataRow(glyph: String, title: String, detail: String, onClick: (() -> Unit)? = null) { Surface(Modifier.fillMaxWidth().then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier), shape = RoundedCornerShape(16.dp), color = Card, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFEEE9)), contentAlignment = Alignment.Center) { Text(glyph, color = Coral, fontWeight = FontWeight.Bold) }; Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(detail, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }; if (onClick != null) Text("›", color = Muted, fontSize = 22.sp) } } }
@Composable private fun StatusChip(status: String) { val color = if (status.contains("Delivered", true)) Green else if (status.contains("Cancelled", true)) Muted else Coral; Text(status, Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = .10f)).padding(horizontal = 9.dp, vertical = 5.dp), color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
@Composable fun DemoNotice() { Text("DEMO · This configuration stays in RushIt Admin on this device; it does not update the customer app or production services.", Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Color(0xFFFFF4DC)).padding(12.dp), color = Ink, fontSize = 12.sp) }
@Composable private fun FilterChip(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit, modifier: Modifier = Modifier) { androidx.compose.material3.FilterChip(selected = selected, onClick = onClick, label = label, modifier = modifier) }
