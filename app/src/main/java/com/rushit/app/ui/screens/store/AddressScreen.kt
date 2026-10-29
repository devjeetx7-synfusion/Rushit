package com.rushit.app.ui.screens.store

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.rushit.app.R
import com.rushit.app.data.model.SavedAddress
import com.rushit.app.domain.isValidDeliveryAddress
import com.rushit.app.ui.components.MessageCard
import com.rushit.app.ui.components.RushButton
import com.rushit.app.ui.components.RushIcon
import com.rushit.app.ui.components.ScreenHeader
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.Cream
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.Muted
import com.rushit.app.ui.theme.Peach
import com.rushit.app.ui.viewmodel.ShopUiState
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun AddressScreen(
    state: ShopUiState,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onSave: (String?, String, String, String, String, Double?, Double?) -> Unit,
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = editing) { editing = false }
    val current = state.addresses.firstOrNull { it.id == editId }
    Column(Modifier.fillMaxSize().background(Cream)) {
        ScreenHeader(if (editing) if (editId == null) "Add address" else "Edit address" else "Delivery addresses",
            onBack = { if (editing) editing = false else onBack() })
        if (editing) {
            AddressForm(current, onCancel = { editing = false }, onSave = { type, line, city, pin, lat, lng ->
                onSave(editId, type, line, city, pin, lat, lng)
                editing = false
            })
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item(key = "note") {
                    Text("Saved only on this device. Delivery serviceability is not connected yet.", style = MaterialTheme.typography.bodyMedium, color = Muted,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
                }
                if (state.addresses.isEmpty()) {
                    item(key = "empty") { MessageCard("No saved addresses", "Add an address when you're ready. You can enter it manually.") }
                }
                items(state.addresses, key = { it.id }) { address ->
                    AddressCard(address, selected = address.id == state.selectedAddressId,
                        onSelect = { onSelect(address.id) }, onEdit = { editId = address.id; editing = true }, onDelete = { deleteId = address.id })
                }
                item(key = "add") {
                    RushButton("Add address", { editId = null; editing = true }, modifier = Modifier.padding(top = 10.dp), arrow = false)
                }
            }
        }
    }
    if (deleteId != null) AlertDialog(onDismissRequest = { deleteId = null }, title = { Text("Remove address?") },
        text = { Text("This removes it from this device.") },
        confirmButton = { TextButton(onClick = { deleteId?.let(onDelete); deleteId = null }) { Text("Remove") } },
        dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Cancel") } }, containerColor = CardWhite)
}

