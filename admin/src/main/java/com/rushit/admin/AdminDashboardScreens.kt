package com.rushit.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

@Composable
fun DashboardScreen(
    products: List<String>,
    orders: List<String>,
    customerCount: Int,
    adminName: String,
    campaigns: List<String>,
    offers: List<String>,
    onOpen: (String) -> Unit,
) {
    val themeConfig = LocalAdminThemeConfig.current
    val brand = themeConfig.activePrimary
    val brandSoft = themeConfig.activePrimarySoft
    val firstName = adminName.substringBefore(' ').ifBlank { adminName }
    val initials = adminName.split(' ').mapNotNull { it.firstOrNull() }.take(2).joinToString("").ifBlank { "A" }

    val lowStockProducts = remember(products) {
        products.filter {
            val record = visibleProductRecord(it)
            record.contains("Low stock", true) || record.contains("Out of stock", true)
        }
    }
    val orderStatuses = remember(orders) { orders.map { extractOrderStatus(it) } }
    val inProgressCount = orderStatuses.count { it !in setOf("Delivered", "Cancelled") }
    val deliveredCount = orderStatuses.count { it.equals("Delivered", true) }
    val cancelledCount = orderStatuses.count { it.equals("Cancelled", true) }
    val pendingTasks = inProgressCount + lowStockProducts.size
    val activeCampaigns = remember(campaigns) {
        campaigns.count { raw ->
            val fields = raw.split('|')
            fields.getOrNull(4) != "Disabled"
        }
    }
    val activeOffers = remember(offers) {
        offers.count { it.split('|').getOrNull(8) != "Disabled" }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "RushIt Admin",
                        color = brand,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp,
                    )
                    Text(
                        text = "${greetingForHour()}, $firstName",
                        color = AdminColorScheme.primaryText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = "Store overview · Local demo data · ${todayDisplayDate()}",
                        color = AdminColorScheme.secondaryText,
                        fontSize = 12.sp,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(AdminColorScheme.accentSoft)
                        .clickable(role = Role.Button) { onOpen("notifications") },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = AdminIcons.Bell,
                        contentDescription = "Notifications",
                        tint = brand,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(brandSoft)
                        .clickable(role = Role.Button) { onOpen("profile") },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(initials, color = brand, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiCard(
                    title = "Orders",
                    value = orders.size.toString(),
                    detail = "Local demo records",
                    icon = AdminIcons.Orders,
                    tint = brand,
                    soft = brandSoft,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpen("orders") },
                )
                KpiCard(
                    title = "In Progress",
                    value = inProgressCount.toString(),
                    detail = "Active fulfillment",
                    icon = AdminIcons.Clock,
                    tint = AdminColorScheme.warning,
                    soft = AdminColorScheme.warningSoft,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpen("orders") },
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiCard(
                    title = "Products",
                    value = products.size.toString(),
                    detail = "Local catalog",
                    icon = AdminIcons.Package,
                    tint = AdminColorScheme.info,
                    soft = AdminColorScheme.infoSoft,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpen("catalog") },
                )
                KpiCard(
                    title = "Delivered",
                    value = deliveredCount.toString(),
                    detail = "Completed demo orders",
                    icon = AdminIcons.Check,
                    tint = AdminColorScheme.success,
                    soft = AdminColorScheme.successSoft,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpen("orders") },
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiCard(
                    title = "Customers",
                    value = customerCount.toString(),
                    detail = "Illustrative demo list",
                    icon = AdminIcons.Users,
                    tint = Color(0xFF7C4DFF),
                    soft = Color(0xFFF2EEFF),
                    modifier = Modifier.weight(1f),
                    onClick = { onOpen("customers") },
                )
                KpiCard(
                    title = "Attention",
                    value = lowStockProducts.size.toString(),
                    detail = "Low / out of stock",
                    icon = AdminIcons.AlertTriangle,
                    tint = AdminColorScheme.error,
                    soft = AdminColorScheme.errorSoft,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpen("catalog") },
                )
            }
        }

        item { SalesAnalyticsCard(orders = orders, onOpenOrders = { onOpen("orders") }) }
        item { OrderStatusDistributionCard(orders = orders, onOpenOrders = { onOpen("orders") }) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniInsightCard(
                    title = "Campaigns",
                    value = "$activeCampaigns active",
                    detail = "${campaigns.size} local drafts",
                    icon = AdminIcons.Sparkles,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpen("festivals") },
                )
                MiniInsightCard(
                    title = "Coupons",
                    value = "$activeOffers live",
                    detail = "${offers.size} local offers",
                    icon = AdminIcons.Tag,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpen("offers") },
                )
            }
        }

        item {
            AdminSurfaceCard(modifier = Modifier.clickable { onOpen("catalog") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (lowStockProducts.isEmpty()) AdminColorScheme.successSoft else AdminColorScheme.warningSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (lowStockProducts.isEmpty()) AdminIcons.Check else AdminIcons.AlertTriangle,
                            contentDescription = null,
                            tint = if (lowStockProducts.isEmpty()) AdminColorScheme.success else AdminColorScheme.warning,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Inventory health", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (lowStockProducts.isEmpty()) "No demo products need restocking"
                            else "${lowStockProducts.size} products need attention",
                            color = AdminColorScheme.secondaryText,
                            fontSize = 12.sp,
                        )
                    }
                    StatusChip(if (lowStockProducts.isEmpty()) "Healthy" else "Low stock")
                }
            }
        }

        item { SectionTitle("Low stock alerts", "Manage", onAction = { onOpen("catalog") }) }
        if (lowStockProducts.isEmpty()) {
            item { EmptyState("Stock looks healthy", "No demo product is marked low or out of stock.") }
        }
        items(lowStockProducts.take(3), key = { it }) { product ->
            val record = visibleProductRecord(product)
            DataRow(
                glyph = "!",
                title = record.substringBefore(" · "),
                detail = record.split(" · ").getOrNull(2).orEmpty(),
                onClick = { onOpen("catalog") },
                icon = AdminIcons.AlertTriangle,
                trailingBadge = record.split(" · ").getOrNull(2),
            )
        }

        item { SectionTitle("Best-selling products", "Catalog", onAction = { onOpen("catalog") }) }
        item {
            EmptyState(
                title = "Sales ranking unavailable",
                message = "Demo orders have no line-item quantities, so no best seller can be calculated honestly.",
            )
        }

        item { SectionTitle("Quick actions", "Shortcuts") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickActionCard("Add Product", AdminIcons.Plus, Modifier.weight(1f)) { onOpen("product_edit") }
                QuickActionCard("Home Designer", AdminIcons.Layout, Modifier.weight(1f)) { onOpen("designer") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickActionCard("View Orders", AdminIcons.Orders, Modifier.weight(1f)) { onOpen("orders") }
                QuickActionCard("Send Notification", AdminIcons.Bell, Modifier.weight(1f)) { onOpen("notifications") }
            }
        }

        item {
            AdminSurfaceCard {
                Text("Pending task summary", fontWeight = FontWeight.Bold)
                Text(
                    text = "$pendingTasks open items · $inProgressCount orders in progress · ${lowStockProducts.size} inventory alerts · $cancelledCount cancelled",
                    color = AdminColorScheme.secondaryText,
                    fontSize = 12.5.sp,
                )
            }
        }

        item { SectionTitle("Recent orders", "View all", onAction = { onOpen("orders") }) }
        itemsIndexed(orders.take(4), key = { index, value -> "$index:$value" }) { index, order ->
            val record = visibleProductRecord(order)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(themeConfig.cardCornerRadius))
                    .clickable { onOpen("order_detail/$index") },
                shape = RoundedCornerShape(themeConfig.cardCornerRadius),
                color = AdminColorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, AdminColorScheme.divider),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(brandSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(AdminIcons.Orders, contentDescription = null, tint = brand, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = extractOrderId(record),
                            fontWeight = FontWeight.Bold,
                            color = AdminColorScheme.primaryText,
                        )
                        Text(
                            text = "${extractOrderCustomer(record)} · ₹${extractOrderAmount(record)}",
                            fontSize = 12.sp,
                            color = AdminColorScheme.secondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    StatusChip(extractOrderStatus(record))
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    detail: String,
    icon: ImageVector,
    tint: Color,
    soft: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = AdminColorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, AdminColorScheme.divider),
        shadowElevation = 1.dp,
        tonalElevation = 0.dp,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(soft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            }
            Text(title, color = AdminColorScheme.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(value, color = AdminColorScheme.primaryText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(detail, color = AdminColorScheme.secondaryText, fontSize = 11.sp, maxLines = 2)
        }
    }
}

