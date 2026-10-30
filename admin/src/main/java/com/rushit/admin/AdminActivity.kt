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
import kotlinx.coroutines.delay
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

private val initialProducts = listOf("Tomato · ₹24/kg · In stock · Vegetables · Visible", "Banana · ₹32/kg · In stock · Fruits · Visible", "Milk · ₹56/L · In stock · Dairy · Visible", "Bread · ₹32/piece · Low stock · Bakery · Visible", "Apple · ₹120/kg · In stock · Fruits · Visible")
private val initialCategories = listOf("Fruits|Visible", "Vegetables|Visible", "Dairy|Visible", "Snacks|Visible", "Beverages|Visible", "Bakery|Visible", "Personal Care|Visible")
private val initialCampaigns = listOf("Diwali|2026-11-10|2026-11-20|1|Enabled", "Holi|2027-03-01|2027-03-10|2|Enabled", "Christmas|2026-12-20|2026-12-26|3|Disabled", "Eid|2027-04-10|2027-04-11|2|Disabled", "Valentine's Day|2027-02-14|2027-02-14|3|Disabled")
private val initialOffers = listOf("Welcome offer|WELCOME10|Percentage|10|499|500|2026-10-01|2026-12-31|Enabled", "First order|FIRSTORDER|Percentage|20|399|1000|2026-10-01|2027-03-31|Enabled", "Flat discount|SOMAI50|Fixed|50|299|500|2026-11-01|2026-12-15|Disabled")
private val initialOrders = listOf("#RI172805456  ·  Ankit Kumar  ·  ₹542  ·  Preparing", "#RI172805455  ·  Priya Singh  ·  ₹386  ·  On the way", "#RI172805454  ·  Rohit Sharma  ·  ₹724  ·  Delivered", "#RI172805453  ·  Sneha Verma  ·  ₹218  ·  Cancelled")

