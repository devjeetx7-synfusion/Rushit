package com.rushit.app.domain

import com.rushit.app.data.model.Category
import com.rushit.app.data.model.Product
import com.rushit.app.data.model.ShoppingMode
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

fun isValidIndianMobile(value: String): Boolean =
    value.length == 10 && value.all { it in '0'..'9' } && value.first() in '6'..'9'

fun maskedIndianMobile(value: String): String =
    if (value.length == 10) "+91 ••••••${value.takeLast(4)}" else "your mobile number"

fun isValidDeliveryAddress(line: String, city: String, postalCode: String): Boolean =
    line.trim().length >= 3 && city.trim().length >= 2 &&
        postalCode.length == 6 && postalCode.first() in '1'..'9' && postalCode.all { it in '0'..'9' }

fun recentSearches(previous: List<String>, query: String): List<String> {
    val clean = query.replace('\n', ' ').replace('\r', ' ').trim().take(80)
    if (clean.isBlank()) return previous
    return (listOf(clean) + previous.filterNot { it.equals(clean, ignoreCase = true) }).take(6)
}

fun categoriesForMode(categories: List<Category>, mode: ShoppingMode): List<Category> =
    categories.filter { it.mode == mode }

fun productsForMode(products: List<Product>, mode: ShoppingMode): List<Product> =
    products.filter { it.mode == mode }

/**
 * A category is only selectable inside its own mode. Anything else falls back to "All", so switching
 * shopping mode can never leave an incompatible category highlighted.
 */
fun resolveCategorySelection(categories: List<Category>, mode: ShoppingMode, selectedId: String?): String? =
    selectedId?.takeIf { id -> categories.any { it.mode == mode && it.id == id } }

/** Products for the active mode, narrowed to one category when one is selected (null means all). */
fun productsForSelection(products: List<Product>, mode: ShoppingMode, categoryId: String?): List<Product> =
    products.filter { it.mode == mode && (categoryId == null || it.categoryId == categoryId) }

/** A single map entry per product prevents duplicate cart lines. */
fun updateCart(cart: Map<String, Int>, productId: String, delta: Int, maxQuantity: Int = 99): Map<String, Int> {
    val quantity = ((cart[productId] ?: 0).toLong() + delta).coerceIn(0, maxQuantity.coerceAtLeast(0).toLong()).toInt()
    return cart.toMutableMap().apply {
        if (quantity == 0) remove(productId) else put(productId, quantity)
    }
}

/** Each shopping mode has its own cart; preview listings can never be added. */
fun updateModeCart(
    carts: Map<ShoppingMode, Map<String, Int>>,
    product: Product,
    delta: Int,
): Map<ShoppingMode, Map<String, Int>> {
    if (delta > 0 && (product.previewOnly || !product.isAvailable || (product.stockQuantity != null && product.stockQuantity <= 0))) return carts
    val maximum = product.stockQuantity?.coerceIn(0, 99) ?: 99
    val updated = updateCart(carts[product.mode].orEmpty(), product.id, delta, maximum)
    return carts.toMutableMap().apply {
        if (updated.isEmpty()) remove(product.mode) else put(product.mode, updated)
    }
}

data class CartTotals(val itemCount: Int, val totalPaise: Long)

fun cartTotals(cart: Map<String, Int>, products: List<Product>): CartTotals {
    val byId = products.associateBy { it.id }
    var count = 0
    var total = 0L
    cart.forEach { (id, quantity) ->
        val product = byId[id]
        if (product != null && quantity > 0) {
            count += quantity
            total += product.pricePaise * quantity
        }
    }
    return CartTotals(count, total)
}

fun formatPrice(paise: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    formatter.minimumFractionDigits = if (paise % 100 == 0L) 0 else 2
    formatter.maximumFractionDigits = 2
    return "₹" + formatter.format(BigDecimal.valueOf(paise, 2))
}

fun searchProducts(products: List<Product>, categories: List<Category>, query: String): List<Product> {
    val term = query.trim()
    if (term.isEmpty()) return products
    val namesById = categories.associate { it.id to it.name }
    return products.filter { product ->
        product.name.contains(term, ignoreCase = true) ||
            product.description.contains(term, ignoreCase = true) ||
            namesById[product.categoryId].orEmpty().contains(term, ignoreCase = true)
    }
}
