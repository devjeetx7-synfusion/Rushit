package com.rushit.admin

import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Immutable
data class AdminPermissions(
    val roleName: String,
    val roleSummary: String,
    val canViewDashboard: Boolean,
    val canManageProducts: Boolean,
    val canManageCategories: Boolean,
    val canViewOrders: Boolean,
    val canManageOrders: Boolean,
    val canManageCustomers: Boolean,
    val canManageContent: Boolean,
    val canManageCampaigns: Boolean,
    val canManageOffers: Boolean,
    val canManageNotifications: Boolean,
    val canManageSettings: Boolean,
    val canManageAdmins: Boolean,
) {
    fun capabilitiesList(): List<Pair<String, Boolean>> = listOf(
        "View store dashboard & KPIs" to canViewDashboard,
        "Create, edit & adjust products" to canManageProducts,
        "Manage categories & visibility" to canManageCategories,
        "Lookup & inspect orders" to canViewOrders,
        "Advance or cancel order status" to canManageOrders,
        "View customers & support notes" to canManageCustomers,
        "Edit Home Designer & themes" to canManageContent,
        "Manage festivals & schedule campaigns" to canManageCampaigns,
        "Create & edit discount coupons" to canManageOffers,
        "Draft customer notifications" to canManageNotifications,
        "Configure store settings & reset data" to canManageSettings,
        "Configure admin roles & permissions" to canManageAdmins,
    )
}

fun AdminPermissions.allowsDestination(route: String?): Boolean {
    val r = route ?: return true
    return when (r) {
        AdminRoutes.SPLASH, AdminRoutes.LOGIN, AdminRoutes.MORE, AdminRoutes.PROFILE,
        AdminRoutes.ROLES, AdminRoutes.ACTIVITY_LOGS,
        -> true
        AdminRoutes.HOME -> canViewDashboard
        AdminRoutes.CATALOG, AdminRoutes.PRODUCT_EDIT, AdminRoutes.PRODUCT_EDIT_INDEX ->
            canManageProducts || canManageCategories
        AdminRoutes.CATEGORIES -> canManageCategories || canManageProducts
        AdminRoutes.ORDERS, AdminRoutes.ORDER_DETAIL -> canViewOrders
        AdminRoutes.THEMES, AdminRoutes.DESIGNER, AdminRoutes.SECTION, AdminRoutes.PREVIEW -> canManageContent
        AdminRoutes.FESTIVALS, AdminRoutes.FESTIVAL_EDIT, AdminRoutes.SCHEDULE -> canManageCampaigns
        AdminRoutes.CUSTOMERS, AdminRoutes.CUSTOMER_DETAIL -> canManageCustomers
        AdminRoutes.OFFERS, AdminRoutes.OFFER_EDIT -> canManageOffers
        AdminRoutes.NOTIFICATIONS -> canManageNotifications
        AdminRoutes.SETTINGS -> canManageSettings
        else -> true
    }
}