@Composable private fun AdminRoot() {
    val nav = rememberNavController()
    val back by nav.currentBackStackEntryAsState()
    val route = back?.destination?.route ?: "login"
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("rushit_admin_demo", android.content.Context.MODE_PRIVATE) }
    val products = remember(preferences) { mutableStateListOf(*(preferences.getString("products", null)?.split("##")?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() } ?: initialProducts).toTypedArray()) }
    val orders = remember(preferences) { mutableStateListOf(*(preferences.getString("orders", null)?.split("##")?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() } ?: initialOrders).toTypedArray()) }
    val sections = remember(preferences) { mutableStateListOf(*(preferences.getString("sections", null)?.split("##")?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() } ?: listOf("Top Banner|Festival Deals|Visible", "Category Icons|8 categories|Visible", "Flash Deals|Horizontal products|Visible", "Best Selling|Product grid|Visible", "Recommended|Personalized|Visible")).toTypedArray()) }
    val categories = remember(preferences) { mutableStateListOf(*(preferences.getString("categories", null)?.split("##")?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() } ?: initialCategories).toTypedArray()) }
    val campaigns = remember(preferences) { mutableStateListOf(*(preferences.getString("campaigns", null)?.split("##")?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() } ?: initialCampaigns).toTypedArray()) }
    val offers = remember(preferences) { mutableStateListOf(*(preferences.getString("offers", null)?.split("##")?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() } ?: initialOffers).toTypedArray()) }
    var themeName by remember(preferences) { mutableStateOf(preferences.getString("theme_name", "RushIt Coral") ?: "RushIt Coral") }
    var backgroundStyle by remember(preferences) { mutableStateOf(preferences.getString("background_style", "Gradient") ?: "Gradient") }
    var sectionRadius by remember(preferences) { mutableFloatStateOf(preferences.getFloat("section_radius", 16f)) }
    var settingsVersion by remember { mutableIntStateOf(0) }
    var snackbar by remember { mutableStateOf("") }
    val snackbarState = remember { SnackbarHostState() }
    LaunchedEffect(snackbar) { if (snackbar.isNotBlank()) { snackbarState.showSnackbar(snackbar); snackbar = "" } }
    val showTabs = route !in setOf("login", "splash", "product_edit", "product_edit/{index}", "section/{index}", "festival_edit/{index}", "offer_edit/{index}", "order_detail/{index}", "customer_detail/{index}")
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
                    if (route != dest.route) nav.navigate(dest.route) {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }, icon = { Text(dest.glyph, fontSize = 20.sp, color = if (selected) Coral else Muted) }, label = { Text(dest.label, fontSize = 10.sp) })
            }
        }
    }) { padding ->
        CompositionLocalProvider(LocalAdminNavigation provides nav) {
        NavHost(navController = nav, startDestination = "splash", modifier = Modifier.padding(padding)) {
            composable("splash") { SplashScreen(onFinish = { nav.navigate("login") { popUpTo("splash") { inclusive = true }; launchSingleTop = true } }) }
            composable("login") { LoginScreen(onLogin = { nav.navigate("home") { popUpTo("login") { inclusive = true }; launchSingleTop = true } }) }
            composable("home") { Dashboard(products, orders, demoCustomers.size, preferences.getString("admin_name", "Dharmjeet Singh") ?: "Dharmjeet Singh", onOpen = { nav.navigate(it) }) }
            composable("catalog") { CatalogScreen(products, categories, onAdd = { nav.navigate("product_edit") }, onEdit = { index -> nav.navigate("product_edit/$index") }) }
            composable("orders") { OrdersScreen(orders, onOpen = { index -> nav.navigate("order_detail/$index") }) }
            composable("themes") { ThemeScreen(themeName, backgroundStyle, sectionRadius, onThemeChange = { themeName = it; preferences.edit().putString("theme_name", it).apply() }, onBackgroundChange = { backgroundStyle = it; preferences.edit().putString("background_style", it).apply() }, onRadiusChange = { sectionRadius = it; preferences.edit().putFloat("section_radius", it).apply() }, onPreview = { nav.navigate("preview") }) }
            composable("more") { MoreScreen(onOpen = { nav.navigate(it) }) }
            composable("designer") { DesignerScreen(sections, onEdit = { index -> nav.navigate("section/$index") }, onSave = { preferences.edit().putString("sections", sections.joinToString("##")).apply(); snackbar = it }, onPreview = { nav.navigate("preview") }) }
            composable("section/{index}") { entry ->
                val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
                SectionEditorScreen(sections.getOrNull(index).orEmpty(), onSave = { updated ->
                    if (index in sections.indices) sections[index] = updated
                    preferences.edit().putString("sections", sections.joinToString("##")).apply()
                    snackbar = "Section saved to local draft"
                    nav.popBackStack()
                }, onCancel = { nav.popBackStack() })
            }
            composable("preview") { PreviewScreen(sections = sections, themeName = themeName, backgroundStyle = backgroundStyle, sectionRadius = sectionRadius, onBack = { nav.popBackStack() }, onPublish = { snackbar = "Published to local demo preview only" }) }
            composable("festivals") { CampaignManagerScreen(campaigns, onAdd = { nav.navigate("festival_edit/new") }, onEdit = { index -> nav.navigate("festival_edit/$index") }) }
            composable("festival_edit/{index}") { entry ->
                val key = entry.arguments?.getString("index") ?: "new"
                val index = key.toIntOrNull()
                CampaignEditorScreen(campaign = index?.let { campaigns.getOrNull(it) }, onSave = { updated ->
                    if (index == null) campaigns.add(updated) else if (index in campaigns.indices) campaigns[index] = updated
                    preferences.edit().putString("campaigns", campaigns.joinToString("##")).apply()
                    snackbar = "Campaign saved locally · not published"
                    nav.popBackStack()
                })
            }
            composable("schedule") { CampaignManagerScreen(campaigns, scheduler = true, onAdd = { nav.navigate("festival_edit/new") }, onEdit = { index -> nav.navigate("festival_edit/$index") }) }
            composable("product_edit") { ProductForm(categories = categories, onSave = { products.add(it); preferences.edit().putString("products", products.joinToString("##")).apply(); snackbar = "Product saved locally"; nav.popBackStack() }) }
            composable("product_edit/{index}") { entry ->
                val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
                ProductForm(product = products.getOrNull(index), categories = categories, onSave = { updated ->
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
            composable("offers") { OfferManagerScreen(offers, onAdd = { nav.navigate("offer_edit/new") }, onEdit = { index -> nav.navigate("offer_edit/$index") }) }
            composable("offer_edit/{index}") { entry ->
                val key = entry.arguments?.getString("index") ?: "new"
                val index = key.toIntOrNull()
                OfferEditorScreen(offers.getOrNull(index ?: -1), onSave = { offer ->
                    if (index == null) offers.add(offer) else if (index in offers.indices) offers[index] = offer
                    preferences.edit().putString("offers", offers.joinToString("##")).apply()
                    snackbar = "Offer saved locally · not published to customers"
                    nav.popBackStack()
                })
            }
            composable("notifications") { NotificationScreen(preferences, onSend = { snackbar = "Demo notification recorded locally · not delivered" }) }
            composable("settings") { key(settingsVersion) { SettingsScreen(preferences, themeName, onReset = {
                preferences.edit().clear().apply()
                products.clear(); products.addAll(initialProducts)
                orders.clear(); orders.addAll(initialOrders)
                sections.clear(); sections.addAll(listOf("Top Banner|Festival Deals|Visible", "Category Icons|8 categories|Visible", "Flash Deals|Horizontal products|Visible", "Best Selling|Product grid|Visible", "Recommended|Personalized|Visible"))
                categories.clear(); categories.addAll(initialCategories)
                campaigns.clear(); campaigns.addAll(initialCampaigns)
                offers.clear(); offers.addAll(initialOffers)
                themeName = "RushIt Coral"
                backgroundStyle = "Gradient"
                sectionRadius = 16f
                settingsVersion++
                snackbar = "Local demo data reset"
            }) } }
            composable("profile") { ProfileScreen(preferences, onLogout = { nav.navigate("login") { popUpTo("home") { inclusive = true }; launchSingleTop = true } }) }
        }
        }
    }
}

@Composable private fun SplashScreen(onFinish: () -> Unit) {
    var entered by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (entered) 1f else 0.82f, animationSpec = spring(dampingRatio = 0.62f), label = "admin logo entrance")
    LaunchedEffect(Unit) {
        entered = true
        delay(560)
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

@Composable private fun LoginScreen(onLogin: () -> Unit) {
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
                Text("Remember me", fontSize = 13.sp, color = Muted)
            }
            TextButton(onClick = { error = "Password recovery is not connected in this local demo." }) { Text("Forgot?", color = Coral, fontSize = 12.sp) }
        }
        if (error.isNotBlank()) Text(error, Modifier.fillMaxWidth().padding(bottom = 4.dp), color = Coral, fontSize = 12.sp)
        PrimaryButton("Sign in · Demo mode", Modifier.fillMaxWidth()) {
            error = when {
                email.isBlank() -> "Enter an email or phone number."
                password.isBlank() -> "Enter a password to continue."
                else -> ""
            }
            if (error.isBlank()) onLogin()
        }
        TextButton(onClick = onLogin, modifier = Modifier.fillMaxWidth().padding(top = 5.dp)) { Text("Continue with demo access", color = Coral) }
        Text("Prototype only · This is not production authentication.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        Text("Restricted area · Administrators only", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 10.dp))
    }
}

@Composable private fun Dashboard(products: List<String>, orders: List<String>, customerCount: Int, adminName: String, onOpen: (String) -> Unit) {
    val productCount = products.size
    val lowStockCount = products.count { it.contains("Low stock", true) || it.contains("Out of stock", true) }
    val salesValues = orders.mapNotNull { Regex("₹(\\d+)").find(it)?.groupValues?.getOrNull(1)?.toIntOrNull() }
    val salesTotal = salesValues.sum()
    val chartMaximum = (salesValues.maxOrNull() ?: 1).coerceAtLeast(1)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("RushIt Admin", color = Coral, fontSize = 13.sp, fontWeight = FontWeight.Bold); Text("Good morning, ${adminName.substringBefore(' ')}", color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold); Text("Store overview · Local demo data", color = Muted, fontSize = 13.sp) }; Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFFFF1DF)).clickable { onOpen("notifications") }, contentAlignment = Alignment.Center) { Text("♧", color = Coral, fontSize = 19.sp) }; Spacer(Modifier.width(8.dp)); Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFFFE8E2)).clickable { onOpen("profile") }, contentAlignment = Alignment.Center) { Text("DS", color = Coral, fontWeight = FontWeight.Bold, fontSize = 13.sp) } } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCard("Today's Orders", "${orders.size}", "Demo records", Modifier.weight(1f)) { onOpen("orders") }; StatCard("Total Orders", "${orders.size}", "Local demo", Modifier.weight(1f)) { onOpen("orders") } } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCard("Products", productCount.toString(), "Local catalog", Modifier.weight(1f)) { onOpen("catalog") }; StatCard("Customers", customerCount.toString(), "Demo", Modifier.weight(1f)) { onOpen("customers") } } }
        item { Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = Coral) { Column(Modifier.padding(18.dp)) {
            Text("SALES · DEMO ORDERS", color = Color.White.copy(alpha = .82f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("₹$salesTotal", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text("Sum of the order records shown below", color = Color.White.copy(alpha = .86f), fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                salesValues.ifEmpty { listOf(0) }.takeLast(7).forEach { amount ->
                    val fraction = (amount.toFloat() / chartMaximum).coerceIn(.12f, 1f)
                    Box(Modifier.width(22.dp).fillMaxHeight(fraction).clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)).background(Color.White.copy(alpha = .62f)))
                }
            }
        } } }
        item { AdminCard(Modifier.clickable { onOpen("catalog") }) { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Inventory health", fontWeight = FontWeight.Bold); Text("$lowStockCount products need attention", color = Muted, fontSize = 12.sp) }; StatusChip(if (lowStockCount > 0) "Low stock" else "Healthy") } } }
        item { SectionTitle("Quick actions", "Shortcuts"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { QuickAction("＋", "Add Product") { onOpen("product_edit") }; QuickAction("⌘", "Home Designer") { onOpen("designer") } }; Spacer(Modifier.height(8.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { QuickAction("▤", "View Orders") { onOpen("orders") }; QuickAction("♧", "Send Notification") { onOpen("notifications") } } }
        item { SectionTitle("Recent orders", "View all", onAction = { onOpen("orders") }) }
        itemsIndexed(orders.take(4)) { index, order -> Surface(Modifier.fillMaxWidth().clickable { onOpen("order_detail/$index") }, shape = RoundedCornerShape(17.dp), color = Card, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Text("🛍️", fontSize = 22.sp); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(order.substringBefore(" · "), fontWeight = FontWeight.Bold, color = Ink); Text(order.substringAfter(" · "), fontSize = 12.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis) }; StatusChip(order.substringAfterLast(" · ")) } } }
    }
}

@Composable private fun StatCard(title: String, value: String, change: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) { Surface(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(19.dp), color = Card, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Column(Modifier.padding(14.dp)) { Text(title, color = Muted, fontSize = 12.sp); Text(value, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp)); Text(change, color = Green, fontSize = 11.sp) } } }
@Composable private fun RowScope.QuickAction(symbol: String, label: String, onClick: () -> Unit) { Surface(Modifier.weight(1f).height(66.dp).clickable(onClick = onClick), shape = RoundedCornerShape(15.dp), color = Card, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) { Text(symbol, color = Coral, fontSize = 21.sp); Spacer(Modifier.width(8.dp)); Text(label, fontSize = 12.sp, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 2) } } }
@Composable private fun SectionTitle(title: String, action: String, onAction: (() -> Unit)? = null) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(title, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink); Text(action, Modifier.clickable(enabled = onAction != null) { onAction?.invoke() }, color = Coral, fontSize = 12.sp) } }

