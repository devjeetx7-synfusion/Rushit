package com.rushit.app.ui.screens.store

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rushit.app.R
import com.rushit.app.data.model.Product
import com.rushit.app.domain.formatPrice
import com.rushit.app.ui.components.CatalogImage
import com.rushit.app.ui.components.MessageCard
import com.rushit.app.ui.components.RushIcon
import com.rushit.app.ui.components.ScreenHeader
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.Cream
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.Muted
import com.rushit.app.ui.theme.Peach
import com.rushit.app.ui.theme.SoftRed
import com.rushit.app.ui.viewmodel.ShopUiState

@Composable
fun CartScreen(state: ShopUiState, onBack: () -> Unit, onChangeQuantity: (String, Int) -> Unit) {
    val products = state.products.filter { (state.cart[it.id] ?: 0) > 0 }
    Column(Modifier.fillMaxSize().background(Cream)) {
        ScreenHeader("Your cart", onBack)
        if (products.isEmpty()) {
            MessageCard("Your cart is empty", "Add a few favorites to see them here.", Modifier.padding(16.dp), action = "Continue shopping", onAction = onBack)
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.isSample) item(key = "sample_note") {
                    Text("Sample cart · Prices are illustrative", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 4.dp))
                }
                items(products, key = { it.id }) { product ->
                    CartLine(product, state.cart[product.id] ?: 0, onMinus = { onChangeQuantity(product.id, -1) }, onPlus = { onChangeQuantity(product.id, 1) })
                }
                item(key = "totals") {
                    Surface(shape = RoundedCornerShape(18.dp), color = CardWhite, border = BorderStroke(1.dp, Border)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subtotal", style = MaterialTheme.typography.bodyLarge, color = Muted)
                                Text(formatPrice(state.totals.totalPaise), style = MaterialTheme.typography.titleMedium, color = Ink)
                            }
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = Border)
                            Spacer(Modifier.height(10.dp))
                            Text("Checkout and delivery fees aren't available in this sample store.", color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartLine(product: Product, quantity: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = CardWhite, border = BorderStroke(1.dp, Border.copy(alpha = .7f))) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)).background(Peach.copy(alpha = .5f)), contentAlignment = Alignment.Center) {
                CatalogImage(product.imageUrl, product.imageRes, product.name, Modifier.size(64.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.labelLarge, color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(product.unit, style = MaterialTheme.typography.bodySmall, color = Muted)
                Spacer(Modifier.height(4.dp))
                Text(formatPrice(product.pricePaise * quantity), style = MaterialTheme.typography.labelLarge, color = Ink)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QuantityButton(R.drawable.ic_minus_sign, "Remove one ${product.name}", onMinus)
                    Text("$quantity", style = MaterialTheme.typography.labelMedium, color = Ink)
                    QuantityButton(R.drawable.ic_add_01, "Add one ${product.name}", onPlus)
                }
            }
        }
    }
}

@Composable
private fun QuantityButton(icon: Int, description: String, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = description, onClick = onClick), contentAlignment = Alignment.Center) {
        Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(SoftRed), contentAlignment = Alignment.Center) {
            RushIcon(icon, description, Modifier.size(18.dp), Coral)
        }
    }
}
