import re

with open('app/src/main/java/com/rushit/app/ui/screens/store/CartScreen.kt', 'r') as f:
    content = f.read()

replacement = """@Composable
fun CartScreen(state: ShopUiState, onBack: () -> Unit, onChangeQuantity: (String, String?, Int) -> Unit, onCheckout: () -> Unit, onAddress: () -> Unit, onProduct: (String) -> Unit) {
    // Generate a list of cart items
    val cartItems = remember(state.products, state.cart) {
        state.cart.mapNotNull { (key, quantity) ->
            if (quantity <= 0) return@mapNotNull null
            val (productId, variantId) = com.rushit.app.domain.parseCartKey(key)
            val product = state.products.firstOrNull { it.id == productId } ?: return@mapNotNull null
            Triple(product, variantId, quantity)
        }
    }
    Column(Modifier.fillMaxSize().background(Cream)) {
        ScreenHeader("Your cart", onBack)
        if (cartItems.isEmpty()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                MessageCard("Your ${state.selectedMode.label} cart is empty", "Add a few favorites to see them here. Carts are kept separately for each mode.",
                    Modifier.padding(16.dp), action = "Continue shopping", onAction = onBack)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item(key = "sample_note") {
                    Text(if (state.isSample) "${state.selectedMode.label} sample cart · Prices are illustrative" else "${state.selectedMode.label} cart",
                        color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 4.dp))
                }
                items(cartItems, key = { com.rushit.app.domain.cartKey(it.first.id, it.second) }) { (product, variantId, quantity) ->
                    CartLine(product, variantId, quantity, onMinus = { onChangeQuantity(product.id, variantId, -1) },
                        onPlus = { onChangeQuantity(product.id, variantId, 1) }, onOpen = { onProduct(product.id) })
                }
                item(key = "address") {
                    Surface(shape = RoundedCornerShape(18.dp), color = CardWhite, border = BorderStroke(1.dp, Border)) {
                        Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onAddress).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            RushIcon(R.drawable.ic_location_01, null, Modifier.size(22.dp), Coral)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Delivery address", style = MaterialTheme.typography.labelLarge, color = Ink)
                                Text(state.address ?: "Add an address", style = MaterialTheme.typography.bodySmall, color = Muted)
                            }
                            RushIcon(R.drawable.ic_arrow_right_01, null, Modifier.size(20.dp), Coral)
                        }
                    }
                }
                item(key = "totals") {
                    Surface(shape = RoundedCornerShape(18.dp), color = CardWhite, border = BorderStroke(1.dp, Border)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subtotal", style = MaterialTheme.typography.bodyLarge, color = Muted)
                                Text(formatPrice(state.totals.totalPaise), style = MaterialTheme.typography.titleMedium, color = Ink)
                            }
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = Border)
                            Spacer(Modifier.height(10.dp))
                            Text("Delivery fees and payment aren't configured. No order can be placed yet.", color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Surface(color = CardWhite, shadowElevation = 8.dp) {
                RushButton("Review checkout", onCheckout, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
            }
        }
    }
}

@Composable
private fun CartLine(product: Product, variantId: String?, quantity: Int, onMinus: () -> Unit, onPlus: () -> Unit, onOpen: () -> Unit) {
    val variant = product.variants.find { it.id == variantId }
    val price = variant?.pricePaise ?: product.pricePaise
    val stock = variant?.stockQuantity ?: product.stockQuantity
    Surface(shape = RoundedCornerShape(18.dp), color = CardWhite, border = BorderStroke(1.dp, Border.copy(alpha = .7f))) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)).background(Peach.copy(alpha = .5f)), contentAlignment = Alignment.Center) {
                CatalogImage(product.imageUrl, product.imageRes, product.name, Modifier.size(64.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button, onClickLabel = "View ${product.name}", onClick = onOpen)) {
                Text(product.name, style = MaterialTheme.typography.labelLarge, color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (variant != null) {
                    Text(variant.attributes.entries.joinToString(", ") { "${it.key}: ${it.value}" }, style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                Text(product.unit, style = MaterialTheme.typography.bodySmall, color = Muted)
                Spacer(Modifier.height(4.dp))
                Text(formatPrice(price * quantity), style = MaterialTheme.typography.labelLarge, color = Ink)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QuantityButton(R.drawable.ic_minus_sign, "Remove one ${product.name}", onMinus)
                    Text("$quantity", style = MaterialTheme.typography.labelMedium, color = Ink)
                    QuantityButton(R.drawable.ic_add_01, "Add one ${product.name}", onPlus,
                        enabled = !product.previewOnly && product.isAvailable && quantity < (stock?.coerceAtMost(99) ?: 99))
                }
            }
        }
    }
}"""

content = re.sub(
    r'@Composable\nfun CartScreen.*?}(?=\n\n@Composable\nprivate fun QuantityButton)',
    replacement,
    content,
    flags=re.DOTALL
)

with open('app/src/main/java/com/rushit/app/ui/screens/store/CartScreen.kt', 'w') as f:
    f.write(content)