@Composable private fun CatalogScreen(products: List<String>, categories: List<String>, onAdd: () -> Unit, onEdit: (Int) -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    val filtered = products.withIndex().filter { indexed ->
        indexed.value.contains(search, true) && when (filter) {
            "Low stock" -> indexed.value.contains("Low", true)
            "Out of stock" -> indexed.value.contains("Out of stock", true)
            "All" -> true
            else -> indexed.value.split(" · ").getOrNull(3) == filter
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        ScreenHeader("Products", "${products.size} items in the demo catalog", onAdd, "＋ Add")
        OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Search products") }, singleLine = true, shape = AppShapes.input)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(top = 6.dp)) {
            items((listOf("All", "Low stock", "Out of stock") + categories.map { it.substringBefore('|') }).distinct()) { chip -> FilterChip(filter == chip, { filter = chip }, { Text(chip) }) }
        }
        LazyColumn(contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (filtered.isEmpty()) item { EmptyState("No matching products", "Try changing your search or filters.", "Clear filters") { search = ""; filter = "All" } }
            items(filtered) { indexed ->
                val product = indexed.value
                DataRow("🍅", product.substringBefore(" · "), product.substringAfter(" · "), { onEdit(indexed.index) })
            }
        }
    }
}

@Composable private fun OrdersScreen(orders: List<String>, onOpen: (Int) -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    val filtered = orders.withIndex().filter { indexed -> indexed.value.contains(search, true) && (filter == "All" || indexed.value.contains(filter, true)) }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        ScreenHeader("Orders", "${orders.size} local demo orders")
        OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Search order or customer") }, singleLine = true, shape = AppShapes.input)
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 6.dp)) {
            listOf("All", "Preparing", "On the way", "Delivered").forEach { FilterChip(filter == it, { filter = it }, { Text(it, fontSize = 11.sp) }) }
        }
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
    onThemeChange: (String) -> Unit,
    onBackgroundChange: (String) -> Unit,
    onRadiusChange: (Float) -> Unit,
    onPreview: () -> Unit,
) {
    val names = listOf("RushIt Coral", "Sunrise", "Forest", "Ocean")
    val colors = listOf(Coral, Amber, Green, Color(0xFF4975C6))
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ScreenHeader("Theme & Colors", "Customize the local Home preview", onPreview, "Preview") }
        item { AdminCard {
            Text("Color palette", fontWeight = FontWeight.Bold)
            Text("Choose a coordinated brand palette", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                names.forEachIndexed { index, name ->
                    Column(Modifier.clickable { onThemeChange(name) }.padding(3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(colors[index]), contentAlignment = Alignment.Center) { if (themeName == name) Text("✓", color = Color.White, fontWeight = FontWeight.Bold) }
                        Text(name, fontSize = 10.sp, color = if (themeName == name) Coral else Muted)
                    }
                }
            }
        } }
        item { AdminCard {
            Text("Background style", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 8.dp)) { listOf("Default", "Gradient").forEach { FilterChip(backgroundStyle == it, { onBackgroundChange(it) }, { Text(it) }) } }
            Text("Applied live in Home Preview", color = Muted, fontSize = 12.sp)
        } }
        item { AdminCard {
            Text("Section corner radius · ${radius.toInt()}dp", fontWeight = FontWeight.Bold)
            Slider(value = radius, onValueChange = onRadiusChange, valueRange = 8f..28f)
            Text("Applied to the Home preview banner", color = Muted, fontSize = 12.sp)
        } }
        item { PrimaryButton("Preview theme", Modifier.fillMaxWidth(), onPreview) }
    }
}

