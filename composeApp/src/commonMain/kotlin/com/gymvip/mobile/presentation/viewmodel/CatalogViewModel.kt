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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class CatalogUiState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val selectedCategory: ProductCategory? = null,
    val onlyPromotions: Boolean = false,
    val selectedSubTab: Int = 0, // 0 = Catálogo de Productos y Planes, 1 = Mis Favoritos
    val errorMessage: String? = null
)

class CatalogViewModel(
    private val getCatalogUseCase: GetCatalogUseCase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        observeProducts()
        refresh()
    }

    private fun observeProducts() {
        getCatalogUseCase.observe(
            categoryFilter = _uiState.value.selectedCategory,
            onlyPromotions = _uiState.value.onlyPromotions
        ).onEach { list ->
            _uiState.value = _uiState.value.copy(products = list)
        }.launchIn(scope)
    }

    fun setCategoryFilter(category: ProductCategory?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        observeProducts()
    }

    fun togglePromotionsOnly(onlyPromo: Boolean) {
        _uiState.value = _uiState.value.copy(onlyPromotions = onlyPromo)
        observeProducts()
    }

    fun setSubTab(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedSubTab = tab)
    }

    fun refresh() {
        scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = getCatalogUseCase.refresh()
            result.onFailure {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Mostrando catálogo offline local."
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = null)
            }
        }
    }

    fun onCleared() {
        scope.cancel()
    }
}
