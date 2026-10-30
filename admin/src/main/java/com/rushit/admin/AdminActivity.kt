package com.rushit.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(255, 253, 247)
        window.navigationBarColor = android.graphics.Color.rgb(255, 253, 247)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContent { AdminRoot() }
    }
}

@Composable
private fun AdminRoot() {
    val nav = rememberNavController()
    val back by nav.currentBackStackEntryAsState()
    val route = back?.destination?.route ?: "login"
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences(ADMIN_PREFS_NAME, android.content.Context.MODE_PRIVATE) }

    val products = remember(preferences) {
        mutableStateListOf(*(loadPrefList(preferences, "products", initialProducts).toTypedArray()))
    }
    val orders = remember(preferences) {
        mutableStateListOf(*(loadPrefList(preferences, "orders", initialOrders).toTypedArray()))
    }
    val sections = remember(preferences) {
        mutableStateListOf(*(loadPrefList(preferences, "sections", defaultHomeSections).toTypedArray()))
    }
    val publishedSections = remember(preferences) {
        mutableStateListOf(*(preferences.getString("published_sections", null)?.split("##")?.filter { it.isNotBlank() }?.toTypedArray() ?: emptyArray()))
    }
    val categories = remember(preferences) {
        mutableStateListOf(*(loadPrefList(preferences, "categories", initialCategories).toTypedArray()))
    }
    val campaigns = remember(preferences) {
        mutableStateListOf(*(loadPrefList(preferences, "campaigns", initialCampaigns).toTypedArray()))
    }
    val offers = remember(preferences) {
        mutableStateListOf(*(loadPrefList(preferences, "offers", initialOffers).toTypedArray()))
    }
    val activityLogs = remember(preferences) { mutableStateListOf<ActivityLogEntry>().apply { addAll(loadActivityLogs(preferences)) } }

    var publishedTheme by remember(preferences) { mutableStateOf(preferences.getString("published_theme", "") ?: "") }
    var publishedBackground by remember(preferences) { mutableStateOf(preferences.getString("published_background", "") ?: "") }
    var publishedRadius by remember(preferences) { mutableFloatStateOf(preferences.getFloat("published_radius", -1f)) }
    var publishedButtonStyle by remember(preferences) { mutableStateOf(preferences.getString("published_button_style", "") ?: "") }
    var publishedTextTone by remember(preferences) { mutableStateOf(preferences.getString("published_text_tone", "") ?: "") }

    var themeName by remember(preferences) { mutableStateOf(preferences.getString("theme_name", "RushIt Coral") ?: "RushIt Coral") }
    var accentName by remember(preferences) { mutableStateOf(preferences.getString("accent_name", "Amber") ?: "Amber") }
    var backgroundStyle by remember(preferences) { mutableStateOf(preferences.getString("background_style", "Gradient") ?: "Gradient") }
    var sectionRadius by remember(preferences) { mutableFloatStateOf(preferences.getFloat("section_radius", 16f)) }
    var buttonStyle by remember(preferences) { mutableStateOf(preferences.getString("button_style", "Filled") ?: "Filled") }
    var cardStyle by remember(preferences) { mutableStateOf(preferences.getString("card_style", "Elevated") ?: "Elevated") }
    var previewTextTone by remember(preferences) { mutableStateOf(preferences.getString("preview_text_tone", "Ink") ?: "Ink") }
    var typographyScale by remember(preferences) { mutableFloatStateOf(preferences.getFloat("typography_scale", 1f)) }
    var animationsEnabled by remember(preferences) { mutableStateOf(preferences.getBoolean("animations", true)) }
    var adminRole by remember(preferences) { mutableStateOf(preferences.getString("admin_role", "Super Admin") ?: "Super Admin") }

    var settingsVersion by remember { mutableIntStateOf(0) }
    var homeDraftDirty by remember { mutableStateOf(false) }
    var homeDraftRevision by remember { mutableIntStateOf(0) }
    var pendingDesignerDestination by remember { mutableStateOf<String?>(null) }
    var snackbar by remember { mutableStateOf("") }
    val snackbarState = remember { SnackbarHostState() }
    LaunchedEffect(snackbar) { if (snackbar.isNotBlank()) { snackbarState.showSnackbar(snackbar); snackbar = "" } }

    val themeConfig = AdminThemeConfig(
        themeName = themeName,
        accentName = accentName,
        backgroundStyle = backgroundStyle,
        sectionRadius = sectionRadius,
        cardStyle = cardStyle,
        buttonStyle = buttonStyle,
        typographyScale = typographyScale,
        previewTextTone = previewTextTone,
        animationsEnabled = animationsEnabled,
    )
    val roleSpec = AdminRoleSpec.fromLabel(adminRole)
    val permissions = roleSpec.permissions

    val showTabs = route !in setOf(
        "login", "splash", "product_edit", "product_edit/{index}", "section/{index}",
        "festival_edit/{index}", "offer_edit/{index}", "order_detail/{index}", "customer_detail/{index}",
    )
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

    fun persistProducts() = savePrefList(preferences, "products", products)
    fun persistOrders() = savePrefList(preferences, "orders", orders)
    fun persistSections() = savePrefList(preferences, "sections", sections)
    fun persistCategories() = savePrefList(preferences, "categories", categories)
    fun persistCampaigns() = savePrefList(preferences, "campaigns", campaigns)
    fun persistOffers() = savePrefList(preferences, "offers", offers)
    fun log(category: String, title: String, detail: String) {
        appendActivityLog(preferences, activityLogs, category, title, detail)
    }
    fun saveTheme(next: AdminThemeConfig) {
        themeName = next.themeName
        accentName = next.accentName
        backgroundStyle = next.backgroundStyle
        sectionRadius = next.sectionRadius
        buttonStyle = next.buttonStyle
        cardStyle = next.cardStyle
        previewTextTone = next.previewTextTone
        typographyScale = next.typographyScale
        preferences.edit()
            .putString("theme_name", next.themeName)
            .putString("accent_name", next.accentName)
            .putString("background_style", next.backgroundStyle)
            .putFloat("section_radius", next.sectionRadius)
            .putString("button_style", next.buttonStyle)
            .putString("card_style", next.cardStyle)
            .putString("preview_text_tone", next.previewTextTone)
            .putFloat("typography_scale", next.typographyScale)
            .apply()
        log("Content", "Theme updated", "${next.themeName} · ${next.accentName} accent · ${next.cardStyle} cards")
        snackbar = "Theme preferences saved on this device"
    }

    val inProgressBadge = orders.count { extractOrderStatus(it) !in setOf("Delivered", "Cancelled") }

    RushItAdminTheme(config = themeConfig) {
        CompositionLocalProvider(
            LocalAdminNavigation provides nav,
            LocalAdminPermissions provides permissions,
        ) {
            Scaffold(
                containerColor = AdminColorScheme.background,
                snackbarHost = { SnackbarHost(snackbarState) },
                bottomBar = {
                    if (showTabs) {
                        FloatingAdminBottomBar(
                            selectedRoute = selectedTab,
                            onSelectTab = { dest ->
                                if (route != dest) {
                                    if (route == "designer" && homeDraftDirty) pendingDesignerDestination = dest
                                    else navigateToRootTab(dest)
                                }
                            },
                            badgeCounts = mapOf("orders" to inProgressBadge),
                        )
                    }
                },
            ) { padding ->
                NavHost(navController = nav, startDestination = "splash", modifier = Modifier.padding(padding)) {
                    composable("splash") {
                        SplashScreen(onFinish = {
                            val destination = if (preferences.getBoolean("demo_session", false)) "home" else "login"
                            nav.navigate(destination) { popUpTo("splash") { inclusive = true }; launchSingleTop = true }
                        })
                    }
                    composable("login") {
                        LoginScreen(onLogin = { rememberSession ->
                            if (rememberSession) preferences.edit().putBoolean("demo_session", true).apply()
                            else preferences.edit().remove("demo_session").apply()
                            nav.navigate("home") { popUpTo("login") { inclusive = true }; launchSingleTop = true }
                        })
                    }
                    composable("home") {
                        DashboardScreen(
                            products = products,
                            orders = orders,
                            customerCount = demoCustomers.size,
                            adminName = preferences.getString("admin_name", "Dharmjeet Singh") ?: "Dharmjeet Singh",
                            campaigns = campaigns,
                            offers = offers,
                            onOpen = { nav.navigate(it) },
                        )
                    }
                    composable("catalog") {
                        if (!permissions.canManageProducts && !permissions.canManageCategories) {
                            AccessDeniedScreen("Products", "Manage Products", permissions.roleName, { nav.navigate("roles") }, { nav.popBackStack() })
                        } else {
                            CatalogScreen(
                                products = products,
                                categories = categories,
                                onAdd = { nav.navigate("product_edit") },
                                onEdit = { index -> nav.navigate("product_edit/$index") },
                                onDelete = { index ->
                                    if (index in products.indices) {
                                        val name = visibleProductRecord(products[index]).substringBefore(" · ")
                                        products.removeAt(index)
                                        persistProducts()
                                        log("Products", "Product deleted", name)
                                        snackbar = "Product removed from this device's demo catalog"
                                    }
                                },
                                onDuplicate = { index ->
                                    if (index in products.indices) {
                                        val source = products[index]
                                        val visible = visibleProductRecord(source)
                                        val name = visible.substringBefore(" · ")
                                        val copyVisible = visible.replaceFirst(name, "$name (Copy)")
                                        val meta = productMetadata(source)
                                        products.add(index + 1, withProductMetadata(copyVisible, meta.copy(sku = if (meta.sku.isBlank()) "" else "${meta.sku}-COPY")))
                                        persistProducts()
                                        log("Products", "Product duplicated", "$name (Copy)")
                                        snackbar = "Product duplicated locally"
                                    }
                                },
                                onToggleVisibility = { index ->
                                    if (index in products.indices) {
                                        val raw = products[index]
                                        val visible = visibleProductRecord(raw)
                                        val meta = productMetadata(raw)
                                        val hidden = visible.endsWith("Hidden")
                                        val nextVisible = if (hidden) visible.removeSuffix("Hidden") + "Visible" else visible.removeSuffix("Visible") + "Hidden"
                                        products[index] = withProductMetadata(nextVisible, meta)
                                        persistProducts()
                                        log("Products", if (hidden) "Product activated" else "Product hidden", visible.substringBefore(" · "))
                                    }
                                },
                                onBulkDelete = { indices ->
                                    indices.sortedDescending().forEach { index -> if (index in products.indices) products.removeAt(index) }
                                    persistProducts()
                                    log("Products", "Bulk delete", "${indices.size} products removed locally")
                                    snackbar = "${indices.size} products removed from this device"
                                },
                                onOpenCategories = { nav.navigate("categories") },
                                onOpenRoles = { nav.navigate("roles") },
                            )
                        }
                    }
                    composable("orders") {
                        if (!permissions.canViewOrders) {
                            AccessDeniedScreen("Orders", "View Orders", permissions.roleName, { nav.navigate("roles") }, { nav.popBackStack() })
                        } else {
                            OrdersScreen(orders, onOpen = { index -> nav.navigate("order_detail/$index") }, onOpenRoles = { nav.navigate("roles") })
                        }
                    }
                    composable("themes") {
                        if (!permissions.canManageContent) {
                            AccessDeniedScreen("Theme & Colors", "Manage Content & Themes", permissions.roleName, { nav.navigate("roles") }, { nav.popBackStack() })
                        } else {
                            ThemeScreen(themeConfig, onSave = ::saveTheme, onPreview = { nav.navigate("preview") }, onOpenRoles = { nav.navigate("roles") })
                        }
                    }
                    composable("more") {
                        MoreScreen(onOpen = { nav.navigate(it) }, currentRole = permissions.roleName)
                    }
                    composable("designer") {
                        if (!permissions.canManageContent) {
                            AccessDeniedScreen("Home Designer", "Manage Home Content", permissions.roleName, { nav.navigate("roles") }, { nav.popBackStack() })
                        } else {
                            key(homeDraftRevision) {
                                DesignerScreen(
                                    sections = sections,
                                    onEdit = { index -> nav.navigate("section/$index") },
                                    onSave = {
                                        persistSections()
                                        homeDraftDirty = false
                                        log("Content", "Home draft saved", "${sections.size} sections")
                                        snackbar = it
                                    },
                                    onDirtyChanged = { homeDraftDirty = it },
                                    onLeave = {
                                        val previous = nav.previousBackStackEntry?.destination?.route ?: "more"
                                        if (homeDraftDirty) pendingDesignerDestination = previous else nav.popBackStack()
                                    },
                                    onPreview = { nav.navigate("preview") },
                                    onOpenRoles = { nav.navigate("roles") },
                                )
                            }
                        }
                    }
                    composable("section/{index}") { entry ->
                        val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
                        SectionEditorScreen(
                            sections.getOrNull(index).orEmpty(),
                            onSave = { updated ->
                                if (index in sections.indices) sections[index] = updated
                                persistSections()
                                homeDraftDirty = false
                                homeDraftRevision++
                                log("Content", "Section updated", parseHomeSection(updated).title)
                                snackbar = "Section saved to local draft"
                                nav.popBackStack()
                            },
                            onCancel = { nav.popBackStack() },
                        )
                    }
                    composable("preview") {
                        PreviewScreen(
                            sections = sections,
                            products = products,
                            categories = categories,
                            campaigns = campaigns,
                            offers = offers,
                            publishedSections = publishedSections,
                            themeConfig = themeConfig,
                            publishedTheme = publishedTheme,
                            publishedBackground = publishedBackground,
                            publishedRadius = publishedRadius,
                            publishedButtonStyle = publishedButtonStyle,
                            publishedTextTone = publishedTextTone,
                            storeName = preferences.getString("store_name", "RushIt") ?: "RushIt",
                            maintenancePreview = preferences.getBoolean("maintenance", false),
                            onBack = { nav.popBackStack() },
                            onPublish = {
                                publishedSections.clear(); publishedSections.addAll(sections)
                                publishedTheme = themeName; publishedBackground = backgroundStyle; publishedRadius = sectionRadius
                                publishedButtonStyle = buttonStyle; publishedTextTone = previewTextTone
                                preferences.edit()
                                    .putString("sections", sections.joinToString("##"))
                                    .putString("published_sections", publishedSections.joinToString("##"))
                                    .putString("published_theme", publishedTheme)
                                    .putString("published_background", publishedBackground)
                                    .putFloat("published_radius", publishedRadius)
                                    .putString("published_button_style", publishedButtonStyle)
                                    .putString("published_text_tone", publishedTextTone)
                                    .apply()
                                homeDraftDirty = false
                                homeDraftRevision++
                                log("Content", "Home snapshot published locally", "${sections.size} sections · User app unchanged")
                                snackbar = "Published snapshot saved on this device only · User app unchanged"
                            },
                        )
                    }
                    composable("festivals") {
                        if (!permissions.canManageCampaigns) {
                            AccessDeniedScreen("Festival Manager", "Manage Campaigns", permissions.roleName, { nav.navigate("roles") }, { nav.popBackStack() })
                        } else {
                            CampaignManagerScreen(
                                campaigns,
                                onAdd = { nav.navigate("festival_edit/new") },
                                onEdit = { index -> nav.navigate("festival_edit/$index") },
                                onDelete = { index ->
                                    if (index in campaigns.indices) {
                                        val title = campaigns[index].substringBefore('|')
                                        campaigns.removeAt(index)
                                        persistCampaigns()
                                        log("Content", "Festival removed", title)
                                        snackbar = "Local festival removed"
                                    }
                                },
                                onOpenRoles = { nav.navigate("roles") },
                            )
                        }
                    }
                    composable("festival_edit/{index}") { entry ->
                        val keyArg = entry.arguments?.getString("index") ?: "new"
                        val index = keyArg.toIntOrNull()
                        val fromScheduler = nav.previousBackStackEntry?.destination?.route == "schedule"
                        CampaignEditorScreen(
                            campaign = index?.let { campaigns.getOrNull(it) },
                            scheduler = fromScheduler,
                            onCancel = { nav.popBackStack() },
                            onSave = { updated ->
                                if (index == null) campaigns.add(updated) else if (index in campaigns.indices) campaigns[index] = updated
                                persistCampaigns()
                                log("Content", "Campaign saved", updated.substringBefore('|'))
                                snackbar = "Campaign saved locally · not published"
                                nav.popBackStack()
                            },
                        )
                    }
                    composable("schedule") {
                        if (!permissions.canManageCampaigns) {
                            AccessDeniedScreen("Campaign Scheduler", "Manage Campaigns", permissions.roleName, { nav.navigate("roles") }, { nav.popBackStack() })
                        } else {
                            CampaignManagerScreen(
                                campaigns,
                                scheduler = true,
                                onAdd = { nav.navigate("festival_edit/new") },
                                onEdit = { index -> nav.navigate("festival_edit/$index") },
                                onDelete = { index ->
                                    if (index in campaigns.indices) {
                                        val title = campaigns[index].substringBefore('|')
                                        campaigns.removeAt(index)
                                        persistCampaigns()
                                        log("Content", "Campaign removed", title)
                                        snackbar = "Local campaign removed"
                                    }
                                },
                                onOpenRoles = { nav.navigate("roles") },
                            )
                        }
                    }
                    composable("product_edit") {
                        ProductForm(categories = categories, onCancel = { nav.popBackStack() }, onSave = {
                            products.add(it)
                            persistProducts()
                            log("Products", "Product created", visibleProductRecord(it).substringBefore(" · "))
                            snackbar = "Product saved locally"
                            nav.popBackStack()
                        })
                    }
                    composable("product_edit/{index}") { entry ->
                        val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
                        ProductForm(product = products.getOrNull(index), categories = categories, onCancel = { nav.popBackStack() }, onSave = { updated ->
                            if (index in products.indices) products[index] = updated
                            persistProducts()
                            log("Products", "Product updated", visibleProductRecord(updated).substringBefore(" · "))
                            snackbar = "Product updated locally"
                            nav.popBackStack()
                        })
                    }
                    composable("categories") {
                        if (!permissions.canManageCategories) {
                            AccessDeniedScreen("Categories", "Manage Categories", permissions.roleName, { nav.navigate("roles") }, { nav.popBackStack() })
                        } else {
                            CategoryManagementScreen(categories, products, onSave = { message ->
                                persistCategories()
                                persistProducts()
                                log("Products", "Category updated", message)
                                snackbar = message
                            }, onOpenRoles = { nav.navigate("roles") })
                        }
                    }
                    composable("order_detail/{index}") { entry ->
                        val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
                        val order = orders.getOrNull(index) ?: orders.firstOrNull().orEmpty()
                        val orderId = extractOrderId(order)
                        val notes = parseNotesMap(preferences.getString("order_notes", null))
                        val timelineMap = parseTimeline(preferences.getString("order_timeline", null))
                        OrderDetailScreen(
                            order = order,
                            note = notes[orderId].orEmpty(),
                            timeline = timelineMap[orderId].orEmpty(),
                            onUpdate = { newStatus ->
                                if (index in orders.indices) {
                                    orders[index] = order.substringBeforeLast(" · ") + " · $newStatus"
                                    persistOrders()
                                }
                                val events = timelineMap.getOrPut(orderId) { mutableListOf() }
                                events.add("$newStatus|${System.currentTimeMillis()}")
                                preferences.edit().putString("order_timeline", encodeTimeline(timelineMap)).apply()
                                log("Orders", "Order status changed", "$orderId · $newStatus")
                                snackbar = "Order status updated in local demo"
                            },
                            onSaveNote = { note ->
                                notes[orderId] = note
                                preferences.edit().putString("order_notes", encodeNotesMap(notes)).apply()
                                log("Orders", "Order note saved", orderId)
                                snackbar = "Internal note saved locally"
                            },
                            onOpenRoles = { nav.navigate("roles") },
                        )
                    }
                    composable("customers") {
                        if (!permissions.canManageCustomers) {
                            AccessDeniedScreen("Customers", "Manage Customers", permissions.roleName, { nav.navigate("roles") }, { nav.popBackStack() })
                        } else {
                            CustomersScreen(
                                onOpen = { index -> nav.navigate("customer_detail/$index") },
                                notes = parseNotesMap(preferences.getString("customer_notes", null)),
                                onOpenRoles = { nav.navigate("roles") },
                            )
                        }
                    }
                    composable("customer_detail/{index}") { entry ->
                        val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
                        val notes = parseNotesMap(preferences.getString("customer_notes", null))
                        CustomerDetailScreen(index, notes[index.toString()].orEmpty(), onSaveNote = { note ->
                            notes[index.toString()] = note
                            preferences.edit().putString("customer_notes", encodeNotesMap(notes)).apply()
                            snackbar = "Support note saved locally"
                        })
                    }
                    composable("offers") {
                        if (!permissions.canManageOffers) {
                            AccessDeniedScreen("Offers & Coupons", "Manage Offers", permissions.roleName, { nav.navigate("roles") }, { nav.popBackStack() })
                        } else {
                            OfferManagerScreen(
                                offers,
                                onAdd = { nav.navigate("offer_edit/new") },
                                onEdit = { index -> nav.navigate("offer_edit/$index") },
                                onDelete = { index ->
                                    if (index in offers.indices) {
                                        val code = offers[index].split('|').getOrNull(1).orEmpty()
                                        offers.removeAt(index)
                                        persistOffers()
                                        log("Offers", "Offer deleted", code)
                                        snackbar = "Local coupon removed"
                                    }
                                },
                                onToggleOffer = { index, updated ->
                                    if (index in offers.indices) {
                                        offers[index] = updated
                                        persistOffers()
                                        log("Offers", "Offer status changed", updated.split('|').getOrNull(1).orEmpty())
                                    }
                                },
                                onOpenRoles = { nav.navigate("roles") },
                            )
                        }
                    }
                    composable("offer_edit/{index}") { entry ->
                        val keyArg = entry.arguments?.getString("index") ?: "new"
                        val index = keyArg.toIntOrNull()
                        OfferEditorScreen(offers.getOrNull(index ?: -1), existingOffers = offers, onCancel = { nav.popBackStack() }, onSave = { offer ->
                            if (index == null) offers.add(offer) else if (index in offers.indices) offers[index] = offer
                            persistOffers()
                            log("Offers", "Offer saved", offer.split('|').getOrNull(1).orEmpty())
                            snackbar = "Offer saved locally · not published to customers"
                            nav.popBackStack()
                        })
                    }
                    composable("notifications") {
                        NotificationScreen(preferences, onSend = {
                            log("System", "Notification preview saved", "Not delivered")
                            snackbar = "Local notification preview saved · not delivered"
                        }, onOpenRoles = { nav.navigate("roles") })
                    }
                    composable("settings") {
                        key(settingsVersion) {
                            SettingsScreen(preferences, themeName, onAnimationsChange = { animationsEnabled = it }, onReset = {
                                preferences.edit().clear().apply()
                                replaceList(products, initialProducts)
                                replaceList(orders, initialOrders)
                                replaceList(sections, defaultHomeSections)
                                publishedSections.clear()
                                publishedTheme = ""; publishedBackground = ""; publishedRadius = -1f; publishedButtonStyle = ""; publishedTextTone = ""
                                buttonStyle = "Filled"; previewTextTone = "Ink"; cardStyle = "Elevated"; accentName = "Amber"; typographyScale = 1f
                                homeDraftDirty = false; homeDraftRevision++
                                replaceList(categories, initialCategories)
                                replaceList(campaigns, initialCampaigns)
                                replaceList(offers, initialOffers)
                                themeName = "RushIt Coral"
                                backgroundStyle = "Gradient"
                                sectionRadius = 16f
                                adminRole = "Super Admin"
                                activityLogs.clear()
                                activityLogs.addAll(defaultSeedActivityLogs())
                                persistProducts(); persistOrders(); persistSections(); persistCategories(); persistCampaigns(); persistOffers()
                                settingsVersion++
                                log("System", "Local demo data reset", "Catalog, orders, theme and drafts restored")
                                snackbar = "Local demo data reset"
                            }, onOpenRoles = { nav.navigate("roles") })
                        }
                    }
                    composable("profile") {
                        ProfileScreen(
                            prefs = preferences,
                            currentRole = adminRole,
                            onRoleChange = { role ->
                                adminRole = role
                                preferences.edit().putString("admin_role", role).apply()
                                log("System", "Demo role switched", role)
                            },
                            onLogout = {
                                preferences.edit().remove("demo_session").apply()
                                nav.navigate("login") { popUpTo("home") { inclusive = true }; launchSingleTop = true }
                            },
                            onOpenRoles = { nav.navigate("roles") },
                        )
                    }
                    composable("roles") {
                        AdminRolesScreen(currentRole = adminRole, onSelectRole = { role ->
                            adminRole = role
                            preferences.edit().putString("admin_role", role).apply()
                            log("System", "Demo role switched", role)
                            snackbar = "Demo role set to $role · local UI preview only"
                        })
                    }
                    composable("activity_logs") {
                        ActivityLogsScreen(logs = activityLogs, onClearLogs = {
                            activityLogs.clear()
                            activityLogs.addAll(defaultSeedActivityLogs())
                            preferences.edit().putString("activity_logs", activityLogs.joinToString("##") { it.encode() }).apply()
                            snackbar = "Local activity log reset to sample events"
                        })
                    }
                }
                pendingDesignerDestination?.let { destination ->
                    AdminDialog(
                        title = "Discard Home draft changes?",
                        message = "Your unsaved changes to sections will be discarded before leaving Home Designer.",
                        confirmLabel = "Discard and leave",
                        isDestructive = true,
                        onConfirm = {
                            val savedSections = preferences.getString("sections", null)?.split("##")?.filter { it.isNotBlank() } ?: defaultHomeSections
                            sections.clear(); sections.addAll(savedSections)
                            homeDraftDirty = false
                            pendingDesignerDestination = null
                            if (destination in setOf("home", "catalog", "orders", "themes", "more")) navigateToRootTab(destination)
                            else nav.popBackStack()
                        },
                        onDismiss = { pendingDesignerDestination = null },
                    )
                }
            }
        }
    }
}

