package com.rushit.admin

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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProductForm(
    product: String? = null,
    categories: List<String> = initialCategories,
    onCancel: () -> Unit,
    onSave: (String) -> Unit,
) {
    val original = remember(product) { visibleProductRecord(product.orEmpty()).split(" · ") }
    val metadata = remember(product) { productMetadata(product.orEmpty()) }
    val initialPrice = remember(product) { Regex("[0-9]+(?:\\.[0-9]+)?").find(original.getOrNull(1).orEmpty())?.value.orEmpty() }
    var name by rememberSaveable(product) { mutableStateOf(original.getOrNull(0).orEmpty()) }
    var price by rememberSaveable(product) { mutableStateOf(initialPrice) }
    val initialUnit = remember(product) { original.getOrNull(1)?.substringAfter('/', "unit") ?: "kg" }
    var unit by rememberSaveable(product) { mutableStateOf(initialUnit) }
    val initialStock = remember(product) {
        metadata.stockQuantity.ifBlank {
            if (original.getOrNull(2)?.contains("Low", true) == true) "3"
            else if (original.getOrNull(2)?.contains("Out", true) == true) "0"
            else "10"
        }
    }
    var stock by rememberSaveable(product) { mutableStateOf(initialStock) }
    var description by rememberSaveable(product) { mutableStateOf(metadata.description) }
    var sku by rememberSaveable(product) { mutableStateOf(metadata.sku) }
    var salePrice by rememberSaveable(product) { mutableStateOf(metadata.salePrice) }
    var category by rememberSaveable(product) { mutableStateOf(original.getOrNull(3) ?: "Vegetables") }
    var visible by rememberSaveable(product) { mutableStateOf(original.getOrNull(4) != "Hidden") }
    var saved by rememberSaveable(product) { mutableStateOf(false) }
    var error by rememberSaveable(product) { mutableStateOf("") }
    var confirmDiscard by remember { mutableStateOf(false) }
    val hasChanges = name != original.getOrNull(0).orEmpty() || price != initialPrice || unit != initialUnit ||
        description != metadata.description || sku != metadata.sku || salePrice != metadata.salePrice ||
        category != (original.getOrNull(3) ?: "Vegetables") || stock != initialStock || visible != (original.getOrNull(4) != "Hidden")
    val cancel = { if (hasChanges) confirmDiscard = true else onCancel() }
    androidx.activity.compose.BackHandler(enabled = hasChanges) { confirmDiscard = true }
    val brand = LocalAdminThemeConfig.current.activePrimary

    val window = LocalAdminWindow.current
    AdaptiveContent(Modifier.fillMaxSize()) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = window.pagePadding),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { ScreenHeader(if (product == null) "Add Product" else "Edit Product", "Saved in the local demo catalog", backAction = cancel) }
        item {
            AdminSurfaceCard {
                Text("Product image", fontWeight = FontWeight.Bold)
                Box(
                    Modifier.fillMaxWidth().height(if (window.isWide) 168.dp else 120.dp).padding(top = 8.dp).clip(RoundedCornerShape(15.dp)).background(AdminColorScheme.primarySoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(AdminIcons.ImagePlaceholder, null, Modifier.size(28.dp), brand)
                        Spacer(Modifier.height(6.dp))
                        Text("Image picker not connected in demo", color = AdminColorScheme.secondaryText, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        item { AdminField("Product name", name, { name = it }) }
        item { AdminField("Description", description, { description = it }, singleLine = false) }
        item {
            AdaptiveFormRow(
                columns = window.formColumns,
                first = { AdminField("SKU · optional", sku, { sku = it.trim().take(40) }) },
                second = { AdminField("Price (₹)", price, { price = it }, KeyboardType.Decimal) },
            )
        }
        item {
            AdaptiveFormRow(
                columns = window.formColumns,
                first = { AdminField("Sale price (₹) · optional", salePrice, { salePrice = it }, KeyboardType.Decimal) },
                second = { AdminField("Stock quantity", stock, { stock = it }, KeyboardType.Number) },
            )
        }
        item {
            AdminSurfaceCard {
                Text("Unit", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("kg", "piece", "L", "pack").forEach { AdminFilterChip(unit == it, { unit = it }, it) }
                }
            }
        }
        item {
            AdminSurfaceCard {
                Text("Category", fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories.map { it.substringBefore('|') }.filter { it.isNotBlank() }) { option ->
                        AdminFilterChip(category == option, { category = option }, option)
                    }
                }
            }
        }

        item { AdminSurfaceCard { ToggleRow("Visible in demo catalog", visible) { visible = it } } }
        item { if (error.isNotBlank()) Text(error, color = AdminColorScheme.error, fontSize = 13.sp) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminSecondaryButton("Cancel", Modifier.weight(1f), onClick = cancel)
                PrimaryButton(if (product == null) "Save Product" else "Save Changes", Modifier.weight(1f), enabled = !saved) {
                    if (saved) return@PrimaryButton
                    val parsedPrice = price.toDoubleOrNull()
                    val parsedSalePrice = salePrice.toDoubleOrNull()
                    val count = stock.toIntOrNull()
                    error = when {
                        name.isBlank() -> "Product name is required."
                        parsedPrice == null || parsedPrice < 0.01 -> "Enter a price of at least ₹0.01."
                        salePrice.isNotBlank() && (parsedSalePrice == null || parsedSalePrice < 0.01 || parsedSalePrice >= parsedPrice) ->
                            "Sale price must be greater than zero and lower than the regular price."
                        count == null || count < 0 -> "Stock must be a non-negative whole number."
                        else -> ""
                    }
                    if (error.isBlank()) {
                        val validCount = count ?: return@PrimaryButton
                        val validPrice = parsedPrice ?: return@PrimaryButton
                        saved = true
                        onSave(
                            withProductMetadata(
                                "${cleanStoredField(name)} · ₹${formatRupees(validPrice)}/$unit · ${productAvailabilityLabel(validCount)} · ${cleanStoredField(category)} · ${if (visible) "Visible" else "Hidden"}",
                                ProductMetadata(cleanStoredField(sku), cleanStoredField(description), salePrice.trim(), validCount.toString()),
                            )
                        )
                    }
                }
            }
        }
    }
    if (confirmDiscard) {
        AdminDialog(
            title = "Discard product changes?",
            message = "Your unsaved product edits will be lost.",
            confirmLabel = "Discard",
            isDestructive = true,
            onConfirm = { confirmDiscard = false; onCancel() },
            onDismiss = { confirmDiscard = false },
        )
    }
    }
}