enum class AdminRoleSpec(
    val label: String,
    val shortTag: String,
    val description: String,
    val accentColor: Color,
    val softColor: Color,
    val permissions: AdminPermissions,
) {
    SUPER_ADMIN(
        label = "Super Admin",
        shortTag = "Full Access",
        description = "Full access to all catalog, order, content, customer, marketing, and system configuration features.",
        accentColor = Color(0xFFE7473C),
        softColor = Color(0xFFFFF0ED),
        permissions = AdminPermissions(
            roleName = "Super Admin",
            roleSummary = "You have complete access to all features in this local demo.",
            canViewDashboard = true,
            canManageProducts = true,
            canManageCategories = true,
            canViewOrders = true,
            canManageOrders = true,
            canManageCustomers = true,
            canManageContent = true,
            canManageCampaigns = true,
            canManageOffers = true,
            canManageNotifications = true,
            canManageSettings = true,
            canManageAdmins = true,
        ),
    ),
    CATALOG_MANAGER(
        label = "Catalog Manager",
        shortTag = "Products & Stock",
        description = "Manage product listings, create/edit items, update stock quantities, and organize categories.",
        accentColor = Color(0xFF27845B),
        softColor = Color(0xFFE8F6EF),
        permissions = AdminPermissions(
            roleName = "Catalog Manager",
            roleSummary = "Scoped to product catalog, inventory updates, and category management.",
            canViewDashboard = true,
            canManageProducts = true,
            canManageCategories = true,
            canViewOrders = false,
            canManageOrders = false,
            canManageCustomers = false,
            canManageContent = false,
            canManageCampaigns = false,
            canManageOffers = false,
            canManageNotifications = false,
            canManageSettings = false,
            canManageAdmins = false,
        ),
    ),
    ORDER_MANAGER(
        label = "Order Manager",
        shortTag = "Order Operations",
        description = "View incoming orders, inspect order details, record internal notes, and execute allowed status transitions.",
        accentColor = Color(0xFFD99024),
        softColor = Color(0xFFFFF6E5),
        permissions = AdminPermissions(
            roleName = "Order Manager",
            roleSummary = "Scoped to order queue, status transitions, and customer lookup for fulfillment.",
            canViewDashboard = true,
            canManageProducts = false,
            canManageCategories = false,
            canViewOrders = true,
            canManageOrders = true,
            canManageCustomers = true,
            canManageContent = false,
            canManageCampaigns = false,
            canManageOffers = false,
            canManageNotifications = false,
            canManageSettings = false,
            canManageAdmins = false,
        ),
    ),
    CONTENT_MANAGER(
        label = "Content Manager",
        shortTag = "Home, Themes & Campaigns",
        description = "Design Home layout sections, customize themes, manage festival collections, and schedule campaigns.",
        accentColor = Color(0xFF2E6B9E),
        softColor = Color(0xFFEAF3FA),
        permissions = AdminPermissions(
            roleName = "Content Manager",
            roleSummary = "Scoped to Home Designer, Section Editor, Theme Engine, Festivals, Offers, and Notifications.",
            canViewDashboard = true,
            canManageProducts = false,
            canManageCategories = false,
            canViewOrders = false,
            canManageOrders = false,
            canManageCustomers = false,
            canManageContent = true,
            canManageCampaigns = true,
            canManageOffers = true,
            canManageNotifications = true,
            canManageSettings = false,
            canManageAdmins = false,
        ),
    ),
    SUPPORT_AGENT(
        label = "Support Agent",
        shortTag = "Customers & Lookup",
        description = "View customer profiles, inspect order history, and add internal support notes without altering catalog or settings.",
        accentColor = Color(0xFFB8336A),
        softColor = Color(0xFFFCEEF4),
        permissions = AdminPermissions(
            roleName = "Support Agent",
            roleSummary = "Scoped to customer directory, read-only order lookup, and internal support notes.",
            canViewDashboard = true,
            canManageProducts = false,
            canManageCategories = false,
            canViewOrders = true,
            canManageOrders = false,
            canManageCustomers = true,
            canManageContent = false,
            canManageCampaigns = false,
            canManageOffers = false,
            canManageNotifications = false,
            canManageSettings = false,
            canManageAdmins = false,
        ),
    );

    companion object {
        fun fromLabel(label: String?): AdminRoleSpec =
            entries.firstOrNull { it.label.equals(label?.trim(), ignoreCase = true) } ?: SUPER_ADMIN
    }
}

val LocalAdminPermissions = staticCompositionLocalOf { AdminRoleSpec.SUPER_ADMIN.permissions }

