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

data class CartItem(val productId: String, val quantity: Int, val unitPricePaise: Long)

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
    val isDemo: Boolean = true,
    /** Set once the celebration has been shown, so reopening never replays it. */
    val celebratedAt: String? = null,
) {
    val itemCount: Int get() = items.sumOf { it.quantity }
}

data class Catalog(val categories: List<Category>, val products: List<Product>, val isSample: Boolean)
