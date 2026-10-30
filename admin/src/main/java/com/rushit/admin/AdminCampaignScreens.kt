package com.rushit.admin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun campaignStorageField(value: String): String = value.replace('|', ' ').replace("##", " ").trim()
private fun isoDate(calendar: Calendar): String = String.format(Locale.US, "%04d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
private fun displayCampaignDate(value: String): String = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value)?.let { SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(it) } ?: value }.getOrDefault(value)
private fun validIsoDate(value: String): Boolean = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(value)?.let { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it) == value } == true }.getOrDefault(false)
private fun validCampaignTime(value: String): Boolean = Regex("(?:[01]\\d|2[0-3]):[0-5]\\d").matches(value)
private fun campaignStatus(start: String, end: String, enabled: Boolean, today: String): String = when {
    !enabled -> "Inactive"
    start > today -> "Scheduled"
    end < today -> "Expired"
    else -> "Active"
}

@Composable
fun CampaignManagerScreen(campaigns: List<String>, scheduler: Boolean = false, onAdd: () -> Unit, onEdit: (Int) -> Unit, onDelete: (Int) -> Unit) {
    var filter by rememberSaveable { mutableStateOf("All") }
    var query by rememberSaveable { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<Pair<Int, String>?>(null) }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val filtered = campaigns.withIndex().filter { (_, record) ->
        val fields = record.split('|')
        val status = campaignStatus(fields.getOrNull(1).orEmpty(), fields.getOrNull(2).orEmpty(), fields.getOrNull(4) != "Disabled", today)
        record.contains(query, ignoreCase = true) && (filter == "All" || status == filter)
    }
    var previewRecord by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item { ScreenHeader(if (scheduler) "Campaign Scheduler" else "Festival Manager", "Local demo campaigns · no production publishing") }
        item { DemoNotice() }
        item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Search campaigns") }, singleLine = true, shape = AppShapes.input) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("All", "Scheduled", "Active", "Inactive", "Expired").forEach { FilterChip(filter == it, { filter = it }, { Text(it, fontSize = 11.sp) }) } } }
        if (filtered.isEmpty()) item { EmptyState("No campaigns in this view", "Change the status filter or add a campaign.", if (scheduler) "Schedule campaign" else "Add festival", onAdd) }
        itemsIndexed(filtered) { _, indexed ->
            val fields = indexed.value.split('|')
            val title = fields.getOrNull(0).orEmpty()
            val start = fields.getOrNull(1).orEmpty()
            val end = fields.getOrNull(2).orEmpty()
            val priority = fields.getOrNull(3) ?: "1"
            val enabled = fields.getOrNull(4) != "Disabled"
            val startTime = fields.getOrNull(7).orEmpty()
            val endTime = fields.getOrNull(8).orEmpty()
            val status = campaignStatus(start, end, enabled, today)
            AdminSurfaceCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(when (title.lowercase()) { "diwali" -> "🪔"; "holi" -> "🎨"; "christmas" -> "🎄"; "eid" -> "🌙"; else -> "✦" }, fontSize = 27.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, fontWeight = FontWeight.Bold)
                        Text("${displayCampaignDate(start)} – ${displayCampaignDate(end)}", color = AppColors.muted, fontSize = 12.sp)
                        if (scheduler && startTime.isNotBlank() && endTime.isNotBlank()) Text("$startTime to $endTime · local time", color = AppColors.muted, fontSize = 11.sp)
                        Text("Priority $priority", color = AppColors.muted, fontSize = 11.sp)
                    }
                    CampaignStatus(status)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onEdit(indexed.index) }) { Text("Edit") }
                    TextButton(onClick = { previewRecord = indexed.value }) { Text("Preview") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { deleteTarget = indexed.index to title }) { Text("Delete", color = AppColors.error) }
                }
            }
        }
        item { Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text(if (scheduler) "＋ Schedule Campaign" else "＋ Add Festival") } }
        item { Text("Scheduling is evaluated while this screen is open. No background activation is provided.", color = AppColors.muted, fontSize = 12.sp) }
    }
    previewRecord?.let { record ->
        val fields = record.split('|')
        AlertDialog(
            onDismissRequest = { previewRecord = null },
            title = { Text(fields.getOrNull(0).orEmpty()) },
            text = { Column { Text(fields.getOrNull(5).orEmpty().ifBlank { "Seasonal RushIt demo campaign" }); Text("${displayCampaignDate(fields.getOrNull(1).orEmpty())} – ${displayCampaignDate(fields.getOrNull(2).orEmpty())}", color = AppColors.muted); Text("Priority ${fields.getOrNull(3).orEmpty()}", color = AppColors.muted) } },
            confirmButton = { TextButton(onClick = { previewRecord = null }) { Text("Close") } },
        )
    }
    deleteTarget?.let { (index, title) -> AlertDialog(
        onDismissRequest = { deleteTarget = null },
        title = { Text(if (scheduler) "Delete local campaign?" else "Delete festival?") },
        text = { Text("Remove $title from this device's demo list? This will not change the customer app or activate/deactivate a live campaign.") },
        confirmButton = { TextButton(onClick = { onDelete(index); deleteTarget = null }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Keep") } },
    ) }
}

@Composable
private fun CampaignStatus(status: String) {
    val color = when (status) { "Active" -> AppColors.success; "Scheduled" -> AppColors.info; "Expired" -> AppColors.muted; else -> AppColors.warning }
    Text(status, Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = color, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
}

@Composable
fun CampaignEditorScreen(campaign: String?, scheduler: Boolean = false, onCancel: () -> Unit, onSave: (String) -> Unit) {
    val fields = remember(campaign) { campaign?.split('|').orEmpty() }
    val initialStart = remember(campaign) { fields.getOrNull(1) ?: Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }.let(::isoDate) }
    val initialEnd = remember(campaign) { fields.getOrNull(2) ?: Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 14) }.let(::isoDate) }
    val initialStartTime = fields.getOrNull(7) ?: "09:00"
    val initialEndTime = fields.getOrNull(8) ?: "23:59"
    var name by rememberSaveable(campaign) { mutableStateOf(fields.getOrNull(0).orEmpty()) }
    var start by rememberSaveable(campaign) { mutableStateOf(initialStart) }
    var end by rememberSaveable(campaign) { mutableStateOf(initialEnd) }
    var startTime by rememberSaveable(campaign) { mutableStateOf(initialStartTime) }
    var endTime by rememberSaveable(campaign) { mutableStateOf(initialEndTime) }
    var priority by rememberSaveable(campaign) { mutableStateOf(fields.getOrNull(3) ?: "1") }
    var enabled by rememberSaveable(campaign) { mutableStateOf(fields.getOrNull(4) != "Disabled") }
    var description by rememberSaveable(campaign) { mutableStateOf(fields.getOrNull(5).orEmpty()) }
    var color by rememberSaveable(campaign) { mutableStateOf(fields.getOrNull(6) ?: "Coral") }
    var error by rememberSaveable(campaign) { mutableStateOf("") }
    var confirmDiscard by remember { mutableStateOf(false) }
    var saved by rememberSaveable(campaign) { mutableStateOf(false) }
    val initialName = fields.getOrNull(0).orEmpty()
    val initialPriority = fields.getOrNull(3) ?: "1"
    val initialEnabled = fields.getOrNull(4) != "Disabled"
    val initialDescription = fields.getOrNull(5).orEmpty()
    val initialColor = fields.getOrNull(6) ?: "Coral"
    val hasChanges = name != initialName || start != initialStart || end != initialEnd || startTime != initialStartTime || endTime != initialEndTime || priority != initialPriority || enabled != initialEnabled || description != initialDescription || color != initialColor
    val cancel = { if (hasChanges) confirmDiscard = true else onCancel() }
    androidx.activity.compose.BackHandler(enabled = hasChanges) { confirmDiscard = true }
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenHeader(if (campaign == null) "Festival Editor" else "Edit Campaign", "Saved locally · no automatic publishing", backAction = cancel) }
        item { DemoNotice() }
        item { AdminSurfaceCard {
            Text("Campaign details", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Campaign name") }, singleLine = true, shape = AppShapes.input)
            OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Description") }, shape = AppShapes.input)
        } }
        item { AdminSurfaceCard {
            Text("Schedule", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            OutlinedButton(onClick = {
                val parts = start.split('-'); val y = parts.getOrNull(0)?.toIntOrNull() ?: calendar.get(Calendar.YEAR); val m = (parts.getOrNull(1)?.toIntOrNull() ?: 1) - 1; val d = parts.getOrNull(2)?.toIntOrNull() ?: 1
                DatePickerDialog(context, { _, year, month, day -> start = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day) }, y, m, d).show()
            }, modifier = Modifier.fillMaxWidth()) { Text("Start date · ${displayCampaignDate(start)}") }
            OutlinedButton(onClick = {
                val parts = end.split('-'); val y = parts.getOrNull(0)?.toIntOrNull() ?: calendar.get(Calendar.YEAR); val m = (parts.getOrNull(1)?.toIntOrNull() ?: 1) - 1; val d = parts.getOrNull(2)?.toIntOrNull() ?: 1
                DatePickerDialog(context, { _, year, month, day -> end = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day) }, y, m, d).show()
            }, modifier = Modifier.fillMaxWidth()) { Text("End date · ${displayCampaignDate(end)}") }
            if (scheduler) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val parts = startTime.split(':'); val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9; val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        TimePickerDialog(context, { _, h, m -> startTime = String.format(Locale.US, "%02d:%02d", h, m) }, hour, minute, true).show()
                    }, modifier = Modifier.weight(1f)) { Text("Starts · $startTime") }
                    OutlinedButton(onClick = {
                        val parts = endTime.split(':'); val hour = parts.getOrNull(0)?.toIntOrNull() ?: 23; val minute = parts.getOrNull(1)?.toIntOrNull() ?: 59
                        TimePickerDialog(context, { _, h, m -> endTime = String.format(Locale.US, "%02d:%02d", h, m) }, hour, minute, true).show()
                    }, modifier = Modifier.weight(1f)) { Text("Ends · $endTime") }
                }
                Text("Local time only · this Admin app does not run a background scheduler.", color = AppColors.muted, fontSize = 11.sp)
            }
            OutlinedTextField(priority, { priority = it.filter(Char::isDigit).take(2) }, Modifier.fillMaxWidth(), label = { Text("Priority (1 is highest)") }, singleLine = true, shape = AppShapes.input)
            ToggleRow("Enable in local schedule", enabled) { enabled = it }
        } }
        item { AdminSurfaceCard {
            Text("Campaign color", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("Coral", "Amber", "Green", "Indigo").forEach { FilterChip(color == it, { color = it }, { Text(it) }) } }
            Text("Preview · $name", color = when (color) { "Amber" -> AppColors.warning; "Green" -> AppColors.success; "Indigo" -> AppColors.info; else -> AppColors.primary }, fontWeight = FontWeight.Bold)
        } }
        item {
            if (error.isNotBlank()) Text(error, color = AppColors.error, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = cancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(onClick = {
                    val p = priority.toIntOrNull()
                    error = when {
                        name.isBlank() -> "Campaign name is required."
                        !validIsoDate(start) || !validIsoDate(end) -> "Select valid start and end dates."
                        end < start -> "End date must not be before the start date."
                        scheduler && (!validCampaignTime(startTime) || !validCampaignTime(endTime)) -> "Choose valid 24-hour start and end times."
                        scheduler && start == end && endTime <= startTime -> "End time must be after start time on the same day."
                        p == null || p !in 1..99 -> "Priority must be between 1 and 99."
                        else -> ""
                    }
                    if (error.isBlank()) {
                        saved = true
                        onSave(listOf(campaignStorageField(name), start, end, priority, if (enabled) "Enabled" else "Disabled", campaignStorageField(description), color, startTime, endTime).joinToString("|"))
                    }
                }, enabled = !saved, modifier = Modifier.weight(1f)) { Text("Save Draft") }
            }
        }
    }
    if (confirmDiscard) AlertDialog(
        onDismissRequest = { confirmDiscard = false },
        title = { Text("Discard campaign edits?") },
        text = { Text("Your unsaved campaign changes will be lost.") },
        confirmButton = { TextButton(onClick = { confirmDiscard = false; onCancel() }) { Text("Discard") } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } },
    )
}
