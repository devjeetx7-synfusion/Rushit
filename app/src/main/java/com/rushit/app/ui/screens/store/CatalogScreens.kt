package com.rushit.app.ui.screens.store

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.rushit.app.R
import com.rushit.app.data.model.Product
import com.rushit.app.domain.searchProducts
import com.rushit.app.ui.components.CatalogImage
import com.rushit.app.ui.components.MessageCard
import com.rushit.app.ui.components.ProductCard
import com.rushit.app.ui.components.RoundIconButton
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

@Composable
fun CategoriesScreen(state: ShopUiState, onCategory: (String) -> Unit, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Cream)) {
        Text("Categories", style = MaterialTheme.typography.headlineMedium, color = Ink, modifier = Modifier.padding(start = 20.dp, top = 22.dp))
        Text(if (state.isSample) "Explore our sample catalog" else "Find your everyday essentials", style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 18.dp))
        when {
            state.loading -> Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Coral) }
            state.error != null -> MessageCard("Couldn't load categories", state.error, Modifier.padding(horizontal = 16.dp), action = "Try again", onAction = onRetry)
            state.categories.isEmpty() -> MessageCard("No categories yet", "Check back later for more groceries.", Modifier.padding(horizontal = 16.dp))
            else -> LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.categories, key = { it.id }) { category ->
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
    onRetry: () -> Unit,
    emptyTitle: String = "Nothing here yet",
    emptyMessage: String = "There are no products in this collection right now.",
) {
    Column(Modifier.fillMaxSize().background(Cream)) {
        ScreenHeader(title, onBack)
        if (state.isSample) Text("Sample catalog · Prices are illustrative", style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp))
        ProductResults(products, state, onAdd, onWishlist, onRetry, emptyTitle, emptyMessage)
    }
}

@Composable
fun SearchScreen(state: ShopUiState, onBack: () -> Unit, onQuery: (String) -> Unit, onAdd: (String) -> Unit, onWishlist: (String) -> Unit, onRetry: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val results = remember(state.products, state.categories, state.searchQuery) { searchProducts(state.products, state.categories, state.searchQuery) }
    Column(Modifier.fillMaxSize().background(Cream)) {
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
                    decorationBox = { inner -> Box { if (state.searchInput.isEmpty()) Text("Search groceries...", style = MaterialTheme.typography.bodyMedium, color = Muted); inner() } })
                if (state.searchInput.isNotEmpty()) {
                    RoundIconButton(R.drawable.ic_cancel_01, "Clear search", { onQuery("") }, modifier = Modifier.size(44.dp), background = CardWhite, tint = Muted)
                }
            }
        }
        Text(if (state.searchInput.isBlank()) "Search by product or category" else "Results", style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
        if (state.isSample) Text("Sample catalog · Prices are illustrative", style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        if (state.searchInput != state.searchQuery) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(28.dp), color = Coral, strokeWidth = 2.dp) }
        } else {
            ProductResults(results, state, onAdd, onWishlist, onRetry, "No matching products", "Try a different product name or category.")
        }
    }
}

@Composable
private fun ProductResults(
    products: List<Product>, state: ShopUiState, onAdd: (String) -> Unit, onWishlist: (String) -> Unit, onRetry: () -> Unit,
    emptyTitle: String, emptyMessage: String,
) {
    when {
        state.loading -> Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Coral) }
        state.error != null -> MessageCard("Couldn't load products", state.error, Modifier.padding(16.dp), action = "Try again", onAction = onRetry)
        products.isEmpty() -> MessageCard(emptyTitle, emptyMessage, Modifier.padding(16.dp), icon = R.drawable.ic_search_01)
        else -> LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 116.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(products, key = { it.id }) { product ->
                ProductCard(product, state.cart[product.id] ?: 0, product.id in state.wishlist,
                    onAdd = { onAdd(product.id) }, onWishlist = { onWishlist(product.id) }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
