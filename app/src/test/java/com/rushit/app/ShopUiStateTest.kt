package com.rushit.app

import com.rushit.app.data.model.Category
import com.rushit.app.data.model.Product
import com.rushit.app.data.model.SavedAddress
import com.rushit.app.data.model.ShoppingMode
import com.rushit.app.domain.resolveCategorySelection
import com.rushit.app.ui.viewmodel.ShopUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopUiStateTest {
    @Test fun `cart and totals follow the selected mode without losing other carts`() {
        val grocery = Product("grocery", "Tomato", "", "produce", pricePaise = 2_400, unit = "1 kg")
        val lamp = Product("lamp", "Lamp", "", "home", pricePaise = 99_900, unit = "1", mode = ShoppingMode.ECOMMERCE)
        val state = ShopUiState(products = listOf(grocery, lamp), carts = mapOf(
            ShoppingMode.GROCERY to mapOf(grocery.id to 2), ShoppingMode.ECOMMERCE to mapOf(lamp.id to 1),
        ))
        assertEquals(2, state.totals.itemCount)
        assertEquals(4_800L, state.totals.totalPaise)
        val ecommerce = state.copy(selectedMode = ShoppingMode.ECOMMERCE)
        assertEquals(mapOf("lamp" to 1), ecommerce.cart)
        assertEquals(99_900L, ecommerce.totals.totalPaise)
        assertEquals(2, ecommerce.quantityFor(grocery))
        assertEquals(mapOf("grocery" to 2), ecommerce.copy(selectedMode = ShoppingMode.GROCERY).cart)
        assertEquals(emptyMap<String, Int>(), ecommerce.copy(selectedMode = ShoppingMode.GIFTS).cart)
    }

    @Test fun `selected address is derived only from saved entries`() {
        val home = SavedAddress("home", "Home", "12 Main Road", "Patna", "800001")
        val state = ShopUiState(addresses = listOf(home), selectedAddressId = "home")
        assertEquals("12 Main Road, Patna, 800001", state.address)
        assertNull(state.copy(selectedAddressId = "missing").address)
    }

    @Test fun `selected category narrows products and never survives a mode change`() {
        val produce = Category("produce", "Produce", sortOrder = 0)
        val snacks = Category("snacks", "Snacks", sortOrder = 1)
        val home = Category("e_home", "Home", sortOrder = 0, mode = ShoppingMode.ECOMMERCE)
        val tomato = Product("tomato", "Tomato", "", "produce", pricePaise = 1_000, unit = "1")
        val chips = Product("chips", "Chips", "", "snacks", pricePaise = 500, unit = "1")
        val lamp = Product("lamp", "Lamp", "", "e_home", pricePaise = 9_000, unit = "1", mode = ShoppingMode.ECOMMERCE)
        val state = ShopUiState(
            categories = listOf(produce, snacks, home),
            products = listOf(tomato, chips, lamp),
            selectedCategoryId = "produce",
        )
        assertEquals(listOf(tomato), state.visibleProducts())
        assertEquals("Produce", state.selectedCategory?.name)
        assertEquals(listOf(produce, snacks), state.categoriesInMode())

        // The view model resolves the selection on a mode change; a raw copy would keep a stale id.
        val switched = state.copy(
            selectedMode = ShoppingMode.ECOMMERCE,
            selectedCategoryId = resolveCategorySelection(state.categories, ShoppingMode.ECOMMERCE, state.selectedCategoryId),
        )
        assertNull(switched.selectedCategoryId)
        assertEquals(listOf(lamp), switched.visibleProducts())
        assertTrue(switched.categoriesInMode().none { it.mode == ShoppingMode.GROCERY })
    }
}
