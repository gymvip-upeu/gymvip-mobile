package com.gymvip.mobile.presentation.viewmodel

import com.gymvip.mobile.domain.model.InboxItem
import com.gymvip.mobile.domain.error.AppError
import com.gymvip.mobile.domain.usecase.GetInboxUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InboxUiState(
    val isLoading: Boolean = true,
    val items: List<InboxItem> = emptyList(),
    val archivedItems: List<InboxItem> = emptyList(),
    val archivedLoading: Boolean = false,
    val archivedError: String? = null,
    val archiving: Boolean = false,
    val loadFailed: Boolean = false,
    val loadErrorMessage: String? = null,
    val readingId: String? = null,
    val readFailed: Boolean = false,
    val readErrorMessage: String? = null,
    val pushItemId: String? = null,
    val openedItemId: String? = null,
    val pushErrorMessage: String? = null
) {
    val unreadCount: Int get() = items.count { !it.isRead }
}

class InboxViewModel(
    private val getInboxUseCase: GetInboxUseCase,
    private val accountId: String,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()
    private var refreshJob: Job? = null

    init {
        getInboxUseCase.observe(accountId).onEach { list ->
            _uiState.update { state ->
                val previous = state.items.associateBy { it.id }
                state.copy(items = list.filter { it.accountId == accountId }.map { item ->
                    item.copy(archivable = previous[item.id]?.archivable ?: item.archivable)
                })
            }
        }.launchIn(scope)

        refresh()
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = getInboxUseCase.refresh(accountId)
            _uiState.update { current ->
                val items = result.getOrNull()?.filter { it.accountId == accountId } ?: current.items
                val target = current.pushItemId
                val found = target != null && items.any { it.id == target }
                current.copy(items = items, isLoading = false, loadFailed = result.isFailure,
                    loadErrorMessage = result.exceptionOrNull()?.let(::describeInboxError),
                    openedItemId = if (found) target else null,
                    pushErrorMessage = when {
                        target == null || found -> null
                        result.isFailure -> "No pudimos descargar este aviso. Conéctate y pulsa Reintentar."
                        else -> "Este aviso ya no está disponible para tu cuenta."
                    })
            }
        }
    }

    fun openFromPush(itemId: String) {
        _uiState.update { it.copy(pushItemId = itemId, pushErrorMessage = null,
            openedItemId = it.items.firstOrNull { item -> item.id == itemId && item.accountId == accountId }?.id) }
        refresh()
    }

    fun dismissPush() {
        _uiState.update { it.copy(pushItemId = null, openedItemId = null, pushErrorMessage = null) }
    }

    fun markAsRead(itemId: String) {
        if (_uiState.value.readingId != null) return
        scope.launch {
            _uiState.update { it.copy(readingId = itemId, readFailed = false, readErrorMessage = null) }
            val result = getInboxUseCase.markAsRead(accountId, itemId)
            _uiState.update { it.copy(readingId = null, readFailed = result.isFailure,
                readErrorMessage = result.exceptionOrNull()?.let(::describeInboxError)) }
        }
    }

    fun loadArchived() {
        if (_uiState.value.archivedLoading) return
        scope.launch {
            _uiState.update { it.copy(archivedLoading = true, archivedError = null) }
            val result = getInboxUseCase.loadArchived(accountId)
            _uiState.update { state -> state.copy(
                archivedItems = result.getOrNull()?.filter { it.accountId == accountId } ?: state.archivedItems,
                archivedLoading = false,
                archivedError = result.exceptionOrNull()?.let(::describeInboxError)
            ) }
        }
    }

    fun archive(itemId: String) {
        if (_uiState.value.archiving) return
        _uiState.update { it.copy(archiving = true, readErrorMessage = null) }
        scope.launch {
            val result = getInboxUseCase.archive(accountId, itemId)
            _uiState.update { state -> state.copy(
                items = if (result.isSuccess) state.items.filterNot { it.id == itemId } else state.items,
                archiving = false,
                readErrorMessage = result.exceptionOrNull()?.let(::describeInboxError)
            ) }
            if (result.isSuccess) refresh()
        }
    }

    fun onCleared() {
        scope.cancel()
    }
}

private fun describeInboxError(error: Throwable): String = when (error) {
    is AppError.Network -> "Sin conexión a internet. Reintente cuando vuelva la conexión."
    is AppError.Server -> "El servidor no pudo procesar los avisos. Reintente más tarde."
    is AppError.Unauthorized -> "La sesión venció. Inicie sesión de nuevo."
    is AppError.Forbidden -> "Esta cuenta no tiene permiso para confirmar la lectura en el servidor actual."
    else -> "No se pudo actualizar el aviso. Reintente o consulte soporte."
}
