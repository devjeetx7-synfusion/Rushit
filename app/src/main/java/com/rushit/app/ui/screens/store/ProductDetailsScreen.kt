package com.rushit.app.ui.screens.store

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rushit.app.R
import com.rushit.app.domain.formatPrice
import com.rushit.app.ui.components.CatalogImage
import com.rushit.app.ui.components.MessageCard
import com.rushit.app.ui.components.RoundIconButton
import com.rushit.app.ui.components.RushButton
import com.rushit.app.ui.components.ScreenHeader
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.Cream
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.Muted
import com.rushit.app.ui.theme.Peach
import com.rushit.app.ui.viewmodel.ShopUiState

@Composable
fun ProductDetailsScreen(
    state: ShopUiState,
    productId: String,
    onBack: () -> Unit,
    onChangeQuantity: (String, Int) -> Unit,
    onWishlist: (String) -> Unit,
    onCart: () -> Unit,
    onRetry: () -> Unit,
) {
    val product = remember(state.products, productId) { state.products.firstOrNull { it.id == productId } }
    Column(Modifier.fillMaxSize().background(Cream)) {
        ScreenHeader("Product details", onBack)
        if (state.loading) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Coral) }
        } else if (state.error != null) {
            MessageCard("Couldn't load the product", state.error, Modifier.padding(16.dp), action = "Try again", onAction = onRetry)
        } else if (product == null) {
            MessageCard("Product not found", "This listing may no longer be available.", Modifier.padding(16.dp))
        } else {
            val quantity = state.quantityFor(product)
            val canAdd = !product.previewOnly && product.isAvailable && (product.stockQuantity == null || product.stockQuantity > 0)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Box(Modifier.fillMaxWidth().height(250.dp).background(Peach, RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
                    CatalogImage(product.imageUrl, product.imageRes, product.name, Modifier.size(220.dp))
                    RoundIconButton(if (product.id in state.wishlist) R.drawable.ic_heart_filled else R.drawable.ic_heart,
                        if (product.id in state.wishlist) "Remove ${product.name} from wishlist" else "Save ${product.name} to wishlist",
                        { onWishlist(product.id) }, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp), background = CardWhite, tint = Coral)
                }
                Spacer(Modifier.height(18.dp))
                Text(product.name, style = MaterialTheme.typography.headlineMedium, color = Ink)
                Text(product.unit, style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.padding(top = 4.dp))
                Text(formatPrice(product.pricePaise), style = MaterialTheme.typography.headlineMedium, color = Ink, modifier = Modifier.padding(top = 10.dp))
                if (state.isSample || product.previewOnly) {
                    Text("Example price · This is not a live listing", style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.padding(top = 4.dp))
                }
                Text("About this product", style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.padding(top = 22.dp, bottom = 6.dp))
                Text(product.description.ifBlank { "Product information is not configured yet." }, style = MaterialTheme.typography.bodyMedium, color = Muted)
                Text("Delivery & availability", style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.padding(top = 22.dp, bottom = 6.dp))
                Text(if (product.previewOnly) "This example is for preview only and cannot be added to a cart." else "Delivery and live stock information aren't connected yet.",
                    style = MaterialTheme.typography.bodyMedium, color = Muted)
                Spacer(Modifier.height(24.dp))
            }
            Surface(color = CardWhite, shadowElevation = 8.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!canAdd) {
                        Text(if (product.previewOnly) "Preview only · Not available to order" else "Currently unavailable", style = MaterialTheme.typography.bodyMedium, color = Muted)
                    } else if (quantity == 0) {
                        RushButton("Add to cart", { onChangeQuantity(product.id, 1) })
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RoundIconButton(R.drawable.ic_minus_sign, "Remove one ${product.name}", { onChangeQuantity(product.id, -1) }, background = Peach, tint = Coral)
                            Text(quantity.toString(), style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.padding(horizontal = 10.dp))
                            RoundIconButton(R.drawable.ic_add_01, "Add one ${product.name}", { onChangeQuantity(product.id, 1) }, background = Peach, tint = Coral,
                                enabled = quantity < (product.stockQuantity?.coerceAtMost(99) ?: 99))
                        }
                        RushButton("View cart", onCart, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