@Composable
fun RoleAccessBanner(
    roleName: String,
    requiredCapability: String,
    onSwitchRole: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AdminColorScheme.errorSoft,
        border = BorderStroke(1.dp, AdminColorScheme.error.copy(alpha = 0.28f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = AdminIcons.Shield,
                    contentDescription = null,
                    tint = AdminColorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Restricted for $roleName (Demo Role)",
                    color = AdminColorScheme.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onSwitchRole) {
                    Text("Change Role", color = AdminColorScheme.error, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            Text(
                text = "Your active local role ($roleName) does not include '$requiredCapability'. Switch to Super Admin or an authorized role in Admin Roles to perform write actions.",
                color = AdminColorScheme.primaryText,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
fun AccessDeniedScreen(
    screenTitle: String,
    requiredCapability: String,
    currentRole: String,
    onOpenRoles: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            title = screenTitle,
            subtitle = "Role-restricted workspace · local demo",
            backAction = onBack,
        )
        AdminSurfaceCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(AdminColorScheme.errorSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = AdminIcons.Shield,
                        contentDescription = null,
                        tint = AdminColorScheme.error,
                        modifier = Modifier.size(28.dp),
                    )
                }
                StatusChip("Access Denied · $currentRole")
                Text(
                    text = "$requiredCapability Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AdminColorScheme.primaryText,
                )
                Text(
                    text = "The active demo role '$currentRole' cannot access $screenTitle. Switch roles in Admin Roles & Permissions to preview this workspace.",
                    color = AdminColorScheme.secondaryText,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Text(
                    text = "Note: Client-side role switching is a local UI preview and not server-enforced authorization.",
                    color = AdminColorScheme.mutedText,
                    fontSize = 11.5.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AdminSecondaryButton(
                        label = "Go Back",
                        modifier = Modifier.weight(1f),
                        onClick = onBack,
                    )
                    PrimaryButton(
                        label = "Switch Demo Role",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenRoles,
                    )
                }
            }
        }
    }
}

@Composable
fun AdminRolesScreen(
    currentRole: String,
    onSelectRole: (String) -> Unit,
) {
    val activeSpec = remember(currentRole) { AdminRoleSpec.fromLabel(currentRole) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .adminPageInsets(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "Admin Roles",
                subtitle = "Role-aware UI preview · local demo only",
            )
        }
        item {
            DemoNotice(
                message = "SECURITY NOTICE · Role selection is stored locally on this device to preview role-scoped navigation and actions. Production authorization must be enforced on a backend server.",
            )
        }
        items(AdminRoleSpec.entries, key = { it.name }) { spec ->
            val selected = spec == activeSpec
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(role = Role.RadioButton) { onSelectRole(spec.label) },
                shape = RoundedCornerShape(18.dp),
                color = if (selected) AdminColorScheme.surface else AdminColorScheme.surface,
                border = BorderStroke(
                    width = if (selected) 1.8.dp else 1.dp,
                    color = if (selected) spec.accentColor else AdminColorScheme.divider,
                ),
                shadowElevation = if (selected) 3.dp else 1.dp,
            ) {
                Row(
                    modifier = Modifier.padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(spec.softColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = when (spec) {
                                AdminRoleSpec.SUPER_ADMIN -> AdminIcons.Shield
                                AdminRoleSpec.CATALOG_MANAGER -> AdminIcons.Catalog
                                AdminRoleSpec.ORDER_MANAGER -> AdminIcons.Orders
                                AdminRoleSpec.CONTENT_MANAGER -> AdminIcons.Themes
                                AdminRoleSpec.SUPPORT_AGENT -> AdminIcons.Users
                            },
                            contentDescription = null,
                            tint = spec.accentColor,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = spec.label,
                                fontWeight = FontWeight.Bold,
                                color = AdminColorScheme.primaryText,
                                fontSize = 15.sp,
                            )
                            Surface(
                                shape = CircleShape,
                                color = spec.softColor,
                            ) {
                                Text(
                                    text = spec.shortTag,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    color = spec.accentColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = spec.description,
                            color = AdminColorScheme.secondaryText,
                            fontSize = 12.sp,
                            lineHeight = 16.5.sp,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (selected) spec.accentColor else AdminColorScheme.surfaceSecondary),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = AdminIcons.Check,
                                contentDescription = "Selected role",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
            }
        }

        // Selected Role Summary Card (matches the warm amber/blush card at the bottom of the mockup)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = activeSpec.softColor,
                border = BorderStroke(1.dp, activeSpec.accentColor.copy(alpha = 0.30f)),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Selected Role",
                        color = activeSpec.accentColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = activeSpec.label,
                        color = AdminColorScheme.primaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = activeSpec.permissions.roleSummary,
                        color = AdminColorScheme.secondaryText,
                        fontSize = 12.5.sp,
                    )
                }
            }
        }

        // Detailed Permission Matrix Preview
        item {
            AdminSurfaceCard {
                Text(
                    text = "Permission Matrix · ${activeSpec.label}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Capabilities enabled for this local demo role:",
                    color = AdminColorScheme.secondaryText,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(4.dp))
                activeSpec.permissions.capabilitiesList().forEach { (capability, allowed) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(
                                    if (allowed) AdminColorScheme.successSoft
                                    else AdminColorScheme.errorSoft
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (allowed) "✓" else "✕",
                                color = if (allowed) AdminColorScheme.success else AdminColorScheme.error,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = capability,
                            modifier = Modifier.weight(1f),
                            color = if (allowed) AdminColorScheme.primaryText else AdminColorScheme.secondaryText,
                            fontSize = 13.sp,
                            fontWeight = if (allowed) FontWeight.Medium else FontWeight.Normal,
                        )
                        StatusChip(if (allowed) "Allowed" else "Restricted")
                    }
                }
            }
        }
    }
}

