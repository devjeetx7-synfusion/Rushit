package com.rushit.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

private val Coral = Color(0xFFE7473C)
private val CoralSoft = Color(0xFFF47B6C)
private val Amber = Color(0xFFFFB84D)
private val Cream = Color(0xFFFFFDF7)
private val Ink = Color(0xFF292524)
private val Muted = Color(0xFF78716C)
private val Green = Color(0xFF27845B)
private val Line = Color(0xFFEDE8DF)
private val Card = Color.White

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(255, 253, 247)
        window.navigationBarColor = android.graphics.Color.rgb(255, 253, 247)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContent { AdminTheme { AdminRoot() } }
    }
}

@Composable private fun AdminTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Coral, onPrimary = Color.White, secondary = Amber, background = Cream, onBackground = Ink, surface = Card, onSurface = Ink, surfaceVariant = Color(0xFFF6F1EA), onSurfaceVariant = Muted, outline = Line, error = Color(0xFFB42318)), content = content)
}

private data class Destination(val route: String, val label: String, val glyph: String)
private val tabs = listOf(Destination("home", "Home", "⌂"), Destination("catalog", "Catalog", "▦"), Destination("orders", "Orders", "▤"), Destination("themes", "Themes", "◈"), Destination("more", "More", "•••"))
private val featureRoutes = listOf(
    Destination("designer", "Home Designer", "⌘"), Destination("section", "Section Editor", "▧"), Destination("preview", "Home Preview", "◉"),
    Destination("festivals", "Festival Manager", "✦"), Destination("festival_edit", "Festival Editor", "✎"), Destination("schedule", "Campaign Scheduler", "◷"),
    Destination("product_edit", "Add / Edit Product", "+"), Destination("categories", "Category Management", "▥"), Destination("order_detail", "Order Details", "▣"),
    Destination("customers", "Customer Management", "♙"), Destination("offers", "Offers & Coupons", "%"), Destination("notifications", "Notifications Manager", "♧"),
    Destination("settings", "App Settings", "⚙"), Destination("profile", "Admin Profile & Security", "♙")
)

private val initialProducts = listOf("Tomato · ₹24/kg · In stock", "Banana · ₹32/kg · In stock", "Milk · ₹56/L · In stock", "Bread · ₹32 · Low stock", "Apple · ₹120/kg · In stock")
private val initialOrders = listOf("#RI172805456  ·  Ankit Kumar  ·  ₹542  ·  Preparing", "#RI172805455  ·  Priya Singh  ·  ₹386  ·  On the way", "#RI172805454  ·  Rohit Sharma  ·  ₹724  ·  Delivered", "#RI172805453  ·  Sneha Verma  ·  ₹218  ·  Cancelled")