@Composable private fun MoreScreen(onOpen: (String) -> Unit) { LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentPadding = PaddingValues(bottom = 20.dp)) { item { ScreenHeader("More tools", "Manage content and store operations") }; items(featureRoutes) { dest -> DataRow(dest.glyph, dest.label, "Local demo workspace", { onOpen(dest.route) }) } } }

@Composable private fun DesignerScreen(
    sections: MutableList<String>,
    onEdit: (Int) -> Unit,
    onSave: (String) -> Unit,
    onPreview: () -> Unit,
) {
    val baseline = remember { mutableStateListOf<String>().apply { addAll(sections) } }
    var dirty by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { ScreenHeader("Home Designer", "Configure a local customer Home preview") }
        item { DemoNotice() }
        if (sections.isEmpty()) item { EmptyState("No Home sections", "Add a supported section to start composing your preview.", "Add section") { showAdd = true } }
        items(sections.toList()) { row ->
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
                        val index = sections.indexOf(row)
                        if (index >= 0) sections[index] = row.substringBeforeLast('|') + "|" + if (checked) "Visible" else "Hidden"
                        dirty = true
                    })
                }
                Row {
                    TextButton(onClick = { onEdit(sections.indexOf(row)) }) { Text("Edit", color = Coral) }
                    TextButton(onClick = {
                        val index = sections.indexOf(row)
                        if (index > 0) { val moved = sections.removeAt(index); sections.add(index - 1, moved); dirty = true }
                    }) { Text("↑") }
                    TextButton(onClick = {
                        val index = sections.indexOf(row)
                        if (index in 0 until sections.lastIndex) { val moved = sections.removeAt(index); sections.add(index + 1, moved); dirty = true }
                    }) { Text("↓") }
                }
            }
        }
        item {
            OutlinedButton(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) { Text("＋ Add Section") }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPreview, modifier = Modifier.weight(1f)) { Text("Preview") }
                Button(onClick = { onSave("Draft saved on this device"); baseline.clear(); baseline.addAll(sections); dirty = false }, modifier = Modifier.weight(1f)) { Text("Save Draft") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { confirmDiscard = true }, modifier = Modifier.weight(1f)) { Text("Discard changes", color = Muted) }
                Button(onClick = { onSave("Published to local demo preview only"); baseline.clear(); baseline.addAll(sections); dirty = false }, modifier = Modifier.weight(1f)) { Text("Publish demo") }
            }
        }
    }
    if (showAdd) {
        val choices = listOf("Category Icons", "Category Selector", "Product Grid", "Product Shelf", "Bestsellers", "Recommended", "Featured Collections", "Promotional Banner", "Festival Collection", "Recently Viewed")
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add a Home section") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { choices.forEach { type -> TextButton(onClick = { sections.add("$type|$type|Visible"); dirty = true; showAdd = false }, modifier = Modifier.fillMaxWidth()) { Text(type, modifier = Modifier.fillMaxWidth(), color = Ink) } } } },
            confirmButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } },
        )
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard Home changes?") },
            text = { Text(if (dirty) "Unsaved edits will be replaced with the last saved local draft." else "Return to the last saved local draft?") },
            confirmButton = { TextButton(onClick = { sections.clear(); sections.addAll(baseline); dirty = false; confirmDiscard = false }) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } },
        )
    }
}

