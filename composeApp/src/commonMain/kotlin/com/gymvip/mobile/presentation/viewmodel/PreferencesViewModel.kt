package com.gymvip.mobile.presentation.viewmodel

import com.gymvip.mobile.domain.repository.ConfigRepository
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

data class PreferencesUiState(
    val currentTheme: String = "SYSTEM" // SYSTEM, DARK, LIGHT
)

class PreferencesViewModel(
    private val configRepository: ConfigRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow(PreferencesUiState())
    val uiState: StateFlow<PreferencesUiState> = _uiState.asStateFlow()

    init {
        configRepository.getThemePreference().onEach { theme ->
            _uiState.value = _uiState.value.copy(currentTheme = theme)
        }.launchIn(scope)
    }

    fun setTheme(theme: String) {
        scope.launch {
            configRepository.setThemePreference(theme)
        }
    }

    fun onCleared() {
        scope.cancel()
    }
}
