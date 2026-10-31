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
    onProduct: (String) -> Unit,
    onFullDetails: () -> Unit,
    onReviews: () -> Unit,
    onComments: () -> Unit,
    onHelpfulReview: (String) -> Unit,
    onHelpfulComment: (String) -> Unit,
    onHelpfulQuestion: (String) -> Unit,
    onSubmitReview: (Review) -> Unit,
    onSubmitComment: (Comment) -> Unit,
    onSubmitQuestion: (Question) -> Unit,
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
                SwipeableProductMediaGallery(product.media.ifEmpty { listOf(com.rushit.app.data.model.ProductMedia("1", product.imageUrl, product.imageRes)) }, product.name)

                Spacer(Modifier.height(18.dp))
                if (product.brand != null) {
                    Text(product.brand.uppercase(), style = MaterialTheme.typography.labelSmall, color = Muted, modifier = Modifier.padding(bottom = 4.dp))
                }
                Text(product.name, style = MaterialTheme.typography.headlineMedium, color = Ink)
                if (product.rating != null) {
                    Text("⭐ ${product.rating} (${product.reviewCount} reviews)", style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.padding(top = 4.dp))
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                    Text(formatPrice(currentPrice), style = MaterialTheme.typography.headlineMedium, color = Ink)
                    if (currentOriginalPrice != null && currentOriginalPrice > currentPrice) {
                        val savings = currentOriginalPrice - currentPrice
                        val discountPercent = (savings * 100) / currentOriginalPrice
                        Text(
                            formatPrice(currentOriginalPrice),
                            style = MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.LineThrough),
                            color = Muted,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                        Surface(shape = RoundedCornerShape(8.dp), color = Coral.copy(alpha = 0.1f), modifier = Modifier.padding(start = 8.dp)) {
                            Text("$discountPercent% OFF", color = Coral, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
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
                Text(product.description.take(150) + if(product.description.length > 150) "..." else "", style = MaterialTheme.typography.bodyMedium, color = Muted)
                
                if (product.keyFeatures.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    product.keyFeatures.take(2).forEach { feature ->
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.Top) {
                            Text("•", style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.padding(end = 8.dp))
                            Text(feature, style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.weight(1f))
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth().clickable { onFullDetails() }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("View Full Product Details", style = MaterialTheme.typography.labelLarge, color = Coral, modifier = Modifier.weight(1f))
                    Text("→", style = MaterialTheme.typography.labelLarge, color = Coral)
                }
                
                HorizontalDivider(color = Border)

                ProductOffers(product.availableOffers)
                PincodeChecker()
                
                ProductReviewsSection(state.reviews(product.id).take(2), onHelpful = onHelpfulReview, onSubmitReview = { r -> onSubmitReview(r.copy(productId = product.id)) })
                Row(Modifier.fillMaxWidth().clickable { onReviews() }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("View All Reviews", style = MaterialTheme.typography.labelLarge, color = Coral, modifier = Modifier.weight(1f))
                    Text("→", style = MaterialTheme.typography.labelLarge, color = Coral)
                }

                ProductQASection(state.comments(product.id).take(2), state.questions(product.id).take(2), onHelpfulComment = onHelpfulComment, onHelpfulQuestion = onHelpfulQuestion, onSubmitComment = { c -> onSubmitComment(c.copy(productId = product.id)) }, onSubmitQuestion = { q -> onSubmitQuestion(q.copy(productId = product.id)) })
                Row(Modifier.fillMaxWidth().clickable { onComments() }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("View All Comments & Q&A", style = MaterialTheme.typography.labelLarge, color = Coral, modifier = Modifier.weight(1f))
                    Text("→", style = MaterialTheme.typography.labelLarge, color = Coral)
                }
                
                FrequentlyBoughtTogether(product, state.recommendations(product.id), onAddToCart = { id -> onChangeQuantity(id, null, 1) })
                ProductRecommendations("Similar Products", state.recommendations(product.id), onProduct = onProduct)
                ProductRecommendations("Recently Viewed", state.recentlyViewedProducts(product.id), onProduct = onProduct)

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