@Composable
fun CustomersScreen(onOpen: (Int) -> Unit, notes: Map<String, String>, onOpenRoles: () -> Unit) {
    val permissions = LocalAdminPermissions.current
    var search by rememberSaveable { mutableStateOf("") }
    val filtered by remember(search) {
        derivedStateOf { demoCustomers.withIndex().filter { it.value.contains(search, true) } }
    }
    val window = LocalAdminWindow.current
    Column(Modifier.fillMaxSize().adminPageInsets()) {
        ScreenHeader("Customers", "${demoCustomers.size} demo customers")
        if (!permissions.canManageCustomers) {
            RoleAccessBanner(permissions.roleName, "Manage Customers", onOpenRoles)
            Spacer(Modifier.height(8.dp))
        }
        AdminSearchField(search, { search = it }, "Search customers…")
        LazyColumn(contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (filtered.isEmpty()) item { EmptyState("No customers found", "Try another name or city.") }
            items(
                filtered.chunked(window.listColumns.coerceAtLeast(1)),
                key = { row -> row.joinToString("-") { it.index.toString() } },
            ) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { indexed ->
                        val hasNote = notes[indexed.index.toString()].orEmpty().isNotBlank()
                        DataRow(
                            title = indexed.value.substringBefore(" · "),
                            detail = indexed.value.substringAfter(" · ") + if (hasNote) " · note saved" else "",
                            onClick = { onOpen(indexed.index) },
                            icon = AdminIcons.Users,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(window.listColumns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
fun CustomerDetailScreen(
    index: Int,
    note: String,
    onSaveNote: (String) -> Unit,
) {
    val record = demoCustomers.getOrNull(index) ?: demoCustomers.first()
    var draft by rememberSaveable(index, note) { mutableStateOf(note) }
    val permissions = LocalAdminPermissions.current
    LazyColumn(Modifier.fillMaxSize().adminPageInsets(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { ScreenHeader(record.substringBefore(" · "), "Customer profile · local demo") }
        item { DemoNotice() }
        item {
            AdaptiveTwoPane(
                start = {
                    AdminSurfaceCard {
                        Text("Customer information", fontWeight = FontWeight.Bold)
                        Text("Demo ID · CUST-${1001 + index}")
                        Text("City · ${record.split(" · ").getOrNull(1)}")
                        Text("Contact details are illustrative only", color = AdminColorScheme.secondaryText)
                        Spacer(Modifier.height(8.dp))
                        Text("Account status", fontWeight = FontWeight.Bold)
                        StatusChip("Active · Demo")
                        Text("Local status only · no customer account is changed", color = AdminColorScheme.secondaryText, fontSize = 12.sp)
                    }
                },
                end = {
                    AdminSurfaceCard {
                        Text("Order history", fontWeight = FontWeight.Bold)
                        Text(record.substringAfterLast(" · "))
                        Text("Recent order · ${initialOrders[index % initialOrders.size].substringBefore(" · ")}", color = AdminColorScheme.secondaryText)
                        Text("Total spend is not calculated because demo orders are not linked to customer IDs.", color = AdminColorScheme.secondaryText, fontSize = 12.sp)
                    }
                },
            )
        }
        item {
            AdminSurfaceCard {
                Text("Support notes", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(240) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    label = { Text("Internal note · local only") },
                    enabled = permissions.canManageCustomers,
                    shape = AdminShapes.input,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LocalAdminThemeConfig.current.activePrimary,
                        unfocusedBorderColor = AdminColorScheme.divider,
                        cursorColor = LocalAdminThemeConfig.current.activePrimary,
                    ),
                )
                if (permissions.canManageCustomers) {
                    PrimaryButton("Save note", Modifier.fillMaxWidth()) { onSaveNote(draft.trim()) }
                }
            }
        }
    }
}

@Composable
fun NotificationScreen(
    prefs: android.content.SharedPreferences,
    onSend: () -> Unit,
    onOpenRoles: () -> Unit,
) {
    val permissions = LocalAdminPermissions.current
    val canManage = permissions.canManageNotifications
    val brand = LocalAdminThemeConfig.current.activePrimary
    var title by rememberSaveable { mutableStateOf(prefs.getString("notification_draft_title", "") ?: "") }
    var message by rememberSaveable { mutableStateOf(prefs.getString("notification_draft_message", "") ?: "") }
    var audience by rememberSaveable { mutableStateOf(prefs.getString("notification_draft_audience", "All demo customers") ?: "All demo customers") }
    var error by rememberSaveable { mutableStateOf("") }
    var feedback by rememberSaveable { mutableStateOf("") }
    val history = remember(prefs) {
        mutableStateListOf(*(prefs.getString("notifications", null)?.split("##")?.filter { it.isNotBlank() }?.toTypedArray() ?: emptyArray()))
    }
    val audienceCount = when (audience) {
        "Patna demo customers" -> demoCustomers.count { it.split(" · ").getOrNull(1) == "Patna" }
        "Other demo cities" -> demoCustomers.count { it.split(" · ").getOrNull(1) != "Patna" }
        else -> demoCustomers.size
    }
    LazyColumn(
        Modifier.fillMaxSize().adminPageInsets(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { ScreenHeader("Notifications", "Local drafts and previews · no push service") }
        if (!canManage) item { RoleAccessBanner(permissions.roleName, "Manage Notifications", onOpenRoles) }
        item { DemoNotice() }
        item { AdminField("Notification title", title, { title = it.take(60) }) }
        item {
            OutlinedTextField(
                message,
                { message = it.take(240) },
                Modifier.fillMaxWidth().heightIn(min = 110.dp),
                label = { Text("Message · max 240 characters") },
                enabled = canManage,
                shape = AdminShapes.input,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = brand,
                    unfocusedBorderColor = AdminColorScheme.divider,
                    cursorColor = brand,
                ),
            )
        }
        item { Text("Demo audience", fontWeight = FontWeight.Bold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("All demo customers", "Patna demo customers", "Other demo cities").forEach {
                    AdminFilterChip(audience == it, { if (canManage) audience = it }, it)
                }
            }
        }
        item { Text("Estimated sample audience · $audienceCount demo profiles · no real recipients", color = AdminColorScheme.secondaryText, fontSize = 12.sp) }
        item {
            AdminSurfaceCard {
                Text("Preview", fontWeight = FontWeight.Bold)
                Text(title.ifBlank { "Notification title" }, color = AdminColorScheme.primaryText, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                Text(message.ifBlank { "Your message preview will appear here." }, color = AdminColorScheme.secondaryText)
                Text(audience, color = brand, fontSize = 11.sp)
            }
        }
        item { Text("Push notifications are not configured. These actions only save a local draft or preview; no customer is contacted.", color = AdminColorScheme.secondaryText, fontSize = 12.sp) }
        if (canManage) {
            item {
                if (error.isNotBlank()) Text(error, color = AdminColorScheme.error)
                if (feedback.isNotBlank()) Text(feedback, color = AdminColorScheme.success)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminSecondaryButton("Save draft", Modifier.weight(1f)) {
                        prefs.edit().putString("notification_draft_title", title).putString("notification_draft_message", message).putString("notification_draft_audience", audience).apply()
                        feedback = "Draft saved on this device."
                    }
                    PrimaryButton("Save preview", Modifier.weight(1f)) {
                        error = when {
                            title.isBlank() -> "Add a notification title."
                            message.isBlank() -> "Add a notification message."
                            else -> ""
                        }
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
        }
        item { SectionTitle("Saved local previews", "${history.size} records") }
        if (history.isEmpty()) item { EmptyState("No saved previews", "Your locally saved notification previews will appear here.") }
        itemsIndexed(history, key = { index, value -> "$index:${value.hashCode()}" }) { _, record ->
            DataRow("N", record, "Not delivered to real devices", icon = AdminIcons.Bell)
        }
    }
}

@Composable
fun SettingsScreen(
    prefs: android.content.SharedPreferences,
    themeName: String,
    onReset: () -> Unit,
    onOpenRoles: () -> Unit,
    onAnimationsChange: (Boolean) -> Unit = {},
) {
    val permissions = LocalAdminPermissions.current
    val canManage = permissions.canManageSettings
    var storeName by remember(prefs) { mutableStateOf(prefs.getString("store_name", "RushIt") ?: "RushIt") }
    var supportEmail by remember(prefs) { mutableStateOf(prefs.getString("support_email", "support@rushit.in") ?: "support@rushit.in") }
    var maintenancePreview by remember(prefs) { mutableStateOf(prefs.getBoolean("maintenance", false)) }
    var animations by remember(prefs) { mutableStateOf(prefs.getBoolean("animations", true)) }
    var error by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    var confirmReset by remember { mutableStateOf(false) }

    AdaptiveContent(Modifier.fillMaxSize()) {
    LazyColumn(
        Modifier.fillMaxSize().adminPageInsets(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item { ScreenHeader("App Settings", "Preferences stored in this Admin app only") }
        if (!canManage) item { RoleAccessBanner(permissions.roleName, "Manage Settings", onOpenRoles) }
        item { DemoNotice() }
        item {
            AdminSurfaceCard {
                Text("Store identity · local demo", fontWeight = FontWeight.Bold)
                AdaptiveFormRow(
                    first = { AdminField("Store name", storeName, { if (canManage) storeName = it.take(50) }) },
                    second = { AdminField("Support email", supportEmail, { if (canManage) supportEmail = it.trim().take(80) }, KeyboardType.Email) },
                )
                Text("Currency · Indian Rupee (₹) · fixed for this local India demo", color = AdminColorScheme.secondaryText, fontSize = 12.sp)
                Text("Current preview palette · $themeName", color = AdminColorScheme.secondaryText, fontSize = 12.sp)
            }
        }
        item {
            AdminSurfaceCard {
                Text("Local preview controls", fontWeight = FontWeight.Bold)
                ToggleRow("Preview maintenance banner flag", maintenancePreview, enabled = canManage) {
                    maintenancePreview = it
                    prefs.edit().putBoolean("maintenance", it).apply()
                }
                Text("This flag is visible only in Admin settings; it does not change the User app or store availability.", color = AdminColorScheme.secondaryText, fontSize = 11.sp)
                ToggleRow("Preview motion", animations, enabled = canManage) {
                    animations = it
                    prefs.edit().putBoolean("animations", it).apply()
                    onAnimationsChange(it)
                }
                Text("Disabling motion removes section entrance transitions in the local Home Preview.", color = AdminColorScheme.secondaryText, fontSize = 11.sp)
            }
        }
        if (canManage) {
            item {
                if (error.isNotBlank()) Text(error, color = AdminColorScheme.error)
                if (feedback.isNotBlank()) Text(feedback, color = AdminColorScheme.success)
                PrimaryButton("Save local settings", Modifier.fillMaxWidth()) {
                    error = when {
                        storeName.isBlank() -> "Enter a store name."
                        !android.util.Patterns.EMAIL_ADDRESS.matcher(supportEmail).matches() -> "Enter a valid support email address."
                        else -> ""
                    }
                    if (error.isBlank()) {
                        prefs.edit().putString("store_name", storeName.trim()).putString("support_email", supportEmail.trim()).apply()
                        feedback = "Local store identity saved. The customer app remains unchanged."
                    }
                }
            }
        }
        item {
            AdminSurfaceCard {
                Text("Information", fontWeight = FontWeight.Bold)
                Text("RushIt Admin · version 1.0 demo", color = AdminColorScheme.secondaryText, modifier = Modifier.padding(top = 7.dp))
                Text("No admin backend, policy links, payment settings, tax configuration, or delivery controls are connected.", color = AdminColorScheme.secondaryText, fontSize = 12.sp)
            }
        }
        if (canManage) {
            item { AdminSecondaryButton("Reset local demo data", Modifier.fillMaxWidth()) { confirmReset = true } }
        }
    }
    if (confirmReset) {
        AdminDialog(
            title = "Reset local demo data?",
            message = "Saved catalog, sections, theme, local preferences, drafts, and profile details on this device will be reset.",
            confirmLabel = "Reset",
            isDestructive = true,
            onConfirm = { onReset(); confirmReset = false },
            onDismiss = { confirmReset = false },
        )
    }
    }
}

@Composable
fun ProfileScreen(
    prefs: android.content.SharedPreferences,
    currentRole: String,
    onRoleChange: (String) -> Unit,
    onLogout: () -> Unit,
    onOpenRoles: () -> Unit,
) {
    var name by remember(prefs) { mutableStateOf(prefs.getString("admin_name", "Dharmjeet Singh") ?: "Dharmjeet Singh") }
    var email by remember(prefs) { mutableStateOf(prefs.getString("admin_email", "admin@rushit.in") ?: "admin@rushit.in") }
    var edit by remember { mutableStateOf(false) }
    var profileError by remember { mutableStateOf("") }
    var passwordInfo by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    val brand = LocalAdminThemeConfig.current.activePrimary
    val initials = name.split(' ').mapNotNull { it.firstOrNull() }.take(2).joinToString("")

    LazyColumn(Modifier.fillMaxSize().adminPageInsets(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { ScreenHeader("Admin Profile", "Demo account · local only") }
        item {
            AdminSurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).clip(CircleShape).background(AdminColorScheme.primarySoft), contentAlignment = Alignment.Center) {
                        Text(initials, color = brand, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text(email, color = AdminColorScheme.secondaryText, fontSize = 13.sp)
                        StatusChip(currentRole)
                    }
                    TextButton(onClick = { edit = true }) { Text("Edit", color = brand, fontWeight = FontWeight.Bold) }
                }
            }
        }
        item {
            AdminSurfaceCard {
                Text("Role visibility · demo only", fontWeight = FontWeight.Bold)
                Text("Client-side role visibility is not an authorization boundary.", color = AdminColorScheme.secondaryText, fontSize = 12.sp)
                AdminRoleSpec.entries.forEach { spec ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onRoleChange(spec.label) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AdminRadioButton(currentRole == spec.label, { onRoleChange(spec.label) })
                        Column(Modifier.weight(1f)) {
                            Text(spec.label, color = AdminColorScheme.primaryText, fontWeight = FontWeight.SemiBold)
                            Text(spec.shortTag, color = AdminColorScheme.secondaryText, fontSize = 11.sp)
                        }
                    }
                }
                TextButton(onClick = onOpenRoles) { Text("Open full permission matrix", color = brand, fontWeight = FontWeight.Bold) }
            }
        }
        item { DataRow("P", "Change password", "Demo only · no real password is stored", { passwordInfo = true }, icon = AdminIcons.Lock) }
        item {
            AdminSurfaceCard {
                Text("Session", fontWeight = FontWeight.Bold)
                Text("Signed in · Demo session", color = AdminColorScheme.secondaryText)
                Text("Login history · Today", color = AdminColorScheme.secondaryText)
            }
        }
        item { Text("This local prototype does not authenticate or authorize production administrators.", color = AdminColorScheme.secondaryText, fontSize = 12.sp) }
        item { AdminSecondaryButton("Log out", Modifier.fillMaxWidth()) { confirmLogout = true } }
    }
    if (confirmLogout) {
        AdminDialog(
            title = "End this demo session?",
            message = "You will return to the local demo sign-in screen. No production account is affected.",
            confirmLabel = "Log out",
            isDestructive = true,
            onConfirm = { confirmLogout = false; onLogout() },
            onDismiss = { confirmLogout = false },
        )
    }
    if (edit) {
        var draftName by remember(name) { mutableStateOf(name) }
        var draftEmail by remember(email) { mutableStateOf(email) }
        AdminDialog(
            title = "Edit demo profile",
            confirmLabel = "Save",
            onConfirm = {
                profileError = when {
                    draftName.isBlank() -> "Enter a display name."
                    !android.util.Patterns.EMAIL_ADDRESS.matcher(draftEmail).matches() -> "Enter a valid email address."
                    else -> ""
                }
                if (profileError.isBlank()) {
                    name = draftName.trim()
                    email = draftEmail.trim()
                    prefs.edit().putString("admin_name", name).putString("admin_email", email).apply()
                    edit = false
                }
            },
            onDismiss = { edit = false; profileError = "" },
        ) {
            AdaptiveFormRow(
                first = { AdminField("Display name", draftName, { draftName = it }) },
                second = { AdminField("Demo email", draftEmail, { draftEmail = it }, KeyboardType.Email) },
            )
            if (profileError.isNotBlank()) Text(profileError, color = AdminColorScheme.error, fontSize = 12.sp)
        }
    }
    if (passwordInfo) {
        AdminDialog(
            title = "Password settings",
            message = "Password changes are not available in this offline demo. No password is stored or changed.",
            confirmLabel = "Got it",
            onConfirm = { passwordInfo = false },
            onDismiss = { passwordInfo = false },
        )
    }
}
