import re

with open('app/src/main/java/com/rushit/app/ui/viewmodel/ShopViewModel.kt', 'r') as f:
    content = f.read()

# quantityFor(product: Product)
content = re.sub(
    r'fun quantityFor\(product: Product\): Int = carts\[product.mode\]\?\.get\(product.id\) \?: 0',
    r'fun quantityFor(product: Product, variantId: String? = null): Int = carts[product.mode]?.get(com.rushit.app.domain.cartKey(product.id, variantId)) ?: 0',
    content
)

# changeQuantity(productId: String, delta: Int)
content = re.sub(
    r'fun changeQuantity\(productId: String, delta: Int\) \{',
    r'fun changeQuantity(productId: String, variantId: String? = null, delta: Int) {',
    content
)
content = re.sub(
    r'updateModeCart\(it.carts, product, delta\)',
    r'updateModeCart(it.carts, product, variantId, delta)',
    content
)

with open('app/src/main/java/com/rushit/app/ui/viewmodel/ShopViewModel.kt', 'w') as f:
    f.write(content)
