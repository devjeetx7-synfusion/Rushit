package com.rushit.app.ui.screens.store

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.rushit.app.R
import com.rushit.app.data.model.Category
import com.rushit.app.data.model.Product
import com.rushit.app.data.model.ShoppingMode
import com.rushit.app.domain.categoriesForMode
import com.rushit.app.domain.productsForMode
import com.rushit.app.domain.searchProducts
import com.rushit.app.ui.components.CatalogImage
import com.rushit.app.ui.components.MessageCard
import com.rushit.app.ui.components.ProductCard
import com.rushit.app.ui.components.RoundIconButton
import com.rushit.app.ui.components.RushIcon
import com.rushit.app.ui.components.ScreenHeader
import com.rushit.app.ui.components.ShoppingModeSwitcher
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.Cream
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.Muted
import com.rushit.app.ui.theme.Peach
import com.rushit.app.ui.viewmodel.ShopUiState

@Composable
fun CategoriesScreen(state: ShopUiState, onModeSelected: (ShoppingMode) -> Unit, onCategory: (String) -> Unit, onRetry: () -> Unit) {
    val categories = remember(state.categories, state.selectedMode) { categoriesForMode(state.categories, state.selectedMode) }
    Column(Modifier.fillMaxSize().background(Cream)) {
        Text("Categories", style = MaterialTheme.typography.headlineMedium, color = Ink, modifier = Modifier.padding(start = 20.dp, top = 22.dp))
        Text(if (state.isSample) "Explore ${state.selectedMode.label.lowercase()} examples" else "Explore ${state.selectedMode.label.lowercase()}",
            style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 10.dp))
        ShoppingModeSwitcher(state.selectedMode, onModeSelected, Modifier.padding(bottom = 12.dp))
        when {
            state.loading -> Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Coral) }
            state.error != null -> MessageCard("Couldn't load categories", state.error, Modifier.padding(horizontal = 16.dp), action = "Try again", onAction = onRetry)
            categories.isEmpty() -> MessageCard("No categories yet", "There are no categories configured for this mode.", Modifier.padding(horizontal = 16.dp))
            else -> LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(categories, key = { it.id }) { category ->
                    Surface(shape = RoundedCornerShape(18.dp), color = CardWhite, shadowElevation = 1.dp) {
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(role = Role.Button, onClickLabel = "Browse ${category.name}") { onCategory(category.id) }
                            .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(64.dp).clip(CircleShape).background(Peach), contentAlignment = Alignment.Center) {
                                CatalogImage(category.imageUrl, category.imageRes, null, Modifier.size(55.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Text(category.name, style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.weight(1f))
                            RushIcon(R.drawable.ic_arrow_right_01, null, Modifier.size(22.dp), Coral)
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductListingScreen(
    title: String,
    products: List<Product>,
    state: ShopUiState,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    onWishlist: (String) -> Unit,
    onProduct: (String) -> Unit,
    onRetry: () -> Unit,
    category: Category? = null,
    emptyTitle: String = "Nothing here yet",
    emptyMessage: String = "There are no products in this collection right now.",
) {
    var sort by rememberSaveable(title) { mutableStateOf("Recommended") }
    var expanded by remember { mutableStateOf(false) }
    val sorted = remember(products, sort) {
        when (sort) {
            "Price: low to high" -> products.sortedBy { it.pricePaise }
            "Price: high to low" -> products.sortedByDescending { it.pricePaise }
            "Name" -> products.sortedBy { it.name.lowercase() }
            else -> products
        }
    }
    Column(Modifier.fillMaxSize().background(Cream)) {
        ScreenHeader(title, onBack)
        if (category != null) {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(Peach), contentAlignment = Alignment.Center) {
                    CatalogImage(category.imageUrl, category.imageRes, null, Modifier.size(42.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("Explore ${category.name}", style = MaterialTheme.typography.bodyMedium, color = Muted)
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (state.isSample) "Sample products · Illustrative prices" else "${products.size} products",
                style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.weight(1f))
            Box {
                Text("Sort: $sort", style = MaterialTheme.typography.labelSmall, color = Coral,
                    modifier = Modifier.clip(CircleShape).clickable(role = Role.Button) { expanded = true }.padding(horizontal = 8.dp, vertical = 12.dp))
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("Recommended", "Price: low to high", "Price: high to low", "Name").forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = { sort = option; expanded = false })
                    }
                }
            }
        }
        ProductResults(sorted, state, onAdd, onWishlist, onProduct, onRetry, emptyTitle, emptyMessage)
    }
}

@Composable
fun SearchScreen(
    state: ShopUiState,
    onBack: () -> Unit,
    onQuery: (String) -> Unit,
    onSubmit: () -> Unit,
    onRecent: (String) -> Unit,
    onClearRecent: () -> Unit,
    onCategoryFilter: (String?) -> Unit,
    onAdd: (String) -> Unit,
    onWishlist: (String) -> Unit,
    onProduct: (String) -> Unit,
    onRetry: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focus.requestFocus() }
    val categories = remember(state.categories, state.selectedMode) { categoriesForMode(state.categories, state.selectedMode) }
    val products = remember(state.products, state.selectedMode) { productsForMode(state.products, state.selectedMode) }
    val results = remember(products, categories, state.searchQuery, state.searchCategoryId) {
        searchProducts(products, categories, state.searchQuery).filter { state.searchCategoryId == null || it.categoryId == state.searchCategoryId }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(Cream).imePadding()) {
        // Keep results reachable above the IME on very short screens; browse chips return when it closes.
        val compact = maxHeight < (330.dp * LocalDensity.current.fontScale.coerceAtLeast(1f))
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(R.drawable.ic_arrow_left_01, "Go back", onBack, background = Peach)
                Spacer(Modifier.width(8.dp))
                Row(Modifier.weight(1f).heightIn(min = 52.dp).clip(CircleShape).background(CardWhite).border(1.dp, Border, CircleShape).padding(start = 14.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    RushIcon(R.drawable.ic_search_01, null, Modifier.size(22.dp), Ink)
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(value = state.searchInput, onValueChange = onQuery, singleLine = true,
                        modifier = Modifier.weight(1f).focusRequester(focus).semantics { contentDescription = "Search products by name or category" },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Ink), cursorBrush = SolidColor(Coral),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSubmit(); keyboard?.hide() }),
                        decorationBox = { inner -> Box { if (state.searchInput.isEmpty()) Text("Search ${state.selectedMode.label.lowercase()}...", style = MaterialTheme.typography.bodyMedium, color = Muted); inner() } })
                    if (state.searchInput.isNotEmpty()) {
                        RoundIconButton(R.drawable.ic_cancel_01, "Clear search", { onQuery("") }, modifier = Modifier.size(44.dp), background = CardWhite, tint = Muted)
                    }
                }
            }
            if (!compact && state.searchInput.isBlank() && state.recentSearches.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Recent searches", style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.weight(1f))
                    Text("Clear", style = MaterialTheme.typography.labelMedium, color = Coral,
                        modifier = Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onClearRecent).padding(horizontal = 12.dp, vertical = 12.dp))
                }
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.recentSearches, key = { it.lowercase() }) { recent ->
                        SearchChip(recent, selected = false, onClick = { onRecent(recent) })
                    }
                }
            }
            if (!compact && categories.isNotEmpty()) {
                Text("Browse categories", style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item(key = "all") { SearchChip("All", state.searchCategoryId == null) { onCategoryFilter(null) } }
                    items(categories, key = { it.id }) { category ->
                        SearchChip(category.name, state.searchCategoryId == category.id) { onCategoryFilter(category.id) }
                    }
                }
            }
            Text(if (state.searchInput.isBlank()) "Explore products" else "Results", style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
            if (state.isSample) Text("Sample products · Illustrative prices", style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp))
            if (state.searchInput != state.searchQuery) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(28.dp), color = Coral, strokeWidth = 2.dp) }
            } else {
                ProductResults(results, state, onAdd, onWishlist, { id -> onSubmit(); onProduct(id) }, onRetry,
                    "No matching products", "Try a different product name or category.")
            }
        }
    }
}

@Composable
private fun SearchChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(label, color = if (selected) Coral else Ink, style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.clip(CircleShape).background(if (selected) Peach else CardWhite).border(1.dp, Border, CircleShape)
            .selectable(selected = selected, role = Role.Button, onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp))
}

@Composable
private fun ProductResults(
    products: List<Product>, state: ShopUiState, onAdd: (String) -> Unit, onWishlist: (String) -> Unit,
    onProduct: (String) -> Unit, onRetry: () -> Unit, emptyTitle: String, emptyMessage: String,
) {
    when {
        state.loading -> Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Coral) }
        state.error != null -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            MessageCard("Couldn't load products", state.error, Modifier.padding(16.dp), action = "Try again", onAction = onRetry)
        }
        products.isEmpty() -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            MessageCard(emptyTitle, emptyMessage, Modifier.padding(16.dp), icon = R.drawable.ic_search_01)
        }
        else -> LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 116.dp), modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(products, key = { it.id }) { product ->
                ProductCard(product, state.quantityFor(product), product.id in state.wishlist,
                    onAdd = { onAdd(product.id) }, onWishlist = { onWishlist(product.id) }, onOpen = { onProduct(product.id) },
                    modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