@Composable private fun AdminRoot() {
    val nav = rememberNavController()
    val back by nav.currentBackStackEntryAsState()
    val route = back?.destination?.route ?: "login"
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("rushit_admin_demo", android.content.Context.MODE_PRIVATE) }
    val products = remember(preferences) { mutableStateListOf(*(preferences.getString("products", null)?.split("##")?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() } ?: initialProducts).toTypedArray()) }
    val orders = remember { mutableStateListOf(*initialOrders.toTypedArray()) }
    val sections = remember(preferences) { mutableStateListOf(*(preferences.getString("sections", null)?.split("##")?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() } ?: listOf("Top Banner|Festival Deals|Visible", "Category Icons|8 categories|Visible", "Flash Deals|Horizontal products|Visible", "Best Selling|Product grid|Visible", "Recommended|Personalized|Visible")).toTypedArray()) }
    var snackbar by remember { mutableStateOf("") }
    val snackbarState = remember { SnackbarHostState() }
    LaunchedEffect(snackbar) { if (snackbar.isNotBlank()) { snackbarState.showSnackbar(snackbar); snackbar = "" } }
    val showTabs = route !in setOf("login")
    Scaffold(containerColor = Cream, snackbarHost = { SnackbarHost(snackbarState) }, bottomBar = {
        if (showTabs) NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
            tabs.forEach { dest -> NavigationBarItem(selected = back?.destination?.hierarchy?.any { it.route == dest.route } == true, onClick = { nav.navigate(dest.route) { launchSingleTop = true; restoreState = true } }, icon = { Text(dest.glyph, fontSize = 20.sp, color = if (route == dest.route) Coral else Muted) }, label = { Text(dest.label, fontSize = 10.sp) }) }
        }
    }) { padding ->
        NavHost(navController = nav, startDestination = "login", modifier = Modifier.padding(padding)) {
            composable("login") { LoginScreen(onLogin = { nav.navigate("home") { popUpTo("login") { inclusive = true } } }) }
            composable("home") { Dashboard(products.size, orders, onOpen = { nav.navigate(it) }) }
            composable("catalog") { CatalogScreen(products, onAdd = { nav.navigate("product_edit") }, onSave = { products.add(it); preferences.edit().putString("products", products.joinToString("##")).apply(); snackbar = "Product saved to this device" }) }
            composable("orders") { OrdersScreen(orders, onOpen = { nav.navigate("order_detail") }) }
            composable("themes") { ThemeScreen(onPreview = { nav.navigate("preview") }) }
            composable("more") { MoreScreen(onOpen = { nav.navigate(it) }) }
            composable("designer") { DesignerScreen(sections, onEdit = { nav.navigate("section") }, onSave = { preferences.edit().putString("sections", sections.joinToString("##")).apply(); snackbar = it }, onPreview = { nav.navigate("preview") }) }
            composable("section") { FeatureForm("Edit Home Section", listOf("Section title", "Subtitle", "Layout", "Background", "Animation preset"), onSave = { snackbar = "Section draft saved locally" }) }
            composable("preview") { PreviewScreen(onBack = { nav.popBackStack() }, onPublish = { snackbar = "Published to local demo preview only" }) }
            composable("festivals") { ListFeature("Festival Manager", listOf("Diwali · 10–20 Nov · Active", "Holi · 1–10 Mar · Scheduled", "Christmas · 20–26 Dec · Inactive", "Eid · Draft campaign", "Valentine's Day · Inactive"), action = "Add Festival", onAction = { nav.navigate("festival_edit") }, onItem = { nav.navigate("festival_edit") }) }
            composable("festival_edit") { FeatureForm("Festival Editor", listOf("Festival name", "Description", "Start date", "End date", "Priority"), onSave = { snackbar = "Festival draft saved locally" }) }
            composable("schedule") { ListFeature("Campaign Scheduler", listOf("Diwali · Scheduled · Priority 1", "Holi · Draft · Priority 2", "Fallback theme · Default Home"), action = "Schedule Campaign", onAction = { nav.navigate("festival_edit") }, onItem = { nav.navigate("festival_edit") }) }
            composable("product_edit") { ProductForm(onSave = { products.add(it); preferences.edit().putString("products", products.joinToString("##")).apply(); snackbar = "Product saved locally"; nav.popBackStack() }) }
            composable("categories") { ListFeature("Categories", listOf("🍎 Fruits · 12 products", "🥬 Vegetables · 28 products", "🥛 Dairy · 16 products", "🍿 Snacks · 22 products", "🥤 Beverages · 10 products", "🧴 Personal Care · 16 products"), action = "Add Category", onAction = { snackbar = "Category editor is ready for demo changes" }, onItem = { snackbar = "Category opened · demo mode" }) }
            composable("order_detail") { OrderDetailScreen(onUpdate = { snackbar = "Order status updated in local demo" }) }
            composable("customers") { ListFeature("Customers", listOf("Ankit Kumar · Patna · 5 orders", "Priya Singh · Delhi · 12 orders", "Rohit Sharma · Mumbai · 8 orders", "Sneha Verma · Patna · 3 orders", "Aman Gupta · Bengaluru · 6 orders"), action = "", onAction = {}, onItem = { nav.navigate("order_detail") }) }
            composable("offers") { ListFeature("Offers & Coupons", listOf("WELCOME10 · 10% off · Active", "SOMAI50 · ₹50 off · Scheduled", "FREESHIP · Free delivery · Active", "FIRSTORDER · 20% off · Draft"), action = "Create Offer", onAction = { nav.navigate("section") }, onItem = { nav.navigate("section") }) }
            composable("notifications") { NotificationScreen(onSend = { snackbar = "Demo notification added to local send history · not delivered" }) }
            composable("settings") { SettingsScreen(onReset = { snackbar = "Demo data reset is available from app restart" }) }
            composable("profile") { ProfileScreen(onLogout = { nav.navigate("login") { popUpTo("home") { inclusive = true }; launchSingleTop = true } }) }
        }
    }
}