@Composable
private fun MiniInsightCard(
    title: String,
    value: String,
    detail: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val brand = LocalAdminThemeConfig.current.activePrimary
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = AdminColorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, AdminColorScheme.divider),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = brand, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, color = AdminColorScheme.secondaryText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(detail, color = AdminColorScheme.secondaryText, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val themeConfig = LocalAdminThemeConfig.current
    Surface(
        modifier = modifier
            .height(64.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = AdminColorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, AdminColorScheme.divider),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = themeConfig.activePrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, fontSize = 12.5.sp, color = AdminColorScheme.primaryText, fontWeight = FontWeight.SemiBold, maxLines = 2)
        }
    }
}

@Composable
fun CatalogScreen(
    products: List<String>,
    categories: List<String>,
    onAdd: () -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onDuplicate: (Int) -> Unit,
    onToggleVisibility: (Int) -> Unit,
    onBulkDelete: (List<Int>) -> Unit,
    onOpenCategories: () -> Unit,
    onOpenRoles: () -> Unit,
) {
    val permissions = LocalAdminPermissions.current
    val canManage = permissions.canManageProducts
    val brand = LocalAdminThemeConfig.current.activePrimary

    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    var sort by rememberSaveable { mutableStateOf("Name") }
    var deleteTarget by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var selected by remember { mutableStateOf(setOf<Int>()) }
    var confirmBulk by remember { mutableStateOf(false) }

    val filtered by remember(products, search, filter, sort, categories) {
        derivedStateOf {
            products.withIndex().filter { indexed ->
                val product = visibleProductRecord(indexed.value)
                val metadata = productMetadata(indexed.value)
                (product.contains(search, true) || metadata.sku.contains(search, true) || metadata.description.contains(search, true)) &&
                    when (filter) {
                        "Low stock" -> product.contains("Low", true)
                        "Out of stock" -> product.contains("Out of stock", true)
                        "In stock" -> product.contains("In stock", true)
                        "Hidden" -> product.contains("Hidden", true)
                        "All" -> true
                        else -> product.split(" · ").getOrNull(3) == filter
                    }
            }.let { list ->
                when (sort) {
                    "Price ↑" -> list.sortedBy {
                        Regex("₹([0-9]+(?:\\.[0-9]+)?)").find(visibleProductRecord(it.value))?.groupValues?.getOrNull(1)?.toDoubleOrNull()
                            ?: Double.MAX_VALUE
                    }
                    "Stock ↑" -> list.sortedBy {
                        productMetadata(it.value).stockQuantity.toIntOrNull()
                            ?: visibleProductRecord(it.value).split(" · ").getOrNull(2)?.let { value ->
                                Regex("[0-9]+").find(value)?.value?.toIntOrNull()
                            } ?: Int.MAX_VALUE
                    }
                    else -> list.sortedBy { visibleProductRecord(it.value).substringBefore(" · ").lowercase() }
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        ScreenHeader(
            title = "Products",
            subtitle = "${products.size} items · local demo catalog",
            onAction = if (canManage) onAdd else null,
            action = "+ Add",
        )
        if (!canManage) {
            RoleAccessBanner(
                roleName = permissions.roleName,
                requiredCapability = "Manage Products",
                onSwitchRole = onOpenRoles,
            )
            Spacer(Modifier.height(8.dp))
        }
        AdminSearchField(search, { search = it }, "Search products, SKU or description…")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 6.dp),
        ) {
            val chips = listOf("All", "In stock", "Low stock", "Out of stock", "Hidden") +
                categories.map { it.substringBefore('|') }.distinct()
            items(chips, key = { it }) { chip ->
                AdminFilterChip(filter == chip, { filter = chip }, chip)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Name", "Price ↑", "Stock ↑").forEach { option ->
                AdminFilterChip(sort == option, { sort = option }, "Sort: $option")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onOpenCategories) {
                Text("Manage categories", color = brand, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
            }
            Spacer(Modifier.weight(1f))
            if (canManage && selected.isNotEmpty()) {
                TextButton(onClick = { confirmBulk = true }) {
                    Text("Delete ${selected.size}", color = AdminColorScheme.error, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (filtered.isEmpty()) {
                item {
                    EmptyState("No matching products", "Try changing your search or filters.", "Clear filters") {
                        search = ""; filter = "All"
                    }
                }
            }
            items(filtered, key = { it.index }) { indexed ->
                val record = visibleProductRecord(indexed.value)
                val metadata = productMetadata(indexed.value)
                val name = record.substringBefore(" · ")
                val status = record.split(" · ").getOrNull(2).orEmpty()
                val hidden = record.contains("Hidden", true)
                val checked = indexed.index in selected
                AdminSurfaceCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (canManage) {
                            AdminCheckbox(checked, {
                                selected = if (it) selected + indexed.index else selected - indexed.index
                            })
                            Spacer(Modifier.width(4.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AdminColorScheme.primarySoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(AdminIcons.Package, null, Modifier.size(20.dp), brand)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val supplemental = listOfNotNull(
                                record.substringAfter(" · ", "").substringBefore(" · ").takeIf { it.isNotBlank() },
                                metadata.salePrice.takeIf { it.isNotBlank() }?.let { "Sale ₹$it" },
                                metadata.sku.takeIf { it.isNotBlank() }?.let { "SKU $it" },
                            ).joinToString(" · ")
                            Text(supplemental, color = AdminColorScheme.secondaryText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (metadata.description.isNotBlank()) {
                                Text(metadata.description, color = AdminColorScheme.secondaryText, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        StatusChip(if (hidden) "Hidden" else status)
                    }
                    if (canManage) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onEdit(indexed.index) }) {
                                Text("Edit", color = brand, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            }
                            TextButton(onClick = { onDuplicate(indexed.index) }) {
                                Text("Duplicate", color = AdminColorScheme.primaryText, fontSize = 12.5.sp)
                            }
                            TextButton(onClick = { onToggleVisibility(indexed.index) }) {
                                Text(if (hidden) "Show" else "Hide", fontSize = 12.5.sp)
                            }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { deleteTarget = indexed.index to name }) {
                                Text("Delete", color = AdminColorScheme.error, fontSize = 12.5.sp)
                            }
                        }
                    }
                }
            }
        }
    }
    deleteTarget?.let { (index, name) ->
        AdminDialog(
            title = "Remove product?",
            message = "Remove $name from this device's local demo catalog? This does not change the RushIt customer app.",
            confirmLabel = "Remove",
            isDestructive = true,
            onConfirm = { onDelete(index); selected = selected - index; deleteTarget = null },
            onDismiss = { deleteTarget = null },
        )
    }
    if (confirmBulk) {
        AdminDialog(
            title = "Delete ${selected.size} products?",
            message = "Selected items will be removed from this device's local demo catalog.",
            confirmLabel = "Delete",
            isDestructive = true,
            onConfirm = {
                onBulkDelete(selected.sortedDescending())
                selected = emptySet()
                confirmBulk = false
            },
            onDismiss = { confirmBulk = false },
        )
    }
}

@Composable
fun OrdersScreen(orders: List<String>, onOpen: (Int) -> Unit, onOpenRoles: () -> Unit) {
    val permissions = LocalAdminPermissions.current
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    val filtered by remember(orders, search, filter) {
        derivedStateOf {
            orders.withIndex().filter { indexed ->
                indexed.value.contains(search, true) && (filter == "All" || indexed.value.contains(filter, true))
            }
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        ScreenHeader("Orders", "${orders.size} local demo orders")
        if (!permissions.canViewOrders) {
            RoleAccessBanner(permissions.roleName, "View Orders", onOpenRoles)
            Spacer(Modifier.height(8.dp))
        }
        AdminSearchField(search, { search = it }, "Search order or customer…")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)) {
            items(listOf("All", "Confirmed", "Preparing", "Rider Ready", "On the way", "Almost there", "Delivered", "Cancelled"), key = { it }) { status ->
                AdminFilterChip(filter == status, { filter = status }, status)
            }
        }
        Text(
            "Date and payment filters are unavailable because these local sample orders contain no timestamps or payment status.",
            color = AdminColorScheme.secondaryText,
            fontSize = 11.sp,
        )
        LazyColumn(
            contentPadding = PaddingValues(vertical = 10.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (filtered.isEmpty()) {
                item { EmptyState("No matching orders", "Try another search or order status.", "Clear filters") { search = ""; filter = "All" } }
            }
            items(filtered, key = { it.index }) { indexed ->
                DataRow(
                    glyph = "#",
                    title = extractOrderId(indexed.value),
                    detail = "${extractOrderCustomer(indexed.value)} · ₹${extractOrderAmount(indexed.value)}",
                    onClick = { onOpen(indexed.index) },
                    icon = AdminIcons.Orders,
                    trailingBadge = extractOrderStatus(indexed.value),
                )
            }
        }
    }
}

@Composable
fun OrderDetailScreen(
    order: String,
    note: String,
    timeline: List<String>,
    onUpdate: (String) -> Unit,
    onSaveNote: (String) -> Unit,
    onOpenRoles: () -> Unit,
) {
    val permissions = LocalAdminPermissions.current
    val canManage = permissions.canManageOrders
    var status by rememberSaveable(order) { mutableStateOf(extractOrderStatus(order)) }
    var confirmAdvance by remember { mutableStateOf(false) }
    var cancelling by remember { mutableStateOf(false) }
    var draftNote by rememberSaveable(order, note) { mutableStateOf(note) }
    val amount = extractOrderAmount(order)
    val next = nextOrderStatus(status)
    val brand = LocalAdminThemeConfig.current.activePrimary

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader(extractOrderId(order), "Creation date unavailable · demo record") }
        item { DemoNotice() }
        if (!canManage) {
            item { RoleAccessBanner(permissions.roleName, "Manage Orders", onOpenRoles) }
        }
        item {
            AdminSurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Customer", fontWeight = FontWeight.Bold)
                        Text(extractOrderCustomer(order).ifBlank { "Customer name unavailable" }, color = AdminColorScheme.secondaryText)
                        Text("Contact and delivery address are not present in this sample order.", color = AdminColorScheme.secondaryText, fontSize = 12.sp)
                    }
                    StatusChip(status)
                }
            }
        }
        item {
            AdminSurfaceCard {
                Text("Order total", fontWeight = FontWeight.Bold)
                Text("₹$amount", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("Line items, quantities, fees and price breakdown are not included in this demo record.", color = AdminColorScheme.secondaryText, fontSize = 12.sp)
            }
        }
        item {
            AdminSurfaceCard {
                Text("Payment status", fontWeight = FontWeight.Bold)
                StatusChip("Not supplied · unknown")
                Text("No payment confirmation or refund operation is available in this demo.", color = AdminColorScheme.secondaryText, fontSize = 12.sp)
            }
        }
        item {
            AdminSurfaceCard {
                Text("Status timeline", fontWeight = FontWeight.Bold)
                Text("Current recorded status · $status", color = brand, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (timeline.isEmpty()) {
                    Text("The sample order originally stored only its current status. New transitions are logged locally on this device.", color = AdminColorScheme.secondaryText, fontSize = 12.sp)
                } else {
                    timeline.forEach { event ->
                        val label = event.substringBefore('|')
                        val stamp = event.substringAfter('|')
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(brand))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(formatActivityTimestamp(stamp.toLongOrNull() ?: 0L), color = AdminColorScheme.secondaryText, fontSize = 11.sp)
                            }
                        }
                    }
                }
                if (canManage) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        if (next != null) {
                            PrimaryButton("Advance to $next", Modifier.weight(1f)) {
                                cancelling = false
                                confirmAdvance = true
                            }
                        }
                        if (canCancelOrder(status)) {
                            AdminSecondaryButton("Cancel order", Modifier.weight(1f), onClick = {
                                cancelling = true
                                confirmAdvance = true
                            })
                        }
                    }
                }
            }
        }
        item {
            AdminSurfaceCard {
                Text("Internal notes", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = draftNote,
                    onValueChange = { draftNote = it.take(240) },
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                    label = { Text("Fulfillment note · local only") },
                    shape = AdminShapes.input,
                    enabled = canManage,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = brand,
                        unfocusedBorderColor = AdminColorScheme.divider,
                        cursorColor = brand,
                    ),
                )
                if (canManage) {
                    PrimaryButton("Save note", Modifier.fillMaxWidth()) { onSaveNote(draftNote.trim()) }
                }
            }
        }
    }
    if (confirmAdvance) {
        AdminDialog(
            title = if (cancelling) "Cancel demo order?" else "Advance demo order?",
            message = if (cancelling) "Cancel this local demo order? No real order or payment is affected."
            else "Update status from $status to ${next ?: status}? No payment, rider, or real order data is involved.",
            confirmLabel = if (cancelling) "Cancel order" else "Update",
            isDestructive = cancelling,
            onConfirm = {
                status = if (cancelling) "Cancelled" else (next ?: status)
                confirmAdvance = false
                onUpdate(status)
            },
            onDismiss = { confirmAdvance = false },
        )
    }
}
