package com.rushit.app

import com.rushit.app.data.model.Category
import com.rushit.app.data.model.Product
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
}
