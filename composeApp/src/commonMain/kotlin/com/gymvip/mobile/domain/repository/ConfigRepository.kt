package com.gymvip.mobile.domain.repository

import kotlinx.coroutines.flow.Flow

interface ConfigRepository {
    suspend fun setActiveAccount(accountId: String?)
    val receptionWhatsAppNumber: String?
    suspend fun fetchNoticeBanner(): String?
    suspend fun isFeatureEnabled(featureKey: String, defaultValue: Boolean): Boolean
    
    // Tema y apariencia
    fun getThemePreference(): Flow<String>
    suspend fun setThemePreference(theme: String)
    
    // Opciones de accesibilidad reales
    fun getFontScale(): Flow<Float>
    suspend fun setFontScale(scale: Float)
    
    fun getHighContrast(): Flow<Boolean>
    suspend fun setHighContrast(enabled: Boolean)
    
    fun getReduceMotion(): Flow<Boolean>
    suspend fun setReduceMotion(enabled: Boolean)
    
    // Preferencias de banderas (onboarding, ayudas contextuales, permisos)
    suspend fun getBooleanPreference(key: String, defaultValue: Boolean = false): Boolean
    suspend fun setBooleanPreference(key: String, value: Boolean)
    
    // Semilla/selección de avatar local aislada por cuenta
    suspend fun getAvatarSeed(accountId: String): String
    suspend fun setAvatarSeed(accountId: String, seed: String)
    
    // Restaurar valores predeterminados
    suspend fun resetAllPreferences()
}