@Composable
private fun SplashScreen(onFinish: () -> Unit) {
    var entered by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.86f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 180f),
        label = "admin logo scale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0.0f,
        animationSpec = tween(520),
        label = "admin logo fade",
    )
    LaunchedEffect(Unit) {
        entered = true
        delay(1450)
        if (!finished) {
            finished = true
            onFinish()
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFFE8E0), AdminColorScheme.primary, AdminColorScheme.primaryDark)
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
                .padding(28.dp),
        ) {
            RushItBrandLogo(size = 88.dp, inverted = true)
            Spacer(Modifier.height(18.dp))
            Text("RushIt", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            Text("ADMIN", color = Color.White.copy(alpha = 0.92f), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 5.sp)
            Spacer(Modifier.height(10.dp))
            Text("Manage your store with clarity.", color = Color.White.copy(alpha = 0.94f), fontSize = 14.sp)
            Spacer(Modifier.height(22.dp))
            SplashProgressBar()
        }
    }
}

@Composable
private fun SplashProgressBar() {
    val transition = rememberInfiniteTransition(label = "admin splash progress")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0.52f,
        animationSpec = infiniteRepeatable(tween(1050, easing = LinearEasing), RepeatMode.Reverse),
        label = "progress",
    )
    Canvas(
        Modifier
            .width(168.dp)
            .height(7.dp)
            .semantics {
                contentDescription = "Loading RushIt Admin"
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            },
    ) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(Color.White.copy(alpha = 0.22f), cornerRadius = radius)
        drawRoundRect(
            Color.White,
            topLeft = Offset(size.width * offset, 0f),
            size = Size(size.width * 0.48f, size.height),
            cornerRadius = radius,
        )
    }
}

