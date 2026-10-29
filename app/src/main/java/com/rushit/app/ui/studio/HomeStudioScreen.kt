package com.rushit.app.ui.studio

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rushit.app.data.repository.SampleCatalogRepository
import com.rushit.app.domain.updateModeCart
import com.rushit.app.ui.components.CartSummary
import com.rushit.app.ui.components.RushBottomBar
import com.rushit.app.ui.screens.home.HomeScreen
import com.rushit.app.ui.theme.LocalReducedMotion
import com.rushit.app.ui.viewmodel.ShopUiState

/** Debug-only tooling. Preview has its own sample catalog and in-memory cart, never ShopViewModel. */
@Composable
fun HomeStudioScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { HomeStudioStore(context) }
    var draft by remember { mutableStateOf(store.load()) }
    var preview by remember { mutableStateOf<Boolean?>(null) } // false = original, true = draft
    var confirmReset by remember { mutableStateOf(false) }
    fun update(next: HomeStudioConfig) { draft = next.validated(); store.save(draft) }
    BackHandler(enabled = preview != null) { preview = null }

    if (preview != null) {
        StudioPreview(
            config = if (preview == true) draft else null,
            onBack = { preview = null },
            onMode = { preview = it },
            onNextFestival = {
                val presets = Festival.entries
                val next = presets[(draft.top.festival.ordinal + 1) % presets.size]
                update(draft.copy(top = draft.top.copy(festival = next, heading = "", subtitle = "")))
            },
        )
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = onBack) { Text("‹  Settings") }
        Text("Home UI Studio", style = MaterialTheme.typography.headlineMedium)
        Text("Debug only · Drafts stay on this device. Production Home is never changed.")
        HorizontalDivider()
        Text("Festival Theme Manager", style = MaterialTheme.typography.titleLarge)
        Text("Choose a preset. Bottom section settings are independent.")
        Festival.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { festival ->
                    FilterChip(selected = draft.top.festival == festival, onClick = {
                        update(draft.copy(top = draft.top.copy(festival = festival, heading = "", subtitle = "")))
                    }, label = { Text(festival.label) })
                }
            }
        }
        HorizontalDivider()
        Text("Top Blade Customization", style = MaterialTheme.typography.titleLarge)
        Text("Festival banner uses the selected preset when fields are blank. Normal keeps the existing banner.")
        OutlinedTextField(draft.top.heading, { update(draft.copy(top = draft.top.copy(heading = it))) }, label = { Text("Festival heading") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(draft.top.subtitle, { update(draft.copy(top = draft.top.copy(subtitle = it))) }, label = { Text("Festival subtitle") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(draft.top.cta, { update(draft.copy(top = draft.top.copy(cta = it))) }, label = { Text("Banner button text") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(draft.top.searchPlaceholder, { update(draft.copy(top = draft.top.copy(searchPlaceholder = it))) }, label = { Text("Search placeholder (blank = original)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        HorizontalDivider()
        Text("Bottom Section Customization", style = MaterialTheme.typography.titleLarge)
        StudioSwitch("Mode selector", draft.bottom.showModes) { update(draft.copy(bottom = draft.bottom.copy(showModes = it))) }
        StudioSwitch("Category chips", draft.bottom.showCategories) { update(draft.copy(bottom = draft.bottom.copy(showCategories = it))) }
        StudioSwitch("Product section", draft.bottom.showProducts) { update(draft.copy(bottom = draft.bottom.copy(showProducts = it))) }
        StudioSwitch("Floating cart summary", draft.bottom.showCart) { update(draft.copy(bottom = draft.bottom.copy(showCart = it))) }
        OutlinedTextField(draft.bottom.groceryTitle, { update(draft.copy(bottom = draft.bottom.copy(groceryTitle = it))) }, label = { Text("Grocery section title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        HorizontalDivider()
        Text("Animation Playground", style = MaterialTheme.typography.titleLarge)
        StudioSwitch("Festival banner fade", draft.top.animate) { update(draft.copy(top = draft.top.copy(animate = it))) }
        Text("Fade duration: ${draft.top.durationMs} ms · respects system reduced motion")
        Slider(value = draft.top.durationMs.toFloat(), onValueChange = { update(draft.copy(top = draft.top.copy(durationMs = it.toInt()))) }, valueRange = 0f..1000f)
        HorizontalDivider()
        Text("Live Test Preview", style = MaterialTheme.typography.titleLarge)
        Text("Preview uses an isolated sample catalog and temporary cart. Navigation and checkout are disabled.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { preview = false }) { Text("Original Preview") }
            Button(onClick = { preview = true }) { Text("Test Preview") }
        }
        OutlinedButton(onClick = { confirmReset = true }) { Text("Restore Default Configuration") }
        Spacer(Modifier.height(16.dp))
    }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("Discard draft?") },
        text = { Text("This removes the local experimental configuration. Production Home is unchanged.") },
        confirmButton = { TextButton(onClick = { store.reset(); draft = HomeStudioConfig(); confirmReset = false }) { Text("Restore") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
    )
}

@Composable
private fun StudioSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f).padding(top = 12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun StudioPreview(config: HomeStudioConfig?, onBack: () -> Unit, onMode: (Boolean) -> Unit, onNextFestival: () -> Unit) {
    val context = LocalContext.current
    var sample by remember { mutableStateOf(ShopUiState()) }
    LaunchedEffect(Unit) {
        SampleCatalogRepository().loadCatalog().onSuccess { catalog ->
            sample = ShopUiState(loading = false, isSample = true, categories = catalog.categories, products = catalog.products)
        }.onFailure { sample = ShopUiState(loading = false, error = "Sample catalog unavailable") }
    }
    val notice = { Toast.makeText(context, "Preview only: navigation and checkout are disabled", Toast.LENGTH_SHORT).show(); Unit }
    val reduced = LocalReducedMotion.current
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("‹ Studio") }
            Text(if (config == null) "ORIGINAL · SAMPLE" else "TEST · SAMPLE", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelMedium)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = config == null, onClick = { onMode(false) }, label = { Text("Original") })
            FilterChip(selected = config != null, onClick = { onMode(true) }, label = { Text("Test") })
            if (config != null) TextButton(onClick = onNextFestival) { Text("Theme: ${config.top.festival.label} ›") }
        }
        // Turning off experimental motion also disables the existing Home's decorative motion in test mode.
        CompositionLocalProvider(LocalReducedMotion provides (reduced || config?.top?.animate == false)) {
            Box(Modifier.weight(1f)) {
                HomeScreen(
                    state = sample, previewConfig = config,
                    onSearch = notice, onSeeAll = notice, onShopNow = notice, onAccount = notice, onCart = notice, onAddress = notice,
                    onModeSelected = { mode -> sample = sample.copy(selectedMode = mode, selectedCategoryId = null) },
                    onCategorySelected = { id -> sample = sample.copy(selectedCategoryId = id) },
                    onProduct = { notice() },
                    onAdd = { id -> sample.products.firstOrNull { it.id == id }?.let { product ->
                        sample = sample.copy(carts = updateModeCart(sample.carts, product, 1))
                    } },
                    onWishlist = { id -> sample = sample.copy(wishlist = if (id in sample.wishlist) sample.wishlist - id else sample.wishlist + id) },
                    onRetry = notice,
                )
            }
        }
        if (config?.bottom?.showCart != false) CartSummary(sample.totals, onClick = notice, isSample = true)
        RushBottomBar("home", onSelect = { notice() })
    }
}
