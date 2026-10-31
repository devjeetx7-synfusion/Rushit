import re

with open('app/src/main/java/com/rushit/app/ui/screens/store/ProductDetailsScreen.kt', 'r') as f:
    content = f.read()

# Replace the specific block of specs and features with View Full Details and Highlights
replacement = """                Text("About this product", style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                Text(product.description.take(150) + if(product.description.length > 150) "..." else "", style = MaterialTheme.typography.bodyMedium, color = Muted)
                
                if (product.keyFeatures.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    product.keyFeatures.take(2).forEach { feature ->
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.Top) {
                            Text("•", style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.padding(end = 8.dp))
                            Text(feature, style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.weight(1f))
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth().clickable { onFullDetails() }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("View Full Product Details", style = MaterialTheme.typography.labelLarge, color = Coral, modifier = Modifier.weight(1f))
                    Text("→", style = MaterialTheme.typography.labelLarge, color = Coral)
                }
                
                HorizontalDivider(color = Border)

                ProductOffers(product.availableOffers)
                PincodeChecker()
                
                ProductReviewsSection(state.reviews(product.id).take(2))
                Row(Modifier.fillMaxWidth().clickable { onReviews() }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("View All Reviews", style = MaterialTheme.typography.labelLarge, color = Coral, modifier = Modifier.weight(1f))
                    Text("→", style = MaterialTheme.typography.labelLarge, color = Coral)
                }

                ProductQASection(state.comments(product.id).take(2), state.questions(product.id).take(2))
                Row(Modifier.fillMaxWidth().clickable { onComments() }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("View All Comments & Q&A", style = MaterialTheme.typography.labelLarge, color = Coral, modifier = Modifier.weight(1f))
                    Text("→", style = MaterialTheme.typography.labelLarge, color = Coral)
                }
                
                ProductRecommendations("Similar Products", state.recommendations(product.id), onProduct = onProduct)
                ProductRecommendations("Recently Viewed", state.recentlyViewedProducts(product.id), onProduct = onProduct)

                Spacer(Modifier.height(24.dp))"""

# Locate the start of About this product
# and replace down to Spacer(Modifier.height(24.dp))
content = re.sub(
    r'                Text\("About this product".*?Spacer\(Modifier.height\(24.dp\)\)',
    replacement,
    content,
    flags=re.DOTALL
)

# And we need to add `onFullDetails`, `onReviews`, `onComments` to the parameter list
content = re.sub(
    r'    onProduct: \(String\) -> Unit,\n\)',
    r'    onProduct: (String) -> Unit,\n    onFullDetails: () -> Unit,\n    onReviews: () -> Unit,\n    onComments: () -> Unit,\n)',
    content
)

# Also we need to replace the gallery
gallery_replacement = """            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                SwipeableProductMediaGallery(product.media.ifEmpty { listOf(com.rushit.app.data.model.ProductMedia("1", product.imageUrl, product.imageRes)) }, product.name)

                Spacer(Modifier.height(18.dp))
                if (product.brand != null) {
                    Text(product.brand.uppercase(), style = MaterialTheme.typography.labelSmall, color = Muted, modifier = Modifier.padding(bottom = 4.dp))
                }
                Text(product.name, style = MaterialTheme.typography.headlineMedium, color = Ink)"""

content = re.sub(
    r'            Column\(Modifier.weight\(1f\).verticalScroll\(rememberScrollState\(\)\).padding\(horizontal = 20.dp\)\) \{\n                // Main Media View.*?Text\(product.name, style = MaterialTheme.typography.headlineMedium, color = Ink\)',
    gallery_replacement,
    content,
    flags=re.DOTALL
)

with open('app/src/main/java/com/rushit/app/ui/screens/store/ProductDetailsScreen.kt', 'w') as f:
    f.write(content)