@Composable private fun LoginScreen(onLogin: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }; var password by rememberSaveable { mutableStateOf("") }; var reveal by remember { mutableStateOf(false) }; var error by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFFE8E2), Cream, Cream))).padding(horizontal = 26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(72.dp).clip(RoundedCornerShape(24.dp)).background(Coral), contentAlignment = Alignment.Center) { Text("🛒", fontSize = 33.sp) }
        Spacer(Modifier.height(18.dp)); Text("RushIt Admin", fontSize = 29.sp, fontWeight = FontWeight.ExtraBold, color = Coral)
        Text("Welcome back!", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(top = 26.dp))
        Text("Sign in to manage your store", color = Muted, modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))
        AdminField("Email or phone", email, { email = it }, keyboard = KeyboardType.Email)
        Spacer(Modifier.height(12.dp)); OutlinedTextField(value = password, onValueChange = { password = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Password") }, singleLine = true, shape = RoundedCornerShape(16.dp), visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { TextButton(onClick = { reveal = !reveal }) { Text(if (reveal) "Hide" else "Show", color = Coral) } })
        if (error.isNotBlank()) Text(error, color = Coral, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(18.dp)); PrimaryButton("Sign in · Demo mode", Modifier.fillMaxWidth()) { if (email.isBlank() || password.isBlank()) error = "Enter any demo email and password to continue" else onLogin() }
        Text("Prototype only. This demo sign-in is not production authentication.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 18.dp))
        Text("Only authorized administrators can access this area.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 14.dp))
    }
}

@Composable private fun Dashboard(productCount: Int, orders: List<String>, onOpen: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("RushIt Admin", color = Coral, fontSize = 13.sp, fontWeight = FontWeight.Bold); Text("Good morning, Admin", color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold); Text("Your store at a glance · Demo data", color = Muted, fontSize = 13.sp) }; Box(Modifier.size(46.dp).clip(CircleShape).background(Color(0xFFFFE8E2)), contentAlignment = Alignment.Center) { Text("🔔") } } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCard("Today's Orders", "${orders.size}", "+8%", Modifier.weight(1f)); StatCard("Total Orders", "${orders.size}", "Local demo", Modifier.weight(1f)) } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCard("Products", productCount.toString(), "Live", Modifier.weight(1f)); StatCard("Customers", "5", "Demo", Modifier.weight(1f)) } }
        item { Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = Coral) { Column(Modifier.padding(18.dp)) { Text("TODAY'S SALES", color = Color.White.copy(alpha = .8f), fontSize = 11.sp, fontWeight = FontWeight.Bold); Text("₹28,450", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold); Text("Illustrative demo figures", color = Color.White.copy(alpha = .8f), fontSize = 12.sp); Spacer(Modifier.height(10.dp)); Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) { listOf(.34f, .58f, .43f, .87f, .64f, .94f, .56f).forEach { h -> Box(Modifier.width(18.dp).fillMaxHeight(h).clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)).background(Color.White.copy(alpha = .55f))) } } } } }
        item { SectionTitle("Quick actions", "Shortcuts"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { QuickAction("＋", "Add Product") { onOpen("product_edit") }; QuickAction("⌘", "Home Designer") { onOpen("designer") } }; Spacer(Modifier.height(8.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { QuickAction("▤", "View Orders") { onOpen("orders") }; QuickAction("♧", "Send Notification") { onOpen("notifications") } } }
        item { SectionTitle("Recent orders", "View all") }
        items(orders.take(4)) { order -> Surface(Modifier.fillMaxWidth().clickable { onOpen("order_detail") }, shape = RoundedCornerShape(17.dp), color = Card, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Text("🛍️", fontSize = 22.sp); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(order.substringBefore(" · "), fontWeight = FontWeight.Bold, color = Ink); Text(order.substringAfter(" · "), fontSize = 12.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis) }; StatusChip(order.substringAfterLast(" · ")) } } }
    }
}

