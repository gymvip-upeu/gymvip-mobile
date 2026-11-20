package com.gymvip.mobile.telemetry

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.HttpMetric

/**
 * Adaptador de Firebase Performance Monitoring para Android.
 * Instrumenta las peticiones Ktor mediante HttpMetric oficial de Firebase.
 */
class FirebaseNetworkTracker : NetworkPerformanceTracker {

    override fun startTrace(url: String, httpMethod: String): NetworkTraceHandle {
        return try {
            val metric = FirebasePerformance.getInstance().newHttpMetric(url, httpMethod)
            metric.start()
            FirebaseTraceHandle(metric)
        } catch (_: Throwable) {
            NoOpTraceHandle
        }
    }

    private class FirebaseTraceHandle(private val metric: HttpMetric) : NetworkTraceHandle {
        private var isStopped = false

        override fun stop(httpStatusCode: Int, responseSizeBytes: Long) {
            if (!isStopped) {
                isStopped = true
                try {
                    metric.setHttpResponseCode(httpStatusCode)
                    if (responseSizeBytes > 0) {
                        metric.setResponsePayloadSize(responseSizeBytes)
                    }
                    metric.stop()
                } catch (_: Throwable) {}
            }
        }

        override fun recordError(throwable: Throwable) {
            if (!isStopped) {
                isStopped = true
                try {
                    metric.setHttpResponseCode(0)
                    metric.stop()
                } catch (_: Throwable) {}
            }
        }
    }

    private object NoOpTraceHandle : NetworkTraceHandle {
        override fun stop(httpStatusCode: Int, responseSizeBytes: Long) {}
        override fun recordError(throwable: Throwable) {}
    }
}
