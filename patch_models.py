import re

with open('app/src/main/java/com/rushit/app/data/model/Models.kt', 'r') as f:
    content = f.read()

# Replace Product
content = re.sub(
    r'val previewOnly: Boolean = false,\n\)',
    r'val previewOnly: Boolean = false,\n    val media: List<ProductMedia> = emptyList(),\n    val options: List<ProductOption> = emptyList(),\n    val variants: List<ProductVariant> = emptyList(),\n    val rating: Double? = null,\n    val reviewCount: Int = 0,\n    val specifications: Map<String, String> = emptyMap(),\n)\n\nenum class MediaType { IMAGE, VIDEO }\n\ndata class ProductMedia(\n    val id: String,\n    val url: String? = null,\n    @DrawableRes val resId: Int? = null,\n    val type: MediaType = MediaType.IMAGE,\n    val variantId: String? = null\n)\n\ndata class ProductOption(val name: String, val values: List<String>)\n\ndata class ProductVariant(\n    val id: String,\n    val attributes: Map<String, String>,\n    val pricePaise: Long? = null,\n    val stockQuantity: Int? = null,\n    val sku: String? = null\n)\n',
    content
)

# Replace CartItem
content = re.sub(
    r'data class CartItem\(val productId: String, val quantity: Int, val unitPricePaise: Long\)',
    r'data class CartItem(val productId: String, val variantId: String? = null, val quantity: Int, val unitPricePaise: Long)',
    content
)

# Replace OrderLine
content = re.sub(
    r'val productId: String,\n    val name: String,',
    r'val productId: String,\n    val variantId: String? = null,\n    val variantName: String? = null,\n    val name: String,',
    content
)

# Replace Order
content = re.sub(
    r'val addressSummary: String\? = null,\n    val isDemo: Boolean = true,',
    r'val addressSummary: String? = null,\n    val paymentMethod: String = "COD",\n    val paymentStatus: String = "Pending",\n    val isDemo: Boolean = true,',
    content
)

# Add Payment, Refund, SellerSettlement before Catalog
content = re.sub(
    r'data class Catalog',
    r'data class Payment(val id: String, val orderId: String, val amountPaise: Long, val method: String, val status: String, val date: String, val demoReference: String? = null)\n\ndata class Refund(val id: String, val orderId: String, val amountPaise: Long, val status: String, val date: String)\n\ndata class SellerSettlement(val id: String, val sellerId: String, val period: String, val grossSalesPaise: Long, val platformCommissionPaise: Long, val refundAdjustmentsPaise: Long, val payoutPaise: Long, val status: String)\n\ndata class Payout(val id: String, val sellerId: String, val amountPaise: Long, val status: String, val date: String)\n\ndata class Catalog',
    content
)

with open('app/src/main/java/com/rushit/app/data/model/Models.kt', 'w') as f:
    f.write(content)
