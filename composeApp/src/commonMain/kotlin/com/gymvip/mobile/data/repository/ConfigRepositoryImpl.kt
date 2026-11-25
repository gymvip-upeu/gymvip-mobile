package com.gymvip.mobile.data.repository

import com.gymvip.mobile.data.local.GymVipDatabase
import com.gymvip.mobile.domain.repository.ConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConfigRepositoryImpl(private val database: GymVipDatabase) : ConfigRepository {

    override val receptionWhatsAppNumber: String
        get() = com.gymvip.mobile.config.RuntimeSettings.receptionWhatsAppNumber

    private val _themePreference = MutableStateFlow("SYSTEM") // SYSTEM, DARK, LIGHT, DARK_GOLD
    private val _fontScale = MutableStateFlow(1.0f) // 1.0f, 1.15f, 1.3f
    private val _highContrast = MutableStateFlow(false)
    private val _reduceMotion = MutableStateFlow(false)

    private val deviceScope = "__device__"
    private val deviceKeys = setOf("onboarding_completed", "permissions_explainer_completed")
    private var activeAccountId: String? = null
    private val avatarStore = mutableMapOf<String, String>()

    private val localDefaults = mapOf(
        "feature_promotions_highlight" to true,
        "feature_favorites_notes_enabled" to true
    )

    private fun read(scope: String, key: String): String? =
        database.gymVipDatabaseQueries.selectPreference(scope, key).executeAsOneOrNull()

    private fun write(scope: String, key: String, value: String) {
        database.gymVipDatabaseQueries.upsertPreference(scope, key, value)
    }

    override suspend fun setActiveAccount(accountId: String?) {
        val resolved = accountId?.takeIf(String::isNotBlank)
        if (activeAccountId == resolved) return
        activeAccountId = resolved
        _themePreference.value = resolved?.let { read(it, "theme") }
            ?.takeIf { it in setOf("SYSTEM", "DARK", "LIGHT", "DARK_GOLD", "BLUE_DARK", "BLUE_LIGHT") } ?: "SYSTEM"
        _fontScale.value = resolved?.let { read(it, "font_scale") }
            ?.toFloatOrNull()?.coerceIn(0.85f, 1.4f) ?: 1.0f
        _highContrast.value = resolved?.let { read(it, "high_contrast") } == "true"
        _reduceMotion.value = resolved?.let { read(it, "reduce_motion") } == "true"
    }

    override suspend fun fetchNoticeBanner(): String? {
        // Los avisos operativos reales provienen de la bandeja oficial del backend (/api/v1/member-app/inbox).
        // Se evita cualquier banner estático no administrable de feriado.
        val remoteNotice = com.gymvip.mobile.config.SafeRemoteConfig.getString("notice_banner_text", "")
        return if (remoteNotice.isNotBlank()) remoteNotice else null
    }

    override suspend fun isFeatureEnabled(featureKey: String, defaultValue: Boolean): Boolean {
        val remoteValue = com.gymvip.mobile.config.SafeRemoteConfig.getBoolean(featureKey, defaultValue)
        val value = localDefaults[featureKey]
        return if (value is Boolean) value else remoteValue
    }

    override fun getThemePreference(): Flow<String> = _themePreference.asStateFlow()

    override suspend fun setThemePreference(theme: String) {
        if (theme in listOf("SYSTEM", "DARK", "LIGHT", "DARK_GOLD", "BLUE_DARK", "BLUE_LIGHT")) {
            val scope = activeAccountId ?: return
            write(scope, "theme", theme)
            _themePreference.value = theme
        }
    }

    override fun getFontScale(): Flow<Float> = _fontScale.asStateFlow()

    override suspend fun setFontScale(scale: Float) {
        val scope = activeAccountId ?: return
        val valid = scale.coerceIn(0.85f, 1.4f)
        write(scope, "font_scale", valid.toString())
        _fontScale.value = valid
    }

    override fun getHighContrast(): Flow<Boolean> = _highContrast.asStateFlow()

    override suspend fun setHighContrast(enabled: Boolean) {
        val scope = activeAccountId ?: return
        write(scope, "high_contrast", enabled.toString())
        _highContrast.value = enabled
    }

    override fun getReduceMotion(): Flow<Boolean> = _reduceMotion.asStateFlow()

    override suspend fun setReduceMotion(enabled: Boolean) {
        val scope = activeAccountId ?: return
        write(scope, "reduce_motion", enabled.toString())
        _reduceMotion.value = enabled
    }

    override suspend fun getBooleanPreference(key: String, defaultValue: Boolean): Boolean {
        val scope = if (key in deviceKeys) deviceScope else activeAccountId ?: return defaultValue
        return read(scope, key)?.toBooleanStrictOrNull() ?: defaultValue
    }

    override suspend fun setBooleanPreference(key: String, value: Boolean) {
        val scope = if (key in deviceKeys) deviceScope else activeAccountId ?: return
        write(scope, key, value.toString())
    }

    override suspend fun getAvatarSeed(accountId: String): String {
        return avatarStore[accountId] ?: read(accountId, "avatar_seed") ?: accountId.ifBlank { "default_vip" }
    }

    override suspend fun setAvatarSeed(accountId: String, seed: String) {
        write(accountId, "avatar_seed", seed)
        avatarStore[accountId] = seed
    }

    override suspend fun resetAllPreferences() {
        val scope = activeAccountId ?: return
        database.gymVipDatabaseQueries.deletePreferencesByAccount(scope)
        _themePreference.value = "SYSTEM"
        _fontScale.value = 1.0f
        _highContrast.value = false
        _reduceMotion.value = false
    }
}
