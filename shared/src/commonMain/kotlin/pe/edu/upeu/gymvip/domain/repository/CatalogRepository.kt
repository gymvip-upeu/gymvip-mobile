package pe.edu.upeu.gymvip.domain.repository

import pe.edu.upeu.gymvip.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    fun observeProducts(): Flow<List<Product>>
    suspend fun refreshCatalog(): Result<List<Product>>
    suspend fun getProductById(id: String): Product?
}
