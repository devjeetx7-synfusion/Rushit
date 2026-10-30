package com.rushit.admin

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

data class AdminNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val section: String = "Workspace",
)

fun primaryNavItems(): List<AdminNavItem> = listOf(
    AdminNavItem(AdminRoutes.HOME, "Home", AdminIcons.Home, "Overview"),
    AdminNavItem(AdminRoutes.CATALOG, "Catalog", AdminIcons.Catalog, "Catalog"),
    AdminNavItem(AdminRoutes.ORDERS, "Orders", AdminIcons.Orders, "Operations"),
    AdminNavItem(AdminRoutes.THEMES, "Themes", AdminIcons.Themes, "Content"),
    AdminNavItem(AdminRoutes.MORE, "More", AdminIcons.More, "Workspace"),
)

fun expandedNavItems(): List<AdminNavItem> = listOf(
    AdminNavItem(AdminRoutes.HOME, "Dashboard", AdminIcons.Home, "Overview"),
    AdminNavItem(AdminRoutes.CATALOG, "Catalog", AdminIcons.Catalog, "Catalog"),
    AdminNavItem(AdminRoutes.ORDERS, "Orders", AdminIcons.Orders, "Operations"),
    AdminNavItem(AdminRoutes.CUSTOMERS, "Customers", AdminIcons.Users, "Operations"),
    AdminNavItem(AdminRoutes.THEMES, "Themes", AdminIcons.Themes, "Content"),
    AdminNavItem(AdminRoutes.FESTIVALS, "Campaigns", AdminIcons.Sparkles, "Content"),
    AdminNavItem(AdminRoutes.MORE, "More", AdminIcons.More, "System"),
)

/**
 * Switch root tabs without stacking duplicates. Nested destinations on the same
 * tab pop back to the tab root; switching tabs pops to Home (saved) then restores.
 */
fun NavHostController.navigateToAdminTab(destination: String, currentRoute: String?) {
    val parent = AdminRoutes.parentTab(currentRoute)
    if (parent == destination) {
        if (currentRoute != destination) {
            val popped = popBackStack(destination, inclusive = false)
            if (!popped) {
                navigate(destination) { launchSingleTop = true }
            }
        }
        return
    }
    navigate(destination) {
        popUpTo(AdminRoutes.HOME) {
            saveState = true
            inclusive = false
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AdminNavRail(
    selectedRoute: String,
    onSelectTab: (String) -> Unit,
    onOpenProfile: () -> Unit,
    initials: String,
    modifier: Modifier = Modifier,
    badgeCounts: Map<String, Int> = emptyMap(),
) {
    val themeConfig = LocalAdminThemeConfig.current
    val brand = themeConfig.activePrimary
    val brandSoft = themeConfig.activePrimarySoft
    val items = remember { primaryNavItems() }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(88.dp),
        color = AdminColorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 10.dp, horizontal = 8.dp)
                .selectableGroup(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RushItBrandLogo(size = 40.dp)
            Spacer(Modifier.height(14.dp))
            items.forEach { item ->
                val selected = selectedRoute == item.route
                RailItem(
                    item = item,
                    selected = selected,
                    brand = brand,
                    brandSoft = brandSoft,
                    badge = badgeCounts[item.route] ?: 0,
                    compact = true,
                    onClick = { onSelectTab(item.route) },
                )
                Spacer(Modifier.height(4.dp))
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(brandSoft)
                    .clickable(role = Role.Button) { onOpenProfile() }
                    .semantics { contentDescription = "Admin profile" },
                contentAlignment = Alignment.Center,
            ) {
                Text(initials, color = brand, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun AdminNavSidebar(
    selectedRoute: String,
    onSelect: (String) -> Unit,
    onOpenProfile: () -> Unit,
    storeName: String,
    adminName: String,
    roleName: String,
    initials: String,
    modifier: Modifier = Modifier,
    badgeCounts: Map<String, Int> = emptyMap(),
) {
    val themeConfig = LocalAdminThemeConfig.current
    val brand = themeConfig.activePrimary
    val brandSoft = themeConfig.activePrimarySoft
    val items = remember { expandedNavItems() }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(248.dp),
        color = AdminColorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 14.dp, vertical = 16.dp),
        ) {
            RowBrand(storeName)
            Spacer(Modifier.height(18.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                var lastSection = ""
                items.forEach { item ->
                    if (item.section != lastSection) {
                        Text(
                            text = item.section.uppercase(),
                            color = AdminColorScheme.mutedText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(start = 10.dp, top = 10.dp, bottom = 4.dp),
                        )
                        lastSection = item.section
                    }
                    RailItem(
                        item = item,
                        selected = AdminRoutes.isNavItemSelected(item.route, selectedRoute),
                        brand = brand,
                        brandSoft = brandSoft,
                        badge = badgeCounts[item.route] ?: 0,
                        compact = false,
                        onClick = { onSelect(item.route) },
                    )
                }
            }
            HorizontalDivider(color = AdminColorScheme.divider, modifier = Modifier.padding(vertical = 10.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button) { onOpenProfile() },
                color = brandSoft,
                tonalElevation = 0.dp,
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(brand),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(adminName, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(roleName, color = AdminColorScheme.secondaryText, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowBrand(storeName: String) {
    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
        RushItBrandLogo(size = 40.dp)
        Spacer(Modifier.width(10.dp))
        Column {
            Text("RushIt Admin", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = AdminColorScheme.primaryText)
            Text(storeName, color = AdminColorScheme.secondaryText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RailItem(
    item: AdminNavItem,
    selected: Boolean,
    brand: Color,
    brandSoft: Color,
    badge: Int,
    compact: Boolean,
    onClick: () -> Unit,
) {
    val tint by animateColorAsState(
        targetValue = if (selected) brand else AdminColorScheme.secondaryText,
        animationSpec = tween(160),
        label = "rail_${item.route}",
    )
    val shape = RoundedCornerShape(if (compact) 16.dp else 14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .background(if (selected) brandSoft else Color.Transparent)
            .then(if (selected) Modifier.border(1.dp, brand.copy(alpha = 0.18f), shape) else Modifier)
            .semantics { contentDescription = "${item.label} destination" }
            .padding(vertical = if (compact) 8.dp else 10.dp, horizontal = if (compact) 4.dp else 10.dp),
        horizontalAlignment = if (compact) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                if (badge > 0) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(brand),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(if (badge > 9) "9+" else badge.toString(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (!compact) {
                Spacer(Modifier.width(10.dp))
                Text(
                    item.label,
                    color = tint,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (compact) {
            Spacer(Modifier.height(4.dp))
            Text(
                item.label,
                color = tint,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