@Composable private fun SectionEditorScreen(section: String, onSave: (String) -> Unit, onCancel: () -> Unit) {
    val parts = remember(section) { section.split('|') }
    var title by remember(section) { mutableStateOf(parts.getOrNull(0).orEmpty()) }
    val type = parts.getOrNull(1).orEmpty().ifBlank { "Product shelf" }
    var layout by remember(section) { mutableStateOf(parts.getOrNull(2) ?: "Carousel") }
    var card by remember(section) { mutableStateOf(parts.getOrNull(3) ?: "Soft") }
    var animation by remember(section) { mutableStateOf(parts.getOrNull(4) ?: "Fade") }
    var visible by remember(section) { mutableStateOf(section.endsWith("Visible")) }
    var error by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item { ScreenHeader("Edit Section", type) }
        item { AdminCard {
            Text("Content", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(8.dp))
            AdminField("Section title", title, { title = it })
            Spacer(Modifier.height(8.dp))
            Text("Section type · $type", color = Muted, fontSize = 13.sp)
            ToggleRow("Visible on Home preview", visible) { visible = it }
        } }
        item { AdminCard {
            Text("Layout", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("Carousel", "Grid").forEach { FilterChip(layout == it, { layout = it }, { Text(it) }) } }
            Text("Card surface", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("Soft", "Glass", "Elevated").forEach { FilterChip(card == it, { card = it }, { Text(it) }) } }
        } }
        item { AdminCard {
            Text("Motion preset", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("None", "Fade", "Slide").forEach { FilterChip(animation == it, { animation = it }, { Text(it) }) } }
            Text("Lightweight preview transitions only.", color = Muted, fontSize = 12.sp)
        } }
        item {
            if (error.isNotBlank()) Text(error, color = Coral)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(onClick = {
                    if (title.isBlank()) error = "Add a title for this section."
                    else onSave(listOf(title.trim(), type, layout, card, animation, if (visible) "Visible" else "Hidden").joinToString("|"))
                }, modifier = Modifier.weight(1f)) { Text("Save Changes") }
            }
        }
    }
}

