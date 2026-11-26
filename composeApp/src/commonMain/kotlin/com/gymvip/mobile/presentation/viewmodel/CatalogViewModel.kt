package com.gymvip.mobile.presentation.viewmodel

import com.gymvip.mobile.domain.model.Product
import com.gymvip.mobile.domain.model.ProductCategory
import com.gymvip.mobile.domain.usecase.GetCatalogUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogUiState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val selectedCategory: ProductCategory? = null,
    val onlyPromotions: Boolean = false,
    val selectedSubTab: Int = 0, // 0 = productos, 1 = promociones, 2 = favoritos
    val errorMessage: String? = null
)

class CatalogViewModel(
    private val getCatalogUseCase: GetCatalogUseCase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    fun setCategoryFilter(category: ProductCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun togglePromotionsOnly(onlyPromo: Boolean) {
        _uiState.update { it.copy(onlyPromotions = onlyPromo) }
    }

    fun setSubTab(tab: Int) {
        _uiState.update { it.copy(selectedSubTab = tab.coerceIn(0, 2)) }
    }

    fun refresh() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        scope.launch {
            getCatalogUseCase.refresh()
                .onFailure {
                    _uiState.update { state -> state.copy(
                        isLoading = false,
                        products = emptyList(),
                        errorMessage = "No pudimos consultar el catálogo."
                    ) }
                }
                .onSuccess { products ->
                    _uiState.update { it.copy(isLoading = false, products = products, errorMessage = null) }
                }
        }
    }

    suspend fun getThumbnail(path: String): ByteArray = getCatalogUseCase.getThumbnail(path)

    fun onCleared() {
        scope.cancel()
    }
}
