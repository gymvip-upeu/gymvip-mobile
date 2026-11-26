package com.gymvip.mobile.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.gymvip.mobile.data.local.GymVipDatabase
import com.gymvip.mobile.data.mapper.toDomain
import com.gymvip.mobile.data.remote.api.CatalogApi
import com.gymvip.mobile.data.remote.dto.AdminSupplementDto
import com.gymvip.mobile.domain.model.Product
import com.gymvip.mobile.domain.repository.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class CatalogRepositoryImpl(
    private val catalogApi: CatalogApi,
    private val database: GymVipDatabase
) : CatalogRepository {

    override fun observeProducts(): Flow<List<Product>> {
        return database.gymVipDatabaseQueries
            .selectAllProducts()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun refreshCatalog(): Result<List<Product>> {
        val result = catalogApi.getCatalogProducts()
        return result.map { list ->
            val now = Clock.System.now().toString()
            val domainList = list.map { it.toDomain() }
            database.gymVipDatabaseQueries.transaction {
                database.gymVipDatabaseQueries.clearProducts()
                domainList.forEach { item ->
                    database.gymVipDatabaseQueries.insertOrReplaceProduct(
                        id = item.id,
                        name = item.name,
                        description = item.description,
                        category = item.category.name,
                        priceCents = item.price.cents,
                        isPromotion = if (item.isPromotion) 1L else 0L,
                        promotionDiscountPercent = item.promotionDiscountPercent?.toLong(),
                        imageUrl = item.imageUrl,
                        lastSyncedAt = now
                    )
                }
            }
            domainList
        }
    }

    override suspend fun getThumbnail(path: String): ByteArray = catalogApi.getThumbnail(path)

    override suspend fun getProductById(id: String): Product? {
        return database.gymVipDatabaseQueries.selectProductById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun scanSupplement(code: String): Result<AdminSupplementDto> {
        return catalogApi.scanSupplement(code.trim())
    }

    override suspend fun updateSupplementBarcode(id: String, barcode: String): Result<AdminSupplementDto> {
        return catalogApi.updateSupplementBarcode(id, barcode.trim())
    }
}
