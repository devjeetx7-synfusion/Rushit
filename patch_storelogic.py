import re

with open('app/src/main/java/com/rushit/app/domain/StoreLogic.kt', 'r') as f:
    content = f.read()

# Replace updateCart to handle string keys (already handles String)
# Just need to update updateModeCart signature and cartTotals

replacement = """fun cartKey(productId: String, variantId: String?): String = if (variantId != null) "${productId}_${variantId}" else productId

fun parseCartKey(key: String): Pair<String, String?> {
    val parts = key.split("_", limit = 2)
    return parts[0] to parts.getOrNull(1)
}

fun updateCart(cart: Map<String, Int>, key: String, delta: Int, maxQuantity: Int = 99): Map<String, Int> {
    val quantity = ((cart[key] ?: 0).toLong() + delta).coerceIn(0, maxQuantity.coerceAtLeast(0).toLong()).toInt()
    return cart.toMutableMap().apply {
        if (quantity == 0) remove(key) else put(key, quantity)
    }
}

fun updateModeCart(
    carts: Map<ShoppingMode, Map<String, Int>>,
    product: Product,
    variantId: String?,
    delta: Int,
): Map<ShoppingMode, Map<String, Int>> {
    if (delta > 0 && (product.previewOnly || !product.isAvailable || (product.stockQuantity != null && product.stockQuantity <= 0))) return carts
    val maximum = product.stockQuantity?.coerceIn(0, 99) ?: 99
    val key = cartKey(product.id, variantId)
    val updated = updateCart(carts[product.mode].orEmpty(), key, delta, maximum)
    return carts.toMutableMap().apply {
        if (updated.isEmpty()) remove(product.mode) else put(product.mode, updated)
    }
}

data class CartTotals(val itemCount: Int, val totalPaise: Long)

fun cartTotals(cart: Map<String, Int>, products: List<Product>): CartTotals {
    val byId = products.associateBy { it.id }
    var count = 0
    var total = 0L
    cart.forEach { (key, quantity) ->
        val (id, variantId) = parseCartKey(key)
        val product = byId[id]
        if (product != null && quantity > 0) {
            count += quantity
            val variantPrice = product.variants.find { it.id == variantId }?.pricePaise ?: product.pricePaise
            total += variantPrice * quantity
        }
    }
    return CartTotals(count, total)
}"""

content = re.sub(
    r'fun updateCart.*}?',
    replacement,
    content,
    flags=re.DOTALL
)

with open('app/src/main/java/com/rushit/app/domain/StoreLogic.kt', 'w') as f:
    f.write(content)
