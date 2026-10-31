import re

with open('app/src/main/java/com/rushit/app/ui/navigation/RushItApp.kt', 'r') as f:
    content = f.read()

# Fix NavBackStackEntry type mismatches
# SearchScreen
new_search = """                        if (!needsStartupRoute) SearchScreen(shopState, onBack = { nav.popBackStack() }, onQuery = shop::updateSearch,
                            onSubmit = shop::recordSearch, onRecent = { r -> shop.updateSearch(r); shop.recordSearch() }, onClearRecent = shop::clearRecentSearches,
                            onCategoryFilter = shop::selectSearchCategory,
                            onAdd = { id -> shop.changeQuantity(id, delta = 1) }, onWishlist = shop::toggleWishlist, onProduct = ::openProduct, onRetry = shop::loadCatalog)"""
content = re.sub(
    r'                        if \(\!needsStartupRoute\) SearchScreen\(shopState, onBack = \{ nav.popBackStack\(\) \}, onQuery = shop::updateSearch,\n                            onSubmit = shop::recordSearch, onRecent = \{ shop.updateSearch\(it\); shop.recordSearch\(\) \}, onClearRecent = shop::clearRecentSearches,\n                            onCategoryFilter = shop::selectSearchCategory,\n                            onAdd = \{ shop.changeQuantity\(it, delta = 1\) \}, onWishlist = shop::toggleWishlist, onProduct = ::openProduct, onRetry = shop::loadCatalog\)',
    new_search,
    content
)

# all
new_all = """                        if (!needsStartupRoute) ProductListingScreen("All ${shopState.selectedMode.label}", modeProducts, shopState, onBack = { nav.popBackStack() },
                            onAdd = { id -> shop.changeQuantity(id, delta = 1) }, onWishlist = shop::toggleWishlist, onProduct = ::openProduct, onRetry = shop::loadCatalog,
                            emptyMessage = "No products have been configured for this mode yet.")"""
content = re.sub(
    r'                        if \(\!needsStartupRoute\) ProductListingScreen\("All \$\{shopState.selectedMode.label\}", modeProducts, shopState, onBack = \{ nav.popBackStack\(\) \},\n                            onAdd = \{ shop.changeQuantity\(it, delta = 1\) \}, onWishlist = shop::toggleWishlist, onProduct = ::openProduct, onRetry = shop::loadCatalog,\n                            emptyMessage = "No products have been configured for this mode yet."\)',
    new_all,
    content
)

# bestsellers
new_best = """                        if (!needsStartupRoute) ProductListingScreen("Bestsellers", modeProducts.filter { it.isBestseller }, shopState, onBack = { nav.popBackStack() },
                            onAdd = { id -> shop.changeQuantity(id, delta = 1) }, onWishlist = shop::toggleWishlist, onProduct = ::openProduct, onRetry = shop::loadCatalog)"""
content = re.sub(
    r'                        if \(\!needsStartupRoute\) ProductListingScreen\("Bestsellers", modeProducts.filter \{ it.isBestseller \}, shopState, onBack = \{ nav.popBackStack\(\) \},\n                            onAdd = \{ shop.changeQuantity\(it, delta = 1\) \}, onWishlist = shop::toggleWishlist, onProduct = ::openProduct, onRetry = shop::loadCatalog\)',
    new_best,
    content
)

# wishlist
new_wish = """                        if (!needsStartupRoute) ProductListingScreen("Wishlist", modeProducts.filter { it.id in shopState.wishlist }, shopState,
                            onBack = { nav.popBackStack() }, onAdd = { id -> shop.changeQuantity(id, delta = 1) }, onWishlist = shop::toggleWishlist,
                            onProduct = ::openProduct, onRetry = shop::loadCatalog, emptyTitle = "No saved ${shopState.selectedMode.label.lowercase()} items",
                            emptyMessage = "Tap a heart to save an item. Switch modes to see saved items from another mode.")"""
