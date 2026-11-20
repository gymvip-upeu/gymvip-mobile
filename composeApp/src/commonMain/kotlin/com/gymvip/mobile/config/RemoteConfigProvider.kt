package com.gymvip.mobile.config

/**
 * Abstracción de configuración remota segura.
 * Nunca almacena secretos, contraseñas, tokens ni reglas de autorización de acceso.
 * Permite flags de interfaz (e.g. promociones destacadas) con valores por defecto seguros.
 */
interface RemoteConfigProvider {
    suspend fun fetchAndActivate(): Boolean
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun getString(key: String, defaultValue: String): String
}

object SafeRemoteConfig {
    var provider: RemoteConfigProvider? = null

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return provider?.getBoolean(key, defaultValue) ?: defaultValue
    }

    fun getString(key: String, defaultValue: String): String {
        return provider?.getString(key, defaultValue) ?: defaultValue
    }
}
