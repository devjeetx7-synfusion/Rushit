package com.rushit.app.ui.screens.store
import androidx.compose.foundation.BorderStroke

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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rushit.app.R
import com.rushit.app.data.model.Category
import com.rushit.app.data.model.Product
import com.rushit.app.data.model.ShoppingMode
import com.rushit.app.domain.categoriesForMode
import com.rushit.app.domain.productsForMode
import com.rushit.app.domain.searchProducts
import com.rushit.app.ui.components.CatalogImage
import com.rushit.app.ui.components.RushButton
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
    val countsByCategory = remember(state.products, state.selectedMode) {
        state.products.filter { it.mode == state.selectedMode }.groupingBy { it.categoryId }.eachCount()
    }
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
                            Column(Modifier.weight(1f)) {
                                Text(category.name, style = MaterialTheme.typography.titleMedium, color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                val count = countsByCategory[category.id] ?: 0
                                Text(
                                    if (count > 0) "$count ${if (count == 1) "item" else "items"}" else "Preview only · no items yet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Muted,
                                )
                            }
                            RushIcon(R.drawable.ic_arrow_right_01, null, Modifier.size(22.dp), Coral)
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
    var sort by rememberSaveable(title) { mutableStateOf("Popularity") }
    var expandedSort by remember { mutableStateOf(false) }
    
    // Filters
    var showFilters by remember { mutableStateOf(false) }
    var filterAvailability by rememberSaveable { mutableStateOf(false) }
    var filterDiscount by rememberSaveable { mutableStateOf(false) }
    var filterBrand by rememberSaveable { mutableStateOf<String?>(null) }
    var filterMinPrice by rememberSaveable { mutableStateOf<Float?>(null) }
    var filterMaxPrice by rememberSaveable { mutableStateOf<Float?>(null) }
    var filterMinRating by rememberSaveable { mutableStateOf<Float?>(null) }
    
    val brands = remember(products) { products.mapNotNull { it.brand }.distinct().sorted() }
    
    val filteredAndSorted = remember(products, sort, filterAvailability, filterDiscount, filterBrand, filterMinPrice, filterMaxPrice, filterMinRating) {
        var res = products
        if (filterAvailability) res = res.filter { it.isAvailable && (it.stockQuantity == null || it.stockQuantity > 0) }
        if (filterDiscount) res = res.filter { it.originalPricePaise != null && it.originalPricePaise > it.pricePaise }
        if (filterBrand != null) res = res.filter { it.brand == filterBrand }
        if (filterMinPrice != null) res = res.filter { it.pricePaise >= filterMinPrice!! * 100 }
        if (filterMaxPrice != null) res = res.filter { it.pricePaise <= filterMaxPrice!! * 100 }
        if (filterMinRating != null) res = res.filter { (it.rating ?: 0.0) >= filterMinRating!! }
        
        when (sort) {
            "Price low to high" -> res.sortedBy { it.pricePaise }
            "Price high to low" -> res.sortedByDescending { it.pricePaise }
            "Newest" -> res.sortedByDescending { it.id }
            else -> res // Popularity / Recommended
        }
    }
    
    val activeFilterCount = (if(filterAvailability) 1 else 0) + (if(filterDiscount) 1 else 0) + (if(filterBrand != null) 1 else 0) + (if(filterMinPrice != null || filterMaxPrice != null) 1 else 0) + (if(filterMinRating != null) 1 else 0)

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
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(16.dp), 
                color = if (activeFilterCount > 0) Coral else CardWhite,
                border = BorderStroke(1.dp, if (activeFilterCount > 0) Coral else Border),
                modifier = Modifier.clickable { showFilters = true }
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    RushIcon(R.drawable.ic_search_01, null, Modifier.size(14.dp), if (activeFilterCount > 0) Color.White else Ink)
                    Spacer(Modifier.width(6.dp))
                    Text("Filters" + if (activeFilterCount > 0) " ($activeFilterCount)" else "", style = MaterialTheme.typography.labelMedium, color = if (activeFilterCount > 0) Color.White else Ink)
                }
            }
            Spacer(Modifier.width(12.dp))
            Box {
                Surface(
                    shape = RoundedCornerShape(16.dp), 
                    color = CardWhite, border = BorderStroke(1.dp, Border),
                    modifier = Modifier.clickable { expandedSort = true }
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Sort: $sort", style = MaterialTheme.typography.labelMedium, color = Ink)
                        Spacer(Modifier.width(6.dp))
                        RushIcon(R.drawable.ic_arrow_down_01, null, Modifier.size(14.dp), Ink)
                    }
                }
                DropdownMenu(expanded = expandedSort, onDismissRequest = { expandedSort = false }) {
                    listOf("Popularity", "Price low to high", "Price high to low", "Newest").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = { sort = option; expandedSort = false }
                        )
                    }
                }
            }
        }
        
        // Active Filter Chips
        if (activeFilterCount > 0) {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                if (filterAvailability) item { FilterChip("In Stock") { filterAvailability = false } }
                if (filterDiscount) item { FilterChip("Discounted") { filterDiscount = false } }
                if (filterBrand != null) item { FilterChip(filterBrand!!) { filterBrand = null } }
                if (filterMinPrice != null || filterMaxPrice != null) item { FilterChip("Price") { filterMinPrice = null; filterMaxPrice = null } }
                if (filterMinRating != null) item { FilterChip("${filterMinRating}★ & above") { filterMinRating = null } }
                item { 
                    Text("Clear All", color = Coral, style = MaterialTheme.typography.labelMedium, modifier = Modifier.clickable { 
                        filterAvailability = false; filterDiscount = false; filterBrand = null; filterMinPrice = null; filterMaxPrice = null; filterMinRating = null 
                    }.padding(vertical = 6.dp, horizontal = 4.dp))
                }
            }
        }

        ProductResults(filteredAndSorted, state, onAdd, onWishlist, onProduct, onRetry, emptyTitle, emptyMessage)
    }
    
    if (showFilters) {
        ModalBottomSheet(onDismissRequest = { showFilters = false }) {
            Column(Modifier.padding(20.dp)) {
                Text("Filters", style = MaterialTheme.typography.titleLarge, color = Ink)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { filterAvailability = !filterAvailability }.padding(vertical = 8.dp)) {
                    Checkbox(checked = filterAvailability, onCheckedChange = { filterAvailability = it }, colors = CheckboxDefaults.colors(checkedColor = Coral))
                    Text("Show only In Stock", style = MaterialTheme.typography.bodyMedium, color = Ink)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { filterDiscount = !filterDiscount }.padding(vertical = 8.dp)) {
                    Checkbox(checked = filterDiscount, onCheckedChange = { filterDiscount = it }, colors = CheckboxDefaults.colors(checkedColor = Coral))
                    Text("Has Discount", style = MaterialTheme.typography.bodyMedium, color = Ink)
                }
                if (brands.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("Brand", style = MaterialTheme.typography.titleMedium, color = Ink)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        items(brands) { b ->
                            Surface(
                                shape = RoundedCornerShape(16.dp), color = if (b == filterBrand) Coral else CardWhite, border = BorderStroke(1.dp, if (b == filterBrand) Coral else Border),
                                modifier = Modifier.clickable { filterBrand = if (filterBrand == b) null else b }
                            ) {
                                Text(b, color = if (b == filterBrand) Color.White else Ink, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Minimum Rating", style = MaterialTheme.typography.titleMedium, color = Ink)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    listOf(4f, 3f, 2f, 1f).forEach { r ->
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp), color = if (r == filterMinRating) Coral else CardWhite, border = BorderStroke(1.dp, if (r == filterMinRating) Coral else Border),
                                modifier = Modifier.clickable { filterMinRating = if (filterMinRating == r) null else r }
                            ) {
                                Text("$r★ & above", color = if (r == filterMinRating) Color.White else Ink, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                RushButton("Apply", { showFilters = false })
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun FilterChip(label: String, onRemove: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = Peach, border = BorderStroke(1.dp, Coral)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Coral)
            Spacer(Modifier.width(4.dp))
            RushIcon(R.drawable.ic_cancel_01, "Remove", Modifier.size(12.dp).clickable { onRemove() }, Coral)
        }
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
