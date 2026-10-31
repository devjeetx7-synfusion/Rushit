package com.rushit.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rushit.app.data.model.Category
import com.rushit.app.data.model.Product
import com.rushit.app.data.model.SavedAddress
import com.rushit.app.data.model.ShoppingMode
import com.rushit.app.data.repository.CatalogRepository
import com.rushit.app.domain.CartTotals
import com.rushit.app.domain.cartTotals
import com.rushit.app.domain.isValidDeliveryAddress
import com.rushit.app.domain.productsForSelection
import com.rushit.app.domain.recentSearches
import com.rushit.app.domain.resolveCategorySelection
import com.rushit.app.domain.updateModeCart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ShopUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val isSample: Boolean = true,
    val categories: List<Category> = emptyList(),
    val products: List<Product> = emptyList(),
    val allReviews: List<com.rushit.app.data.model.Review> = emptyList(),
    val allComments: List<com.rushit.app.data.model.Comment> = emptyList(),
    val allQuestions: List<com.rushit.app.data.model.Question> = emptyList(),
    val selectedMode: ShoppingMode = ShoppingMode.GROCERY,
    val carts: Map<ShoppingMode, Map<String, Int>> = emptyMap(),
    val wishlist: Set<String> = emptySet(),
    val addresses: List<SavedAddress> = emptyList(),
    val selectedAddressId: String? = null,
    val searchInput: String = "",
    val searchQuery: String = "",
    val searchCategoryId: String? = null,
    val recentSearches: List<String> = emptyList(),
    val recentlyViewed: List<String> = emptyList(),
    val selectedCategoryId: String? = null,
) {
    val cart: Map<String, Int> get() = carts[selectedMode].orEmpty()
    val totals: CartTotals get() = cartTotals(cart, products)
    val selectedAddress: SavedAddress? get() = addresses.firstOrNull { it.id == selectedAddressId }
    val address: String? get() = selectedAddress?.summary
    fun quantityFor(product: Product, variantId: String? = null): Int = carts[product.mode]?.get(com.rushit.app.domain.cartKey(product.id, variantId)) ?: 0

    /** Categories of the active mode; the single source for the home rail and listing screens. */
    fun categoriesInMode(): List<Category> = categories.filter { it.mode == selectedMode }.sortedBy { it.sortOrder }

    val selectedCategory: Category? get() = selectedCategoryId?.let { id -> categories.firstOrNull { it.id == id && it.mode == selectedMode } }

    /** Products for the active mode, narrowed by the selected category. */
    fun visibleProducts(): List<Product> = productsForSelection(products, selectedMode, selectedCategoryId)

    fun reviews(productId: String) = allReviews.filter { it.productId == productId }
    fun comments(productId: String) = allComments.filter { it.productId == productId }
    fun questions(productId: String) = allQuestions.filter { it.productId == productId }
    
    fun recommendations(productId: String): List<Product> {
        val target = products.find { it.id == productId } ?: return emptyList()
        return products.filter { it.id != productId && it.categoryId == target.categoryId }.take(10)
    }
    
    fun recentlyViewedProducts(currentProductId: String): List<Product> {
        val byId = products.associateBy { it.id }
        return recentlyViewed.filter { it != currentProductId }.mapNotNull { byId[it] }
    }

}