@Composable private fun StatCard(title: String, value: String, change: String, modifier: Modifier = Modifier) { Surface(modifier, shape = RoundedCornerShape(19.dp), color = Card, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Column(Modifier.padding(14.dp)) { Text(title, color = Muted, fontSize = 12.sp); Text(value, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp)); Text(change, color = Green, fontSize = 11.sp) } } }
@Composable private fun RowScope.QuickAction(symbol: String, label: String, onClick: () -> Unit) { Surface(Modifier.weight(1f).height(66.dp).clickable(onClick = onClick), shape = RoundedCornerShape(15.dp), color = Card, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) { Text(symbol, color = Coral, fontSize = 21.sp); Spacer(Modifier.width(8.dp)); Text(label, fontSize = 12.sp, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 2) } } }
@Composable private fun SectionTitle(title: String, action: String) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(title, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink); Text(action, color = Coral, fontSize = 12.sp) } }

@Composable private fun CatalogScreen(products: List<String>, onAdd: () -> Unit, onSave: (String) -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }; var filter by rememberSaveable { mutableStateOf("All") }; var showForm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) { ScreenHeader("Products", "${products.size} items in demo catalog", onAdd); OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Search products") }, singleLine = true, shape = RoundedCornerShape(14.dp)); Spacer(Modifier.height(8.dp)); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("All", "Active", "Low stock").forEach { FilterChip(selected = filter == it, onClick = { filter = it }, label = { Text(it) }) } }; LazyColumn(contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(products.filter { it.contains(search, true) && (filter != "Low stock" || it.contains("Low", true)) }) { item -> DataRow("🍅", item, "Available · Demo catalog", onClick = { showForm = true }) } } }
    if (showForm) ProductDialog(onDismiss = { showForm = false }, onSave = { onSave(it); showForm = false })
}

@Composable private fun OrdersScreen(orders: List<String>, onOpen: () -> Unit) { var search by rememberSaveable { mutableStateOf("") }; var filter by rememberSaveable { mutableStateOf("All") }; Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) { ScreenHeader("Orders", "${orders.size} local demo orders"); OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Search order or customer") }, singleLine = true, shape = RoundedCornerShape(14.dp)); Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("All", "Preparing", "On the way", "Delivered").forEach { FilterChip(selected = filter == it, onClick = { filter = it }, label = { Text(it, fontSize = 11.sp) }) } }; LazyColumn(contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(orders.filter { it.contains(search, true) && (filter == "All" || it.contains(filter, true)) }) { DataRow("🛍️", it, "Demo order · payment not processed", onOpen) } } } }

@Composable private fun ThemeScreen(onPreview: () -> Unit) { var selected by rememberSaveable { mutableStateOf("RushIt Coral") }; var radius by rememberSaveable { mutableFloatStateOf(16f) }; LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { ScreenHeader("Theme & Colors", "Customize the local preview", onPreview) }; item { AdminCard { Text("Color palette", fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp)); Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { listOf("RushIt Coral", "Sunrise", "Forest", "Ocean").forEachIndexed { i, title -> Column(Modifier.clickable { selected = title }, horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(42.dp).clip(CircleShape).background(listOf(Coral, Amber, Green, Color(0xFF4975C6))[i])); Text(if (selected == title) "✓" else "", color = Coral); Text(title, fontSize = 10.sp) } } } }; item { AdminCard { Text("Background style", fontWeight = FontWeight.Bold); Row { listOf("Default", "Gradient", "Image").forEach { FilterChip(selected = selected == it, onClick = { selected = it }, label = { Text(it) }, modifier = Modifier.padding(end = 4.dp)) } }; Text("Available styles are applied to this prototype preview only.", color = Muted, fontSize = 12.sp) } }; item { AdminCard { Text("Section radius · ${radius.toInt()}dp", fontWeight = FontWeight.Bold); Slider(value = radius, onValueChange = { radius = it }, valueRange = 8f..28f) } }; item { PrimaryButton("Preview theme", Modifier.fillMaxWidth(), onPreview) } } }

@Composable private fun MoreScreen(onOpen: (String) -> Unit) { LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentPadding = PaddingValues(bottom = 20.dp)) { item { ScreenHeader("More tools", "Manage content and store operations") }; items(featureRoutes) { dest -> DataRow(dest.glyph, dest.label, "Local demo workspace", { onOpen(dest.route) }) } } }

