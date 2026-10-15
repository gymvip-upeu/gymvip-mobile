package com.gymvip.mobile.domain.usecase

import com.gymvip.mobile.domain.repository.FavoriteRepository

class SyncFavoritesUseCase(private val favoriteRepository: FavoriteRepository) {
    suspend operator fun invoke(accountId: String): Result<Int> {
        return favoriteRepository.syncPending(accountId)
    }
}
