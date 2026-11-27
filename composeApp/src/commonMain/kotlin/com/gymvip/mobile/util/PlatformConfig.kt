package com.gymvip.mobile.util

/**
 * Indicador de entorno de compilación (Desarrollo vs Producción / Release).
 * En compilaciones de producción (Release), bloquea selectores de servidor
 * y restringe conexiones estrictamente a endpoints oficiales HTTPS de GYM VIP.
 */
expect val isDevelopmentBuild: Boolean
expect val installedAndroidVersionCode: Int?
