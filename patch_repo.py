import re

with open('app/src/main/java/com/rushit/app/data/repository/CatalogRepository.kt', 'r') as f:
    content = f.read()

# Make demo_headphones purchasable and add options and variants
headphones_replacement = """Product("demo_headphones", "Wireless Headphones", "High quality noise-cancelling wireless headphones.", "e_electronics", imageRes = R.drawable.ecommerce_headphones, pricePaise = 129_900, unit = "1 pair", isAvailable = true, mode = ShoppingMode.ECOMMERCE, previewOnly = false, 
                    media = listOf(
                        com.rushit.app.data.model.ProductMedia("m1", resId = R.drawable.ecommerce_headphones),
                        com.rushit.app.data.model.ProductMedia("m2", resId = R.drawable.ic_launcher), # dummy
                        com.rushit.app.data.model.ProductMedia("m3", type = com.rushit.app.data.model.MediaType.VIDEO)
                    ),
                    options = listOf(
                        com.rushit.app.data.model.ProductOption("Color", listOf("Black", "White")),
                        com.rushit.app.data.model.ProductOption("Size", listOf("Standard", "Large"))
                    ),
                    variants = listOf(
                        com.rushit.app.data.model.ProductVariant("v1", mapOf("Color" to "Black", "Size" to "Standard"), stockQuantity = 10),
                        com.rushit.app.data.model.ProductVariant("v2", mapOf("Color" to "Black", "Size" to "Large"), stockQuantity = 0),
                        com.rushit.app.data.model.ProductVariant("v3", mapOf("Color" to "White", "Size" to "Standard"), stockQuantity = 5, pricePaise = 139_900)
                    ),
                    rating = 4.5,
                    reviewCount = 128,
                    specifications = mapOf("Battery Life" to "30 hours", "Bluetooth" to "5.2", "Weight" to "250g")
                )"""

content = re.sub(
    r'Product\("demo_headphones",.*?\)',
    headphones_replacement,
    content,
    flags=re.DOTALL
)

with open('app/src/main/java/com/rushit/app/data/repository/CatalogRepository.kt', 'w') as f:
    f.write(content)
