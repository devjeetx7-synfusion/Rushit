import re

with open('app/src/main/java/com/rushit/app/domain/OrderJourney.kt', 'r') as f:
    content = f.read()

replacement = """fun orderLinesFor(cart: Map<String, Int>, products: List<Product>): List<OrderLine> =
    cart.mapNotNull { (key, quantity) ->
        if (quantity <= 0) return@mapNotNull null
        val (productId, variantId) = parseCartKey(key)
        val product = products.firstOrNull { it.id == productId } ?: return@mapNotNull null
        val variant = product.variants.find { it.id == variantId }
        val price = variant?.pricePaise ?: product.pricePaise
        val variantName = variant?.attributes?.entries?.joinToString(", ") { "${it.key}: ${it.value}" }
        OrderLine(
            productId = product.id,
            variantId = variantId,
            variantName = variantName,
            name = product.name,
            unit = product.unit,
            quantity = quantity,
            unitPricePaise = price,
            imageRes = product.imageRes,
            imageUrl = product.imageUrl,
        )
    }"""

content = re.sub(
    r'fun orderLinesFor\(cart: Map<String, Int>, products: List<Product>\): List<OrderLine> =\n.*?\}',
    replacement,
    content,
    flags=re.DOTALL
)

with open('app/src/main/java/com/rushit/app/domain/OrderJourney.kt', 'w') as f:
    f.write(content)
