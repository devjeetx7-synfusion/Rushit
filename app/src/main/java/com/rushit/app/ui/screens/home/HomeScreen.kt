package com.rushit.app.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rushit.app.R
import com.rushit.app.data.model.Product
import com.rushit.app.data.model.ShoppingMode
import com.rushit.app.domain.categoriesForMode
import com.rushit.app.domain.productsForSelection
import com.rushit.app.ui.components.CategoryRail
import com.rushit.app.ui.components.MessageCard
import com.rushit.app.ui.components.ProductCard
import com.rushit.app.ui.components.RoundIconButton
import com.rushit.app.ui.components.RushIcon
import com.rushit.app.ui.components.ShoppingModeSwitcher
import com.rushit.app.ui.theme.BannerPeach
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.Cream
import com.rushit.app.ui.theme.CtaRed
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.LocalReducedMotion
import com.rushit.app.ui.theme.Motion
import com.rushit.app.ui.theme.Muted
import com.rushit.app.ui.theme.Peach
import com.rushit.app.ui.theme.motionDuration
import com.rushit.app.ui.studio.HomeStudioConfig
import com.rushit.app.ui.studio.Festival
import com.rushit.app.ui.viewmodel.ShopUiState
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    state: ShopUiState,
    onSearch: () -> Unit,
    onSeeAll: () -> Unit,
    onShopNow: () -> Unit,
    onAccount: () -> Unit,
    onCart: () -> Unit,
    onAddress: () -> Unit,
    onModeSelected: (ShoppingMode) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onProduct: (String) -> Unit,
    onAdd: (String) -> Unit,
    onWishlist: (String) -> Unit,
    onRetry: () -> Unit,
    previewConfig: HomeStudioConfig? = null,
) {
    var showNotifications by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val categories = remember(state.categories, state.selectedMode) { categoriesForMode(state.categories, state.selectedMode) }
    val selectedCategory = remember(categories, state.selectedCategoryId) { categories.firstOrNull { it.id == state.selectedCategoryId } }
    val products = remember(state.products, state.selectedMode, state.selectedCategoryId) {
        productsForSelection(state.products, state.selectedMode, state.selectedCategoryId)
    }
    val sectionTitle = selectedCategory?.name ?: when (state.selectedMode) {
        ShoppingMode.GROCERY -> previewConfig?.bottom?.groceryTitle?.takeIf { it.isNotBlank() } ?: "Bestsellers"
        ShoppingMode.ECOMMERCE -> "Explore examples"
        ShoppingMode.GIFTS -> "Gift collections"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Cream),
        state = listState,
        contentPadding = PaddingValues(bottom = 18.dp),
    ) {
        item(key = "header") {
            LocationHeader(state.address, onAddress = onAddress, onNotifications = { showNotifications = true }, onAccount = onAccount)
        }
        item(key = "search") {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchEntry(onSearch, state.selectedMode, Modifier.weight(1f), previewConfig?.top?.searchPlaceholder)
                Spacer(Modifier.width(8.dp))
                RoundIconButton(
                    R.drawable.ic_shopping_cart_01,
                    "Open ${state.selectedMode.label} cart",
                    onCart,
                    background = CardWhite,
                    tint = Coral,
                    badge = state.totals.itemCount,
                )
            }
        }
        item(key = "banner") {
            if (previewConfig == null || previewConfig.top.festival == Festival.NORMAL) {
                PromoBannerCarousel(state.selectedMode, onShopNow, Modifier.padding(horizontal = 16.dp))
            } else {
                FestivalBanner(previewConfig, onShopNow)
            }
        }
        if (state.isSample) {
            item(key = "sample") {
                Text(
                    "Sample products · Illustrative prices",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 18.dp, top = 8.dp),
                )
            }
        }
        if (previewConfig?.bottom?.showModes != false) item(key = "shopping_modes") {
            ShoppingModeSwitcher(
                selectedMode = state.selectedMode,
                onModeSelected = onModeSelected,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            )
        }
        if (previewConfig?.bottom?.showCategories != false) item(key = "categories") {
            if (categories.isNotEmpty()) {
                CategoryRail(
                    categories = categories,
                    selectedCategoryId = state.selectedCategoryId,
                    onCategorySelected = onCategorySelected,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
        }
        if (previewConfig?.bottom?.showProducts != false) item(key = "section_header") {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sectionTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (!state.loading && state.error == null) {
                        Row(
                            Modifier.clip(RoundedCornerShape(12.dp))
                                .clickable(role = Role.Button, onClickLabel = "See all $sectionTitle", onClick = onSeeAll)
                                .padding(start = 10.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("See All", style = MaterialTheme.typography.labelMedium, color = Coral)
                            Spacer(Modifier.width(4.dp))
                            RushIcon(R.drawable.ic_arrow_right_01, null, Modifier.size(16.dp), Coral)
                        }
                    }
                }
                Text(
                    text = when {
                        state.loading -> "Loading the catalog…"
                        state.error != null -> "Catalog unavailable"
                        selectedCategory != null -> "${products.size} ${if (products.size == 1) "item" else "items"} in ${selectedCategory.name}"
                        else -> "${products.size} ${if (products.size == 1) "item" else "items"} · ${state.selectedMode.label}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (previewConfig?.bottom?.showProducts != false) item(key = "products") {
            ProductSection(
                state = state,
                onAdd = onAdd,
                onWishlist = onWishlist,
                onProduct = onProduct,
                onRetry = onRetry,
                onBrowseAll = { onCategorySelected(null) },
            )
        }
    }

    if (showNotifications) {
        AlertDialog(
            onDismissRequest = { showNotifications = false },
            title = { Text("Notifications") },
            text = { Text("Notifications aren't connected yet. We'll show updates here when they are available.") },
            confirmButton = { TextButton(onClick = { showNotifications = false }) { Text("Got it") } },
            containerColor = CardWhite,
        )
    }
}

@Composable
private fun LocationHeader(address: String?, onAddress: () -> Unit, onNotifications: () -> Unit, onAccount: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 14.dp, end = 12.dp, top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                .clickable(role = Role.Button, onClickLabel = "Choose delivery address", onClick = onAddress)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(Peach), contentAlignment = Alignment.Center) {
                RushIcon(R.drawable.ic_location_01, null, Modifier.size(24.dp), Coral)
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text("Deliver to", color = Muted, style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        address ?: "Select delivery location",
                        color = Ink,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(3.dp))
                    RushIcon(R.drawable.ic_arrow_down_01, null, Modifier.size(14.dp), Ink)
                }
                Text(
                    if (address == null) "Add an address to get started" else "Saved on this device",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(2.dp))
        RoundIconButton(R.drawable.ic_notification_01, "Notifications", onNotifications, background = Cream)
        RoundIconButton(R.drawable.ic_user_02, "Account", onAccount, background = Peach.copy(alpha = .7f))
    }
}

