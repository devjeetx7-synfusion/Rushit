package com.rushit.app.data.repository

import com.rushit.app.R
import com.rushit.app.data.model.Catalog
import com.rushit.app.data.model.Category
import com.rushit.app.data.model.Product

interface CatalogRepository {
    suspend fun loadCatalog(): Result<Catalog>
}

/** Explicitly illustrative data, used until an actual catalog schema and RLS policies exist. */
class SampleCatalogRepository : CatalogRepository {
    override suspend fun loadCatalog(): Result<Catalog> = Result.success(
        Catalog(
            isSample = true,
            categories = listOf(
                Category("produce", "Fruits & Vegetables", imageRes = R.drawable.product_tomatoes, sortOrder = 0),
                Category("dairy", "Dairy & Bakery", imageRes = R.drawable.product_milk, sortOrder = 1),
                Category("snacks", "Snacks", imageRes = R.drawable.category_snacks, sortOrder = 2),
                Category("drinks", "Cold Drinks", imageRes = R.drawable.category_drinks, sortOrder = 3),
                Category("care", "Personal Care", imageRes = R.drawable.category_care, sortOrder = 4),
                Category("household", "Household", imageRes = R.drawable.category_household, sortOrder = 5),
            ),
            products = listOf(
                Product("tomato", "Tomato", "A sample listing of fresh tomatoes.", "produce", imageRes = R.drawable.product_tomatoes, pricePaise = 2_400, unit = "1 kg", isBestseller = true),
                Product("banana", "Banana", "A sample listing of ripe bananas.", "produce", imageRes = R.drawable.product_bananas, pricePaise = 3_600, unit = "1 kg", isBestseller = true),
                Product("milk", "Fresh Milk", "A sample listing of milk.", "dairy", imageRes = R.drawable.product_milk, pricePaise = 5_800, unit = "1 L", isBestseller = true),
                Product("chips", "Potato Chips", "A sample listing of potato chips.", "snacks", imageRes = R.drawable.category_snacks, pricePaise = 2_000, unit = "50 g"),
                Product("water", "Sparkling Water", "A sample listing of sparkling water.", "drinks", imageRes = R.drawable.category_drinks, pricePaise = 3_000, unit = "500 ml"),
                Product("wash", "Hand Wash", "A sample listing of hand wash.", "care", imageRes = R.drawable.category_care, pricePaise = 9_500, unit = "200 ml"),
                Product("cleaner", "Surface Cleaner", "A sample listing of surface cleaner.", "household", imageRes = R.drawable.category_household, pricePaise = 11_500, unit = "500 ml"),
            ),
        )
    )
}