/**
 * Local Admin Activity Log Entry model & persistence helper.
 * Format stored in SharedPreferences "activity_logs":
 * category|title|detail|timestampMillis joined by "##"
 */
@Immutable
data class ActivityLogEntry(
    val category: String,
    val title: String,
    val detail: String,
    val timestampMillis: Long,
) {
    fun encode(): String {
        val cleanCat = category.replace('|', ' ').replace("##", " ").trim()
        val cleanTitle = title.replace('|', ' ').replace("##", " ").trim()
        val cleanDetail = detail.replace('|', ' ').replace("##", " ").trim()
        return "$cleanCat|$cleanTitle|$cleanDetail|$timestampMillis"
    }

    companion object {
        fun decode(raw: String): ActivityLogEntry? {
            val parts = raw.split('|')
            if (parts.size < 4) return null
            val millis = parts[3].toLongOrNull() ?: return null
            return ActivityLogEntry(
                category = parts[0],
                title = parts[1],
                detail = parts[2],
                timestampMillis = millis,
            )
        }
    }
}

fun defaultSeedActivityLogs(nowMillis: Long = System.currentTimeMillis()): List<ActivityLogEntry> = listOf(
    ActivityLogEntry(
        category = "Products",
        title = "Product updated",
        detail = "Apple · ₹120/kg · Fruits catalog entry verified",
        timestampMillis = nowMillis - 15 * 60 * 1000L,
    ),
    ActivityLogEntry(
        category = "Orders",
        title = "Order status changed",
        detail = "#RI172805456 · Ankit Kumar · Preparing",
        timestampMillis = nowMillis - 65 * 60 * 1000L,
    ),
    ActivityLogEntry(
        category = "Content",
        title = "Banner updated",
        detail = "Home Designer · Top Banner section configured",
        timestampMillis = nowMillis - 3 * 3600 * 1000L,
    ),
    ActivityLogEntry(
        category = "Offers",
        title = "Offer created",
        detail = "WELCOME10 · 10% off above ₹499",
        timestampMillis = nowMillis - 18 * 3600 * 1000L,
    ),
    ActivityLogEntry(
        category = "Content",
        title = "Theme updated",
        detail = "RushIt Coral color scheme applied locally",
        timestampMillis = nowMillis - 26 * 3600 * 1000L,
    ),
)

fun loadActivityLogs(prefs: SharedPreferences): List<ActivityLogEntry> {
    val raw = prefs.getString("activity_logs", null)
    if (raw.isNullOrBlank()) {
        val seeded = defaultSeedActivityLogs()
        prefs.edit().putString("activity_logs", seeded.joinToString("##") { it.encode() }).apply()
        return seeded
    }
    val parsed = raw.split("##").mapNotNull(ActivityLogEntry::decode)
    return if (parsed.isEmpty()) defaultSeedActivityLogs() else parsed
}

fun appendActivityLog(
    prefs: SharedPreferences,
    logsState: MutableList<ActivityLogEntry>,
    category: String,
    title: String,
    detail: String,
) {
    val entry = ActivityLogEntry(
        category = category,
        title = title,
        detail = detail,
        timestampMillis = System.currentTimeMillis(),
    )
    logsState.add(0, entry)
    while (logsState.size > 80) {
        logsState.removeAt(logsState.lastIndex)
    }
    prefs.edit().putString("activity_logs", logsState.joinToString("##") { it.encode() }).apply()
}