@Composable private fun DesignerScreen(sections: MutableList<String>, onEdit: () -> Unit, onSave: (String) -> Unit, onPreview: () -> Unit) { var dirty by remember { mutableStateOf(false) }; LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { item { ScreenHeader("Home Designer", "Configure a local User Home preview") }; item { DemoNotice() }; items(sections.toList()) { row -> var visible by remember(row) { mutableStateOf(row.endsWith("Visible")) }; AdminCard { Row(verticalAlignment = Alignment.CenterVertically) { Text("☷", color = Coral, fontSize = 20.sp); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(row.substringBefore('|'), fontWeight = FontWeight.Bold); Text(row.substringAfter('|').substringBefore('|'), color = Muted, fontSize = 12.sp) }; Switch(visible, { visible = it; dirty = true }) }; Row { TextButton(onClick = onEdit) { Text("Edit", color = Coral) }; TextButton(onClick = { val i = sections.indexOf(row); if (i > 0) { val item = sections.removeAt(i); sections.add(i - 1, item); dirty = true } }) { Text("↑") }; TextButton(onClick = { val i = sections.indexOf(row); if (i in 0 until sections.lastIndex) { val item = sections.removeAt(i); sections.add(i + 1, item); dirty = true } }) { Text("↓") } } } }; item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { sections.add("New section|Product shelf|Visible"); dirty = true }, modifier = Modifier.weight(1f)) { Text("＋ Add Section") }; Button(onClick = { onSave("Draft saved on this device") }, modifier = Modifier.weight(1f)) { Text("Save Draft") } } }; item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = onPreview, modifier = Modifier.weight(1f)) { Text("Preview") }; Button(onClick = { onSave("Published to local demo preview only"); dirty = false }, modifier = Modifier.weight(1f)) { Text("Publish") } } }; item { TextButton(onClick = { if (dirty) onSave("Changes discarded from this preview"); dirty = false }) { Text("Discard changes") } } } }

@Composable private fun PreviewScreen(onBack: () -> Unit, onPublish: () -> Unit) { LazyColumn(Modifier.fillMaxSize().padding(20.dp), contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { ScreenHeader("Home Preview", "Local prototype · no changes to User app", onBack) }; item { AdminCard { Text("RushIt", color = Coral, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold); Text("Good morning · Patna", color = Muted); Spacer(Modifier.height(12.dp)); Text("Search groceries, products…", Modifier.fillMaxWidth().background(Color(0xFFF6F1EA), RoundedCornerShape(14.dp)).padding(14.dp), color = Muted) } }; item { Text("Shop by Category", fontWeight = FontWeight.Bold); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { listOf("🍎\nFruits", "🥬\nVeggies", "🥛\nDairy", "🍿\nSnacks").forEach { Text(it, Modifier.background(Color.White, RoundedCornerShape(14.dp)).padding(12.dp), color = Ink) } } }; item { Surface(shape = RoundedCornerShape(20.dp), color = Coral) { Column(Modifier.padding(18.dp)) { Text("Fresh Groceries", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("Delivered fast · Demo campaign", color = Color.White) } } }; item { Text("Flash Deals", fontWeight = FontWeight.Bold) }; item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Tomatoes\n₹24/kg", "Banana\n₹32/kg", "Milk\n₹56/L").forEach { AdminCard(Modifier.weight(1f)) { Text("🥑", fontSize = 24.sp); Text(it, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) } } } }; item { DemoNotice() }; item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Return to editor") }; Button(onClick = onPublish, modifier = Modifier.weight(1f)) { Text("Publish demo") } } } } }

@Composable private fun ListFeature(title: String, rows: List<String>, action: String, onAction: () -> Unit, onItem: (String) -> Unit) { Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) { ScreenHeader(title, "Prototype data · local only", if (action.isNotBlank()) onAction else null, action); LazyColumn(contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(rows) { DataRow("✦", it, "Demo record · not published", { onItem(it) }) } } } }

