package com.rushit.app.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rushit.app.R
import com.rushit.app.ui.components.CategoryItem
import com.rushit.app.ui.components.MessageCard
import com.rushit.app.ui.components.ProductCard
import com.rushit.app.ui.components.RoundIconButton
import com.rushit.app.ui.components.RushIcon
import com.rushit.app.ui.components.SectionHeader
import com.rushit.app.ui.theme.BannerPeach
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.Cream
import com.rushit.app.ui.theme.CtaRed
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.Muted
import com.rushit.app.ui.theme.Peach
import com.rushit.app.ui.theme.SoftRed
import com.rushit.app.ui.viewmodel.ShopUiState

@Composable
fun HomeScreen(
    state: ShopUiState,
    onSearch: () -> Unit,
    onCategory: (String) -> Unit,
    onSeeAll: () -> Unit,
    onShopNow: () -> Unit,
    onAccount: () -> Unit,
    onAdd: (String) -> Unit,
    onWishlist: (String) -> Unit,
    onSaveAddress: (String) -> Unit,
    onRetry: () -> Unit,
) {
    var showAddress by rememberSaveable { mutableStateOf(false) }
    var showNotifications by rememberSaveable { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().background(Cream), contentPadding = PaddingValues(bottom = 20.dp)) {
        item(key = "header") {
            LocationHeader(state.address, onAddress = { showAddress = true }, onNotifications = { showNotifications = true }, onAccount = onAccount)
        }
        item(key = "search") {
            SearchEntry(onSearch, Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
        }
        item(key = "banner") {
            PromoBanner(onShopNow, Modifier.padding(horizontal = 16.dp))
        }
        if (state.isSample) {
            item(key = "sample") {
                Text("SAMPLE CATALOG · PRICES ARE ILLUSTRATIVE", color = Muted, style = MaterialTheme.typography.labelSmall,
                    letterSpacing = .4.sp, modifier = Modifier.padding(start = 18.dp, top = 14.dp, bottom = 2.dp))
            }
        } else {
            item(key = "banner_space") { Spacer(Modifier.height(14.dp)) }
        }
        when {
            state.loading -> item(key = "loading") {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Coral)
                }
            }
            state.error != null -> item(key = "error") {
                MessageCard("Couldn't load groceries", state.error, Modifier.padding(16.dp), icon = R.drawable.ic_help_circle, action = "Try again", onAction = onRetry)
            }
            else -> {
                item(key = "categories") {
                    LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(state.categories, key = { it.id }) { category -> CategoryItem(category, onClick = { onCategory(category.id) }) }
                    }
                }
                item(key = "best_header") {
                    SectionHeader("Bestsellers", "See All", onSeeAll, Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 5.dp))
                }
                val bestsellers = state.products.filter { it.isBestseller }
                if (bestsellers.isEmpty()) {
                    item(key = "best_empty") { MessageCard("No bestsellers yet", "Check back later for featured groceries.", Modifier.padding(horizontal = 16.dp)) }
                } else {
                    item(key = "best_products") {
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val cardWidth = ((maxWidth - 48.dp) / 3).coerceIn(100.dp, 145.dp)
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(bestsellers, key = { it.id }) { product ->
                                    ProductCard(product, state.cart[product.id] ?: 0, product.id in state.wishlist,
                                        onAdd = { onAdd(product.id) }, onWishlist = { onWishlist(product.id) }, width = cardWidth)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showAddress) {
        AddressDialog(state.address, onDismiss = { showAddress = false }, onSave = { onSaveAddress(it); showAddress = false })
    }
    if (showNotifications) {
        AlertDialog(onDismissRequest = { showNotifications = false }, title = { Text("Notifications") },
            text = { Text("Notifications aren't connected yet. We'll show updates here when they are available.") },
            confirmButton = { TextButton(onClick = { showNotifications = false }) { Text("Got it") } }, containerColor = CardWhite)
    }
}

@Composable
private fun LocationHeader(address: String?, onAddress: () -> Unit, onNotifications: () -> Unit, onAccount: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 12.dp, top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClickLabel = "Choose delivery address", onClick = onAddress).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(Peach), contentAlignment = Alignment.Center) {
                RushIcon(R.drawable.ic_location_01, null, Modifier.size(26.dp), Coral)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Deliver to", color = Muted, style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(address ?: "Select delivery location", color = Ink, style = MaterialTheme.typography.labelMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(3.dp))
                    RushIcon(R.drawable.ic_arrow_down_01, null, Modifier.size(15.dp), Ink)
                }
                Text(if (address == null) "Add an address to get started" else "Saved on this device", color = Muted,
                    style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(3.dp))
        RoundIconButton(R.drawable.ic_notification_01, "Notifications", onNotifications, background = Cream)
        RoundIconButton(R.drawable.ic_user_02, "Account", onAccount, background = Peach.copy(alpha = .7f))
    }
}

@Composable
private fun SearchEntry(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 54.dp).clip(CircleShape).background(CardWhite.copy(alpha = .76f))
            .border(1.dp, Border, CircleShape).clickable(role = Role.Button, onClickLabel = "Search products", onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        RushIcon(R.drawable.ic_search_01, null, Modifier.size(24.dp), Ink)
        Spacer(Modifier.width(12.dp))
        Text("Search for milk, fruits, snacks...", color = Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PromoBanner(onShop: () -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth().heightIn(min = 164.dp).clip(RoundedCornerShape(22.dp)).background(BannerPeach)) {
        val fadeEnd = with(LocalDensity.current) { maxWidth.toPx() * .68f }
        Image(painterResource(R.drawable.banner_groceries), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(BannerPeach, BannerPeach.copy(alpha = .94f), Color.Transparent), endX = fadeEnd)))
        Column(Modifier.widthIn(max = 200.dp).padding(start = 20.dp, top = 18.dp, bottom = 17.dp)) {
            Text("Everyday\nEssentials,", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 23.sp, lineHeight = 26.sp), color = Ink)
            Text("Delivered.", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 23.sp, lineHeight = 26.sp), color = Coral)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.clip(CircleShape).background(CtaRed).clickable(role = Role.Button, onClickLabel = "Shop now", onClick = onShop)
                .padding(horizontal = 15.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Shop Now", color = CardWhite, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(5.dp))
                RushIcon(R.drawable.ic_arrow_right_01, null, Modifier.size(16.dp), CardWhite)
            }
        }
    }
}

@Composable
fun AddressDialog(address: String?, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var draft by rememberSaveable(address) { mutableStateOf(address.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delivery address") },
        text = {
            Column {
                Text("Add an address or locality. It's saved only on this device; delivery coverage has not been checked.", style = MaterialTheme.typography.bodyMedium, color = Muted)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(value = draft, onValueChange = { draft = it.take(160) }, label = { Text("Address or locality") },
                    modifier = Modifier.fillMaxWidth(), minLines = 1, maxLines = 3, shape = RoundedCornerShape(16.dp))
            }
        },
        confirmButton = { TextButton(onClick = { onSave(draft) }, enabled = draft.isNotBlank()) { Text("Save") } },
        dismissButton = {
            Row {
                if (address != null) TextButton(onClick = { onSave("") }) { Text("Remove") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }, containerColor = CardWhite,
    )
}
