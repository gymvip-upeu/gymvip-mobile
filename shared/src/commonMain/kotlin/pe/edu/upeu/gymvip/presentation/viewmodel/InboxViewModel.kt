package pe.edu.upeu.gymvip.presentation.viewmodel

import pe.edu.upeu.gymvip.domain.model.InboxItem
import pe.edu.upeu.gymvip.domain.usecase.GetInboxUseCase
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

data class InboxUiState(
    val isLoading: Boolean = false,
    val items: List<InboxItem> = emptyList(),
    val unreadCount: Int = 0
)

class InboxViewModel(
    private val getInboxUseCase: GetInboxUseCase,
    private val accountId: String,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    init {
        getInboxUseCase.observe(accountId).onEach { list ->
            val unread = list.count { !it.isRead }
            _uiState.value = _uiState.value.copy(items = list, unreadCount = unread)
        }.launchIn(scope)

        refresh()
    }

    fun refresh() {
        scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            getInboxUseCase.refresh(accountId)
            _uiState.value = _uiState.value.copy(isLoading = false)
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
