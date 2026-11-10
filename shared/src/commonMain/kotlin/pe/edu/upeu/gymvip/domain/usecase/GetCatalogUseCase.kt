package pe.edu.upeu.gymvip.domain.usecase

import pe.edu.upeu.gymvip.domain.model.Product
import pe.edu.upeu.gymvip.domain.model.ProductCategory
import pe.edu.upeu.gymvip.domain.repository.CatalogRepository
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