private val activityTimeFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.US)

fun formatActivityTimestamp(timestampMillis: Long): String {
    val diff = System.currentTimeMillis() - timestampMillis
    return when {
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${(diff / 60_000L).coerceAtLeast(1)}m ago · Today"
        diff < 24 * 3_600_000L -> "Today · ${SimpleDateFormat("hh:mm a", Locale.US).format(Date(timestampMillis))}"
        diff < 48 * 3_600_000L -> "Yesterday · ${SimpleDateFormat("hh:mm a", Locale.US).format(Date(timestampMillis))}"
        else -> activityTimeFormat.format(Date(timestampMillis))
    }
}

@Composable
fun ActivityLogsScreen(
    logs: List<ActivityLogEntry>,
    onClearLogs: () -> Unit,
) {
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }

    val categories = remember { listOf("All", "Products", "Orders", "Content", "Offers", "System") }
    val filteredLogs by remember(logs, selectedCategory, searchQuery) {
        derivedStateOf {
            logs.filter { entry ->
                (selectedCategory == "All" || entry.category.equals(selectedCategory, ignoreCase = true)) &&
                    (searchQuery.isBlank() ||
                        entry.title.contains(searchQuery, ignoreCase = true) ||
                        entry.detail.contains(searchQuery, ignoreCase = true))
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .adminPageInsets(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenHeader(
                title = "Activity Logs",
                subtitle = "${logs.size} local device events recorded",
                onAction = if (logs.isNotEmpty()) ({ confirmClear = true }) else null,
                action = "Clear",
            )
        }
        item {
            DemoNotice(
                message = "LOCAL HISTORY · This log records actions performed inside RushIt Admin on this device. It is not a tamper-proof server audit log.",
            )
        }
        item {
            AdminSearchField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search activity events or records…",
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(categories, key = { it }) { cat ->
                    val count = if (cat == "All") logs.size else logs.count { it.category.equals(cat, ignoreCase = true) }
                    AdminFilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = cat,
                        countBadge = count,
                    )
                }
            }
        }

        if (filteredLogs.isEmpty()) {
            item {
                EmptyState(
                    title = "No matching activity logs",
                    message = "Perform actions in Products, Orders, Offers, or Home Designer to see local events here.",
                    actionLabel = if (selectedCategory != "All" || searchQuery.isNotBlank()) "Reset filters" else null,
                    onAction = {
                        selectedCategory = "All"
                        searchQuery = ""
                    },
                )
            }
        }

        items(
            items = filteredLogs,
            key = { "${it.timestampMillis}:${it.title.hashCode()}:${it.detail.hashCode()}" },
        ) { entry ->
            val (icon, tint, softBg) = when (entry.category.lowercase()) {
                "products" -> Triple(AdminIcons.Package, AdminColorScheme.primary, AdminColorScheme.primarySoft)
                "orders" -> Triple(AdminIcons.Orders, AdminColorScheme.success, AdminColorScheme.successSoft)
                "content" -> Triple(AdminIcons.Themes, AdminColorScheme.warning, AdminColorScheme.warningSoft)
                "offers" -> Triple(AdminIcons.Tag, Color(0xFF7C4DFF), Color(0xFFF2EEFF))
                else -> Triple(AdminIcons.Activity, AdminColorScheme.info, AdminColorScheme.infoSoft)
            }

            AdminSurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(softBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = entry.title,
                                fontWeight = FontWeight.Bold,
                                color = AdminColorScheme.primaryText,
                                fontSize = 14.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(6.dp))
                            StatusChip(entry.category)
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = entry.detail,
                            color = AdminColorScheme.primaryText,
                            fontSize = 12.5.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = formatActivityTimestamp(entry.timestampMillis),
                            color = AdminColorScheme.secondaryText,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AdminDialog(
            title = "Reset local activity log?",
            message = "This will clear custom activity events recorded on this device and restore the initial sample log.",
            confirmLabel = "Reset Log",
            isDestructive = true,
            onConfirm = {
                confirmClear = false
                onClearLogs()
            },
            onDismiss = { confirmClear = false },
        )
    }
}
