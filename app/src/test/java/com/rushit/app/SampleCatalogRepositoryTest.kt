package com.rushit.app

import com.rushit.app.data.model.ShoppingMode
import com.rushit.app.data.repository.SampleCatalogRepository
import com.rushit.app.domain.categoriesForMode
import com.rushit.app.domain.productsForMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleCatalogRepositoryTest {
    @Test fun `sample categories and products belong to the right modes`() = runBlocking {
        val catalog = SampleCatalogRepository().loadCatalog().getOrThrow()
        assertTrue(catalog.isSample)
        assertEquals(catalog.categories.size, catalog.categories.map { it.id }.toSet().size)
        assertEquals(catalog.products.size, catalog.products.map { it.id }.toSet().size)
        catalog.products.forEach { product ->
            assertTrue(catalog.categories.any { it.id == product.categoryId && it.mode == product.mode })
        }
        assertTrue(productsForMode(catalog.products, ShoppingMode.GROCERY).isNotEmpty())
        val examples = productsForMode(catalog.products, ShoppingMode.ECOMMERCE)
        assertTrue(examples.isNotEmpty())
        assertTrue(examples.all { it.previewOnly && !it.isAvailable })
        assertTrue(categoriesForMode(catalog.categories, ShoppingMode.GIFTS).isNotEmpty())
        assertFalse(productsForMode(catalog.products, ShoppingMode.GIFTS).isNotEmpty())
    }
}
