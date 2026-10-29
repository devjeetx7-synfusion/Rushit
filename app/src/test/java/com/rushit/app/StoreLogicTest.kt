package com.rushit.app

import com.rushit.app.data.model.Category
import com.rushit.app.data.model.Product
import com.rushit.app.data.model.ShoppingMode
import com.rushit.app.domain.categoriesForMode
import com.rushit.app.domain.productsForMode
import com.rushit.app.domain.updateModeCart
import com.rushit.app.domain.isValidDeliveryAddress
import com.rushit.app.domain.maskedIndianMobile
import com.rushit.app.domain.recentSearches
import com.rushit.app.domain.cartTotals
import com.rushit.app.domain.formatPrice
import com.rushit.app.domain.isValidIndianMobile
import com.rushit.app.domain.searchProducts
import com.rushit.app.domain.updateCart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreLogicTest {
    private val tomato = Product("tomato", "Tomato", "Fresh produce", "produce", pricePaise = 2_400, unit = "1 kg")
    private val milk = Product("milk", "Milk", "Dairy", "dairy", pricePaise = 5_800, unit = "1 L")
    private val categories = listOf(Category("produce", "Fruits & Vegetables", sortOrder = 0), Category("dairy", "Dairy & Bakery", sortOrder = 1))

    @Test fun `validates exactly ten Indian mobile digits`() {
        assertTrue(isValidIndianMobile("9876543210"))
        assertTrue(isValidIndianMobile("6123456789"))
        assertFalse(isValidIndianMobile("1234567890"))
        assertFalse(isValidIndianMobile("98765"))
        assertFalse(isValidIndianMobile("98765 43210"))
        assertFalse(isValidIndianMobile("９８７６５４３２１０"))
    }

    @Test fun `cart increments one line and computes count and total`() {
        var cart = updateCart(emptyMap(), "tomato", 1)
        cart = updateCart(cart, "tomato", 1)
        cart = updateCart(cart, "milk", 1)
        assertEquals(mapOf("tomato" to 2, "milk" to 1), cart)
        assertEquals(3, cartTotals(cart, listOf(tomato, milk)).itemCount)
        assertEquals(10_600L, cartTotals(cart, listOf(tomato, milk)).totalPaise)
        cart = updateCart(cart, "tomato", -2)
        assertEquals(mapOf("milk" to 1), cart)
    }

    @Test fun `quantity clamps to stock and never becomes negative`() {
        assertEquals(emptyMap<String, Int>(), updateCart(emptyMap(), "tomato", -1))
        assertEquals(mapOf("tomato" to 3), updateCart(emptyMap(), "tomato", 100, maxQuantity = 3))
    }

    @Test fun `search matches product and category case insensitively`() {
        assertEquals(listOf(tomato), searchProducts(listOf(tomato, milk), categories, "tom"))
        assertEquals(listOf(milk), searchProducts(listOf(tomato, milk), categories, "DAIRY"))
        assertEquals(emptyList<Product>(), searchProducts(listOf(tomato, milk), categories, "missing"))
    }

    @Test fun `prices show rupees and paise without adding false fractions`() {
        assertEquals("₹24", formatPrice(2_400))
        assertEquals("₹24.50", formatPrice(2_450))
    }

    @Test fun `mode filters do not leak grocery products or categories`() {
        val lamp = Product("lamp", "Lamp", "Example", "home", pricePaise = 99_900, unit = "1", mode = ShoppingMode.ECOMMERCE)
        val allCategories = categories + Category("home", "Home", sortOrder = 0, mode = ShoppingMode.ECOMMERCE) +
            Category("gifts", "Gifts", sortOrder = 0, mode = ShoppingMode.GIFTS)
        assertEquals(listOf(tomato, milk), productsForMode(listOf(tomato, milk, lamp), ShoppingMode.GROCERY))
        assertEquals(listOf(lamp), productsForMode(listOf(tomato, milk, lamp), ShoppingMode.ECOMMERCE))
        assertTrue(productsForMode(listOf(tomato, milk, lamp), ShoppingMode.GIFTS).isEmpty())
        assertEquals(listOf("home"), categoriesForMode(allCategories, ShoppingMode.ECOMMERCE).map { it.id })
        assertEquals(listOf("gifts"), categoriesForMode(allCategories, ShoppingMode.GIFTS).map { it.id })
    }

    @Test fun `mode carts stay independent and preview items cannot be added`() {
        val lamp = Product("lamp", "Lamp", "", "home", pricePaise = 99_900, unit = "1", mode = ShoppingMode.ECOMMERCE, stockQuantity = 2)
        val preview = lamp.copy(id = "preview", previewOnly = true, isAvailable = true)
        val unavailable = lamp.copy(id = "unavailable", isAvailable = false)
        var carts = updateModeCart(emptyMap(), tomato, 2)
        carts = updateModeCart(carts, lamp, 5)
        carts = updateModeCart(carts, preview, 1)
        carts = updateModeCart(carts, unavailable, 1)
        assertEquals(mapOf("tomato" to 2), carts[ShoppingMode.GROCERY])
        assertEquals(mapOf("lamp" to 2), carts[ShoppingMode.ECOMMERCE])
        carts = updateModeCart(carts, lamp, -2)
        assertFalse(carts.containsKey(ShoppingMode.ECOMMERCE))
        assertEquals(mapOf("tomato" to 2), carts[ShoppingMode.GROCERY])
    }

    @Test fun `validates manually entered address and masks mobile`() {
        assertTrue(isValidDeliveryAddress("12 Main Road", "Patna", "800001"))
        assertFalse(isValidDeliveryAddress("12", "Patna", "800001"))
        assertFalse(isValidDeliveryAddress("12 Main Road", "P", "800001"))
        assertFalse(isValidDeliveryAddress("12 Main Road", "Patna", "000001"))
        assertFalse(isValidDeliveryAddress("12 Main Road", "Patna", "8000A1"))
        assertEquals("+91 ••••••3210", maskedIndianMobile("9876543210"))
        assertEquals("your mobile number", maskedIndianMobile("123"))
    }

    @Test fun `recent searches are bounded sanitized and case insensitive`() {
        val existing = listOf("milk", "Bread", "Tomato", "Eggs", "Rice", "Apple")
        assertEquals(listOf("MILK", "Bread", "Tomato", "Eggs", "Rice", "Apple"), recentSearches(existing, " MILK "))
        assertEquals(listOf("green tea", "milk", "Bread", "Tomato", "Eggs", "Rice"), recentSearches(existing, "green\ntea"))
        assertEquals(existing, recentSearches(existing, "  "))
    }
}
