package com.gymvip.mobile.domain.repository

import com.gymvip.mobile.domain.model.Favorite
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {
    fun observeFavorites(accountId: String): Flow<List<Favorite>>
    suspend fun addFavorite(accountId: String, productId: String, personalNote: String): Result<Favorite>
    suspend fun updateNote(accountId: String, favoriteId: String, newNote: String): Result<Favorite>
    suspend fun removeFavorite(accountId: String, favoriteId: String): Result<Unit>
    suspend fun syncPending(accountId: String): Result<Int>
    suspend fun clearFavorites(accountId: String)
}
