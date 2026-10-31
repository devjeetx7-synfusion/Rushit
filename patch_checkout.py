import re

with open('app/src/main/java/com/rushit/app/ui/screens/store/CheckoutTrackingScreens.kt', 'r') as f:
    content = f.read()

# Replace onChangeQuantity: (String, Int) -> Unit
content = content.replace("onChangeQuantity: (String, Int) -> Unit", "onChangeQuantity: (String, String?, Int) -> Unit")

# But wait, CheckoutLine needs to be updated too. Let's replace the whole CheckoutLine function and where it's called.
replacement = """@Composable
private fun CheckoutLine(product: Product, variantId: String?, quantity: Int, onChangeQuantity: (String, String?, Int) -> Unit) {
    val variant = product.variants.find { it.id == variantId }
    val price = variant?.pricePaise ?: product.pricePaise
    val stock = variant?.stockQuantity ?: product.stockQuantity
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(Peach.copy(alpha = .5f)), contentAlignment = Alignment.Center) {
            CatalogImage(product.imageUrl, product.imageRes, product.name, Modifier.size(48.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(product.name, style = MaterialTheme.typography.labelLarge, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (variant != null) {
                Text(variant.attributes.entries.joinToString(", ") { "${it.key}: ${it.value}" }, style = MaterialTheme.typography.bodySmall, color = Muted)
            } else {
                Text(product.unit, style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            Spacer(Modifier.height(2.dp))
            Text(formatPrice(price), style = MaterialTheme.typography.labelLarge, color = Ink)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            QuantityControl(R.drawable.ic_minus_sign, "Remove one ${product.name}") { onChangeQuantity(product.id, variantId, -1) }
            Text("$quantity", style = MaterialTheme.typography.labelMedium, color = Ink, modifier = Modifier.padding(horizontal = 8.dp))
            QuantityControl(
                R.drawable.ic_add_01, "Add one ${product.name}",
                enabled = !product.previewOnly && product.isAvailable && quantity < (stock?.coerceAtMost(99) ?: 99)
            ) { onChangeQuantity(product.id, variantId, 1) }
        }
    }
}"""

content = re.sub(
    r'@Composable\nprivate fun CheckoutLine.*?}(?=\n\n@Composable\nprivate fun QuantityControl)',
    replacement,
    content,
    flags=re.DOTALL
)

# Replace items call for CheckoutLine
# First find where CheckoutLine is called
call_replacement = """val cartItems = remember(shopState.products, shopState.cart) {
        shopState.cart.mapNotNull { (key, quantity) ->
            if (quantity <= 0) return@mapNotNull null
            val (productId, variantId) = com.rushit.app.domain.parseCartKey(key)
            val product = shopState.products.firstOrNull { it.id == productId } ?: return@mapNotNull null
            Triple(product, variantId, quantity)
        }
    }
    items(cartItems, key = { com.rushit.app.domain.cartKey(it.first.id, it.second) }) { (product, variantId, quantity) ->
        CheckoutLine(product, variantId, quantity, onChangeQuantity)
    }"""

content = re.sub(
    r'items\(shopState.products.filter.*?}\) \{ product ->.*?CheckoutLine\(product.*?\}',
    call_replacement,
    content,
    flags=re.DOTALL
)

with open('app/src/main/java/com/rushit/app/ui/screens/store/CheckoutTrackingScreens.kt', 'w') as f:
    f.write(content)
