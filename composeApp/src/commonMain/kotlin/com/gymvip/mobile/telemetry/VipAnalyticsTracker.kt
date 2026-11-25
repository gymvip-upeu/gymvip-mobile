package com.gymvip.mobile.telemetry

/**
 * Contrato multiplataforma para analítica de uso sin datos personales identificables (PII).
 */
interface AnalyticsTracker {
    fun logEvent(name: String, params: Map<String, String> = emptyMap())
}

object VipAnalyticsTracker {
    var tracker: AnalyticsTracker? = null

    fun logEvent(name: String, params: Map<String, String> = emptyMap()) {
        val safeName = SafeCrashLogger.sanitize(name).take(40)
        val safeParams = params.mapValues { SafeCrashLogger.sanitize(it.value).take(96) }
        tracker?.logEvent(safeName, safeParams)
    }

    fun logScreenView(screenName: String) {
        logEvent("screen_view", mapOf("screen_name" to screenName))
    }
}
