package com.rushit.admin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun campaignStorageField(value: String): String =
    value.replace('|', ' ').replace("##", " ").trim()

private fun isoDate(calendar: Calendar): String = String.format(
    Locale.US,
    "%04d-%02d-%02d",
    calendar.get(Calendar.YEAR),
    calendar.get(Calendar.MONTH) + 1,
    calendar.get(Calendar.DAY_OF_MONTH),
)

private fun displayCampaignDate(value: String): String = runCatching {
    SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value)?.let {
        SimpleDateFormat("d MMM yyyy", Locale.US).format(it)
    } ?: value
}.getOrDefault(value)

private fun validIsoDate(value: String): Boolean = runCatching {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    fmt.parse(value)?.let { fmt.format(it) == value } == true
}.getOrDefault(false)

private fun validCampaignTime(value: String): Boolean =
    Regex("(?:[01]\\d|2[0-3]):[0-5]\\d").matches(value)

private fun campaignStatus(start: String, end: String, enabled: Boolean, today: String): String = when {
    !enabled -> "Inactive"
    start > today -> "Scheduled"
    end < today -> "Expired"
    else -> "Active"
}

private fun campaignEmoji(title: String): String {
    val lower = title.lowercase()
    return when {
        lower.contains("diwali") -> "🪔"
        lower.contains("holi") -> "🎨"
        lower.contains("christmas") || lower.contains("xmas") -> "🎄"
        lower.contains("eid") -> "🌙"
        lower.contains("valentine") -> "💝"
        lower.contains("summer") -> "☀️"
        lower.contains("monsoon") -> "🌧️"
        lower.contains("winter") -> "❄️"
        else -> "✨"
    }
}

private fun campaignThemeColor(colorName: String): Color = when (colorName) {
    "Amber" -> Color(0xFFD99024)
    "Green" -> Color(0xFF27845B)
    "Indigo" -> Color(0xFF2E6B9E)
    "Berry" -> Color(0xFFB8336A)
    else -> AdminColorScheme.primary
}

