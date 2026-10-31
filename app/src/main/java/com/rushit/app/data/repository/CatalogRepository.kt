package com.rushit.app.data.repository

import com.rushit.app.R
import com.rushit.app.data.model.Catalog
import com.rushit.app.data.model.Category
import com.rushit.app.data.model.Product
import com.rushit.app.data.model.ShoppingMode

interface CatalogRepository {
    suspend fun loadCatalog(): Result<Catalog>
}

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
                Category("e_electronics", "Electronics", imageRes = R.drawable.ecommerce_headphones, sortOrder = 0, mode = ShoppingMode.ECOMMERCE),
                Category("e_home", "Home & Living", imageRes = R.drawable.ecommerce_lamp, sortOrder = 1, mode = ShoppingMode.ECOMMERCE),
                Category("e_fashion", "Fashion", imageRes = R.drawable.ic_user_02, sortOrder = 2, mode = ShoppingMode.ECOMMERCE),
                Category("e_beauty", "Beauty & Care", imageRes = R.drawable.category_care, sortOrder = 3, mode = ShoppingMode.ECOMMERCE),
                Category("e_gadgets", "Gadgets & Accessories", imageRes = R.drawable.ecommerce_headphones, sortOrder = 4, mode = ShoppingMode.ECOMMERCE),
                Category("g_bouquets", "Bouquets", imageRes = R.drawable.gift_bouquet, sortOrder = 0, mode = ShoppingMode.GIFTS),
                Category("g_beauty", "Beauty & Fragrance", imageRes = R.drawable.category_care, sortOrder = 1, mode = ShoppingMode.GIFTS),
                Category("g_gadgets", "Gadget Gifting", imageRes = R.drawable.ecommerce_headphones, sortOrder = 2, mode = ShoppingMode.GIFTS),
                Category("g_hampers", "Gift Hampers", imageRes = R.drawable.ic_shopping_bag_01, sortOrder = 3, mode = ShoppingMode.GIFTS),
                Category("g_occasions", "Special Occasions", imageRes = R.drawable.gift_bouquet, sortOrder = 4, mode = ShoppingMode.GIFTS),
            ),
            products = listOf(
                Product("tomato", "Tomato", "Farm fresh red tomatoes.", "produce", imageRes = R.drawable.product_tomatoes, pricePaise = 2_400, originalPricePaise = 3_000, unit = "1 kg", isBestseller = true,
                    keyFeatures = listOf("Sourced directly from local farms", "Rich in Vitamin C", "Ideal for salads and cooking"),
                    careInstructions = "Store in a cool, dry place. Wash before use.",
                    returnPolicy = "No returns for perishable goods.",
                    tags = listOf("fresh", "vegetable", "organic")
                ),
                Product("banana", "Banana", "A sample listing of ripe bananas.", "produce", imageRes = R.drawable.product_bananas, pricePaise = 3_600, unit = "1 kg", isBestseller = true),
                Product("milk", "Fresh Milk", "A sample listing of milk.", "dairy", imageRes = R.drawable.product_milk, pricePaise = 5_800, unit = "1 L", isBestseller = true),
                Product("chips", "Potato Chips", "A sample listing of potato chips.", "snacks", imageRes = R.drawable.category_snacks, pricePaise = 2_000, unit = "50 g"),
                Product("water", "Sparkling Water", "A sample listing of sparkling water.", "drinks", imageRes = R.drawable.category_drinks, pricePaise = 3_000, unit = "500 ml"),
                Product("wash", "Hand Wash", "A sample listing of hand wash.", "care", imageRes = R.drawable.category_care, pricePaise = 9_500, unit = "200 ml"),
                Product("cleaner", "Surface Cleaner", "A sample listing of surface cleaner.", "household", imageRes = R.drawable.category_household, pricePaise = 11_500, unit = "500 ml"),
                Product("demo_headphones", "Wireless Headphones", "High quality noise-cancelling wireless headphones.", "e_electronics", brand = "AudioPro", imageRes = R.drawable.ecommerce_headphones, pricePaise = 129_900, originalPricePaise = 199_900, unit = "1 pair", isAvailable = true, mode = ShoppingMode.ECOMMERCE, previewOnly = false, 
                    media = listOf(
                        com.rushit.app.data.model.ProductMedia("m1", resId = R.drawable.ecommerce_headphones),
                        com.rushit.app.data.model.ProductMedia("m2", resId = R.drawable.ic_launcher), // dummy
                        com.rushit.app.data.model.ProductMedia("m3", type = com.rushit.app.data.model.MediaType.VIDEO)
                    ),
                    options = listOf(
                        com.rushit.app.data.model.ProductOption("Color", listOf("Black", "White")),
                        com.rushit.app.data.model.ProductOption("Size", listOf("Standard", "Large"))
                    ),
                    variants = listOf(
                        com.rushit.app.data.model.ProductVariant("v1", mapOf("Color" to "Black", "Size" to "Standard"), stockQuantity = 10),
                        com.rushit.app.data.model.ProductVariant("v2", mapOf("Color" to "Black", "Size" to "Large"), stockQuantity = 0),
                        com.rushit.app.data.model.ProductVariant("v3", mapOf("Color" to "White", "Size" to "Standard"), stockQuantity = 5, pricePaise = 139_900)
                    ),
                    rating = 4.6,
                    reviewCount = 3,
                    specifications = mapOf("Battery Life" to "30 hours", "Bluetooth" to "5.2", "Weight" to "250g"),
                    keyFeatures = listOf("Active Noise Cancellation", "Multipoint Connection", "Fast Charging"),
                    whatsInTheBox = "Headphones, USB-C Cable, Hard Case",
                    careInstructions = "Wipe with a dry cloth.",
                    warranty = "1 Year Manufacturer Warranty",
                    returnPolicy = "7 Days Replacement",
                    sellerInformation = "AudioPro Official Store",
                    tags = listOf("wireless", "audio", "noise-cancelling"),
                    availableOffers = listOf("10% off with ICICI Bank Cards", "Flat ₹500 off on first order")
                ),
                Product("demo_lamp", "Table Lamp", "An illustrative home and living listing, shown for preview only.", "e_home", imageRes = R.drawable.ecommerce_lamp, pricePaise = 89_900, unit = "1 piece", isAvailable = false, mode = ShoppingMode.ECOMMERCE, previewOnly = true)
            ),
            reviews = listOf(
                com.rushit.app.data.model.Review("r1", "demo_headphones", "Alice", 5, "Great sound!", "The noise cancellation is amazing. Battery life is superb.", "2026-10-15", 12, true),
                com.rushit.app.data.model.Review("r2", "demo_headphones", "Bob", 4, "Good but heavy", "Sound is good, but they feel a bit heavy after a few hours.", "2026-10-18", 4),
                com.rushit.app.data.model.Review("r3", "demo_headphones", "Charlie", 5, "Best purchase", "Highly recommended.", "2026-10-20", 0, false, true)
            ),
            comments = listOf(
                com.rushit.app.data.model.Comment("c1", "demo_headphones", "Dave", "Does it come with a carrying case?", "2026-10-25", listOf(
                    com.rushit.app.data.model.Comment("c2", "demo_headphones", "RushIt Support", "Yes, a hard carrying case is included.", "2026-10-26")
                ), 5)
            ),
            questions = listOf(
                com.rushit.app.data.model.Question("q1", "demo_headphones", "Eve", "Can I connect this to two devices simultaneously?", "2026-10-22", "Yes, it supports multipoint Bluetooth.", 10)
            )
        )
    )
}