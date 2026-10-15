package com.gymvip.mobile.data.repository

import com.gymvip.mobile.domain.repository.ConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConfigRepositoryImpl : ConfigRepository {

    private val _themePreference = MutableStateFlow("SYSTEM") // SYSTEM, DARK, LIGHT

    // Defaults locales informativos (sin impacto financiero ni de permisos)
    private val localDefaults = mapOf(
        "notice_banner_text" to "Horario especial por feriado: atención de 7:00 a.m. a 2:00 p.m.",
        "feature_promotions_highlight" to true,
        "feature_favorites_notes_enabled" to true
    )

    override suspend fun fetchNoticeBanner(): String? {
        // En ausencia de google-services.json o conectividad remota Firebase, se usa fallback local seguro
        return localDefaults["notice_banner_text"] as? String
    }

    override suspend fun isFeatureEnabled(featureKey: String, defaultValue: Boolean): Boolean {
        val value = localDefaults[featureKey]
        return if (value is Boolean) value else defaultValue
    }

    override fun getThemePreference(): Flow<String> = _themePreference.asStateFlow()

    override suspend fun setThemePreference(theme: String) {
        if (theme in listOf("SYSTEM", "DARK", "LIGHT")) {
            _themePreference.value = theme
        }
    }
}
