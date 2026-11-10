package pe.edu.upeu.gymvip.presentation.viewmodel

import pe.edu.upeu.gymvip.domain.model.Favorite
import pe.edu.upeu.gymvip.domain.model.Product
import pe.edu.upeu.gymvip.domain.model.SyncStatus
import pe.edu.upeu.gymvip.domain.usecase.ManageFavoritesUseCase
import pe.edu.upeu.gymvip.domain.usecase.SyncFavoritesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val favorites: List<Favorite> = emptyList(),
    val isSyncing: Boolean = false,
    val pendingCount: Int = 0,
    val activeDialogFavorite: Favorite? = null,
    val isAddingDialogForProduct: Product? = null,
    val noteInput: String = "",
    val feedbackMessage: String? = null,
    val errorMessage: String? = null
)

class FavoritesViewModel(
    private val manageFavoritesUseCase: ManageFavoritesUseCase,
    private val syncFavoritesUseCase: SyncFavoritesUseCase,
    private val accountId: String,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        observeFavorites()
    }

    private fun observeFavorites() {
        manageFavoritesUseCase.observeFavorites(accountId).onEach { list ->
            val pending = list.count { it.syncStatus != SyncStatus.SYNCED }
            _uiState.value = _uiState.value.copy(
                favorites = list,
                pendingCount = pending
            )
        }.launchIn(scope)
    }

    fun openAddDialog(product: Product) {
        _uiState.value = _uiState.value.copy(
            isAddingDialogForProduct = product,
            activeDialogFavorite = null,
            noteInput = ""
        )
    }

    fun openEditNoteDialog(favorite: Favorite) {
        _uiState.value = _uiState.value.copy(
            activeDialogFavorite = favorite,
            isAddingDialogForProduct = null,
            noteInput = favorite.personalNote
        )
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(
            activeDialogFavorite = null,
            isAddingDialogForProduct = null,
            noteInput = ""
        )
    }

    fun updateNoteInput(text: String) {
        _uiState.value = _uiState.value.copy(noteInput = text)
    }

    fun saveFavoriteFromDialog() {
        val product = _uiState.value.isAddingDialogForProduct
        val note = _uiState.value.noteInput
        if (product != null) {
            scope.launch {
                val result = manageFavoritesUseCase.addFavorite(accountId, product.id, note)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isAddingDialogForProduct = null,
                            noteInput = "",
                            feedbackMessage = "Guardado en favoritos (almacenamiento offline activo)."
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            errorMessage = error.message ?: "No se pudo guardar el favorito"
                        )
                    }
                )
            }
        }
    }

    fun saveEditedNote() {
        val fav = _uiState.value.activeDialogFavorite
        val note = _uiState.value.noteInput
        if (fav != null) {
            scope.launch {
                val result = manageFavoritesUseCase.updateNote(accountId, fav.id, note)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            activeDialogFavorite = null,
                            noteInput = "",
                            feedbackMessage = "Nota personal actualizada con éxito."
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            errorMessage = error.message ?: "Error al actualizar nota"
                        )
                    }
                )
            }
        }
    }

    fun deleteFavorite(favorite: Favorite) {
        scope.launch {
            val result = manageFavoritesUseCase.removeFavorite(accountId, favorite.id)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        feedbackMessage = "Favorito eliminado."
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Error al quitar favorito"
                    )
                }
            )
        }
    }

    fun syncPending() {
        scope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, errorMessage = null)
            val result = syncFavoritesUseCase(accountId)
            result.fold(
                onSuccess = { count ->
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        feedbackMessage = if (count > 0) "Sincronizadas $count operaciones con el servidor." else "Todo está al día con el servidor."
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        errorMessage = "Sincronización pausada: ${error.message}"
                    )
                }
            )
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null, errorMessage = null)
    }

    fun onCleared() {
        scope.cancel()
    }
}