@OptIn(FlowPreview::class)
class ShopViewModel(context: Context, private val repository: CatalogRepository) : ViewModel() {
    private val prefs = context.applicationContext.getSharedPreferences("rushit_store", Context.MODE_PRIVATE)
    private val initialMode = runCatching {
        ShoppingMode.valueOf(prefs.getString("shopping_mode", ShoppingMode.GROCERY.name).orEmpty())
    }.getOrDefault(ShoppingMode.GROCERY)
    private val initialAddresses = readAddresses(prefs.getString("addresses", null)).ifEmpty {
        prefs.getString("address", null)?.takeIf { it.isNotBlank() }
            ?.let { listOf(SavedAddress("legacy", "Saved", it, "", "")) }.orEmpty()
    }
    private val _state = MutableStateFlow(
        ShopUiState(
            selectedMode = initialMode,
            wishlist = prefs.getStringSet("wishlist", emptySet()).orEmpty().toSet(),
            addresses = initialAddresses,
            selectedAddressId = prefs.getString("selected_address_id", null)
                ?.takeIf { id -> initialAddresses.any { it.id == id } } ?: initialAddresses.firstOrNull()?.id,
            recentSearches = readRecent(initialMode),
            recentlyViewed = readRecentlyViewed(),
        )
    )
    val state = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        loadCatalog()
        viewModelScope.launch {
            _state.map { it.searchInput }.distinctUntilChanged().debounce(250).collect { query ->
                _state.update { it.copy(searchQuery = query) }
            }
        }
    }

    fun loadCatalog() {
        if (loadJob?.isActive == true) return
        _state.update { it.copy(loading = true, error = null) }
        loadJob = viewModelScope.launch {
            val result = try {
                withContext(Dispatchers.IO) { repository.loadCatalog() }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                Result.failure(IllegalStateException())
            }
            result.fold(
                onSuccess = { catalog ->
                    _state.update {
                        it.copy(
                            loading = false,
                            categories = catalog.categories.sortedBy { category -> category.sortOrder },
                            products = catalog.products,
                            allReviews = catalog.reviews,
                            allComments = catalog.comments,
                            allQuestions = catalog.questions,
                            isSample = catalog.isSample,
                            error = null,
                            selectedCategoryId = resolveCategorySelection(catalog.categories, it.selectedMode, it.selectedCategoryId),
                        )
                    }
                },
                onFailure = { _state.update { it.copy(loading = false, error = "Couldn't load the catalog. Check your connection and try again.") } },
            )
        }
    }

    fun selectMode(mode: ShoppingMode) {
        val current = _state.value
        if (current.selectedMode == mode) return
        prefs.edit().putString("shopping_mode", mode.name).apply()
        _state.update {
            it.copy(
                selectedMode = mode,
                // A category from the previous mode must never stay highlighted.
                selectedCategoryId = resolveCategorySelection(it.categories, mode, it.selectedCategoryId),
                searchInput = "",
                searchQuery = "",
                searchCategoryId = null,
                recentSearches = readRecent(mode),
            )
        }
    }

    /** Selecting an unknown or cross-mode category is ignored rather than shown as active. */
    fun selectCategory(categoryId: String?) {
        _state.update {
            val resolved = resolveCategorySelection(it.categories, it.selectedMode, categoryId)
            if (resolved == it.selectedCategoryId) it else it.copy(selectedCategoryId = resolved)
        }
    }

    /** Called only after the order repository confirmed the order was stored. */
    fun clearCart() {
        val mode = _state.value.selectedMode
        _state.update { it.copy(carts = it.carts - mode) }
    }

    fun changeQuantity(productId: String, variantId: String? = null, delta: Int) {
        val product = _state.value.products.firstOrNull { it.id == productId } ?: return
        _state.update { it.copy(carts = updateModeCart(it.carts, product, variantId, delta)) }
    }

    fun toggleWishlist(productId: String) {
        if (_state.value.products.none { it.id == productId }) return
        _state.update {
            val updated = if (productId in it.wishlist) it.wishlist - productId else it.wishlist + productId
            it.copy(wishlist = updated)
        }
        prefs.edit().putStringSet("wishlist", _state.value.wishlist).apply()
    }

    fun updateSearch(value: String) {
        _state.update { it.copy(searchInput = value.replace('\n', ' ').replace('\r', ' ').take(80)) }
    }

    fun selectSearchCategory(id: String?) { _state.update { it.copy(searchCategoryId = id) } }

    fun recordSearch() {
        val snapshot = _state.value
        val updated = recentSearches(snapshot.recentSearches, snapshot.searchInput)
        prefs.edit().putString("recent_${snapshot.selectedMode.name}", updated.joinToString("\n")).apply()
        _state.update { if (it.selectedMode == snapshot.selectedMode) it.copy(recentSearches = updated) else it }
    }

    fun clearRecentSearches() {
        val mode = _state.value.selectedMode
        prefs.edit().remove("recent_${mode.name}").apply()
        _state.update { if (it.selectedMode == mode) it.copy(recentSearches = emptyList()) else it }
    }

    fun saveAddress(id: String?, type: String, line: String, city: String, postalCode: String, latitude: Double?, longitude: Double?) {
        if (!isValidDeliveryAddress(line, city, postalCode)) return
        val existingId = id?.takeIf { savedId -> _state.value.addresses.any { it.id == savedId } }
        val validCoordinates = latitude != null && longitude != null && latitude.isFinite() && longitude.isFinite() &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0
        val address = SavedAddress(existingId ?: UUID.randomUUID().toString(), type, line.trim().take(160), city.trim().take(80), postalCode,
            if (validCoordinates) latitude else null, if (validCoordinates) longitude else null)
        _state.update {
            val addresses = if (existingId == null) it.addresses + address else it.addresses.map { saved -> if (saved.id == existingId) address else saved }
            it.copy(addresses = addresses, selectedAddressId = address.id)
        }
        persistAddresses()
    }

    fun selectAddress(id: String) {
        if (_state.value.addresses.none { it.id == id }) return
        _state.update { it.copy(selectedAddressId = id) }
        persistAddresses()
    }

    fun removeAddress(id: String) {
        _state.update {
            val remaining = it.addresses.filterNot { address -> address.id == id }
            it.copy(addresses = remaining, selectedAddressId = if (it.selectedAddressId == id) remaining.firstOrNull()?.id else it.selectedAddressId)
        }
        persistAddresses()
    }

    private fun persistAddresses() {
        val snapshot = _state.value
        val json = JSONArray()
        snapshot.addresses.forEach { address ->
            json.put(JSONObject().put("id", address.id).put("type", address.type).put("line", address.line)
                .put("city", address.city).put("postalCode", address.postalCode)
                .put("latitude", address.latitude).put("longitude", address.longitude))
        }
        prefs.edit().putString("addresses", json.toString()).putString("selected_address_id", snapshot.selectedAddressId)
            .remove("address").apply()
    }

    private fun readRecent(mode: ShoppingMode): List<String> =
        prefs.getString("recent_${mode.name}", "").orEmpty().lineSequence().filter { it.isNotBlank() }.take(6).toList()

    private fun readAddresses(raw: String?): List<SavedAddress> = runCatching {
        if (raw.isNullOrBlank()) return@runCatching emptyList()
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                val line = item.optString("line").takeIf { it.isNotBlank() } ?: continue
                val lat = item.optDouble("latitude", Double.NaN).takeIf { it.isFinite() && it in -90.0..90.0 }
                val lng = item.optDouble("longitude", Double.NaN).takeIf { it.isFinite() && it in -180.0..180.0 }
                add(SavedAddress(id, item.optString("type", "Saved"), line, item.optString("city"), item.optString("postalCode"), lat, lng))
            }
        }
    }.getOrDefault(emptyList())


    fun recordView(productId: String) {
        val current = _state.value.recentlyViewed
        if (current.firstOrNull() == productId) return // already at top
        val updated = (listOf(productId) + current.filter { it != productId }).take(10)
        prefs.edit().putString("recently_viewed", updated.joinToString(",")).apply()
        _state.update { it.copy(recentlyViewed = updated) }
    }

    private fun readRecentlyViewed(): List<String> =
        prefs.getString("recently_viewed", "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

    companion object {
        fun factory(context: Context, repository: CatalogRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(ShopViewModel::class.java))
                return ShopViewModel(context, repository) as T
            }
        }
    }
}