@Composable private fun PreviewScreen(sections: List<String>, themeName: String, backgroundStyle: String, sectionRadius: Float, onBack: () -> Unit, onPublish: () -> Unit) {
    val visibleSections = sections.filter { it.endsWith("|Visible") }
    val previewColor = when (themeName) { "Sunrise" -> Amber; "Forest" -> Green; "Ocean" -> Color(0xFF4975C6); else -> Coral }
    val previewBackground = if (backgroundStyle == "Gradient") {
        Modifier.background(Brush.verticalGradient(listOf(Color(0xFFFFEEE9), Cream)))
    } else {
        Modifier.background(Cream)
    }
    LazyColumn(Modifier.fillMaxSize().then(previewBackground).padding(horizontal = 18.dp), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader("Home Preview", "Local preview · not published to the User app") }
        item { DemoNotice() }
        item { AdminCard {
            Text("RushIt", color = Coral, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            Text("Good morning · Patna", color = Muted)
            Spacer(Modifier.height(12.dp))
            Text("⌕   Search groceries and products…", Modifier.fillMaxWidth().background(Color(0xFFF6F1EA), RoundedCornerShape(14.dp)).padding(14.dp), color = Muted)
        } }
        if (visibleSections.isEmpty()) item { EmptyState("No visible sections", "Enable or add a section in Home Designer to see it here.") }
        items(visibleSections) { section ->
            val parts = section.split('|')
            val title = parts.firstOrNull().orEmpty()
            val motion = parts.getOrNull(4) ?: "Fade"
            val layout = parts.getOrNull(2) ?: "Carousel"
            val cardSurface = when (parts.getOrNull(3)) { "Glass" -> Color.White.copy(alpha = .82f); "Elevated" -> Color.White; else -> Color(0xFFFAF7F1) }
            var entered by remember(section) { mutableStateOf(motion == "None") }
            LaunchedEffect(section) { if (motion != "None") { delay(40); entered = true } }
            AnimatedVisibility(visible = entered, enter = when (motion) { "Slide" -> slideInVertically { it / 8 }; "Fade" -> fadeIn(); else -> EnterTransition.None }) {
            when {
                title.contains("Banner", true) || title.contains("Festival", true) || title.contains("Promo", true) -> Surface(shape = RoundedCornerShape(sectionRadius.dp), color = previewColor) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Text(title, color = Color.White.copy(alpha = .82f), fontSize = 12.sp)
                        Text("Fresh Groceries Delivered Fast", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("Demo promotion · Preview only", color = Color.White.copy(alpha = .84f), fontSize = 12.sp)
                    }
                }
                title.contains("Categor", true) -> AdminCard(containerColor = cardSurface, radius = sectionRadius.dp) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(10.dp))
                    val categoriesPreview = listOf("🍎\nFruits", "🥬\nVeggies", "🥛\nDairy", "🍿\nSnacks")
                    if (layout == "Grid") Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        categoriesPreview.chunked(2).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { row.forEach { Text(it, Modifier.weight(1f).background(Color(0xFFFFF4EE), RoundedCornerShape(12.dp)).padding(10.dp), color = Ink, fontSize = 12.sp) } } }
                    } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        categoriesPreview.forEach { Text(it, Modifier.background(Color(0xFFFFF4EE), RoundedCornerShape(12.dp)).padding(10.dp), color = Ink, fontSize = 12.sp) }
                    }
                }
                else -> AdminCard(containerColor = cardSurface, radius = sectionRadius.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("Curated RushIt picks · $layout", color = Muted, fontSize = 12.sp) }
                        Text("See all  ›", color = Coral, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    val goods = listOf("🍅 Tomatoes\n₹24/kg", "🍌 Banana\n₹32/kg", "🥛 Milk\n₹56/L")
                    if (layout == "Grid") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            goods.chunked(2).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { PreviewProductCard(it, Modifier.weight(1f)) }
                                    if (row.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        goods.forEach { PreviewProductCard(it, Modifier.weight(1f)) }
                    }
                }
            }
            }
        }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Return to editor") }
            Button(onClick = onPublish, modifier = Modifier.weight(1f)) { Text("Publish demo") }
        } }
    }
}

@Composable private fun ListFeature(title: String, rows: List<String>, action: String, onAction: () -> Unit, onItem: (String) -> Unit) { Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) { ScreenHeader(title, "Prototype data · local only", if (action.isNotBlank()) onAction else null, action); LazyColumn(contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(rows) { DataRow("✦", it, "Demo record · not published", { onItem(it) }) } } } }


@Composable private fun ProductForm(product: String? = null, categories: List<String> = initialCategories, onSave: (String) -> Unit) {
    val original = remember(product) { product.orEmpty().split(" · ") }
    var name by rememberSaveable(product) { mutableStateOf(original.getOrNull(0).orEmpty()) }
    var price by rememberSaveable(product) { mutableStateOf(Regex("[0-9]+(?:\\.[0-9]+)?").find(original.getOrNull(1).orEmpty())?.value.orEmpty()) }
    var unit by rememberSaveable(product) { mutableStateOf(original.getOrNull(1)?.substringAfter('/', "unit") ?: "kg") }
    var stock by rememberSaveable(product) { mutableStateOf(if (original.getOrNull(2)?.contains("Low", true) == true) "3" else if (original.getOrNull(2)?.contains("Out", true) == true) "0" else "10") }
    var description by rememberSaveable(product) { mutableStateOf("") }
    var category by rememberSaveable(product) { mutableStateOf(original.getOrNull(3) ?: "Vegetables") }
    var visible by rememberSaveable(product) { mutableStateOf(original.getOrNull(4) != "Hidden") }
    var saved by rememberSaveable(product) { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenHeader(if (product == null) "Add Product" else "Edit Product", "Saved in the local demo catalog") }
        item { AdminCard {
            Text("Product image", fontWeight = FontWeight.Bold)
            Box(Modifier.fillMaxWidth().height(120.dp).padding(top = 8.dp).clip(RoundedCornerShape(15.dp)).background(Color(0xFFFFF0E9)), contentAlignment = Alignment.Center) { Text("🍅\nImage picker not connected in demo", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Muted) }
        } }
        item { AdminField("Product name", name, { name = it }) }
        item { AdminField("Description", description, { description = it }) }
        item { AdminField("Price (₹)", price, { price = it }, KeyboardType.Decimal) }
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
        item { Button(onClick = {
            if (saved) return@Button
            val parsedPrice = price.toDoubleOrNull()
            val count = stock.toIntOrNull()
            error = when {
                name.isBlank() -> "Product name is required."
                parsedPrice == null || parsedPrice <= 0 -> "Enter a price greater than zero."
                count == null || count < 0 -> "Stock must be a non-negative whole number."
                else -> ""
            }
            if (error.isBlank()) {
                val validCount = count ?: return@Button
                val validPrice = parsedPrice ?: return@Button
                val availability = if (validCount == 0) "Out of stock" else if (validCount < 5) "Low stock" else "In stock"
                saved = true
                onSave("${name.trim()} · ₹${validPrice.toInt()}/$unit · $availability · $category · ${if (visible) "Visible" else "Hidden"}")
            }
        }, enabled = !saved, modifier = Modifier.fillMaxWidth(), shape = AppShapes.button, colors = ButtonDefaults.buttonColors(containerColor = Coral)) { Text(if (product == null) "Save Product" else "Save Changes") } }
    }
}

