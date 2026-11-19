package com.gymvip.mobile.presentation.viewmodel

import com.gymvip.mobile.domain.model.Faq
import com.gymvip.mobile.domain.usecase.GetFaqsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FaqUiState(
    val items: List<Faq> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false
)

class FaqViewModel(
    private val getFaqs: GetFaqsUseCase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    private val state = MutableStateFlow(FaqUiState())
    val uiState = state.asStateFlow()
    private var refreshJob: Job? = null

    init {
        getFaqs.observe().onEach { items -> state.update { it.copy(items = items) } }.launchIn(scope)
        refresh()
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = scope.launch {
            state.update { it.copy(isLoading = true) }
            val result = getFaqs.refresh()
            state.update { it.copy(isLoading = false, loadFailed = result.isFailure) }
        }
    }

    fun onCleared() {
        scope.cancel()
    }
}
