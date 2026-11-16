package com.gymvip.mobile.presentation.viewmodel

import com.gymvip.mobile.domain.model.InboxItem
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
    val loadFailed: Boolean = false
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
            _uiState.update { it.copy(isLoading = false, loadFailed = result.isFailure) }
        }
    }

    fun markAsRead(itemId: String) {
        scope.launch {
            getInboxUseCase.markAsRead(accountId, itemId)
        }
    }

    fun onCleared() {
        scope.cancel()
    }
}
