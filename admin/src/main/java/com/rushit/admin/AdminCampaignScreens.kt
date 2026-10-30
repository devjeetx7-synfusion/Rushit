package com.rushit.admin

import android.app.DatePickerDialog
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

private fun isoDate(calendar: Calendar): String = String.format(Locale.US, "%04d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
private fun displayCampaignDate(value: String): String = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value)?.let { SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(it) } ?: value }.getOrDefault(value)
private fun validIsoDate(value: String): Boolean = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(value)?.let { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it) == value } == true }.getOrDefault(false)
private fun campaignStatus(start: String, end: String, enabled: Boolean, today: String): String = when {
    !enabled -> "Inactive"
    start > today -> "Scheduled"
    end < today -> "Expired"
    else -> "Active"
}

@Composable
fun CampaignManagerScreen(campaigns: List<String>, scheduler: Boolean = false, onAdd: () -> Unit, onEdit: (Int) -> Unit) {
    var filter by rememberSaveable { mutableStateOf("All") }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val filtered = campaigns.withIndex().filter { (_, record) ->
        val fields = record.split('|')
        val status = campaignStatus(fields.getOrNull(1).orEmpty(), fields.getOrNull(2).orEmpty(), fields.getOrNull(4) != "Disabled", today)
        filter == "All" || status == filter
    }
    var previewRecord by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item { ScreenHeader(if (scheduler) "Campaign Scheduler" else "Festival Manager", "Local demo campaigns · no production publishing") }
        item { DemoNotice() }
        item { Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("All", "Scheduled", "Active", "Inactive", "Expired").forEach { FilterChip(filter == it, { filter = it }, { Text(it, fontSize = 11.sp) }) } } }
        if (filtered.isEmpty()) item { EmptyState("No campaigns in this view", "Change the status filter or add a campaign.", if (scheduler) "Schedule campaign" else "Add festival", onAdd) }
        itemsIndexed(filtered) { _, indexed ->
            val fields = indexed.value.split('|')
            val title = fields.getOrNull(0).orEmpty()
            val start = fields.getOrNull(1).orEmpty()
            val end = fields.getOrNull(2).orEmpty()
            val priority = fields.getOrNull(3) ?: "1"
            val enabled = fields.getOrNull(4) != "Disabled"
            val status = campaignStatus(start, end, enabled, today)
            AdminSurfaceCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(when (title.lowercase()) { "diwali" -> "🪔"; "holi" -> "🎨"; "christmas" -> "🎄"; "eid" -> "🌙"; else -> "✦" }, fontSize = 27.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, fontWeight = FontWeight.Bold)
                        Text("${displayCampaignDate(start)} – ${displayCampaignDate(end)}", color = AppColors.muted, fontSize = 12.sp)
                        Text("Priority $priority", color = AppColors.muted, fontSize = 11.sp)
                    }
                    CampaignStatus(status)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onEdit(indexed.index) }) { Text("Edit") }
                    TextButton(onClick = { previewRecord = indexed.value }) { Text("Preview") }
                    Spacer(Modifier.weight(1f))
                    Text("Priority $priority", color = AppColors.muted, fontSize = 12.sp)
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
}

@Composable
private fun CampaignStatus(status: String) {
    val color = when (status) { "Active" -> AppColors.success; "Scheduled" -> AppColors.info; "Expired" -> AppColors.muted; else -> AppColors.warning }
    Text(status, Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = color, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
}

@Composable
fun CampaignEditorScreen(campaign: String?, onSave: (String) -> Unit) {
    val fields = remember(campaign) { campaign?.split('|').orEmpty() }
    var name by rememberSaveable(campaign) { mutableStateOf(fields.getOrNull(0).orEmpty()) }
    var start by rememberSaveable(campaign) { mutableStateOf(fields.getOrNull(1) ?: run { Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }.let(::isoDate) }) }
    var end by rememberSaveable(campaign) { mutableStateOf(fields.getOrNull(2) ?: run { Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 14) }.let(::isoDate) }) }
    var priority by rememberSaveable(campaign) { mutableStateOf(fields.getOrNull(3) ?: "1") }
    var enabled by rememberSaveable(campaign) { mutableStateOf(fields.getOrNull(4) != "Disabled") }
    var description by rememberSaveable(campaign) { mutableStateOf(fields.getOrNull(5).orEmpty()) }
    var color by rememberSaveable(campaign) { mutableStateOf(fields.getOrNull(6) ?: "Coral") }
    var error by remember { mutableStateOf("") }
    var saved by rememberSaveable(campaign) { mutableStateOf(false) }
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenHeader(if (campaign == null) "Festival Editor" else "Edit Campaign", "Draft stays in this Admin prototype") }
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
            Button(onClick = {
                val p = priority.toIntOrNull()
                error = when { name.isBlank() -> "Campaign name is required."; !validIsoDate(start) || !validIsoDate(end) -> "Select valid start and end dates."; end < start -> "End date must not be before the start date."; p == null || p !in 1..99 -> "Priority must be between 1 and 99."; else -> "" }
                if (error.isBlank()) { saved = true; onSave(listOf(name.trim(), start, end, priority, if (enabled) "Enabled" else "Disabled", description.trim(), color).joinToString("|")) }
            }, enabled = !saved, modifier = Modifier.fillMaxWidth()) { Text("Save Draft") }
        }
    }
}
