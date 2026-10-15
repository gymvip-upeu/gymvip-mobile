package com.gymvip.mobile.domain.usecase

import com.gymvip.mobile.domain.model.Product
import com.gymvip.mobile.domain.model.ProductCategory
import com.gymvip.mobile.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetCatalogUseCase(private val catalogRepository: CatalogRepository) {

    fun observe(categoryFilter: ProductCategory? = null, onlyPromotions: Boolean = false): Flow<List<Product>> {
        return catalogRepository.observeProducts().map { list ->
            list.filter { product ->
                val matchesCategory = categoryFilter == null || product.category == categoryFilter
                val matchesPromo = !onlyPromotions || product.isPromotion
                matchesCategory && matchesPromo
            }
        }
    }

    suspend fun refresh(): Result<List<Product>> {
        return catalogRepository.refreshCatalog()
    }
}
