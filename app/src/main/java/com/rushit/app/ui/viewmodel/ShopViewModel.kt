package com.rushit.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rushit.app.data.model.Category
import com.rushit.app.data.model.Product
import com.rushit.app.data.repository.CatalogRepository
import com.rushit.app.domain.CartTotals
import com.rushit.app.domain.cartTotals
import com.rushit.app.domain.updateCart
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShopUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val isSample: Boolean = true,
    val categories: List<Category> = emptyList(),
    val products: List<Product> = emptyList(),
    val cart: Map<String, Int> = emptyMap(),
    val wishlist: Set<String> = emptySet(),
    val address: String? = null,
    val searchInput: String = "",
    val searchQuery: String = "",
) {
    val totals: CartTotals get() = cartTotals(cart, products)
}

@OptIn(FlowPreview::class)
class ShopViewModel(context: Context, private val repository: CatalogRepository) : ViewModel() {
    private val prefs = context.applicationContext.getSharedPreferences("rushit_store", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(
        ShopUiState(
            wishlist = prefs.getStringSet("wishlist", emptySet()).orEmpty().toSet(),
            address = prefs.getString("address", null),
        )
    )
    val state = _state.asStateFlow()

    init {
        loadCatalog()
        viewModelScope.launch {
            _state.map { it.searchInput }.distinctUntilChanged().debounce(250).collect { query ->
                _state.update { it.copy(searchQuery = query) }
            }
        }
    }

    fun loadCatalog() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = try { repository.loadCatalog() } catch (_: Exception) { Result.failure(IllegalStateException()) }
            result.fold(
                onSuccess = { catalog ->
                    _state.update {
                        it.copy(loading = false, categories = catalog.categories.sortedBy { category -> category.sortOrder },
                            products = catalog.products, isSample = catalog.isSample, error = null)
                    }
                },
                onFailure = { _state.update { it.copy(loading = false, error = "Couldn't load the catalog. Check your connection and try again.") } },
            )
        }
    }

    fun changeQuantity(productId: String, delta: Int) {
        val product = _state.value.products.firstOrNull { it.id == productId } ?: return
        if (delta > 0 && (!product.isAvailable || product.stockQuantity == 0)) return
        val maximum = product.stockQuantity?.coerceIn(0, 99) ?: 99
        _state.update { it.copy(cart = updateCart(it.cart, productId, delta, maximum)) }
    }

    fun toggleWishlist(productId: String) {
        if (_state.value.products.none { it.id == productId }) return
        _state.update {
            val updated = if (productId in it.wishlist) it.wishlist - productId else it.wishlist + productId
            prefs.edit().putStringSet("wishlist", updated).apply()
            it.copy(wishlist = updated)
        }
    }

    fun updateSearch(value: String) { _state.update { it.copy(searchInput = value) } }

    fun saveAddress(value: String) {
        val address = value.trim().take(160).ifBlank { null }
        prefs.edit().putString("address", address).apply()
        _state.update { it.copy(address = address) }
    }

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