content = re.sub(
    r'                        if \(\!needsStartupRoute\) ProductListingScreen\("Wishlist", modeProducts.filter \{ it.id in shopState.wishlist \}, shopState,\n                            onBack = \{ nav.popBackStack\(\) \}, onAdd = \{ shop.changeQuantity\(it, delta = 1\) \}, onWishlist = shop::toggleWishlist,\n                            onProduct = ::openProduct, onRetry = shop::loadCatalog, emptyTitle = "No saved \$\{shopState.selectedMode.label.lowercase\(\)\} items",\n                            emptyMessage = "Tap a heart to save an item. Switch modes to see saved items from another mode."\)',
    new_wish,
    content
)

# category/{id}
new_cat = """                    composable("category/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { backStackEntry ->
                        if (!needsStartupRoute) {
                            val categoryId = backStackEntry.arguments?.getString("id").orEmpty()
                            val category = modeCategories.firstOrNull { it.id == categoryId }
                            ProductListingScreen(category?.name ?: "Category", modeProducts.filter { it.categoryId == categoryId }, shopState, onBack = { nav.popBackStack() },
                                onAdd = { productId -> shop.changeQuantity(productId, delta = 1) }, onWishlist = shop::toggleWishlist, onProduct = ::openProduct, onRetry = shop::loadCatalog,
                                category = category,
                                emptyMessage = if (category == null) "This category isn't part of ${shopState.selectedMode.label}. Pick a category from the current mode."
                                else "No products are configured in this category yet.")
                        }
                    }"""
content = re.sub(
    r'                    composable\("category/\{id\}", arguments = listOf\(navArgument\("id"\) \{ type = NavType.StringType \}\)\) \{ backStackEntry ->\n                        if \(\!needsStartupRoute\) \{\n                            val id = backStackEntry.arguments\?\.getString\("id"\)\.orEmpty\(\)\n                            val category = modeCategories.firstOrNull \{ it.id == id \}\n                            ProductListingScreen\(category\?\.name \?: "Category", modeProducts.filter \{ it.categoryId == id \}, shopState, onBack = \{ nav.popBackStack\(\) \},\n                                onAdd = \{ shop.changeQuantity\(it, delta = 1\) \}, onWishlist = shop::toggleWishlist, onProduct = ::openProduct, onRetry = shop::loadCatalog,\n                                category = category,\n                                emptyMessage = if \(category == null\) "This category isn.t part of \$\{shopState.selectedMode.label\}. Pick a category from the current mode."\n                                else "No products are configured in this category yet."\)\n                        \}\n                    \}',
    new_cat,
    content
)

# AccountScreen
new_account = """                        if (!needsStartupRoute) AccountScreen(authState, shopState, onWishlist = { nav.navigate("wishlist") { launchSingleTop = true } },
                            onAddresses = { nav.navigate("addresses") { launchSingleTop = true } }, onOrders = { orders.refreshOrders(); selectMain("orders") }, onSignInOrOut = auth::signOutOrSignIn,
                            onHelp = { nav.navigate("help") { launchSingleTop = true } },
                            onNotifications = { nav.navigate("notifications") { launchSingleTop = true } })"""
content = re.sub(
    r'                        if \(\!needsStartupRoute\) AccountScreen\(authState, shopState, onWishlist = \{ nav.navigate\("wishlist"\) \{ launchSingleTop = true \} \},\n                            onAddresses = \{ nav.navigate\("addresses"\) \{ launchSingleTop = true \} \}, onOrders = \{ orders.refreshOrders\(\); selectMain\("orders"\) \}, onSignInOrOut = auth::signOutOrSignIn,\n                            onHelp = \{ nav.navigate\("help"\) \{ launchSingleTop = true \} \},\n                            onNotifications = \{ nav.navigate\("notifications"\) \{ launchSingleTop = true \} \}\)',
    new_account,
    content
)

with open('app/src/main/java/com/rushit/app/ui/navigation/RushItApp.kt', 'w') as f:
    f.write(content)
