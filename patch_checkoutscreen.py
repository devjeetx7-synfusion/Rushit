import re

with open('app/src/main/java/com/rushit/app/ui/screens/store/CheckoutTrackingScreens.kt', 'r') as f:
    content = f.read()

replacement = """val cartItems = remember(state.products, state.cart) {
        state.cart.mapNotNull { (key, quantity) ->
            if (quantity <= 0) return@mapNotNull null
            val (productId, variantId) = com.rushit.app.domain.parseCartKey(key)
            val product = state.products.firstOrNull { it.id == productId } ?: return@mapNotNull null
            Triple(product, variantId, quantity)
        }
    }
    val itemsPaise = state.totals.totalPaise"""

content = re.sub(
    r'val products = remember\(state.products, state.cart\) \{ state.products.filter \{ \(state.cart\[it.id\] \?: 0\) > 0 \} \}\n    val itemsPaise = state.totals.totalPaise',
    replacement,
    content
)

# Replace products.isEmpty() with cartItems.isEmpty()
content = content.replace("if (products.isEmpty()) {", "if (cartItems.isEmpty()) {")

# Replace products.size with cartItems.size
content = content.replace("${products.size}", "${cartItems.size}")
content = content.replace("products.size == 1", "cartItems.size == 1")

# Replace products.isNotEmpty() with cartItems.isNotEmpty()
content = content.replace("products.isNotEmpty()", "cartItems.isNotEmpty()")

# Ensure the loop uses cartItems
loop_replacement = """cartItems.forEach { (product, variantId, quantity) ->
                            CheckoutLine(
                                product = product,
                                variantId = variantId,
                                quantity = quantity,
                                onChangeQuantity = onChangeQuantity,
                            )
                        }"""

content = re.sub(
    r'products.forEach \{ product ->\n                            CheckoutLine\(\n                                product = product,\n                                quantity = state.cart\[product.id\] \?: 0,\n                                onChangeQuantity = onChangeQuantity,\n                            \)\n                        \}',
    loop_replacement,
    content
)

with open('app/src/main/java/com/rushit/app/ui/screens/store/CheckoutTrackingScreens.kt', 'w') as f:
    f.write(content)
