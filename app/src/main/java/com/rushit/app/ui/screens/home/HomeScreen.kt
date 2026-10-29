package com.rushit.app.ui.screens.home

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rushit.app.R
import com.rushit.app.data.model.ShoppingMode
import com.rushit.app.domain.categoriesForMode
import com.rushit.app.domain.productsForMode
import com.rushit.app.ui.components.CategoryItem
import com.rushit.app.ui.components.MessageCard
import com.rushit.app.ui.components.ProductCard
import com.rushit.app.ui.components.RoundIconButton
import com.rushit.app.ui.components.RushIcon
import com.rushit.app.ui.components.SectionHeader
import com.rushit.app.ui.components.ShoppingModeSwitcher
import com.rushit.app.ui.theme.BannerPeach
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.Cream
import com.rushit.app.ui.theme.CtaRed
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.Muted
import com.rushit.app.ui.theme.Peach
import com.rushit.app.ui.viewmodel.ShopUiState

@Composable
fun HomeScreen(
    state: ShopUiState,
    onSearch: () -> Unit,
    onCategory: (String) -> Unit,
    onSeeAll: () -> Unit,
    onShopNow: () -> Unit,
    onAccount: () -> Unit,
    onCart: () -> Unit,
    onAddress: () -> Unit,
    onModeSelected: (ShoppingMode) -> Unit,
    onProduct: (String) -> Unit,
    onAdd: (String) -> Unit,
    onWishlist: (String) -> Unit,
    onRetry: () -> Unit,
) {
    var showNotifications by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val categories = remember(state.categories, state.selectedMode) { categoriesForMode(state.categories, state.selectedMode) }
    val products = remember(state.products, state.selectedMode) { productsForMode(state.products, state.selectedMode) }
    val featured = remember(products, state.selectedMode) {
        if (state.selectedMode == ShoppingMode.GROCERY) products.filter { it.isBestseller } else products
    }
    LazyColumn(Modifier.fillMaxSize().background(Cream), state = listState, contentPadding = PaddingValues(bottom = 20.dp)) {
        item(key = "header") {
            LocationHeader(state.address, onAddress = onAddress, onNotifications = { showNotifications = true }, onAccount = onAccount)
        }
        item(key = "search") {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                SearchEntry(onSearch, state.selectedMode, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                RoundIconButton(R.drawable.ic_shopping_cart_01, "Open ${state.selectedMode.label} cart", onCart, background = CardWhite, tint = Coral)
            }
        }
        item(key = "banner") {
            PromoBanner(state.selectedMode, onShopNow, Modifier.padding(horizontal = 16.dp))
        }
        if (state.isSample) {
            item(key = "sample") {
                Text("Sample products · Illustrative prices", color = Muted, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 18.dp, top = 7.dp, bottom = 2.dp))
            }
        }
        item(key = "shopping_modes") {
            ShoppingModeSwitcher(state.selectedMode, onModeSelected, Modifier.padding(top = 5.dp, bottom = 8.dp))
        }
        when {
            state.loading -> item(key = "loading") {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Coral)
                }
            }
            state.error != null -> item(key = "error") {
                MessageCard("Couldn't load the catalog", state.error, Modifier.padding(16.dp), icon = R.drawable.ic_help_circle, action = "Try again", onAction = onRetry)
            }
            else -> {
                if (categories.isNotEmpty()) item(key = "categories") {
                    LazyRow(contentPadding = PaddingValues(start = 14.dp, end = 20.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(categories, key = { it.id }) { category -> CategoryItem(category, onClick = { onCategory(category.id) }) }
                    }
                }
                item(key = "best_header") {
                    val title = when (state.selectedMode) {
                        ShoppingMode.GROCERY -> "Bestsellers"
                        ShoppingMode.ECOMMERCE -> "Explore examples"
                        ShoppingMode.GIFTS -> "Gift collections"
                    }
                    SectionHeader(title, if (featured.isNotEmpty()) "See All" else null,
                        if (featured.isNotEmpty()) onSeeAll else null,
                        Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 5.dp))
                }
                if (featured.isEmpty()) {
                    item(key = "best_empty") {
                        val message = when (state.selectedMode) {
                            ShoppingMode.GIFTS -> "Gift inventory isn't configured yet. Explore the categories for a preview."
                            ShoppingMode.ECOMMERCE -> "No e-commerce listings have been configured yet."
                            ShoppingMode.GROCERY -> "Check back later for featured groceries."
                        }
                        MessageCard("Nothing here yet", message, Modifier.padding(horizontal = 16.dp))
                    }
                } else {
                    item(key = "best_products") {
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val cardWidth = ((maxWidth - 48.dp) / 3).coerceIn(100.dp, 145.dp)
                            LazyRow(contentPadding = PaddingValues(start = 16.dp, end = 20.dp, top = 5.dp, bottom = 5.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(featured, key = { it.id }) { product ->
                                    ProductCard(product, state.quantityFor(product), product.id in state.wishlist,
                                        onAdd = { onAdd(product.id) }, onWishlist = { onWishlist(product.id) },
                                        onOpen = { onProduct(product.id) }, width = cardWidth)
                                }
                            }
                        }
                    }
                }
            }
        }
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
private fun SearchEntry(onClick: () -> Unit, mode: ShoppingMode, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 54.dp).clip(CircleShape).background(CardWhite.copy(alpha = .76f))
            .border(1.dp, Border, CircleShape).clickable(role = Role.Button, onClickLabel = "Search products", onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        RushIcon(R.drawable.ic_search_01, null, Modifier.size(24.dp), Ink)
        Spacer(Modifier.width(12.dp))
        Text(if (mode == ShoppingMode.GROCERY) "Search for milk, fruits, snacks..." else "Search ${mode.label.lowercase()}...", color = Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PromoBanner(mode: ShoppingMode, onShop: () -> Unit, modifier: Modifier = Modifier) {
    val image = when (mode) {
        ShoppingMode.GROCERY -> R.drawable.banner_groceries
        ShoppingMode.ECOMMERCE -> R.drawable.ecommerce_headphones
        ShoppingMode.GIFTS -> R.drawable.gift_bouquet
    }
    val headline = when (mode) {
        ShoppingMode.GROCERY -> "Everyday\nEssentials,"
        ShoppingMode.ECOMMERCE -> "Find more\nfor every day,"
        ShoppingMode.GIFTS -> "Thoughtful gifts\nfor every"
    }
    val accent = when (mode) {
        ShoppingMode.GROCERY -> "Delivered."
        ShoppingMode.ECOMMERCE -> "Explore."
        ShoppingMode.GIFTS -> "moment."
    }
    BoxWithConstraints(modifier.fillMaxWidth().heightIn(min = 164.dp).clip(RoundedCornerShape(22.dp)).background(BannerPeach)) {
        val fadeEnd = with(LocalDensity.current) { maxWidth.toPx() * .68f }
        Image(painterResource(image), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(BannerPeach, BannerPeach.copy(alpha = .94f), Color.Transparent), endX = fadeEnd)))
        Column(Modifier.widthIn(max = 200.dp).padding(start = 20.dp, top = 18.dp, bottom = 17.dp)) {
            Text(headline, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 23.sp, lineHeight = 26.sp), color = Ink)
            Text(accent, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 23.sp, lineHeight = 26.sp), color = Coral)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.clip(CircleShape).background(CtaRed).clickable(role = Role.Button, onClickLabel = "Explore ${mode.label}", onClick = onShop)
                .padding(horizontal = 15.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (mode == ShoppingMode.GROCERY) "Shop Now" else "Explore", color = CardWhite, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(5.dp))
                RushIcon(R.drawable.ic_arrow_right_01, null, Modifier.size(16.dp), CardWhite)
            }
        }
    }
}
