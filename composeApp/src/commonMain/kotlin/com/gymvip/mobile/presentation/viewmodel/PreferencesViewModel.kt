package com.gymvip.mobile.presentation.viewmodel

import com.gymvip.mobile.data.remote.api.GymVipHttpClient
import com.gymvip.mobile.domain.repository.ConfigRepository
import com.gymvip.mobile.util.isDevelopmentBuild
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

data class PreferencesUiState(
    val currentTheme: String = "SYSTEM", // SYSTEM, DARK, LIGHT, DARK_GOLD
    val fontScale: Float = 1.0f,
    val highContrast: Boolean = false,
    val reduceMotion: Boolean = false,
    val showGuides: Boolean = true,
    val preferredBranchName: String = "VIP Center Fit — Sede Central",
    val currentServer: String = GymVipHttpClient.baseUrl
)

class PreferencesViewModel(
    private val configRepository: ConfigRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow(PreferencesUiState())
    val uiState: StateFlow<PreferencesUiState> = _uiState.asStateFlow()

    init {
        combine(
            configRepository.getThemePreference(),
            configRepository.getFontScale(),
            configRepository.getHighContrast(),
            configRepository.getReduceMotion(),
            configRepository.getShowGuides()
        ) { theme, fontScale, highContrast, reduceMotion, showGuides ->
            _uiState.value = _uiState.value.copy(
                currentTheme = theme,
                fontScale = fontScale,
                highContrast = highContrast,
                reduceMotion = reduceMotion,
                showGuides = showGuides,
                currentServer = GymVipHttpClient.baseUrl
            )
        }.launchIn(scope)

        scope.launch {
            configRepository.getPreferredBranchName().collect { name ->
                if (!name.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(preferredBranchName = name)
                }
            }
        }
    }

    fun setTheme(theme: String) {
        scope.launch { configRepository.setThemePreference(theme) }
    }

    fun setFontScale(scale: Float) {
        scope.launch { configRepository.setFontScale(scale) }
    }

    fun setHighContrast(enabled: Boolean) {
        scope.launch { configRepository.setHighContrast(enabled) }
    }

    fun setReduceMotion(enabled: Boolean) {
        scope.launch { configRepository.setReduceMotion(enabled) }
    }

    fun setShowGuides(enabled: Boolean) {
        scope.launch { configRepository.setShowGuides(enabled) }
    }

    fun resetGuides() {
        scope.launch { configRepository.resetGuidesAndTutorials() }
    }

    fun resetToDefaults() {
        scope.launch { configRepository.resetAllPreferences() }
    }

    fun setServer(serverUrl: String) {
        if (isDevelopmentBuild) {
            GymVipHttpClient.baseUrl = serverUrl
            _uiState.value = _uiState.value.copy(currentServer = serverUrl)
        }
    }

    fun onCleared() {
        scope.cancel()
    }
}
