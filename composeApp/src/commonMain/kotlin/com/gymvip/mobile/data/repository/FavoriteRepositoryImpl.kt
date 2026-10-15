package com.gymvip.mobile.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.gymvip.mobile.data.local.GymVipDatabase
import com.gymvip.mobile.data.mapper.toDomain
import com.gymvip.mobile.data.remote.api.FavoriteApi
import com.gymvip.mobile.data.sync.SyncQueueManager
import com.gymvip.mobile.domain.error.AppError
import com.gymvip.mobile.domain.model.Favorite
import com.gymvip.mobile.domain.model.SyncStatus
import com.gymvip.mobile.domain.repository.CatalogRepository
import com.gymvip.mobile.domain.repository.FavoriteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class FavoriteRepositoryImpl(
    private val favoriteApi: FavoriteApi,
    private val catalogRepository: CatalogRepository,
    private val database: GymVipDatabase,
    private val syncQueueManager: SyncQueueManager
) : FavoriteRepository {

    override fun observeFavorites(accountId: String): Flow<List<Favorite>> {
        return database.gymVipDatabaseQueries
            .selectFavoritesByAccountId(accountId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun addFavorite(accountId: String, productId: String, personalNote: String): Result<Favorite> {
        // Verificar unicidad de favorito por producto y usuario
        val existing = database.gymVipDatabaseQueries.selectFavoriteByProduct(accountId, productId).executeAsOneOrNull()
        if (existing != null) {
            return Result.failure(AppError.Conflict("DUPLICATE_FAVORITE", "Este producto ya se encuentra en sus favoritos"))
        }

        val product = catalogRepository.getProductById(productId)
            ?: return Result.failure(AppError.NotFound("El producto seleccionado no existe en el catálogo"))

        val now = Clock.System.now().toString()
        val tempId = "fav_temp_${Clock.System.now().toEpochMilliseconds()}"

        val favorite = Favorite(
            id = tempId,
            accountId = accountId,
            productId = productId,
            productName = product.name,
            productCategory = product.category,
            productPrice = product.price,
            personalNote = personalNote,
            createdAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_CREATE
        )

        database.gymVipDatabaseQueries.transaction {
            database.gymVipDatabaseQueries.insertOrReplaceFavorite(
                id = favorite.id,
                accountId = favorite.accountId,
                productId = favorite.productId,
                productName = favorite.productName,
                productCategory = favorite.productCategory.name,
                productPriceCents = favorite.productPrice.cents,
                personalNote = favorite.personalNote,
                createdAt = favorite.createdAt,
                updatedAt = favorite.updatedAt,
                syncStatus = favorite.syncStatus.name
            )
            syncQueueManager.enqueueFavoriteCreate(tempId, accountId, productId, personalNote)
        }

        // Intento oportunista de sincronización inmediata si hay red
        try {
            syncQueueManager.processQueue(accountId)
        } catch (_: Exception) {
            // El ítem permanece seguro en SQLite con PENDING_CREATE
        }

        val updated = database.gymVipDatabaseQueries.selectFavoriteByProduct(accountId, productId).executeAsOneOrNull()
        return Result.success(updated?.toDomain() ?: favorite)
    }

    override suspend fun updateNote(accountId: String, favoriteId: String, newNote: String): Result<Favorite> {
        val existing = database.gymVipDatabaseQueries.selectFavoriteById(favoriteId, accountId).executeAsOneOrNull()
            ?: return Result.failure(AppError.NotFound("Favorito no encontrado"))

        val now = Clock.System.now().toString()
        val currentStatus = try { SyncStatus.valueOf(existing.syncStatus) } catch (_: Exception) { SyncStatus.SYNCED }
        val newStatus = if (currentStatus == SyncStatus.PENDING_CREATE) SyncStatus.PENDING_CREATE else SyncStatus.PENDING_UPDATE

        database.gymVipDatabaseQueries.transaction {
            database.gymVipDatabaseQueries.insertOrReplaceFavorite(
                id = existing.id,
                accountId = existing.accountId,
                productId = existing.productId,
                productName = existing.productName,
                productCategory = existing.productCategory,
                productPriceCents = existing.productPriceCents,
                personalNote = newNote,
                createdAt = existing.createdAt,
                updatedAt = now,
                syncStatus = newStatus.name
            )
            syncQueueManager.enqueueFavoriteUpdate(favoriteId, accountId, newNote)
        }

        try {
            syncQueueManager.processQueue(accountId)
        } catch (_: Exception) {
        }

        val updated = database.gymVipDatabaseQueries.selectFavoriteById(favoriteId, accountId).executeAsOneOrNull()
        return Result.success(updated?.toDomain() ?: existing.toDomain().copy(personalNote = newNote))
    }

    override suspend fun removeFavorite(accountId: String, favoriteId: String): Result<Unit> {
        val existing = database.gymVipDatabaseQueries.selectFavoriteById(favoriteId, accountId).executeAsOneOrNull()
            ?: return Result.failure(AppError.NotFound("Favorito no encontrado"))

        database.gymVipDatabaseQueries.transaction {
            database.gymVipDatabaseQueries.markFavoritePendingDelete(
                updatedAt = Clock.System.now().toString(),
                id = favoriteId,
                accountId = accountId
            )
            syncQueueManager.enqueueFavoriteDelete(favoriteId, accountId)
        }

        try {
            syncQueueManager.processQueue(accountId)
        } catch (_: Exception) {
        }

        return Result.success(Unit)
    }

    override suspend fun syncPending(accountId: String): Result<Int> {
        return syncQueueManager.processQueue(accountId)
    }

    override suspend fun clearFavorites(accountId: String) {
        database.gymVipDatabaseQueries.deleteFavoritesByAccountId(accountId)
    }
}
