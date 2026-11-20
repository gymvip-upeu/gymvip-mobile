package com.gymvip.mobile.telemetry

/**
 * Abstracción de seguimiento de rendimiento de red para llamadas HTTP.
 * Permite registrar latencias, códigos de respuesta y tamaños de transferencia
 * en Firebase Performance Monitoring de forma desacoplada del motor Ktor.
 */
interface NetworkTraceHandle {
    fun stop(httpStatusCode: Int, responseSizeBytes: Long = 0)
    fun recordError(throwable: Throwable)
}

interface NetworkPerformanceTracker {
    fun startTrace(url: String, httpMethod: String): NetworkTraceHandle
}

object GlobalNetworkTracker {
    var tracker: NetworkPerformanceTracker? = null

    fun startTrace(url: String, httpMethod: String): NetworkTraceHandle {
        return tracker?.startTrace(url, httpMethod) ?: NoOpTraceHandle
    }

    private object NoOpTraceHandle : NetworkTraceHandle {
        override fun stop(httpStatusCode: Int, responseSizeBytes: Long) {}
        override fun recordError(throwable: Throwable) {}
    }
}
