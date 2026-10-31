import re

with open('app/src/main/java/com/rushit/app/ui/navigation/RushItApp.kt', 'r') as f:
    content = f.read()

new_routes = """                    composable("product/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("id").orEmpty()
                        LaunchedEffect(id) { shop.recordView(id) }
                        if (!needsStartupRoute) ProductDetailsScreen(shopState, id,
                            onBack = { nav.popBackStack() }, onChangeQuantity = shop::changeQuantity, onWishlist = shop::toggleWishlist,
                            onCart = { nav.navigate("cart") { launchSingleTop = true } }, onCheckout = { nav.navigate("checkout") { launchSingleTop = true } }, onRetry = shop::loadCatalog, onProduct = ::openProduct,
                            onFullDetails = { nav.navigate("product-details-full/${android.net.Uri.encode(id)}") { launchSingleTop = true } },
                            onReviews = { nav.navigate("product-reviews/${android.net.Uri.encode(id)}") { launchSingleTop = true } },
                            onComments = { nav.navigate("product-comments/${android.net.Uri.encode(id)}") { launchSingleTop = true } })
                    }
                    composable("product-details-full/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("id").orEmpty()
                        if (!needsStartupRoute) com.rushit.app.ui.screens.store.FullProductDetailsScreen(shopState, id, onBack = { nav.popBackStack() })
                    }
                    composable("product-reviews/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("id").orEmpty()
                        if (!needsStartupRoute) com.rushit.app.ui.screens.store.ProductReviewsScreen(shopState, id, onBack = { nav.popBackStack() })
                    }
                    composable("product-comments/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("id").orEmpty()
                        if (!needsStartupRoute) com.rushit.app.ui.screens.store.ProductCommentsScreen(shopState, id, onBack = { nav.popBackStack() })
                    }"""

content = re.sub(
    r'                    composable\("product/\{id\}".*?onProduct = ::openProduct\)\n                    \}',
    new_routes,
    content,
    flags=re.DOTALL
)

with open('app/src/main/java/com/rushit/app/ui/navigation/RushItApp.kt', 'w') as f:
    f.write(content)
