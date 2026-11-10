package com.gymvip.mobile.presentation.viewmodel

import com.gymvip.mobile.data.remote.api.GymVipHttpClient
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
    val currentTheme: String = "SYSTEM", // SYSTEM, DARK, LIGHT
    val currentServer: String = GymVipHttpClient.baseUrl
)

class PreferencesViewModel(
    private val configRepository: ConfigRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow(PreferencesUiState())
    val uiState: StateFlow<PreferencesUiState> = _uiState.asStateFlow()

    init {
        configRepository.getThemePreference().onEach { theme ->
            _uiState.value = _uiState.value.copy(currentTheme = theme, currentServer = GymVipHttpClient.baseUrl)
        }.launchIn(scope)
    }

    fun setTheme(theme: String) {
        scope.launch {
            configRepository.setThemePreference(theme)
        }
    }

    fun setServer(serverUrl: String) {
        GymVipHttpClient.baseUrl = serverUrl
        _uiState.value = _uiState.value.copy(currentServer = serverUrl)
    }

    fun onCleared() {
        scope.cancel()
    }
}
