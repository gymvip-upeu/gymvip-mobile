package pe.edu.upeu.gymvip.domain

import pe.edu.upeu.gymvip.domain.error.AppError
import pe.edu.upeu.gymvip.domain.model.Favorite
import pe.edu.upeu.gymvip.domain.model.MoneyPen
import pe.edu.upeu.gymvip.domain.model.ProductCategory
import pe.edu.upeu.gymvip.domain.model.SyncStatus
import pe.edu.upeu.gymvip.domain.repository.FavoriteRepository
import pe.edu.upeu.gymvip.domain.usecase.ManageFavoritesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ManageFavoritesUseCaseTest {

    private val fakeRepo = object : FavoriteRepository {
        val storage = mutableListOf<Favorite>()

        override fun observeFavorites(accountId: String): Flow<List<Favorite>> = flowOf(storage)

        override suspend fun addFavorite(accountId: String, productId: String, personalNote: String): Result<Favorite> {
            val fav = Favorite(
                id = "fav_test_1",
                accountId = accountId,
                productId = productId,
                productName = "Proteína Whey",
                productCategory = ProductCategory.SUPLEMENTO,
                productPrice = MoneyPen.fromSoles(165),
                personalNote = personalNote,
                createdAt = "2026-10-15T10:00:00Z",
                updatedAt = "2026-10-15T10:00:00Z",
                syncStatus = SyncStatus.PENDING_CREATE
            )
            storage.add(fav)
            return Result.success(fav)
        }

        override suspend fun updateNote(accountId: String, favoriteId: String, newNote: String): Result<Favorite> {
            val item = storage.find { it.id == favoriteId } ?: return Result.failure(AppError.NotFound())
            val updated = item.copy(personalNote = newNote, syncStatus = SyncStatus.PENDING_UPDATE)
            return Result.success(updated)
        }

        override suspend fun removeFavorite(accountId: String, favoriteId: String): Result<Unit> {
            storage.removeAll { it.id == favoriteId }
            return Result.success(Unit)
        }

        override suspend fun syncPending(accountId: String): Result<Int> = Result.success(1)
        override suspend fun clearFavorites(accountId: String) { storage.clear() }
    }

    private val useCase = ManageFavoritesUseCase(fakeRepo)

    @Test
    fun testAddFavoriteWithValidNoteSucceeds() = runTest {
        val result = useCase.addFavorite("acc_1", "prod_1", "Consultar sabor en recepción")
        assertTrue(result.isSuccess)
    }

    @Test
    fun testAddFavoriteWithNoteExceeding250CharsFails() = runTest {
        val longNote = "a".repeat(251)
        val result = useCase.addFavorite("acc_1", "prod_1", longNote)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.Validation)
    }

    @Test
    fun testAddFavoriteWithBlankProductIdFails() = runTest {
        val result = useCase.addFavorite("acc_1", "   ", "Nota válida")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.Validation)
    }
}
