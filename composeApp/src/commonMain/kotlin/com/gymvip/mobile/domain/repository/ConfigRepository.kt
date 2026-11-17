package com.gymvip.mobile.domain.repository

import kotlinx.coroutines.flow.Flow

interface ConfigRepository {
    val receptionWhatsAppNumber: String?
    suspend fun fetchNoticeBanner(): String?
    suspend fun isFeatureEnabled(featureKey: String, defaultValue: Boolean): Boolean
    fun getThemePreference(): Flow<String>
    suspend fun setThemePreference(theme: String)
}
