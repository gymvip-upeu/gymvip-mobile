package pe.edu.upeu.gymvip.domain.usecase

import pe.edu.upeu.gymvip.domain.error.AppError
import pe.edu.upeu.gymvip.domain.model.Favorite
import pe.edu.upeu.gymvip.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow

class ManageFavoritesUseCase(private val favoriteRepository: FavoriteRepository) {

    fun observeFavorites(accountId: String): Flow<List<Favorite>> {
        return favoriteRepository.observeFavorites(accountId)
    }

    suspend fun addFavorite(accountId: String, productId: String, note: String): Result<Favorite> {
        val trimmedNote = note.trim()
        if (productId.isBlank()) {
            return Result.failure(AppError.Validation("El producto seleccionado es inválido"))
        }
        if (trimmedNote.length > MAX_NOTE_LENGTH) {
            return Result.failure(AppError.Validation("La nota personal no puede exceder los $MAX_NOTE_LENGTH caracteres"))
        }
        return favoriteRepository.addFavorite(accountId, productId, trimmedNote)
    }

    suspend fun updateNote(accountId: String, favoriteId: String, newNote: String): Result<Favorite> {
        val trimmedNote = newNote.trim()
        if (favoriteId.isBlank()) {
            return Result.failure(AppError.Validation("ID de favorito inválido"))
        }
        if (trimmedNote.length > MAX_NOTE_LENGTH) {
            return Result.failure(AppError.Validation("La nota personal no puede exceder los $MAX_NOTE_LENGTH caracteres"))
        }
        return favoriteRepository.updateNote(accountId, favoriteId, trimmedNote)
    }

    suspend fun removeFavorite(accountId: String, favoriteId: String): Result<Unit> {
        if (favoriteId.isBlank()) {
            return Result.failure(AppError.Validation("ID de favorito inválido"))
        }
        return favoriteRepository.removeFavorite(accountId, favoriteId)
    }

    companion object {
        const val MAX_NOTE_LENGTH = 250
    }
}
