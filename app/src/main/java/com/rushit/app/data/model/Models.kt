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

data class Order(
    val id: String,
    val userId: String,
    val status: String,
    val subtotalPaise: Long,
    val deliveryFeePaise: Long,
    val discountPaise: Long,
    val totalPaise: Long,
    val createdAt: String,
)

data class Catalog(val categories: List<Category>, val products: List<Product>, val isSample: Boolean)
