package com.gymvip.mobile.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.gymvip.mobile.data.local.GymVipDatabase
import com.gymvip.mobile.data.mapper.toDomain
import com.gymvip.mobile.data.remote.api.CatalogApi
import com.gymvip.mobile.data.remote.dto.AdminSupplementDto
import com.gymvip.mobile.domain.model.MoneyPen
import com.gymvip.mobile.domain.model.Product
import com.gymvip.mobile.domain.model.ProductCategory
import com.gymvip.mobile.domain.repository.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class CatalogRepositoryImpl(
    private val catalogApi: CatalogApi,
    private val database: GymVipDatabase
) : CatalogRepository {

    init {
        seedInitialCatalogIfNeeded()
    }

    private fun seedInitialCatalogIfNeeded() {
        val existing = database.gymVipDatabaseQueries.selectAllProducts().executeAsList()
        if (existing.isEmpty()) {
            val now = Clock.System.now().toString()
            val initial = listOf(
                Product(
                    id = "prod_plan_mensual",
                    name = "Plan VIP Mensual",
                    description = "Acceso ilimitado por 30 días continuos. Cero tolerancia.",
                    category = ProductCategory.PLAN_MEMBRESIA,
                    price = MoneyPen.fromSoles(120),
                    isPromotion = false
                ),
                Product(
                    id = "prod_plan_trimestral",
                    name = "Plan VIP Trimestral",
                    description = "90 días continuos con evaluación corporal incluida.",
                    category = ProductCategory.PLAN_MEMBRESIA,
                    price = MoneyPen.fromSoles(320),
                    isPromotion = true,
                    promotionDiscountPercent = 11
                ),
                Product(
                    id = "prod_pase_diario",
                    name = "Pase Diario VIP",
                    description = "Acceso válido por una única jornada de entrenamiento.",
                    category = ProductCategory.PLAN_MEMBRESIA,
                    price = MoneyPen.fromSoles(25),
                    isPromotion = false
                ),
                Product(
                    id = "prod_whey_protein",
                    name = "Proteína Whey Gold Standard 2lb",
                    description = "Aislado de suero de alta pureza para recuperación muscular.",
                    category = ProductCategory.SUPLEMENTO,
                    price = MoneyPen.fromSoles(165),
                    isPromotion = true,
                    promotionDiscountPercent = 10
                ),
                Product(
                    id = "prod_creatina_creapure",
                    name = "Creatina Monohidratada 300g",
                    description = "Fuerza y potencia muscular sin aditivos.",
                    category = ProductCategory.SUPLEMENTO,
                    price = MoneyPen.fromSoles(95),
                    isPromotion = false
                ),
                Product(
                    id = "prod_shaker_vip",
                    name = "Shaker Metálico GYM VIP",
                    description = "Acero inoxidable térmico edición dorada.",
                    category = ProductCategory.MERCHANDISING,
                    price = MoneyPen.fromSoles(45),
                    isPromotion = false
                )
            )

            database.gymVipDatabaseQueries.transaction {
                initial.forEach { item ->
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
        }
    }

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

    override suspend fun getProductById(id: String): Product? {
        return database.gymVipDatabaseQueries.selectProductById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun scanSupplement(code: String): Result<AdminSupplementDto> {
        val remote = catalogApi.scanSupplement(code.trim())
        if (remote.isSuccess) return remote

        // Fallback para códigos demo si estamos en modo desconectado / pruebas locales
        if (code.trim() == "7751234567890" || code.trim().equals("PROD-WHEY-01", ignoreCase = true)) {
            return Result.success(
                AdminSupplementDto(
                    id = "supp-demo-01",
                    name = "Proteína Whey Gold Standard 2kg",
                    brand = "Optimum Nutrition",
                    category = "PROTEINA",
                    flavor = "Chocolate Doble",
                    weightKg = 2.0,
                    price = 249.90,
                    stock = 15,
                    isAvailable = true,
                    barcode = code.trim(),
                    description = "Aislado de proteína de suero de alta pureza con rápida absorción"
                )
            )
        }
        return remote
    }

    override suspend fun updateSupplementBarcode(id: String, barcode: String): Result<AdminSupplementDto> {
        return catalogApi.updateSupplementBarcode(id, barcode.trim())
    }
}
