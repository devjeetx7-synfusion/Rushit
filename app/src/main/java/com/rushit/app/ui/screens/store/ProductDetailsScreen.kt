package com.rushit.app.ui.screens.store

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.rushit.app.R
import com.rushit.app.domain.formatPrice
import com.rushit.app.data.model.MediaType
import com.rushit.app.ui.components.*
import com.rushit.app.ui.theme.*
import com.rushit.app.ui.viewmodel.ShopUiState

@Composable
fun ProductDetailsScreen(
    state: ShopUiState,
    productId: String,
    onBack: () -> Unit,
    onChangeQuantity: (String, String?, Int) -> Unit,
    onWishlist: (String) -> Unit,
    onCart: () -> Unit,
    onCheckout: () -> Unit,
    onRetry: () -> Unit,
) {
    val product = remember(state.products, productId) { state.products.firstOrNull { it.id == productId } }
    var selectedVariantId by remember { mutableStateOf<String?>(null) }
    var selectedMediaId by remember { mutableStateOf<String?>(null) }
    var selectedOptions by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Initialize defaults
    LaunchedEffect(product) {
        if (product != null) {
            val validVariant = product.variants.firstOrNull { it.stockQuantity == null || it.stockQuantity > 0 } ?: product.variants.firstOrNull()
            if (validVariant != null) {
                selectedVariantId = validVariant.id
                selectedOptions = validVariant.attributes
            }
            selectedMediaId = product.media.firstOrNull()?.id
        }
    }

    Column(Modifier.fillMaxSize().background(Cream)) {
        ScreenHeader("Product details", onBack)
        if (state.loading) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Coral) }
        } else if (state.error != null) {
            MessageCard("Couldn't load the product", state.error, Modifier.padding(16.dp), action = "Try again", onAction = onRetry)
        } else if (product == null) {
            MessageCard("Product not found", "This listing may no longer be available.", Modifier.padding(16.dp))
        } else {
            val variant = product.variants.find { it.id == selectedVariantId }
            val currentPrice = variant?.pricePaise ?: product.pricePaise
            val currentOriginalPrice = product.originalPricePaise
            val currentStock = variant?.stockQuantity ?: product.stockQuantity
            val isAvailable = !product.previewOnly && product.isAvailable && (currentStock == null || currentStock > 0)
            val quantity = state.quantityFor(product, selectedVariantId)
            
            // Media filter
            val variantMedia = product.media.filter { it.variantId == null || it.variantId == selectedVariantId }
            val mainMedia = variantMedia.find { it.id == selectedMediaId } ?: variantMedia.firstOrNull()

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                // Main Media View
                Box(Modifier.fillMaxWidth().height(250.dp).background(Peach, RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
                    if (mainMedia != null) {
                        if (mainMedia.type == MediaType.VIDEO) {
                            Text("Video Preview (Demo)", style = MaterialTheme.typography.titleLarge, color = Coral)
                        } else {
                            CatalogImage(mainMedia.url, mainMedia.resId ?: product.imageRes, product.name, Modifier.size(220.dp))
                        }
                    } else {
                        CatalogImage(product.imageUrl, product.imageRes, product.name, Modifier.size(220.dp))
                    }
                    RoundIconButton(if (product.id in state.wishlist) R.drawable.ic_heart_filled else R.drawable.ic_heart,
                        if (product.id in state.wishlist) "Remove ${product.name} from wishlist" else "Save ${product.name} to wishlist",
                        { onWishlist(product.id) }, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp), background = CardWhite, tint = Coral)
                }
                
                // Media Thumbnails
                if (variantMedia.size > 1) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(variantMedia) { media ->
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Peach)
                                    .border(2.dp, if (media.id == selectedMediaId || (selectedMediaId == null && media == variantMedia.first())) Coral else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { selectedMediaId = media.id },
                                contentAlignment = Alignment.Center
                            ) {
                                if (media.type == MediaType.VIDEO) {
                                    Text("▶", style = MaterialTheme.typography.bodyLarge, color = Coral)
                                } else {
                                    CatalogImage(media.url, media.resId, "Thumbnail", Modifier.size(50.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text(product.name, style = MaterialTheme.typography.headlineMedium, color = Ink)
                if (product.rating != null) {
                    Text("⭐ ${product.rating} (${product.reviewCount} reviews)", style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.padding(top = 4.dp))
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                    Text(formatPrice(currentPrice), style = MaterialTheme.typography.headlineMedium, color = Ink)
                    if (currentOriginalPrice != null && currentOriginalPrice > currentPrice) {
                        Text(
                            formatPrice(currentOriginalPrice),
                            style = MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.LineThrough),
                            color = Muted,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                // Options and Variants
                if (product.options.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    product.options.forEach { option ->
                        Text(option.name, style = MaterialTheme.typography.titleSmall, color = Ink, modifier = Modifier.padding(bottom = 8.dp))
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            option.values.forEach { value ->
                                val isSelected = selectedOptions[option.name] == value
                                val newOptions = selectedOptions.toMutableMap().apply { put(option.name, value) }
                                val matchingVariant = product.variants.find { v -> v.attributes.all { attr -> newOptions[attr.key] == attr.value || newOptions[attr.key] == null } }
                                val isAvailableOption = matchingVariant != null && (matchingVariant.stockQuantity == null || matchingVariant.stockQuantity > 0)
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Coral else if (!isAvailableOption) CardWhite else Peach)
                                        .border(1.dp, if (isSelected) Coral else Muted.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .clickable(enabled = isAvailableOption || isSelected) {
                                            if (!isSelected && matchingVariant != null) {
                                                selectedVariantId = matchingVariant.id
                                                selectedOptions = matchingVariant.attributes
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = value,
                                        color = if (isSelected) Color.White else if (!isAvailableOption) Muted.copy(alpha = 0.5f) else Ink,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }

                Text("About this product", style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                Text(product.description.ifBlank { "Product information is not configured yet." }, style = MaterialTheme.typography.bodyMedium, color = Muted)
                
                if (product.specifications.isNotEmpty()) {
                    Text("Specifications", style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                    product.specifications.forEach { (key, value) ->
                        Row(modifier = Modifier.padding(bottom = 4.dp)) {
                            Text("$key: ", style = MaterialTheme.typography.bodyMedium, color = Ink)
                            Text(value, style = MaterialTheme.typography.bodyMedium, color = Muted)
                        }
                    }
                }
                
                Text("Delivery & availability", style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                Text(if (isAvailable) "In Stock. Delivery in 3-5 days." else "Currently out of stock.", style = MaterialTheme.typography.bodyMedium, color = Muted)
                
                Spacer(Modifier.height(24.dp))
            }
            Surface(color = CardWhite, shadowElevation = 8.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!isAvailable) {
                        Text("Currently unavailable", style = MaterialTheme.typography.bodyMedium, color = Muted)
                    } else if (quantity == 0) {
                        RushButton("Add to cart", { onChangeQuantity(product.id, selectedVariantId, 1) }, modifier = Modifier.weight(1f))
                        RushButton("Buy Now", {
                            onChangeQuantity(product.id, selectedVariantId, 1)
                            onCheckout()
                        }, modifier = Modifier.weight(1f))
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RoundIconButton(R.drawable.ic_minus_sign, "Remove one ${product.name}", { onChangeQuantity(product.id, selectedVariantId, -1) }, background = Peach, tint = Coral)
                            Text(quantity.toString(), style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.padding(horizontal = 10.dp))
                            RoundIconButton(R.drawable.ic_add_01, "Add one ${product.name}", { onChangeQuantity(product.id, selectedVariantId, 1) }, background = Peach, tint = Coral,
                                enabled = quantity < (currentStock?.coerceAtMost(99) ?: 99))
                        }
                        RushButton("View cart", onCart, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
