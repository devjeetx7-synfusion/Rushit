package com.rushit.app.domain

import com.rushit.app.data.model.Category
import com.rushit.app.data.model.Product
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

fun isValidIndianMobile(value: String): Boolean =
    value.length == 10 && value.all { it in '0'..'9' } && value.first() in '6'..'9'

/** A single map entry per product prevents duplicate cart lines. */
fun updateCart(cart: Map<String, Int>, productId: String, delta: Int, maxQuantity: Int = 99): Map<String, Int> {
    val quantity = ((cart[productId] ?: 0).toLong() + delta).coerceIn(0, maxQuantity.coerceAtLeast(0).toLong()).toInt()
    return cart.toMutableMap().apply {
        if (quantity == 0) remove(productId) else put(productId, quantity)
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
