package com.rushit.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.rushit.app.R
import com.rushit.app.data.model.Category
import com.rushit.app.data.model.Product
import com.rushit.app.domain.formatPrice
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.CtaRed
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.Muted
import com.rushit.app.ui.theme.Peach
import com.rushit.app.ui.theme.SoftRed

@Composable
fun CatalogImage(url: String?, localImage: Int?, description: String?, modifier: Modifier = Modifier) {
    val placeholder = painterResource(R.drawable.ic_package_01)
    if (url.isNullOrBlank()) {
        // Coil resolves the requested display size and caches decoded local assets off the UI thread.
        AsyncImage(model = localImage ?: R.drawable.ic_package_01, contentDescription = description,
            placeholder = placeholder, error = placeholder, contentScale = ContentScale.Fit, modifier = modifier)
    } else {
        val fallback = if (localImage != null) rememberAsyncImagePainter(localImage) else placeholder
        AsyncImage(model = url, contentDescription = description, placeholder = fallback, error = fallback, contentScale = ContentScale.Fit, modifier = modifier)
    }
}

@Composable
fun CategoryItem(category: Category, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.width(68.dp).clip(RoundedCornerShape(14.dp)).clickable(role = Role.Button, onClickLabel = "Browse ${category.name}", onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(60.dp).clip(CircleShape).background(Peach), contentAlignment = Alignment.Center) {
            CatalogImage(category.imageUrl, category.imageRes, null, Modifier.size(51.dp))
        }
        Spacer(Modifier.height(7.dp))
        Text(category.name, style = MaterialTheme.typography.labelSmall, color = Ink, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp)
    }
}

@Composable
fun ProductCard(
    product: Product,
    quantity: Int,
    wished: Boolean,
    onAdd: () -> Unit,
    onWishlist: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = Dp.Unspecified,
) {
    val priceText = remember(product.pricePaise) { formatPrice(product.pricePaise) }
    val originalPriceText = remember(product.originalPricePaise) { product.originalPricePaise?.let(::formatPrice) }
    Surface(
        modifier = (if (width == Dp.Unspecified) modifier.fillMaxWidth() else modifier.width(width))
            .heightIn(min = 200.dp).clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClickLabel = "View ${product.name} details", onClick = onOpen),
        shape = RoundedCornerShape(18.dp), color = CardWhite,
        shadowElevation = 2.dp, border = BorderStroke(1.dp, Border.copy(alpha = 0.55f)),
    ) {
        Column(Modifier.padding(start = 9.dp, end = 5.dp, top = 5.dp, bottom = 5.dp)) {
            Box(Modifier.fillMaxWidth().height(104.dp)) {
                CatalogImage(product.imageUrl, product.imageRes, product.name, Modifier.fillMaxSize().padding(top = 9.dp, end = 8.dp, bottom = 4.dp))
                val wishlistDescription = if (wished) "Remove ${product.name} from wishlist" else "Add ${product.name} to wishlist"
                Box(
                    Modifier.align(Alignment.TopEnd).size(44.dp).clip(CircleShape)
                        .clickable(role = Role.Button, onClickLabel = wishlistDescription, onClick = onWishlist),
                    contentAlignment = Alignment.Center,
                ) {
                    RushIcon(if (wished) R.drawable.ic_heart_filled else R.drawable.ic_heart, wishlistDescription, Modifier.size(23.dp), if (wished) Coral else Ink)
                }
                val original = product.originalPricePaise
                if (original != null && original > product.pricePaise && original > 0) {
                    val percent = ((original - product.pricePaise) * 100 / original).toInt()
                    Text("$percent% OFF", modifier = Modifier.align(Alignment.TopStart).clip(RoundedCornerShape(6.dp)).background(CtaRed).padding(horizontal = 5.dp, vertical = 3.dp), color = CardWhite, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(product.name, style = MaterialTheme.typography.labelMedium, color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(product.unit, style = MaterialTheme.typography.bodySmall, color = Muted, maxLines = 1)
            if (product.previewOnly || !product.isAvailable || (product.stockQuantity != null && product.stockQuantity <= 0)) {
                Text(if (product.previewOnly) "Preview only" else "Unavailable", color = Muted, style = MaterialTheme.typography.labelSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(priceText, style = MaterialTheme.typography.labelLarge, color = Ink, maxLines = 1)
                    if (product.originalPricePaise != null && product.originalPricePaise > product.pricePaise && originalPriceText != null) {
                        Text(originalPriceText, style = MaterialTheme.typography.bodySmall, color = Muted, textDecoration = TextDecoration.LineThrough)
                    }
                }
                if (!product.previewOnly && product.isAvailable && (product.stockQuantity == null || product.stockQuantity > 0)) {
                    val canAddMore = quantity < (product.stockQuantity?.coerceAtMost(99) ?: 99)
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = canAddMore, role = Role.Button, onClickLabel = "Add ${product.name} to cart", onClick = onAdd)
                            .semantics { contentDescription = if (canAddMore) "Add ${product.name} to cart${if (quantity > 0) ", $quantity in cart" else ""}" else "Maximum quantity of ${product.name} in cart" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(SoftRed), contentAlignment = Alignment.Center) {
                            RushIcon(R.drawable.ic_add_01, null, Modifier.size(20.dp), if (canAddMore) Coral else Muted)
                        }
                        if (quantity > 0) {
                            Box(Modifier.align(Alignment.TopEnd).size(18.dp).clip(CircleShape).background(Coral), contentAlignment = Alignment.Center) {
                                Text(if (quantity > 9) "9+" else "$quantity", color = CardWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
