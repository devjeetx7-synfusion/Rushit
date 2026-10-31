with open('app/src/main/java/com/rushit/app/domain/StoreLogic.kt', 'a') as f:
    f.write('''

fun formatPrice(paise: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    formatter.minimumFractionDigits = if (paise % 100 == 0L) 0 else 2
    formatter.maximumFractionDigits = 2
    return "₹" + formatter.format(BigDecimal.valueOf(paise, 2))
}

fun searchProducts(products: List<Product>, categories: List<Category>, query: String): List<Product> {
    val term = query.trim()
    if (term.isEmpty()) return products
    val namesById = categories.associate { it.id to it.name }
    return products.filter { product ->
        product.name.contains(term, ignoreCase = true) ||
            product.description.contains(term, ignoreCase = true) ||
            namesById[product.categoryId].orEmpty().contains(term, ignoreCase = true)
    }
}
''')