@Composable
private fun LoginScreen(onLogin: (Boolean) -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var reveal by rememberSaveable { mutableStateOf(false) }
    var rememberAdmin by rememberSaveable { mutableStateOf(true) }
    var error by rememberSaveable { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFFFEEE9), AdminColorScheme.background, AdminColorScheme.background)))
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        RushItBrandLogo(size = 72.dp)
        Spacer(Modifier.height(14.dp))
        Text("RushIt Admin", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = AdminColorScheme.primary)
        Text("Welcome back", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AdminColorScheme.primaryText, modifier = Modifier.padding(top = 22.dp))
        Text("Sign in to manage your store", color = AdminColorScheme.secondaryText, modifier = Modifier.padding(top = 5.dp, bottom = 18.dp))
        AdminField("Email or phone", email, { email = it }, keyboard = KeyboardType.Email)
        Spacer(Modifier.height(10.dp))
        AdminField(
            label = "Password",
            value = password,
            onValue = { password = it },
            visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { reveal = !reveal }) {
                    Text(if (reveal) "Hide" else "Show", color = AdminColorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).clickable { rememberAdmin = !rememberAdmin }, verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Checkbox(
                    rememberAdmin,
                    { rememberAdmin = it },
                    colors = CheckboxDefaults.colors(checkedColor = AdminColorScheme.primary, uncheckedColor = AdminColorScheme.secondaryText),
                )
                Text("Keep demo session on this device", fontSize = 12.sp, color = AdminColorScheme.secondaryText)
            }
            TextButton(onClick = { error = "Password recovery is not connected in this local demo." }) {
                Text("Forgot?", color = AdminColorScheme.primary, fontSize = 12.sp)
            }
        }
        if (error.isNotBlank()) Text(error, Modifier.fillMaxWidth().padding(bottom = 4.dp), color = AdminColorScheme.error, fontSize = 12.sp)
        PrimaryButton("Sign in", Modifier.fillMaxWidth()) {
            error = when {
                email.isBlank() -> "Enter an email or phone number."
                password.isBlank() -> "Enter a password to continue."
                else -> "Authentication is not connected. Use demo access below; entered credentials are not checked."
            }
        }
        Spacer(Modifier.height(8.dp))
        PrimaryButton("Continue as Demo Admin", Modifier.fillMaxWidth()) { onLogin(rememberAdmin) }
        Text(
            "Local demo session only · credentials are not sent or verified.",
            color = AdminColorScheme.secondaryText,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text("Restricted area · Administrators only", color = AdminColorScheme.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp, bottom = 10.dp))
    }
}
