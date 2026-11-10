package pe.edu.upeu.gymvip.domain.usecase

import pe.edu.upeu.gymvip.domain.repository.FavoriteRepository

class SyncFavoritesUseCase(private val favoriteRepository: FavoriteRepository) {
    suspend operator fun invoke(accountId: String): Result<Int> {
        return favoriteRepository.syncPending(accountId)
    }
}
