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
    val loadFailed: Boolean = false,
    val loadErrorMessage: String? = null,
    val readingId: String? = null,
    val readFailed: Boolean = false,
    val readErrorMessage: String? = null
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
            _uiState.update { it.copy(items = list) }
        }.launchIn(scope)

        refresh()
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = getInboxUseCase.refresh(accountId)
            _uiState.update { it.copy(isLoading = false, loadFailed = result.isFailure,
                loadErrorMessage = result.exceptionOrNull()?.let(::describeInboxError)) }
        }
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