@Composable private fun FeatureForm(title: String, fields: List<String>, onSave: () -> Unit) { var values by remember { mutableStateOf(List(fields.size) { "" }) }; var error by remember { mutableStateOf("") }; LazyColumn(Modifier.fillMaxSize().padding(20.dp), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { item { ScreenHeader(title, "Edits stay in the local prototype") }; fields.forEachIndexed { i, field -> item { AdminField(field, values[i], { value -> values = values.toMutableList().also { it[i] = value } }) } }; item { if (error.isNotBlank()) Text(error, color = Coral); PrimaryButton("Save Draft", Modifier.fillMaxWidth()) { if (values.firstOrNull().isNullOrBlank()) error = "Please enter ${fields.first().lowercase()}" else { error = ""; onSave() } } } } }

@Composable private fun ProductForm(onSave: (String) -> Unit) { var name by rememberSaveable { mutableStateOf("") }; var price by rememberSaveable { mutableStateOf("") }; var stock by rememberSaveable { mutableStateOf("10") }; var error by remember { mutableStateOf("") }; Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { ScreenHeader("Add Product", "Local demo catalog"); AdminField("Product name", name, { name = it }); AdminField("Price (₹)", price, { price = it }, KeyboardType.Decimal); AdminField("Stock quantity", stock, { stock = it }, KeyboardType.Number); AdminField("Category", "Vegetables", {}, readOnly = true); if (error.isNotBlank()) Text(error, color = Coral); Spacer(Modifier.weight(1f)); PrimaryButton("Save Product", Modifier.fillMaxWidth()) { val parsed = price.toDoubleOrNull(); val count = stock.toIntOrNull(); if (name.isBlank() || parsed == null || parsed <= 0 || count == null || count < 0) error = "Enter a name, positive price, and non-negative stock" else onSave("$name · ₹${parsed.toInt()} · ${if (count == 0) "Out of stock" else "In stock"}") } } }

@Composable private fun OrderDetailScreen(onUpdate: () -> Unit) { var status by rememberSaveable { mutableStateOf("Preparing") }; var confirm by remember { mutableStateOf(false) }; LazyColumn(Modifier.fillMaxSize().padding(20.dp), contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { ScreenHeader("Order #RI172805456", "10 Oct 2026 · 2:30 PM") }; item { DemoNotice() }; item { AdminCard { Text("Customer", fontWeight = FontWeight.Bold); Text("Ankit Kumar · +91 9876543210", color = Muted); Text("Delivery address · Patna, Bihar 800001", color = Muted) } }; item { AdminCard { Text("Items", fontWeight = FontWeight.Bold); Text("Tomato   2 kg                       ₹48"); Text("Banana   1 kg                       ₹32"); Text("Milk        1 L                         ₹56"); HorizontalDivider(Modifier.padding(vertical = 8.dp)); Text("Subtotal                                  ₹136"); Text("Delivery charge                         ₹0"); Text("Total                                      ₹136", fontWeight = FontWeight.Bold) } }; item { AdminCard { Text("Payment · Demo only", fontWeight = FontWeight.Bold); Text("No payment was processed", color = Muted) } }; item { AdminCard { Text("Status history", fontWeight = FontWeight.Bold); Text("Confirmed  →  Preparing  →  On the way  →  Delivered", color = Muted); Text("Current: $status", color = Coral, fontWeight = FontWeight.Bold); OutlinedButton(onClick = { confirm = true }) { Text("Update status") } } } }; if (confirm) item { AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Advance demo order?") }, text = { Text("Update status from $status? This is a local demo action.") }, confirmButton = { TextButton(onClick = { status = if (status == "Preparing") "On the way" else "Delivered"; confirm = false; onUpdate() }) { Text("Update") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } }) } } } }

@Composable private fun NotificationScreen(onSend: () -> Unit) { var title by rememberSaveable { mutableStateOf("") }; var message by rememberSaveable { mutableStateOf("") }; var history by remember { mutableStateOf(listOf("Weekend fresh picks · Demo preview only")) }; LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { item { ScreenHeader("Send Notification", "No push service connected") }; item { AdminField("Title", title, { title = it }) }; item { AdminField("Message", message, { message = it }) }; item { Text("Audience", fontWeight = FontWeight.Bold) }; item { var audience by rememberSaveable { mutableStateOf("All Demo Customers") }; Row { listOf("All Demo Customers", "Selected Segment").forEach { FilterChip(selected = audience == it, onClick = { audience = it }, label = { Text(it) }, modifier = Modifier.padding(end = 4.dp)) } } }; item { PrimaryButton("Send Demo Notification", Modifier.fillMaxWidth()) { if (title.isNotBlank() && message.isNotBlank()) { history = listOf("$title · Demo send · Not delivered") + history; onSend() } } }; item { SectionTitle("Local send history", "Demo") }; items(history) { DataRow("♧", it, "Not sent to real devices") } } }

@Composable private fun SettingsScreen(onReset: () -> Unit) { var maintenance by rememberSaveable { mutableStateOf(false) }; var sound by rememberSaveable { mutableStateOf(true) }; var animations by rememberSaveable { mutableStateOf(true) }; LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { item { ScreenHeader("App Settings", "Local demo preferences") }; item { AdminCard { Text("General", fontWeight = FontWeight.Bold); ToggleRow("Maintenance mode · demo", maintenance) { maintenance = it }; ToggleRow("Sound preference", sound) { sound = it }; ToggleRow("Animations", animations) { animations = it } } }; item { AdminCard { Text("Information", fontWeight = FontWeight.Bold); Text("App version 1.0 · Demo prototype"); Text("Privacy policy and Terms are not connected", color = Muted) } }; item { OutlinedButton(onClick = onReset) { Text("Reset demo data", color = Coral) } } } }
@Composable private fun ProfileScreen(onLogout: () -> Unit) { LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { item { ScreenHeader("Admin Profile", "Demo account · local only") }; item { AdminCard { Text("👤  Dharmjeet Singh", fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("Super Admin", color = Coral); Text("admin@rushit.in", color = Muted) } }; item { ListFeatureInline("Edit profile", "Profile fields saved in the prototype") }; item { ListFeatureInline("Change password", "No real password is stored or changed") }; item { ListFeatureInline("Login history", "Today · Demo sign-in") }; item { Text("Role visibility in this prototype is not a security boundary.", color = Muted, fontSize = 12.sp) }; item { OutlinedButton(onClick = onLogout) { Text("Log out") } } } }

@Composable private fun ListFeatureInline(title: String, desc: String) { DataRow("•", title, desc, {}) }
@Composable private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f)); Switch(value, onChange) } }
@Composable private fun ProductDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) { var name by remember { mutableStateOf("") }; var price by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = onDismiss, title = { Text("Add product") }, text = { Column { AdminField("Name", name, { name = it }); AdminField("Price", price, { price = it }, KeyboardType.Decimal) } }, confirmButton = { TextButton(onClick = { if (name.isNotBlank() && (price.toDoubleOrNull() ?: 0.0) > 0) onSave("$name · ₹${price} · In stock") }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }) }

