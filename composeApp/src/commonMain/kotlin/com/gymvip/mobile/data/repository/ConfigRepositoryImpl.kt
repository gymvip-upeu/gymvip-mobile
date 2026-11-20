package com.gymvip.mobile.data.repository

import com.gymvip.mobile.domain.repository.ConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConfigRepositoryImpl : ConfigRepository {

    override val receptionWhatsAppNumber: String get() = com.gymvip.mobile.config.RuntimeSettings.receptionWhatsAppNumber

    private val _themePreference = MutableStateFlow("SYSTEM") // SYSTEM, DARK, LIGHT, DARK_GOLD
    private val _fontScale = MutableStateFlow(1.0f) // 1.0f, 1.15f, 1.3f
    private val _highContrast = MutableStateFlow(false)
    private val _reduceMotion = MutableStateFlow(false)

    private val booleanStore = mutableMapOf<String, Boolean>()
    private val avatarStore = mutableMapOf<String, String>()

    private val localDefaults = mapOf(
        "feature_promotions_highlight" to true,
        "feature_favorites_notes_enabled" to true
    )

    override suspend fun fetchNoticeBanner(): String? {
        // Los avisos operativos reales provienen de la bandeja oficial del backend (/api/v1/member-app/inbox).
        // Se evita cualquier banner estático no administrable de feriado.
        val remoteNotice = com.gymvip.mobile.config.SafeRemoteConfig.getString("notice_banner_text", "")
        return if (remoteNotice.isNotBlank()) remoteNotice else null
    }

    override suspend fun isFeatureEnabled(featureKey: String, defaultValue: Boolean): Boolean {
        val remoteValue = com.gymvip.mobile.config.SafeRemoteConfig.getBoolean(featureKey, defaultValue)
        val value = booleanStore[featureKey] ?: localDefaults[featureKey]
        return if (value is Boolean) value else remoteValue
    }

    override fun getThemePreference(): Flow<String> = _themePreference.asStateFlow()

    override suspend fun setThemePreference(theme: String) {
        if (theme in listOf("SYSTEM", "DARK", "LIGHT", "DARK_GOLD", "BLUE_DARK", "BLUE_LIGHT")) {
            _themePreference.value = theme
        }
    }

    override fun getFontScale(): Flow<Float> = _fontScale.asStateFlow()

    override suspend fun setFontScale(scale: Float) {
        _fontScale.value = scale.coerceIn(0.85f, 1.4f)
    }

    override fun getHighContrast(): Flow<Boolean> = _highContrast.asStateFlow()

    override suspend fun setHighContrast(enabled: Boolean) {
        _highContrast.value = enabled
    }

    override fun getReduceMotion(): Flow<Boolean> = _reduceMotion.asStateFlow()

    override suspend fun setReduceMotion(enabled: Boolean) {
        _reduceMotion.value = enabled
    }

    override suspend fun getBooleanPreference(key: String, defaultValue: Boolean): Boolean {
        return booleanStore[key] ?: defaultValue
    }

    override suspend fun setBooleanPreference(key: String, value: Boolean) {
        booleanStore[key] = value
    }

    override suspend fun getAvatarSeed(accountId: String): String {
        return avatarStore[accountId] ?: accountId.ifBlank { "default_vip" }
    }

    override suspend fun setAvatarSeed(accountId: String, seed: String) {
        avatarStore[accountId] = seed
    }

    override suspend fun resetAllPreferences() {
        _themePreference.value = "SYSTEM"
        _fontScale.value = 1.0f
        _highContrast.value = false
        _reduceMotion.value = false
        booleanStore.clear()
    }
}
