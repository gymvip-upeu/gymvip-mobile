package com.gymvip.mobile.telemetry

import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Implementación de CrashReporter delegada a Firebase Crashlytics en Android.
 * Los datos recibidos ya han sido saneados por SafeCrashLogger para prevenir filtración
 * de tokens JWT, números de documento (DNI), contraseñas y correos electrónicos.
 */
class FirebaseCrashReporter : CrashReporter {

    private val crashlytics: FirebaseCrashlytics by lazy {
        FirebaseCrashlytics.getInstance()
    }

    override fun logBreadcrumb(tag: String, message: String) {
        try {
            crashlytics.log("[$tag] $message")
        } catch (_: Throwable) {
            // Protección contra fallos durante el reporte
        }
    }

    override fun recordException(throwable: Throwable, context: Map<String, String>) {
        try {
            context.forEach { (key, value) ->
                crashlytics.setCustomKey(key, value)
            }
            crashlytics.recordException(throwable)
        } catch (_: Throwable) {
            // Protección contra fallos durante el reporte
        }
    }

    override fun setCustomKey(key: String, value: String) {
        try {
            crashlytics.setCustomKey(key, value)
        } catch (_: Throwable) {
            // Protección contra fallos durante el reporte
        }
    }
}