@Composable private fun ScreenHeader(title: String, subtitle: String = "", onAction: (() -> Unit)? = null, action: String = "＋") { Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink); if (subtitle.isNotBlank()) Text(subtitle, fontSize = 12.sp, color = Muted) }; if (onAction != null) TextButton(onClick = onAction) { Text(action, color = Coral) } } }
@Composable private fun AdminField(label: String, value: String, onValue: (String) -> Unit, keyboard: KeyboardType = KeyboardType.Text, readOnly: Boolean = false) { OutlinedTextField(value, onValue, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true, readOnly = readOnly, keyboardOptions = KeyboardOptions(keyboardType = keyboard), shape = RoundedCornerShape(14.dp)) }
@Composable private fun PrimaryButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) { Button(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = Coral)) { Text(label, fontWeight = FontWeight.SemiBold) } }
@Composable private fun AdminCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) { Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Card, border = androidx.compose.foundation.BorderStroke(1.dp, Line), shadowElevation = 1.dp) { Column(Modifier.padding(15.dp), content = content) } }
@Composable private fun DataRow(glyph: String, title: String, detail: String, onClick: () -> Unit = {}) { Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), color = Card, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFEEE9)), contentAlignment = Alignment.Center) { Text(glyph, color = Coral, fontWeight = FontWeight.Bold) }; Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(detail, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }; Text("›", color = Muted, fontSize = 22.sp) } } }
@Composable private fun StatusChip(status: String) { val color = if (status.contains("Delivered", true)) Green else if (status.contains("Cancelled", true)) Muted else Coral; Text(status, Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = .10f)).padding(horizontal = 9.dp, vertical = 5.dp), color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
@Composable private fun DemoNotice() { Text("DEMO · This configuration stays in RushIt Admin on this device; it does not update the customer app or production services.", Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Color(0xFFFFF4DC)).padding(12.dp), color = Ink, fontSize = 12.sp) }
@Composable private fun FilterChip(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit, modifier: Modifier = Modifier) { androidx.compose.material3.FilterChip(selected = selected, onClick = onClick, label = label, modifier = modifier) }