@Composable private fun OrderDetailScreen(order: String, onUpdate: (String) -> Unit) {
    val statuses = listOf("Confirmed", "Preparing", "Rider Ready", "Picked Up", "On the way", "Almost there", "Delivered")
    var status by rememberSaveable(order) { mutableStateOf(order.substringAfterLast(" · ")) }
    var confirm by remember { mutableStateOf(false) }
    var cancelling by remember { mutableStateOf(false) }
    val amount = Regex("₹(\\d+)").find(order)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 136
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader(order.substringBefore(" · "), "10 Oct 2026 · Demo order") }
        item { DemoNotice() }
        item { AdminCard { Text("Customer", fontWeight = FontWeight.Bold); Text(order.split(" · ").getOrNull(1) ?: "Demo customer", color = Muted); Text("Delivery address · Patna, Bihar 800001", color = Muted) } }
        item { AdminCard {
            Text("Items", fontWeight = FontWeight.Bold)
            OrderLine("Demo grocery basket", "1", "₹$amount")
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            OrderLine("Subtotal", "", "₹$amount")
            OrderLine("Delivery charge", "", "₹0")
            OrderLine("Total", "", "₹$amount", bold = true)
        } }
        item { AdminCard { Text("Payment · Demo only", fontWeight = FontWeight.Bold); Text("No payment was processed", color = Muted) } }
        item { AdminCard {
            Text("Status history", fontWeight = FontWeight.Bold)
            Text("Confirmed  →  Preparing  →  On the way  →  Delivered", color = Muted)
            Text("Current: $status", color = Coral, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
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

@Composable private fun OrderLine(label: String, quantity: String, price: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(quantity, Modifier.weight(.7f), color = Muted)
        Text(price, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
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
    var title by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var audience by rememberSaveable { mutableStateOf("All Demo Customers") }
    var error by remember { mutableStateOf("") }
    val history = remember(prefs) { mutableStateListOf(*(prefs.getString("notifications", "Weekend fresh picks · Demo preview only")?.split("##")?.filter { it.isNotBlank() }?.toTypedArray() ?: emptyArray())) }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), contentPadding = PaddingValues(bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenHeader("Notifications", "Compose a local demo message") }
        item { DemoNotice() }
        item { AdminField("Notification title", title, { title = it }) }
        item { OutlinedTextField(message, { message = it }, Modifier.fillMaxWidth().heightIn(min = 110.dp), label = { Text("Message") }, shape = AppShapes.input) }
        item { Text("Audience", fontWeight = FontWeight.Bold) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("All Demo Customers", "Selected Segment").forEach { FilterChip(audience == it, { audience = it }, { Text(it, fontSize = 11.sp) }) }
        } }
        item { Text("No push provider connected. Sending records a local preview only.", color = Muted, fontSize = 12.sp) }
        item { if (error.isNotBlank()) Text(error, color = Coral); PrimaryButton("Send Demo Notification", Modifier.fillMaxWidth()) {
            error = when { title.isBlank() -> "Add a notification title."; message.isBlank() -> "Add a notification message."; else -> "" }
            if (error.isBlank()) {
                val record = "$title · $audience · Demo send · Not delivered"
                history.add(0, record)
                prefs.edit().putString("notifications", history.take(30).joinToString("##")).apply()
                title = ""; message = ""
                onSend()
            }
        } }
        item { SectionTitle("Local send history", "${history.size} records") }
        if (history.isEmpty()) item { EmptyState("No sends yet", "Your demo notification history will appear here.") }
        items(history) { DataRow("♧", it, "Not delivered to real devices") }
    }
}

@Composable private fun SettingsScreen(prefs: android.content.SharedPreferences, themeName: String, onReset: () -> Unit) {
    var maintenance by remember(prefs) { mutableStateOf(prefs.getBoolean("maintenance", false)) }
    var sound by remember(prefs) { mutableStateOf(prefs.getBoolean("sound", true)) }
    var animations by remember(prefs) { mutableStateOf(prefs.getBoolean("animations", true)) }
    var confirmReset by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenHeader("App Settings", "Local demo preferences") }
        item { AdminCard {
            Text("General", fontWeight = FontWeight.Bold)
            Text("App name · RushIt", color = Muted, modifier = Modifier.padding(top = 7.dp))
            Text("Support · support@rushit.in", color = Muted)
            Text("Theme · $themeName", color = Muted)
            ToggleRow("Maintenance mode · demo", maintenance) { maintenance = it; prefs.edit().putBoolean("maintenance", it).apply() }
        } }
        item { AdminCard {
            Text("Preferences", fontWeight = FontWeight.Bold)
            ToggleRow("Sound effects", sound) { sound = it; prefs.edit().putBoolean("sound", it).apply() }
            ToggleRow("Animations", animations) { animations = it; prefs.edit().putBoolean("animations", it).apply() }
        } }
        item { AdminCard {
            Text("Information", fontWeight = FontWeight.Bold)
            Text("Minimum supported version · 1.0", color = Muted, modifier = Modifier.padding(top = 7.dp))
            Text("App version · 1.0 demo prototype", color = Muted)
            Text("Privacy policy and Terms are not connected", color = Muted)
        } }
        item { OutlinedButton(onClick = { confirmReset = true }) { Text("Reset demo data", color = Coral) } }
    }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("Reset local demo data?") },
        text = { Text("Saved catalog, sections, theme, and preferences on this device will be reset.") },
        confirmButton = { TextButton(onClick = { onReset(); confirmReset = false }) { Text("Reset") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
    )
}

