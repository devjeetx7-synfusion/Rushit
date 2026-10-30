package com.rushit.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun defaultOfferDate(offsetDays: Int): String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, offsetDays) }.time)

private fun validOfferDate(value: String): Boolean = runCatching {
    val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply { isLenient = false }
    format.parse(value)?.let { format.format(it) == value } == true
}.getOrDefault(false)

private fun offerStatus(fields: List<String>): String {
    if (fields.lastOrNull() != "Enabled") return "Inactive"
    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
    val start = fields.getOrNull(6).orEmpty(); val end = fields.getOrNull(7).orEmpty()
    return when { today < start -> "Scheduled"; today > end -> "Expired"; else -> "Active" }
}

@Composable
fun OfferManagerScreen(offers: List<String>, onAdd: () -> Unit, onEdit: (Int) -> Unit, onDelete: (Int) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    var deleteTarget by remember { mutableStateOf<Pair<Int, String>?>(null) }
    val visible = offers.withIndex().filter { (_, offer) ->
        offer.contains(query, true) && (filter == "All" || offerStatus(offer.split('|')) == filter)
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentPadding = PaddingValues(bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item { ScreenHeader("Offers & Coupons", "${offers.size} local demo offers") }
        item { DemoNotice() }
        item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Search offer or coupon") }, singleLine = true, shape = AppShapes.input) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("All", "Active", "Scheduled", "Inactive", "Expired").forEach { FilterChip(filter == it, { filter = it }, { Text(it) }) } } }
        if (visible.isEmpty()) item { EmptyState("No matching offers", "Change your search or add a local demo offer.", "Create offer", onAdd) }
        itemsIndexed(visible) { _, indexed ->
            val fields = indexed.value.split('|')
            AdminSurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(fields.getOrNull(0).orEmpty(), fontWeight = FontWeight.Bold)
                        Text(fields.getOrNull(1).orEmpty(), color = AppColors.primary, fontWeight = FontWeight.SemiBold)
                        val kind = fields.getOrNull(2).orEmpty()
                        val discount = fields.getOrNull(3).orEmpty()
                        Text(if (kind == "Percentage") "$discount% off" else "₹$discount off", color = AppColors.text)
                    }
                    OfferState(offerStatus(fields))
                }
                Text("Min. order ₹${fields.getOrNull(4).orEmpty()} · Limit ${fields.getOrNull(5).orEmpty()}", color = AppColors.muted, fontSize = 12.sp)
                Text("${fields.getOrNull(6).orEmpty()} – ${fields.getOrNull(7).orEmpty()}", color = AppColors.muted, fontSize = 12.sp)
                Row {
                    TextButton(onClick = { onEdit(indexed.index) }) { Text("Edit offer") }
                    TextButton(onClick = { deleteTarget = indexed.index to fields.getOrNull(0).orEmpty() }) { Text("Delete", color = AppColors.error) }
                }
            }
        }
        item { Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("＋ Create Offer") } }
        item { Text("Offers are local prototype data and are not redeemable in the customer app.", color = AppColors.muted, fontSize = 12.sp) }
    }
    deleteTarget?.let { (index, name) -> AlertDialog(
        onDismissRequest = { deleteTarget = null },
        title = { Text("Delete local coupon?") },
        text = { Text("Remove $name from this device? It is not applied by checkout or the customer app.") },
        confirmButton = { TextButton(onClick = { onDelete(index); deleteTarget = null }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Keep") } },
    ) }
}

@Composable private fun OfferState(label: String) {
    Text(label, color = when (label) { "Active" -> AppColors.success; "Scheduled" -> AppColors.info; "Expired" -> AppColors.warning; else -> AppColors.muted }, fontSize = 11.sp, fontWeight = FontWeight.Bold)
}

@Composable
fun OfferEditorScreen(offer: String?, existingOffers: List<String>, onCancel: () -> Unit, onSave: (String) -> Unit) {
    val fields = remember(offer) { offer?.split('|').orEmpty() }
    val initialStart = remember(offer) { fields.getOrNull(6) ?: defaultOfferDate(7) }
    val initialEnd = remember(offer) { fields.getOrNull(7) ?: defaultOfferDate(37) }
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
    val initialName = fields.getOrNull(0).orEmpty(); val initialCode = fields.getOrNull(1).orEmpty()
    val initialKind = fields.getOrNull(2) ?: "Percentage"; val initialValue = fields.getOrNull(3).orEmpty()
    val initialMinimum = fields.getOrNull(4).orEmpty(); val initialLimit = fields.getOrNull(5).orEmpty()
    val initialEnabled = fields.getOrNull(8) != "Disabled"
    val hasChanges = name != initialName || code != initialCode || kind != initialKind || value != initialValue || minimum != initialMinimum || limit != initialLimit || start != initialStart || end != initialEnd || enabled != initialEnabled
    val cancel = { if (hasChanges) confirmDiscard = true else onCancel() }
    androidx.activity.compose.BackHandler(enabled = hasChanges) { confirmDiscard = true }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenHeader(if (offer == null) "Create Offer" else "Edit Offer", "Local coupon configuration", backAction = cancel) }
        item { DemoNotice() }
        item { AdminSurfaceCard {
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Offer name") }, singleLine = true)
            OutlinedTextField(code, { code = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(16) }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Coupon code") }, singleLine = true)
        } }
        item { AdminSurfaceCard {
            Text("Discount type", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("Percentage", "Fixed").forEach { FilterChip(kind == it, { kind = it }, { Text(if (it == "Fixed") "Fixed amount" else it) }) } }
            OutlinedTextField(value, { value = it.filter(Char::isDigit).take(5) }, Modifier.fillMaxWidth(), label = { Text(if (kind == "Fixed") "Discount amount (₹)" else "Discount (%)") }, singleLine = true)
            OutlinedTextField(minimum, { minimum = it.filter(Char::isDigit).take(7) }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Minimum order (₹)") }, singleLine = true)
            OutlinedTextField(limit, { limit = it.filter(Char::isDigit).take(7) }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Usage limit") }, singleLine = true)
        } }
        item { AdminSurfaceCard {
            Text("Validity dates · YYYY-MM-DD", fontWeight = FontWeight.Bold)
            OutlinedTextField(start, { start = it.take(10) }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Start date") }, singleLine = true)
            OutlinedTextField(end, { end = it.take(10) }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("End date") }, singleLine = true)
            ToggleRow("Active in local demo", enabled) { enabled = it }
        } }
        item { AdminSurfaceCard {
            Text("Example calculation", fontWeight = FontWeight.Bold)
            val numeric = value.toDoubleOrNull() ?: 0.0
            val example = if (kind == "Percentage") (500 * numeric / 100).toInt() else numeric.toInt()
            Text("On a ₹500 cart, discount would be ₹${example.coerceAtMost(500)} before other rules.", color = AppColors.muted, fontSize = 12.sp)
        } }
        item {
            if (error.isNotBlank()) Text(error, color = AppColors.error, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.OutlinedButton(onClick = cancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(onClick = {
                    val amount = value.toIntOrNull(); val min = minimum.toIntOrNull(); val max = limit.toIntOrNull()
                    error = when {
                        name.isBlank() -> "Offer name is required."
                        code.isBlank() -> "Coupon code is required."
                        existingOffers.any { existing -> existing != offer && existing.split('|').getOrNull(1).equals(code, true) } -> "That coupon code is already used in this local list."
                        amount == null || amount <= 0 || (kind == "Percentage" && amount > 100) -> "Enter a valid discount value."
                        min == null || min < 0 -> "Minimum order must be zero or greater."
                        max == null || max <= 0 -> "Usage limit must be greater than zero."
                        !validOfferDate(start) || !validOfferDate(end) -> "Enter valid YYYY-MM-DD dates."
                        end < start -> "End date must not precede start date."
                        else -> ""
                    }
                    if (error.isBlank()) {
                        saved = true
                        onSave(listOf(name.trim().replace('|', ' ').replace("##", " "), code, kind, amount.toString(), min.toString(), max.toString(), start, end, if (enabled) "Enabled" else "Disabled").joinToString("|"))
                    }
                }, enabled = !saved, modifier = Modifier.weight(1f)) { Text("Save Offer") }
            }
        }
    }
    if (confirmDiscard) AlertDialog(
        onDismissRequest = { confirmDiscard = false },
        title = { Text("Discard coupon edits?") },
        text = { Text("Your unsaved local coupon edits will be lost.") },
        confirmButton = { TextButton(onClick = { confirmDiscard = false; onCancel() }) { Text("Discard") } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } },
    )
}