@Composable
fun CampaignManagerScreen(
    campaigns: List<String>,
    scheduler: Boolean = false,
    onAdd: () -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onOpenRoles: () -> Unit = {},
) {
    val permissions = LocalAdminPermissions.current
    val canManage = permissions.canManageCampaigns
    val brand = LocalAdminThemeConfig.current.activePrimary

    var filter by rememberSaveable { mutableStateOf("All") }
    var query by rememberSaveable { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var previewRecord by remember { mutableStateOf<String?>(null) }

    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val filtered by remember(campaigns, filter, query, today) {
        derivedStateOf {
            campaigns.withIndex().filter { (_, record) ->
                val fields = record.split('|')
                val status = campaignStatus(
                    start = fields.getOrNull(1).orEmpty(),
                    end = fields.getOrNull(2).orEmpty(),
                    enabled = fields.getOrNull(4) != "Disabled",
                    today = today,
                )
                record.contains(query, ignoreCase = true) && (filter == "All" || status == filter)
            }
        }
    }

    val statusFilters = remember { listOf("All", "Active", "Scheduled", "Inactive", "Expired") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenHeader(
                title = if (scheduler) "Campaign Scheduler" else "Festival Manager",
                subtitle = if (scheduler) "Schedule promotional windows · local demo" else "Create seasonal store campaigns · local demo",
                onAction = if (canManage) onAdd else null,
                action = if (scheduler) "+ Schedule" else "+ Festival",
            )
        }
        if (!canManage) {
            item {
                RoleAccessBanner(
                    roleName = permissions.roleName,
                    requiredCapability = "Manage Campaigns & Festivals",
                    onSwitchRole = onOpenRoles,
                )
            }
        }
        item { DemoNotice() }
        item {
            AdminSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = if (scheduler) "Search scheduled campaigns…" else "Search festivals…",
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(statusFilters, key = { it }) { itemFilter ->
                    val count = if (itemFilter == "All") {
                        campaigns.size
                    } else {
                        campaigns.count { raw ->
                            val f = raw.split('|')
                            campaignStatus(f.getOrNull(1).orEmpty(), f.getOrNull(2).orEmpty(), f.getOrNull(4) != "Disabled", today) == itemFilter
                        }
                    }
                    AdminFilterChip(
                        selected = filter == itemFilter,
                        onClick = { filter = itemFilter },
                        label = itemFilter,
                        countBadge = count,
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyState(
                    title = "No campaigns in this view",
                    message = "Change the status filter or create a seasonal campaign.",
                    actionLabel = if (canManage) (if (scheduler) "Schedule Campaign" else "Add Festival") else null,
                    onAction = onAdd,
                )
            }
        }

        itemsIndexed(
            items = filtered,
            key = { _, indexed -> "${indexed.index}:${indexed.value}" },
        ) { _, indexed ->
            val fields = indexed.value.split('|')
            val title = fields.getOrNull(0).orEmpty()
            val start = fields.getOrNull(1).orEmpty()
            val end = fields.getOrNull(2).orEmpty()
            val priority = fields.getOrNull(3) ?: "1"
            val enabled = fields.getOrNull(4) != "Disabled"
            val description = fields.getOrNull(5).orEmpty()
            val colorName = fields.getOrNull(6) ?: "Coral"
            val startTime = fields.getOrNull(7).orEmpty()
            val endTime = fields.getOrNull(8).orEmpty()
            val featuredCats = fields.getOrNull(9).orEmpty()
            val status = campaignStatus(start, end, enabled, today)
            val accentColor = campaignThemeColor(colorName)

            AdminSurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(accentColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(campaignEmoji(title), fontSize = 24.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AdminColorScheme.primaryText,
                        )
                        Text(
                            text = "${displayCampaignDate(start)} – ${displayCampaignDate(end)}",
                            color = AdminColorScheme.secondaryText,
                            fontSize = 12.sp,
                        )
                        if (scheduler && startTime.isNotBlank() && endTime.isNotBlank()) {
                            Text(
                                text = "Window: $startTime to $endTime (local time)",
                                color = accentColor,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        if (description.isNotBlank()) {
                            Text(
                                text = description,
                                color = AdminColorScheme.secondaryText,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                            )
                        }
                        Row(
                            modifier = Modifier.padding(top = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Priority #$priority · $colorName theme",
                                color = AdminColorScheme.mutedText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (featuredCats.isNotBlank()) {
                                Text(
                                    text = "· $featuredCats",
                                    color = accentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    StatusChip(status)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (canManage) {
                        TextButton(onClick = { onEdit(indexed.index) }) {
                            Icon(AdminIcons.Edit, contentDescription = null, tint = brand, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Edit", color = brand, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                    }
                    TextButton(onClick = { previewRecord = indexed.value }) {
                        Text("Preview Banner", color = AdminColorScheme.primaryText, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    if (canManage) {
                        TextButton(onClick = { deleteTarget = indexed.index to title }) {
                            Text("Delete", color = AdminColorScheme.error, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                        }
                    }
                }
            }
        }

        if (canManage) {
            item {
                PrimaryButton(
                    label = if (scheduler) "+ Schedule Campaign" else "+ Add Festival",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onAdd,
                )
            }
        }
        item {
            Text(
                text = "Campaign schedules are evaluated locally while this screen is open. No background job or server push is executed.",
                color = AdminColorScheme.secondaryText,
                fontSize = 11.5.sp,
            )
        }
    }

    previewRecord?.let { record ->
        val fields = record.split('|')
        val title = fields.getOrNull(0).orEmpty()
        val start = fields.getOrNull(1).orEmpty()
        val end = fields.getOrNull(2).orEmpty()
        val priority = fields.getOrNull(3) ?: "1"
        val description = fields.getOrNull(5).orEmpty().ifBlank { "Celebrate with special seasonal RushIt deals!" }
        val colorName = fields.getOrNull(6) ?: "Coral"
        val accent = campaignThemeColor(colorName)

        AdminDialog(
            title = "Campaign Banner Preview",
            confirmLabel = "Close",
            dismissLabel = "Done",
            onConfirm = { previewRecord = null },
            onDismiss = { previewRecord = null },
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = accent,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "${campaignEmoji(title)}  FESTIVAL COLLECTION",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = description,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 12.5.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${displayCampaignDate(start)} – ${displayCampaignDate(end)} · Priority $priority",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }

    deleteTarget?.let { (index, title) ->
        AdminDialog(
            title = if (scheduler) "Delete scheduled campaign?" else "Delete festival?",
            message = "Remove '$title' from this device's local campaign list? The RushIt customer app is not changed.",
            confirmLabel = "Delete",
            isDestructive = true,
            onConfirm = {
                onDelete(index)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
fun CampaignEditorScreen(
    campaign: String?,
    scheduler: Boolean = false,
    onCancel: () -> Unit,
    onSave: (String) -> Unit,
) {
    val fields = remember(campaign) { campaign?.split('|').orEmpty() }
    val initialStart = remember(campaign) {
        fields.getOrNull(1) ?: Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 3) }.let(::isoDate)
    }
    val initialEnd = remember(campaign) {
        fields.getOrNull(2) ?: Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 14) }.let(::isoDate)
    }
    val initialStartTime = fields.getOrNull(7)?.takeIf { it.isNotBlank() } ?: "09:00"
    val initialEndTime = fields.getOrNull(8)?.takeIf { it.isNotBlank() } ?: "23:59"
    val initialFeatured = fields.getOrNull(9) ?: "Sweets, Gifts"

    var name by rememberSaveable(campaign) { mutableStateOf(fields.getOrNull(0).orEmpty()) }
    var start by rememberSaveable(campaign) { mutableStateOf(initialStart) }
    var end by rememberSaveable(campaign) { mutableStateOf(initialEnd) }
    var startTime by rememberSaveable(campaign) { mutableStateOf(initialStartTime) }
    var endTime by rememberSaveable(campaign) { mutableStateOf(initialEndTime) }
    var priority by rememberSaveable(campaign) { mutableStateOf(fields.getOrNull(3) ?: "1") }
    var enabled by rememberSaveable(campaign) { mutableStateOf(fields.getOrNull(4) != "Disabled") }
    var description by rememberSaveable(campaign) { mutableStateOf(fields.getOrNull(5).orEmpty()) }
    var color by rememberSaveable(campaign) { mutableStateOf(fields.getOrNull(6) ?: "Coral") }
    var featuredCategories by rememberSaveable(campaign) { mutableStateOf(initialFeatured) }
    var error by rememberSaveable(campaign) { mutableStateOf("") }
    var confirmDiscard by remember { mutableStateOf(false) }
    var saved by rememberSaveable(campaign) { mutableStateOf(false) }

    val initialName = fields.getOrNull(0).orEmpty()
    val initialPriority = fields.getOrNull(3) ?: "1"
    val initialEnabled = fields.getOrNull(4) != "Disabled"
    val initialDescription = fields.getOrNull(5).orEmpty()
    val initialColor = fields.getOrNull(6) ?: "Coral"
    val hasChanges = name != initialName ||
        start != initialStart ||
        end != initialEnd ||
        startTime != initialStartTime ||
        endTime != initialEndTime ||
        priority != initialPriority ||
        enabled != initialEnabled ||
        description != initialDescription ||
        color != initialColor ||
        featuredCategories != initialFeatured

    val cancel = { if (hasChanges) confirmDiscard = true else onCancel() }
    androidx.activity.compose.BackHandler(enabled = hasChanges) { confirmDiscard = true }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val bannerColor = campaignThemeColor(color)
    val availableCategories = listOf("Sweets", "Gifts", "Decor", "Fruits", "Snacks", "Beverages", "Dairy", "Bakery")
    val selectedCategorySet = remember(featuredCategories) {
        featuredCategories.split(',').map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = if (campaign == null) (if (scheduler) "Schedule Campaign" else "Festival Editor") else "Edit Festival",
                subtitle = name.ifBlank { "Saved locally · no automatic live publishing" },
                backAction = cancel,
            )
        }
        item { DemoNotice() }

        // Hero Banner Preview Card (matches the Festival Editor mockup!)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 4.dp,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    bannerColor,
                                    bannerColor.copy(alpha = 0.82f),
                                    Color(0xFF292524),
                                )
                            )
                        )
                        .padding(20.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.20f),
                            ) {
                                Text(
                                    text = "SEASONAL CAMPAIGN PREVIEW",
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                    color = Color.White,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = name.ifBlank { "Diwali Special" },
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Text(
                                text = description.ifBlank { "Celebrate with special festive offers" },
                                color = Color.White.copy(alpha = 0.90f),
                                fontSize = 13.sp,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "${displayCampaignDate(start)} – ${displayCampaignDate(end)}",
                                color = AdminColorScheme.accent,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(campaignEmoji(name.ifBlank { "Diwali" }), fontSize = 34.sp)
                        }
                    }
                }
            }
        }

        item {
            AdminSurfaceCard {
                Text(
                    text = "Festival Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                AdminField(
                    label = "Festival / Campaign Name",
                    value = name,
                    onValue = { name = it.take(50) },
                )
                AdminField(
                    label = "Promotional Description",
                    value = description,
                    onValue = { description = it.take(120) },
                    singleLine = false,
                )
            }
        }

        item {
            AdminSurfaceCard {
                Text(
                    text = "Schedule & Priority",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminSecondaryButton(
                        label = "Start · ${displayCampaignDate(start)}",
                        icon = AdminIcons.Calendar,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val parts = start.split('-')
                            val y = parts.getOrNull(0)?.toIntOrNull() ?: calendar.get(Calendar.YEAR)
                            val m = (parts.getOrNull(1)?.toIntOrNull() ?: 1) - 1
                            val d = parts.getOrNull(2)?.toIntOrNull() ?: 1
                            DatePickerDialog(context, { _, year, month, day ->
                                start = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                            }, y, m, d).show()
                        },
                    )
                    AdminSecondaryButton(
                        label = "End · ${displayCampaignDate(end)}",
                        icon = AdminIcons.Calendar,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val parts = end.split('-')
                            val y = parts.getOrNull(0)?.toIntOrNull() ?: calendar.get(Calendar.YEAR)
                            val m = (parts.getOrNull(1)?.toIntOrNull() ?: 1) - 1
                            val d = parts.getOrNull(2)?.toIntOrNull() ?: 1
                            DatePickerDialog(context, { _, year, month, day ->
                                end = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                            }, y, m, d).show()
                        },
                    )
                }
                if (scheduler) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdminSecondaryButton(
                            label = "Starts · $startTime",
                            icon = AdminIcons.Clock,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val parts = startTime.split(':')
                                val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
                                val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                TimePickerDialog(context, { _, h, m ->
                                    startTime = String.format(Locale.US, "%02d:%02d", h, m)
                                }, hour, minute, true).show()
                            },
                        )
                        AdminSecondaryButton(
                            label = "Ends · $endTime",
                            icon = AdminIcons.Clock,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val parts = endTime.split(':')
                                val hour = parts.getOrNull(0)?.toIntOrNull() ?: 23
                                val minute = parts.getOrNull(1)?.toIntOrNull() ?: 59
                                TimePickerDialog(context, { _, h, m ->
                                    endTime = String.format(Locale.US, "%02d:%02d", h, m)
                                }, hour, minute, true).show()
                            },
                        )
                    }
                    Text(
                        text = "Local time window · evaluated when viewing Campaign Scheduler.",
                        color = AdminColorScheme.secondaryText,
                        fontSize = 11.sp,
                    )
                }
                AdminField(
                    label = "Priority (1 = highest priority)",
                    value = priority,
                    onValue = { priority = it.filter(Char::isDigit).take(2) },
                    keyboard = KeyboardType.Number,
                )
                ToggleRow(
                    label = "Active in local campaign schedule",
                    subtitle = "Disable to keep as an inactive draft",
                    value = enabled,
                    onChange = { enabled = it },
                )
            }
        }

        // Theme Color Swatches & Featured Categories (matches Festival Editor mockup!)
        item {
            AdminSurfaceCard {
                Text(
                    text = "Theme Color",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    listOf("Coral", "Amber", "Green", "Indigo", "Berry").forEach { option ->
                        val swatchColor = campaignThemeColor(option)
                        val isSelected = color == option
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { color = option },
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) AdminColorScheme.primaryText else Color.Transparent,
                                        shape = CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = AdminIcons.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = option,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) swatchColor else AdminColorScheme.secondaryText,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Featured Categories",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(availableCategories, key = { it }) { cat ->
                        val picked = cat in selectedCategorySet
                        AdminFilterChip(
                            selected = picked,
                            onClick = {
                                val next = selectedCategorySet.toMutableSet()
                                if (picked) next.remove(cat) else next.add(cat)
                                featuredCategories = next.joinToString(", ")
                            },
                            label = cat,
                        )
                    }
                }
            }
        }

        item {
            if (error.isNotBlank()) {
                Text(
                    text = error,
                    color = AdminColorScheme.error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminSecondaryButton(
                    label = "Cancel",
                    modifier = Modifier.weight(1f),
                    onClick = cancel,
                )
                PrimaryButton(
                    label = if (scheduler) "Save Schedule" else "Save Festival",
                    enabled = !saved,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val p = priority.toIntOrNull()
                        error = when {
                            name.isBlank() -> "Campaign name is required."
                            !validIsoDate(start) || !validIsoDate(end) -> "Select valid YYYY-MM-DD start and end dates."
                            end < start -> "End date must not be before the start date."
                            scheduler && (!validCampaignTime(startTime) || !validCampaignTime(endTime)) ->
                                "Choose valid 24-hour start and end times."
                            scheduler && start == end && endTime <= startTime ->
                                "End time must be after start time on the same day."
                            p == null || p !in 1..99 -> "Priority must be between 1 and 99."
                            else -> ""
                        }
                        if (error.isBlank()) {
                            saved = true
                            onSave(
                                listOf(
                                    campaignStorageField(name),
                                    start,
                                    end,
                                    priority,
                                    if (enabled) "Enabled" else "Disabled",
                                    campaignStorageField(description),
                                    color,
                                    startTime,
                                    endTime,
                                    campaignStorageField(featuredCategories),
                                ).joinToString("|")
                            )
                        }
                    },
                )
            }
        }
    }

    if (confirmDiscard) {
        AdminDialog(
            title = "Discard campaign edits?",
            message = "Your unsaved campaign changes will be lost.",
            confirmLabel = "Discard",
            isDestructive = true,
            onConfirm = {
                confirmDiscard = false
                onCancel()
            },
            onDismiss = { confirmDiscard = false },
        )
    }
}
