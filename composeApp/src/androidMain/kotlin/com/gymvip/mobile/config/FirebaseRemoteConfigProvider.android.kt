package com.gymvip.mobile.config

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.tasks.await

/**
 * Proveedor de Firebase Remote Config para Android.
 * Aplica intervalo de obtención seguro de 3600 segundos (1 hora) en producción
 * para prevenir saturación de cuota y sobrecarga de red en dispositivos de socios.
 */
class FirebaseRemoteConfigProvider : RemoteConfigProvider {

    private val remoteConfig: FirebaseRemoteConfig by lazy {
        val config = FirebaseRemoteConfig.getInstance()
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()
        config.setConfigSettingsAsync(settings)
        // Valores predeterminados seguros locales
        val defaults = mapOf(
            "feature_promotions_highlight" to true,
            "feature_favorites_notes_enabled" to true,
            "feature_gym_explorer" to true,
            "feature_online_payments" to false,
            "feature_training" to true,
            "feature_trainer_booking" to false,
            "feature_group_classes" to true,
            "feature_rewards" to false,
            "feature_offline_workout" to true
        )
        config.setDefaultsAsync(defaults)
        config
    }

    override suspend fun fetchAndActivate(): Boolean {
        return try {
            remoteConfig.fetchAndActivate().await()
        } catch (_: Throwable) {
            false
        }
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return try {
            val value = remoteConfig.getBoolean(key)
            // Si el valor retornado coincide con el default de firebase o no existe, verificar fuente
            val info = remoteConfig.getInfo()
            if (info.lastFetchStatus == FirebaseRemoteConfig.LAST_FETCH_STATUS_SUCCESS) {
                value
            } else {
                defaultValue
            }
        } catch (_: Throwable) {
            defaultValue
        }
    }

    override fun getString(key: String, defaultValue: String): String {
        return try {
            val str = remoteConfig.getString(key)
            if (str.isNotBlank()) str else defaultValue
        } catch (_: Throwable) {
            defaultValue
        }
    }
}