@Composable private fun ProfileScreen(prefs: android.content.SharedPreferences, onLogout: () -> Unit) {
    var name by remember(prefs) { mutableStateOf(prefs.getString("admin_name", "Dharmjeet Singh") ?: "Dharmjeet Singh") }
    var email by remember(prefs) { mutableStateOf(prefs.getString("admin_email", "admin@rushit.in") ?: "admin@rushit.in") }
    var role by remember(prefs) { mutableStateOf(prefs.getString("admin_role", "Super Admin") ?: "Super Admin") }
    var edit by remember { mutableStateOf(false) }
    var passwordInfo by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenHeader("Admin Profile", "Demo account · local only") }
        item { AdminCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFFFE8E2)), contentAlignment = Alignment.Center) { Text("DS", color = Coral, fontWeight = FontWeight.Bold, fontSize = 19.sp) }
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
        item { OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Log out") } }
    }
    if (edit) {
        var draftName by remember(name) { mutableStateOf(name) }
        var draftEmail by remember(email) { mutableStateOf(email) }
        AlertDialog(onDismissRequest = { edit = false }, title = { Text("Edit demo profile") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { AdminField("Display name", draftName, { draftName = it }); AdminField("Demo email", draftEmail, { draftEmail = it }, KeyboardType.Email) }
        }, confirmButton = { TextButton(onClick = {
            if (draftName.isNotBlank() && draftEmail.contains('@')) {
                name = draftName.trim(); email = draftEmail.trim()
                prefs.edit().putString("admin_name", name).putString("admin_email", email).apply()
                edit = false
            }
        }) { Text("Save") } }, dismissButton = { TextButton(onClick = { edit = false }) { Text("Cancel") } })
    }
    if (passwordInfo) AlertDialog(onDismissRequest = { passwordInfo = false }, title = { Text("Password settings") }, text = { Text("Password changes are not available in this offline demo. No password is stored or changed.") }, confirmButton = { TextButton(onClick = { passwordInfo = false }) { Text("Got it") } })
}

@Composable private fun ListFeatureInline(title: String, desc: String) { DataRow("•", title, desc, {}) }
@Composable fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f)); Switch(value, onChange) } }
@Composable fun ScreenHeader(title: String, subtitle: String = "", onAction: (() -> Unit)? = null, action: String = "＋") {
    val nav = LocalAdminNavigation.current
    val route = nav?.currentBackStackEntry?.destination?.route
    val showBack = route != null && route !in setOf("home", "catalog", "orders", "themes", "more", "login", "splash")
    Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (showBack) TextButton(onClick = { nav?.popBackStack() }, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("‹", color = Coral, fontSize = 27.sp) }
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

@Composable private fun PreviewProductCard(label: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Color.White, RoundedCornerShape(12.dp)).padding(10.dp)) {
        Text(label.take(2), fontSize = 23.sp)
        Text(label.substringAfter('\n'), color = Muted, fontSize = 11.sp)
    }
}
@Composable private fun DataRow(glyph: String, title: String, detail: String, onClick: () -> Unit = {}) { Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), color = Card, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFEEE9)), contentAlignment = Alignment.Center) { Text(glyph, color = Coral, fontWeight = FontWeight.Bold) }; Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(detail, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }; Text("›", color = Muted, fontSize = 22.sp) } } }
@Composable private fun StatusChip(status: String) { val color = if (status.contains("Delivered", true)) Green else if (status.contains("Cancelled", true)) Muted else Coral; Text(status, Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = .10f)).padding(horizontal = 9.dp, vertical = 5.dp), color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
@Composable fun DemoNotice() { Text("DEMO · This configuration stays in RushIt Admin on this device; it does not update the customer app or production services.", Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Color(0xFFFFF4DC)).padding(12.dp), color = Ink, fontSize = 12.sp) }
@Composable private fun FilterChip(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit, modifier: Modifier = Modifier) { androidx.compose.material3.FilterChip(selected = selected, onClick = onClick, label = label, modifier = modifier) }
