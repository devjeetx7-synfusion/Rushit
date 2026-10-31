import re

with open('app/src/main/java/com/rushit/app/ui/screens/store/CatalogScreens.kt', 'r') as f:
    content = f.read()

new_listing_screen = """@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ProductListingScreen(
    title: String,
    products: List<Product>,
    state: ShopUiState,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    onWishlist: (String) -> Unit,
    onProduct: (String) -> Unit,
    onRetry: () -> Unit,
    category: Category? = null,
    emptyTitle: String = "Nothing here yet",
    emptyMessage: String = "There are no products in this collection right now.",
) {
    var sort by rememberSaveable(title) { mutableStateOf("Popularity") }
    var expandedSort by remember { mutableStateOf(false) }
    
    // Filters
    var showFilters by remember { mutableStateOf(false) }
    var filterAvailability by rememberSaveable { mutableStateOf(false) }
    var filterDiscount by rememberSaveable { mutableStateOf(false) }
    var filterBrand by rememberSaveable { mutableStateOf<String?>(null) }
    var filterMinPrice by rememberSaveable { mutableStateOf<Float?>(null) }
    var filterMaxPrice by rememberSaveable { mutableStateOf<Float?>(null) }
    var filterMinRating by rememberSaveable { mutableStateOf<Float?>(null) }
    
    val brands = remember(products) { products.mapNotNull { it.brand }.distinct().sorted() }
    
    val filteredAndSorted = remember(products, sort, filterAvailability, filterDiscount, filterBrand, filterMinPrice, filterMaxPrice, filterMinRating) {
        var res = products
        if (filterAvailability) res = res.filter { it.isAvailable && (it.stockQuantity == null || it.stockQuantity > 0) }
        if (filterDiscount) res = res.filter { it.originalPricePaise != null && it.originalPricePaise > it.pricePaise }
        if (filterBrand != null) res = res.filter { it.brand == filterBrand }
        if (filterMinPrice != null) res = res.filter { it.pricePaise >= filterMinPrice!! * 100 }
        if (filterMaxPrice != null) res = res.filter { it.pricePaise <= filterMaxPrice!! * 100 }
        if (filterMinRating != null) res = res.filter { (it.rating ?: 0.0) >= filterMinRating!! }
        
        when (sort) {
            "Price low to high" -> res.sortedBy { it.pricePaise }
            "Price high to low" -> res.sortedByDescending { it.pricePaise }
            "Newest" -> res.sortedByDescending { it.id }
            else -> res // Popularity / Recommended
        }
    }
    
    val activeFilterCount = (if(filterAvailability) 1 else 0) + (if(filterDiscount) 1 else 0) + (if(filterBrand != null) 1 else 0) + (if(filterMinPrice != null || filterMaxPrice != null) 1 else 0) + (if(filterMinRating != null) 1 else 0)

    Column(Modifier.fillMaxSize().background(Cream)) {
        ScreenHeader(title, onBack)
        if (category != null) {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(Peach), contentAlignment = Alignment.Center) {
                    CatalogImage(category.imageUrl, category.imageRes, null, Modifier.size(42.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("Explore ${category.name}", style = MaterialTheme.typography.bodyMedium, color = Muted)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(16.dp), 
                color = if (activeFilterCount > 0) Coral else CardWhite,
                border = BorderStroke(1.dp, if (activeFilterCount > 0) Coral else Border),
                modifier = Modifier.clickable { showFilters = true }
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    RushIcon(R.drawable.ic_search_01, null, Modifier.size(14.dp), if (activeFilterCount > 0) Color.White else Ink)
                    Spacer(Modifier.width(6.dp))
                    Text("Filters" + if (activeFilterCount > 0) " ($activeFilterCount)" else "", style = MaterialTheme.typography.labelMedium, color = if (activeFilterCount > 0) Color.White else Ink)
                }
            }
            Spacer(Modifier.width(12.dp))
            Box {
                Surface(
                    shape = RoundedCornerShape(16.dp), 
                    color = CardWhite, border = BorderStroke(1.dp, Border),
                    modifier = Modifier.clickable { expandedSort = true }
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Sort: $sort", style = MaterialTheme.typography.labelMedium, color = Ink)
                        Spacer(Modifier.width(6.dp))
                        RushIcon(R.drawable.ic_arrow_down_01, null, Modifier.size(14.dp), Ink)
                    }
                }
                DropdownMenu(expanded = expandedSort, onDismissRequest = { expandedSort = false }) {
                    listOf("Popularity", "Price low to high", "Price high to low", "Newest").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = { sort = option; expandedSort = false }
                        )
                    }
                }
            }
        }
        
        // Active Filter Chips
        if (activeFilterCount > 0) {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                if (filterAvailability) item { FilterChip("In Stock") { filterAvailability = false } }
                if (filterDiscount) item { FilterChip("Discounted") { filterDiscount = false } }
                if (filterBrand != null) item { FilterChip(filterBrand!!) { filterBrand = null } }
                if (filterMinPrice != null || filterMaxPrice != null) item { FilterChip("Price") { filterMinPrice = null; filterMaxPrice = null } }
                if (filterMinRating != null) item { FilterChip("${filterMinRating}★ & above") { filterMinRating = null } }
                item { 
                    Text("Clear All", color = Coral, style = MaterialTheme.typography.labelMedium, modifier = Modifier.clickable { 
                        filterAvailability = false; filterDiscount = false; filterBrand = null; filterMinPrice = null; filterMaxPrice = null; filterMinRating = null 
                    }.padding(vertical = 6.dp, horizontal = 4.dp))
                }
            }
        }

        ProductResults(filteredAndSorted, state, onAdd, onWishlist, onProduct, onRetry, emptyTitle, emptyMessage)
    }
    
    if (showFilters) {
        ModalBottomSheet(onDismissRequest = { showFilters = false }) {
            Column(Modifier.padding(20.dp)) {
                Text("Filters", style = MaterialTheme.typography.titleLarge, color = Ink)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { filterAvailability = !filterAvailability }.padding(vertical = 8.dp)) {
                    Checkbox(checked = filterAvailability, onCheckedChange = { filterAvailability = it }, colors = CheckboxDefaults.colors(checkedColor = Coral))
                    Text("Show only In Stock", style = MaterialTheme.typography.bodyMedium, color = Ink)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { filterDiscount = !filterDiscount }.padding(vertical = 8.dp)) {
                    Checkbox(checked = filterDiscount, onCheckedChange = { filterDiscount = it }, colors = CheckboxDefaults.colors(checkedColor = Coral))
                    Text("Has Discount", style = MaterialTheme.typography.bodyMedium, color = Ink)
                }
                if (brands.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("Brand", style = MaterialTheme.typography.titleMedium, color = Ink)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        items(brands) { b ->
                            Surface(
                                shape = RoundedCornerShape(16.dp), color = if (b == filterBrand) Coral else CardWhite, border = BorderStroke(1.dp, if (b == filterBrand) Coral else Border),
                                modifier = Modifier.clickable { filterBrand = if (filterBrand == b) null else b }
                            ) {
                                Text(b, color = if (b == filterBrand) Color.White else Ink, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Minimum Rating", style = MaterialTheme.typography.titleMedium, color = Ink)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    listOf(4f, 3f, 2f, 1f).forEach { r ->
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp), color = if (r == filterMinRating) Coral else CardWhite, border = BorderStroke(1.dp, if (r == filterMinRating) Coral else Border),
                                modifier = Modifier.clickable { filterMinRating = if (filterMinRating == r) null else r }
                            ) {
                                Text("$r★ & above", color = if (r == filterMinRating) Color.White else Ink, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                RushButton("Apply", { showFilters = false })
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun FilterChip(label: String, onRemove: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = Peach, border = BorderStroke(1.dp, Coral)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Coral)
            Spacer(Modifier.width(4.dp))
            RushIcon(R.drawable.ic_cancel_01, "Remove", Modifier.size(12.dp).clickable { onRemove() }, Coral)
        }
    }
}"""

content = re.sub(
    r'@Composable\nfun ProductListingScreen.*?(?=\n@Composable\nfun SearchScreen)',
    new_listing_screen,
    content,
    flags=re.DOTALL
)

with open('app/src/main/java/com/rushit/app/ui/screens/store/CatalogScreens.kt', 'w') as f:
    f.write(content)
