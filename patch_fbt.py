import re

with open('app/src/main/java/com/rushit/app/ui/screens/store/ProductExtraComponents.kt', 'r') as f:
    content = f.read()

new_fbt = """@Composable
fun FrequentlyBoughtTogether(
    mainProduct: Product,
    recommendations: List<Product>,
    onAddToCart: (String) -> Unit
) {
    if (recommendations.isEmpty()) return
    
    val combo = recommendations.take(2)
    var selected = remember { mutableStateListOf<Product>().apply { addAll(combo) } }
    
    Column(Modifier.padding(vertical = 16.dp)) {
        Text("Frequently Bought Together (Demo)", style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(12.dp))
        
        Surface(shape = RoundedCornerShape(12.dp), color = CardWhite, border = BorderStroke(1.dp, Border), modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Column(Modifier.padding(16.dp)) {
                // Main Product
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = true, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = Muted, disabledCheckedColor = Muted), enabled = false)
                    Spacer(Modifier.width(8.dp))
                    Text("This item: ${mainProduct.name}", style = MaterialTheme.typography.bodyMedium, color = Ink, modifier = Modifier.weight(1f))
                    Text(formatPrice(mainProduct.pricePaise), style = MaterialTheme.typography.labelMedium, color = Ink)
                }
                
                // Recommended Products
                combo.forEach { p ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Checkbox(
                            checked = selected.contains(p),
                            onCheckedChange = { if (it) selected.add(p) else selected.remove(p) },
                            colors = CheckboxDefaults.colors(checkedColor = Coral)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(p.name, style = MaterialTheme.typography.bodyMedium, color = Ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatPrice(p.pricePaise), style = MaterialTheme.typography.labelMedium, color = Ink)
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Border)
                Spacer(Modifier.height(12.dp))
                
                val total = mainProduct.pricePaise + selected.sumOf { it.pricePaise }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Total Price:", style = MaterialTheme.typography.bodySmall, color = Muted)
                        Text(formatPrice(total), style = MaterialTheme.typography.titleMedium, color = Ink)
                    }
                    Button(
                        onClick = { 
                            onAddToCart(mainProduct.id)
                            selected.forEach { onAddToCart(it.id) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Coral),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Add Selected to Cart")
                    }
                }
            }
        }
    }
}
"""

content = content + "\n" + new_fbt

with open('app/src/main/java/com/rushit/app/ui/screens/store/ProductExtraComponents.kt', 'w') as f:
    f.write(content)

# Add it to ProductDetailsScreen
with open('app/src/main/java/com/rushit/app/ui/screens/store/ProductDetailsScreen.kt', 'r') as f:
    content_pd = f.read()

content_pd = content_pd.replace(
    'ProductRecommendations("Similar Products", state.recommendations(product.id), onProduct = onProduct)',
    'FrequentlyBoughtTogether(product, state.recommendations(product.id), onAddToCart = { id -> onChangeQuantity(id, null, 1) })\n                ProductRecommendations("Similar Products", state.recommendations(product.id), onProduct = onProduct)'
)

with open('app/src/main/java/com/rushit/app/ui/screens/store/ProductDetailsScreen.kt', 'w') as f:
    f.write(content_pd)