@Composable
private fun AddressCard(address: SavedAddress, selected: Boolean, onSelect: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = CardWhite, border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) Coral else Border)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).selectable(selected = selected, role = Role.RadioButton, onClick = onSelect).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                RushIcon(R.drawable.ic_location_01, null, Modifier.size(22.dp), Coral)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(address.type, style = MaterialTheme.typography.titleMedium, color = Ink)
                    Text(address.summary, style = MaterialTheme.typography.bodyMedium, color = Muted)
                }
                Text(if (selected) "Selected" else "Select", color = Coral, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onEdit) { Text("Edit", color = Coral) }
                TextButton(onClick = onDelete) { Text("Remove", color = Muted) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddressForm(
    existing: SavedAddress?,
    onCancel: () -> Unit,
    onSave: (String, String, String, String, Double?, Double?) -> Unit,
) {
    val context = LocalContext.current
    var type by rememberSaveable(existing?.id) { mutableStateOf(existing?.type?.takeIf { it in listOf("Home", "Work", "Other") } ?: "Home") }
    var line by rememberSaveable(existing?.id) { mutableStateOf(existing?.line.orEmpty()) }
    var city by rememberSaveable(existing?.id) { mutableStateOf(existing?.city.orEmpty()) }
    var pin by rememberSaveable(existing?.id) { mutableStateOf(existing?.postalCode.orEmpty()) }
    var latitude by rememberSaveable(existing?.id) { mutableStateOf(existing?.latitude) }
    var longitude by rememberSaveable(existing?.id) { mutableStateOf(existing?.longitude) }
    var locationMessage by remember { mutableStateOf<String?>(null) }
    var locating by remember { mutableStateOf(false) }
    var cancellation by remember { mutableStateOf<CancellationSignal?>(null) }
    var active by remember { mutableStateOf(true) }
    var attemptedSave by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val valid = isValidDeliveryAddress(line, city, pin)

    DisposableEffect(Unit) { onDispose { active = false; cancellation?.cancel() } }
    LaunchedEffect(locating) {
        if (locating) {
            delay(15_000)
            cancellation?.cancel()
            locating = false
            locationMessage = "Location timed out. You can enter the address manually."
        }
    }
    fun requestLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (manager == null || !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationMessage = "Location is unavailable. Turn on location or enter the address manually."
            return
        }
        val signal = CancellationSignal()
        cancellation = signal
        latitude = null
        longitude = null
        locating = true
        locationMessage = "Finding an approximate location…"
        try {
            LocationManagerCompat.getCurrentLocation(manager, LocationManager.NETWORK_PROVIDER, signal, ContextCompat.getMainExecutor(context)) { location ->
                if (active && !signal.isCanceled) {
                    locating = false
                    if (location == null) {
                        locationMessage = "Couldn't get a location. Please enter the address manually."
                    } else {
                        latitude = location.latitude
                        longitude = location.longitude
                        locationMessage = "Approximate location attached. Please enter the full address below."
                    }
                }
            }
        } catch (_: Exception) {
            locating = false
            locationMessage = "Couldn't get a location. Please enter the address manually."
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) requestLocation() else locationMessage = "Location permission wasn't granted. You can still enter the address manually."
    }

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Text("Address type", style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.padding(top = 10.dp, bottom = 8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Home", "Work", "Other").forEach { option ->
                Text(option, color = if (type == option) Coral else Ink, style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.clip(CircleShape).background(if (type == option) Peach else CardWhite)
                        .selectable(selected = type == option, role = Role.RadioButton, onClick = { type = option }).padding(horizontal = 16.dp, vertical = 12.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(line, { line = it.take(160) }, label = { Text("Street, house or building") }, modifier = Modifier.fillMaxWidth(),
            singleLine = true, isError = attemptedSave && line.trim().length < 3, shape = RoundedCornerShape(16.dp))
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(city, { city = it.take(80) }, label = { Text("City or town") }, modifier = Modifier.fillMaxWidth(),
            singleLine = true, isError = attemptedSave && city.trim().length < 2, shape = RoundedCornerShape(16.dp))
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(pin, { pin = it.filter { c -> c in '0'..'9' }.take(6) }, label = { Text("6-digit PIN code") }, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
            isError = attemptedSave && (pin.length != 6 || pin.startsWith('0')), shape = RoundedCornerShape(16.dp))
        if (attemptedSave && !valid) Text("Enter a street, city and valid 6-digit Indian PIN code.", style = MaterialTheme.typography.bodySmall, color = Coral, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(16.dp))
        Text("Optional location", style = MaterialTheme.typography.titleMedium, color = Ink)
        Text("Choose to attach an approximate location. Permission is requested only when you tap below; no address is filled in automatically.",
            style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
        TextButton(onClick = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) requestLocation()
            else permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }, enabled = !locating) {
            RushIcon(R.drawable.ic_location_01, null, Modifier.size(20.dp), Coral)
            Spacer(Modifier.width(6.dp))
            Text(if (locating) "Finding location…" else "Use current location", color = Coral)
            if (locating) { Spacer(Modifier.width(8.dp)); CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Coral) }
        }
        locationMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Muted) }
        if (latitude != null && longitude != null) {
            Text(String.format(Locale.ROOT, "Approximate coordinates: %.4f, %.4f", latitude, longitude), style = MaterialTheme.typography.bodySmall, color = Muted)
            TextButton(onClick = { cancellation?.cancel(); locating = false; latitude = null; longitude = null; locationMessage = "Location removed." }) { Text("Remove location", color = Coral) }
        }
        Spacer(Modifier.height(14.dp))
        RushButton("Save address", { attemptedSave = true; if (valid && !saving) { saving = true; onSave(type, line, city, pin, latitude, longitude) } }, enabled = !saving, arrow = false)
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel", color = Muted) }
        Spacer(Modifier.height(20.dp))
    }
}
