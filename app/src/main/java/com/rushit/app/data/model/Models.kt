package com.rushit.app.data.model

import androidx.annotation.DrawableRes

enum class ShoppingMode(val label: String) {
    GROCERY("Food & Grocery"),
    ECOMMERCE("E-commerce"),
    GIFTS("Gifts & More"),
}

/** Prices are stored as integer paise so totals never accumulate floating-point errors. */
data class Product(
    val id: String,
    val name: String,
    val description: String,
    val categoryId: String,
    val imageUrl: String? = null,
    @DrawableRes val imageRes: Int? = null,
    val pricePaise: Long,
    val originalPricePaise: Long? = null,
    val unit: String,
    val isAvailable: Boolean = true,
    val stockQuantity: Int? = null,
    val isBestseller: Boolean = false,
    val mode: ShoppingMode = ShoppingMode.GROCERY,
    // Preview listings are never purchasable, even if another flag is misconfigured.
    val previewOnly: Boolean = false,
    val media: List<ProductMedia> = emptyList(),
    val options: List<ProductOption> = emptyList(),
    val variants: List<ProductVariant> = emptyList(),
    val rating: Double? = null,
    val reviewCount: Int = 0,
    val specifications: Map<String, String> = emptyMap(),
)

enum class MediaType { IMAGE, VIDEO }

data class ProductMedia(
    val id: String,
    val url: String? = null,
    @DrawableRes val resId: Int? = null,
    val type: MediaType = MediaType.IMAGE,
    val variantId: String? = null
)

data class ProductOption(val name: String, val values: List<String>)

data class ProductVariant(
    val id: String,
    val attributes: Map<String, String>,
    val pricePaise: Long? = null,
    val stockQuantity: Int? = null,
    val sku: String? = null
)


data class Category(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    @DrawableRes val imageRes: Int? = null,
    val sortOrder: Int,
    val mode: ShoppingMode = ShoppingMode.GROCERY,
)

/** A manually entered address stored only on this device. */
data class SavedAddress(
    val id: String,
    val type: String,
    val line: String,
    val city: String,
    val postalCode: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    val summary: String get() = listOf(line, city, postalCode).filter { it.isNotBlank() }.joinToString(", ")
}

data class CartItem(val productId: String, val variantId: String? = null, val quantity: Int, val unitPricePaise: Long)

// These models describe the intended boundaries; no remote schema is assumed or queried yet.
data class Address(
    val id: String,
    val userId: String,
    val recipientName: String,
    val phoneNumber: String,
    val addressLine: String,
    val locality: String,
    val city: String,
    val state: String,
    val postalCode: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

/** A purchased line, snapshotted so an order keeps its prices if the catalog changes. */
data class OrderLine(
    val productId: String,
    val variantId: String? = null,
    val variantName: String? = null,
    val name: String,
    val unit: String,
    val quantity: Int,
    val unitPricePaise: Long,
    @DrawableRes val imageRes: Int? = null,
    val imageUrl: String? = null,
) {
    val lineTotalPaise: Long get() = unitPricePaise * quantity
}

/**
 * A locally stored order. [isDemo] marks orders created by the sample store, which is the only
 * order source today: they are real records on this device, but no payment or fulfilment happens.
 */
data class Order(
    val id: String,
    val userId: String = "local",
    val status: OrderStatus = OrderStatus.CONFIRMED,
    val items: List<OrderLine> = emptyList(),
    val subtotalPaise: Long,
    val deliveryFeePaise: Long = 0,
    val platformFeePaise: Long = 0,
    val discountPaise: Long = 0,
    val totalPaise: Long,
    val createdAt: String,
    val updatedAt: String = createdAt,
    val mode: ShoppingMode = ShoppingMode.GROCERY,
    val addressSummary: String? = null,
    val paymentMethod: String = "COD",
    val paymentStatus: String = "Pending",
    val isDemo: Boolean = true,
    /** Set once the celebration has been shown, so reopening never replays it. */
    val celebratedAt: String? = null,
) {
    val itemCount: Int get() = items.sumOf { it.quantity }
}

data class Payment(val id: String, val orderId: String, val amountPaise: Long, val method: String, val status: String, val date: String, val demoReference: String? = null)

data class Refund(val id: String, val orderId: String, val amountPaise: Long, val status: String, val date: String)

data class SellerSettlement(val id: String, val sellerId: String, val period: String, val grossSalesPaise: Long, val platformCommissionPaise: Long, val refundAdjustmentsPaise: Long, val payoutPaise: Long, val status: String)

data class Payout(val id: String, val sellerId: String, val amountPaise: Long, val status: String, val date: String)

data class Catalog(val categories: List<Category>, val products: List<Product>, val isSample: Boolean)
