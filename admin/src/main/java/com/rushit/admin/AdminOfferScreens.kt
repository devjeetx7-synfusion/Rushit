package com.rushit.admin

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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

private fun defaultOfferDate(offsetDays: Int): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offsetDays) }.time
    )

private fun validOfferDate(value: String): Boolean = runCatching {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    format.parse(value)?.let { format.format(it) == value } == true
}.getOrDefault(false)

private fun offerStatus(fields: List<String>, today: String): String {
    if (fields.getOrNull(8) == "Disabled") return "Inactive"
    val start = fields.getOrNull(6).orEmpty()
    val end = fields.getOrNull(7).orEmpty()
    return when {
        start.isNotBlank() && today < start -> "Scheduled"
        end.isNotBlank() && today > end -> "Expired"
        else -> "Active"
    }
}

@Composable
fun OfferManagerScreen(
    offers: List<String>,
    onAdd: () -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onToggleOffer: ((Int, String) -> Unit)? = null,
    onOpenRoles: () -> Unit = {},
) {
    val permissions = LocalAdminPermissions.current
    val canManage = permissions.canManageOffers
    val brand = LocalAdminThemeConfig.current.activePrimary

    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    var deleteTarget by remember { mutableStateOf<Pair<Int, String>?>(null) }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    val visible by remember(offers, query, filter, today) {
        derivedStateOf {
            offers.withIndex().filter { (_, offer) ->
                val fields = offer.split('|')
                val status = offerStatus(fields, today)
                offer.contains(query, ignoreCase = true) && (filter == "All" || status == filter)
            }
        }
    }

    val statusFilters = remember { listOf("All", "Active", "Scheduled", "Inactive", "Expired") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .adminPageInsets(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenHeader(
                title = "Offers & Coupons",
                subtitle = "${offers.size} local discount codes configured",
                onAction = if (canManage) onAdd else null,
                action = "+ Create",
            )
        }
        if (!canManage) {
            item {
                RoleAccessBanner(
                    roleName = permissions.roleName,
                    requiredCapability = "Manage Offers & Coupons",
                    onSwitchRole = onOpenRoles,
                )
            }
        }
        item { DemoNotice() }
        item {
            AdminSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search coupon code or offer title…",
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(statusFilters, key = { it }) { itemFilter ->
                    val count = if (itemFilter == "All") {
                        offers.size
                    } else {
                        offers.count { offerStatus(it.split('|'), today) == itemFilter }
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

        if (visible.isEmpty()) {
            item {
                EmptyState(
                    title = "No matching offers",
                    message = "Change your search or create a new local discount coupon.",
                    actionLabel = if (canManage) "Create Offer" else null,
                    onAction = onAdd,
                )
            }
        }

        itemsIndexed(
            items = visible,
            key = { _, indexed -> "${indexed.index}:${indexed.value}" },
        ) { _, indexed ->
            val fields = indexed.value.split('|')
            val offerTitle = fields.getOrNull(0).orEmpty()
            val couponCode = fields.getOrNull(1).orEmpty()
            val kind = fields.getOrNull(2).orEmpty()
            val discount = fields.getOrNull(3).orEmpty()
            val minOrder = fields.getOrNull(4).orEmpty()
            val usageLimit = fields.getOrNull(5).orEmpty()
            val startDate = fields.getOrNull(6).orEmpty()
            val endDate = fields.getOrNull(7).orEmpty()
            val isEnabled = fields.getOrNull(8) != "Disabled"
            val status = offerStatus(fields, today)

            val badgeBg = when (status) {
                "Active" -> AdminColorScheme.primarySoft
                "Scheduled" -> AdminColorScheme.infoSoft
                "Expired" -> AdminColorScheme.warningSoft
                else -> Color(0xFFF3EFEA)
            }
            val badgeFg = when (status) {
                "Active" -> brand
                "Scheduled" -> AdminColorScheme.info
                "Expired" -> AdminColorScheme.warning
                else -> AdminColorScheme.secondaryText
            }

            AdminSurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(badgeBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = AdminIcons.Tag,
                            contentDescription = null,
                            tint = badgeFg,
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
                                text = couponCode,
                                fontWeight = FontWeight.ExtraBold,
                                color = AdminColorScheme.primaryText,
                                fontSize = 16.sp,
                            )
                            Surface(
                                shape = CircleShape,
                                color = brand.copy(alpha = 0.12f),
                            ) {
                                Text(
                                    text = if (kind == "Percentage") "$discount% OFF" else "Flat ₹$discount OFF",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    color = brand,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Text(
                            text = offerTitle,
                            color = AdminColorScheme.primaryText,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                        )
                        Text(
                            text = "Min. order ₹$minOrder · Limit $usageLimit uses",
                            color = AdminColorScheme.secondaryText,
                            fontSize = 11.5.sp,
                        )
                        Text(
                            text = "Valid: $startDate to $endDate",
                            color = AdminColorScheme.secondaryText,
                            fontSize = 11.sp,
                        )
                    }
                    StatusChip(status)
                }

                if (canManage) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { onEdit(indexed.index) }) {
                            Icon(AdminIcons.Edit, contentDescription = null, tint = brand, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Edit Offer", color = brand, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                        if (onToggleOffer != null) {
                            TextButton(
                                onClick = {
                                    val updatedFields = fields.toMutableList()
                                    while (updatedFields.size < 9) updatedFields.add("")
                                    updatedFields[8] = if (isEnabled) "Disabled" else "Enabled"
                                    onToggleOffer(indexed.index, updatedFields.joinToString("|"))
                                }
                            ) {
                                Text(
                                    text = if (isEnabled) "Deactivate" else "Activate",
                                    color = AdminColorScheme.secondaryText,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                )
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { deleteTarget = indexed.index to couponCode }) {
                            Text("Delete", color = AdminColorScheme.error, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                        }
                    }
                }
            }
        }

        if (canManage) {
            item {
                PrimaryButton(
                    label = "+ Create Offer",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onAdd,
                )
            }
        }
        item {
            Text(
                text = "Coupons are stored locally on this device for Admin prototyping and are not redeemable in the customer checkout.",
                color = AdminColorScheme.secondaryText,
                fontSize = 11.5.sp,
            )
        }
    }

    deleteTarget?.let { (index, code) ->
        AdminDialog(
            title = "Delete local coupon?",
            message = "Remove '$code' from this device's offer list? This does not affect customer checkout.",
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
fun OfferEditorScreen(
    offer: String?,
    existingOffers: List<String>,
    onCancel: () -> Unit,
    onSave: (String) -> Unit,
) {
    val fields = remember(offer) { offer?.split('|').orEmpty() }
    val initialStart = remember(offer) { fields.getOrNull(6) ?: defaultOfferDate(0) }
    val initialEnd = remember(offer) { fields.getOrNull(7) ?: defaultOfferDate(30) }
    var name by rememberSaveable(offer) { mutableStateOf(fields.getOrNull(0).orEmpty()) }
    var code by rememberSaveable(offer) { mutableStateOf(fields.getOrNull(1).orEmpty()) }
    var kind by rememberSaveable(offer) { mutableStateOf(fields.getOrNull(2) ?: "Percentage") }
    var value by rememberSaveable(offer) { mutableStateOf(fields.getOrNull(3).orEmpty()) }
    var minimum by rememberSaveable(offer) { mutableStateOf(fields.getOrNull(4).orEmpty()) }
    var limit by rememberSaveable(offer) { mutableStateOf(fields.getOrNull(5).orEmpty()) }
    var start by rememberSaveable(offer) { mutableStateOf(initialStart) }
    var end by rememberSaveable(offer) { mutableStateOf(initialEnd) }
    var enabled by rememberSaveable(offer) { mutableStateOf(fields.getOrNull(8) != "Disabled") }
    var error by rememberSaveable(offer) { mutableStateOf("") }
    var saved by rememberSaveable(offer) { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    val initialName = fields.getOrNull(0).orEmpty()
    val initialCode = fields.getOrNull(1).orEmpty()
    val initialKind = fields.getOrNull(2) ?: "Percentage"
    val initialValue = fields.getOrNull(3).orEmpty()
    val initialMinimum = fields.getOrNull(4).orEmpty()
    val initialLimit = fields.getOrNull(5).orEmpty()
    val initialEnabled = fields.getOrNull(8) != "Disabled"
    val hasChanges = name != initialName ||
        code != initialCode ||
        kind != initialKind ||
        value != initialValue ||
        minimum != initialMinimum ||
        limit != initialLimit ||
        start != initialStart ||
        end != initialEnd ||
        enabled != initialEnabled

    val cancel = { if (hasChanges) confirmDiscard = true else onCancel() }
    androidx.activity.compose.BackHandler(enabled = hasChanges) { confirmDiscard = true }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val brand = LocalAdminThemeConfig.current.activePrimary

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .adminPageInsets(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = if (offer == null) "Create Offer" else "Edit Offer",
                subtitle = "Local discount rule configuration",
                backAction = cancel,
            )
        }
        item { DemoNotice() }

        // Live Coupon Preview Ticket Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = AdminColorScheme.primarySoft,
                border = BorderStroke(1.dp, brand.copy(alpha = 0.25f)),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(brand),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = AdminIcons.Tag,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = code.ifBlank { "COUPONCODE" },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = brand,
                        )
                        Text(
                            text = name.ifBlank { "Offer title preview" },
                            fontWeight = FontWeight.Bold,
                            color = AdminColorScheme.primaryText,
                            fontSize = 13.5.sp,
                        )
                        val discDisplay = value.ifBlank { "0" }
                        Text(
                            text = if (kind == "Percentage") "$discDisplay% off above ₹${minimum.ifBlank { "0" }}" else "Flat ₹$discDisplay off above ₹${minimum.ifBlank { "0" }}",
                            color = AdminColorScheme.secondaryText,
                            fontSize = 12.sp,
                        )
                    }
                    StatusChip(if (enabled) "Active" else "Inactive")
                }
            }
        }

        item {
            AdminSurfaceCard {
                Text(
                    text = "Coupon Identity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                AdaptiveFormRow(
                    first = {
                        AdminField(
                            label = "Offer name (e.g. Welcome Offer)",
                            value = name,
                            onValue = { name = it.take(50) },
                        )
                    },
                    second = {
                        AdminField(
                            label = "Coupon code (A–Z, 0–9)",
                            value = code,
                            onValue = { code = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(16) },
                        )
                    },
                )
            }
        }

        item {
            AdminSurfaceCard {
                Text(
                    text = "Discount & Rules",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Percentage", "Fixed").forEach { option ->
                        AdminFilterChip(
                            selected = kind == option,
                            onClick = { kind = option },
                            label = if (option == "Fixed") "Fixed Amount (₹)" else "Percentage (%)",
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminField(
                        label = if (kind == "Fixed") "Discount (₹)" else "Discount (%)",
                        value = value,
                        onValue = { value = it.filter(Char::isDigit).take(5) },
                        keyboard = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                    AdminField(
                        label = "Min. Order (₹)",
                        value = minimum,
                        onValue = { minimum = it.filter(Char::isDigit).take(7) },
                        keyboard = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                }
                AdminField(
                    label = "Total usage limit",
                    value = limit,
                    onValue = { limit = it.filter(Char::isDigit).take(7) },
                    keyboard = KeyboardType.Number,
                )
            }
        }

        item {
            AdminSurfaceCard {
                Text(
                    text = "Validity Window · YYYY-MM-DD",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminSecondaryButton(
                        label = "Start: $start",
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
                        label = "End: $end",
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
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminField(
                        label = "Start date",
                        value = start,
                        onValue = { start = it.take(10) },
                        modifier = Modifier.weight(1f),
                    )
                    AdminField(
                        label = "End date",
                        value = end,
                        onValue = { end = it.take(10) },
                        modifier = Modifier.weight(1f),
                    )
                }
                ToggleRow(
                    label = "Enable coupon in local demo",
                    subtitle = "Inactive coupons remain saved as drafts",
                    value = enabled,
                    onChange = { enabled = it },
                )
            }
        }

        item {
            AdminSurfaceCard {
                Text(
                    text = "Cart Discount Simulator",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                val numeric = value.toDoubleOrNull() ?: 0.0
                val minCart = (minimum.toIntOrNull() ?: 500).coerceAtLeast(500)
                val example = if (kind == "Percentage") (minCart * numeric / 100.0).toInt() else numeric.toInt()
                val finalPayable = (minCart - example.coerceAtMost(minCart)).coerceAtLeast(0)
                Text(
                    text = "On a ₹$minCart qualifying cart: saves ₹${example.coerceAtMost(minCart)} → customer pays ₹$finalPayable.",
                    color = AdminColorScheme.secondaryText,
                    fontSize = 12.sp,
                )
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
                    label = "Save Offer",
                    enabled = !saved,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val amount = value.toIntOrNull()
                        val min = minimum.toIntOrNull()
                        val max = limit.toIntOrNull()
                        error = when {
                            name.isBlank() -> "Offer name is required."
                            code.isBlank() -> "Coupon code is required."
                            existingOffers.any { existing ->
                                existing != offer && existing.split('|').getOrNull(1).equals(code, true)
                            } -> "That coupon code is already used in this local list."
                            amount == null || amount <= 0 || (kind == "Percentage" && amount > 100) ->
                                "Enter a valid discount value (1–100% or positive ₹)."
                            min == null || min < 0 -> "Minimum order must be zero or greater."
                            kind == "Fixed" && min > 0 && amount > min ->
                                "Fixed discount (₹$amount) should not exceed minimum order (₹$min)."
                            max == null || max <= 0 -> "Usage limit must be greater than zero."
                            !validOfferDate(start) || !validOfferDate(end) -> "Enter valid YYYY-MM-DD dates."
                            end < start -> "End date must not precede start date."
                            else -> ""
                        }
                        if (error.isBlank()) {
                            saved = true
                            onSave(
                                listOf(
                                    name.trim().replace('|', ' ').replace("##", " "),
                                    code,
                                    kind,
                                    amount.toString(),
                                    min.toString(),
                                    max.toString(),
                                    start,
                                    end,
                                    if (enabled) "Enabled" else "Disabled",
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
            title = "Discard coupon edits?",
            message = "Your unsaved local coupon edits will be lost.",
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
