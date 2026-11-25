package com.gymvip.mobile.telemetry

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Adaptador Android de Firebase Analytics. Solo recibe eventos y parámetros sanitizados sin PII.
 */
class FirebaseAnalyticsTracker(context: Context) : AnalyticsTracker {
    private val analytics: FirebaseAnalytics by lazy {
        FirebaseAnalytics.getInstance(context.applicationContext)
    }

    override fun logEvent(name: String, params: Map<String, String>) {
        runCatching {
            val bundle = Bundle().apply {
                params.forEach { (key, value) -> putString(key, value) }
            }
            analytics.logEvent(name, bundle)
        }
    }
}
