package com.rushit.app

import com.rushit.app.data.model.Product
import com.rushit.app.data.model.SavedAddress
import com.rushit.app.data.model.ShoppingMode
import com.rushit.app.ui.viewmodel.ShopUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