@Composable
private fun SearchEntry(onClick: () -> Unit, mode: ShoppingMode, modifier: Modifier = Modifier, placeholder: String? = null) {
    Row(
        modifier.heightIn(min = 54.dp).clip(CircleShape).background(CardWhite.copy(alpha = .76f))
            .border(1.dp, Border, CircleShape).clickable(role = Role.Button, onClickLabel = "Search products", onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RushIcon(R.drawable.ic_search_01, null, Modifier.size(22.dp), Ink)
        Spacer(Modifier.width(12.dp))
        Text(
            placeholder?.takeIf { it.isNotBlank() } ?: if (mode == ShoppingMode.GROCERY) "Search for milk, fruits, snacks…" else "Search ${mode.label.lowercase()}…",
            color = Muted,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FestivalBanner(config: HomeStudioConfig, onShop: () -> Unit) {
    val top = config.top
    val festival = top.festival
    val reduced = LocalReducedMotion.current
    AnimatedContent(
        targetState = festival,
        transitionSpec = {
            val duration = if (reduced || !top.animate) 0 else top.durationMs
            fadeIn(tween(duration)).togetherWith(fadeOut(tween(duration)))
        },
        label = "festival banner",
        modifier = Modifier.padding(horizontal = 16.dp),
    ) { selected ->
        Column(
            Modifier.fillMaxWidth().heightIn(min = 162.dp).clip(RoundedCornerShape(22.dp))
                .background(Brush.horizontalGradient(listOf(Color(selected.color), Color(selected.color).copy(alpha = .72f))))
                .padding(20.dp),
        ) {
            Text(top.heading.ifBlank { selected.heading }, style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text(top.subtitle.ifBlank { selected.subtitle }, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            Spacer(Modifier.height(10.dp))
            Text(top.cta.ifBlank { "Shop Now" }, color = Color.White, style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clip(CircleShape).background(CtaRed).clickable(role = Role.Button, onClick = onShop)
                    .padding(horizontal = 15.dp, vertical = 10.dp))
        }
    }
}

private data class PromoPage(val headline: String, val accent: String, val cta: String, val image: Int)

private fun promoPages(mode: ShoppingMode): List<PromoPage> = when (mode) {
    ShoppingMode.GROCERY -> listOf(
        PromoPage("Everyday\nEssentials,", "Delivered.", "Shop Now", R.drawable.banner_groceries),
        PromoPage("Fresh fruit\n& veggies,", "Picked today.", "Shop Now", R.drawable.product_tomatoes),
        PromoPage("Snacks for\nthe evening,", "In minutes.", "Shop Now", R.drawable.category_snacks),
    )
    ShoppingMode.ECOMMERCE -> listOf(
        PromoPage("Find more\nfor every day,", "Explore.", "Explore", R.drawable.ecommerce_headphones),
        PromoPage("Warm light\nfor your home,", "Discover.", "Explore", R.drawable.ecommerce_lamp),
        PromoPage("Beauty &\ncare picks,", "Browse.", "Explore", R.drawable.category_care),
    )
    ShoppingMode.GIFTS -> listOf(
        PromoPage("Thoughtful gifts\nfor every", "moment.", "Explore", R.drawable.gift_bouquet),
        PromoPage("Beauty &\nfragrance,", "Gift ready.", "Explore", R.drawable.category_care),
        PromoPage("Gadgets made\nfor gifting,", "Explore.", "Explore", R.drawable.ecommerce_headphones),
    )
}

/**
 * Auto-advancing promo carousel. Rotation stops while the user drags and is disabled entirely when
 * the system asks for reduced motion; it also stops naturally when Home leaves composition.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PromoBannerCarousel(mode: ShoppingMode, onShop: () -> Unit, modifier: Modifier = Modifier) {
    val pages = remember(mode) { promoPages(mode) }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val reduced = LocalReducedMotion.current

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(pagerState, reduced, pages.size, lifecycle) {
        if (reduced || pages.size <= 1) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(Motion.BANNER_AUTO_ADVANCE_MS)
                if (!pagerState.isScrollInProgress) {
                    pagerState.animateScrollToPage((pagerState.currentPage + 1) % pages.size)
                }
            }
        }
    }

    Column(modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth().heightIn(min = 162.dp),
        ) { index ->
            PromoCard(pages[index], onShop)
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
            pages.indices.forEach { index ->
                val active = index == pagerState.currentPage
                val width by animateDpAsState(if (active) 18.dp else 6.dp, label = "banner dot")
                Box(
                    Modifier.padding(horizontal = 3.dp).height(6.dp).width(width)
                        .clip(CircleShape).background(if (active) Coral else Border),
                )
            }
        }
    }
}

@Composable
private fun PromoCard(page: PromoPage, onShop: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().heightIn(min = 162.dp).clip(RoundedCornerShape(22.dp)).background(BannerPeach)) {
        val fadeEnd = with(LocalDensity.current) { maxWidth.toPx() * .68f }
        Image(painterResource(page.image), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        Box(
            Modifier.matchParentSize().background(
                Brush.horizontalGradient(listOf(BannerPeach, BannerPeach.copy(alpha = .94f), Color.Transparent), endX = fadeEnd),
            ),
        )
        Column(Modifier.widthIn(max = 210.dp).padding(start = 20.dp, top = 18.dp, bottom = 17.dp)) {
            Text(page.headline, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 23.sp, lineHeight = 26.sp), color = Ink, maxLines = 2)
            Text(page.accent, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 23.sp, lineHeight = 26.sp), color = Coral, maxLines = 1)
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.clip(CircleShape).background(CtaRed)
                    .clickable(role = Role.Button, onClickLabel = page.cta, onClick = onShop)
                    .padding(horizontal = 15.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(page.cta, color = CardWhite, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(5.dp))
                RushIcon(R.drawable.ic_arrow_right_01, null, Modifier.size(16.dp), CardWhite)
            }
        }
    }
}

private data class ProductSectionKey(val mode: ShoppingMode, val categoryId: String?, val phase: String)

@Composable
private fun ProductSection(
    state: ShopUiState,
    onAdd: (String) -> Unit,
    onWishlist: (String) -> Unit,
    onProduct: (String) -> Unit,
    onRetry: () -> Unit,
    onBrowseAll: () -> Unit,
) {
    val reduced = LocalReducedMotion.current
    val phase = when {
        state.loading -> "loading"
        state.error != null -> "error"
        else -> "content"
    }
    val target = ProductSectionKey(state.selectedMode, state.selectedCategoryId, phase)
    val duration = motionDuration(Motion.CONTENT_TRANSITION, reduced)

    AnimatedContent(
        targetState = target,
        transitionSpec = {
            (fadeIn(tween(duration)) + slideInVertically(tween(duration)) { height -> height / 24 })
                .togetherWith(fadeOut(tween(duration / 2)))
        },
        label = "product section",
    ) { section ->
        when (section.phase) {
            "loading" -> ProductSkeleton()
            "error" -> MessageCard(
                "Couldn't load the catalog",
                state.error.orEmpty(),
                Modifier.padding(horizontal = 16.dp),
                icon = R.drawable.ic_help_circle,
                action = "Try again",
                onAction = onRetry,
            )
            else -> {
                // Derive from the transition's own key so outgoing content never shows new products.
                val items = remember(section.mode, section.categoryId, state.products) {
                    productsForSelection(state.products, section.mode, section.categoryId)
                }
                if (items.isEmpty()) {
                    EmptyProducts(section.mode, section.categoryId, onBrowseAll)
                } else {
                    ProductRows(items, state.cart, state.wishlist, onAdd, onWishlist, onProduct)
                }
            }
        }
    }
}

@Composable
private fun ProductRows(
    products: List<Product>,
    cart: Map<String, Int>,
    wishlist: Set<String>,
    onAdd: (String) -> Unit,
    onWishlist: (String) -> Unit,
    onProduct: (String) -> Unit,
) {
    val rows = remember(products) { products.chunked(2) }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { product ->
                    ProductCard(
                        product = product,
                        quantity = cart[product.id] ?: 0,
                        wished = product.id in wishlist,
                        onAdd = { onAdd(product.id) },
                        onWishlist = { onWishlist(product.id) },
                        onOpen = { onProduct(product.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EmptyProducts(mode: ShoppingMode, categoryId: String?, onBrowseAll: () -> Unit) {
    val message = when {
        categoryId != null -> "No products are configured in this category yet."
        mode == ShoppingMode.GIFTS -> "Gift inventory isn't configured yet. Explore the categories for a preview."
        mode == ShoppingMode.ECOMMERCE -> "No e-commerce listings have been configured yet."
        else -> "Check back later for featured groceries."
    }
    MessageCard(
        title = "Nothing here yet",
        message = message,
        modifier = Modifier.padding(horizontal = 16.dp),
        action = if (categoryId != null) "Browse all" else null,
        onAction = onBrowseAll,
    )
}

@Composable
private fun ProductSkeleton() {
    val reduced = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "skeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "skeleton pulse",
    )
    val alpha = if (reduced) 0.7f else pulse
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(2) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) {
                    Box(
                        Modifier.weight(1f).height(196.dp).clip(RoundedCornerShape(20.dp))
                            .background(Peach.copy(alpha = alpha)),
                    )
                }
            }
        }
    }
}
