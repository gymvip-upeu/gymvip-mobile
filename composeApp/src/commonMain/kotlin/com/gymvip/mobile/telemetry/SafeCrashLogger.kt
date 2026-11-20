package com.gymvip.mobile.telemetry

/**
 * Abstracción de observabilidad y reporte seguro de fallos sin PII (Personally Identifiable Information).
 * Conforme a Ley N° 29733 de Protección de Datos Personales.
 */
interface CrashReporter {
    fun logBreadcrumb(tag: String, message: String)
    fun recordException(throwable: Throwable, context: Map<String, String> = emptyMap())
    fun setCustomKey(key: String, value: String)
}

object SafeCrashLogger : CrashReporter {

    private var activeReporter: CrashReporter? = null

    private val SENSITIVE_PATTERNS = listOf(
        Regex("Bearer\\s+[A-Za-z0-9-_=.]+", RegexOption.IGNORE_CASE) to "Bearer [REDACTED_JWT]",
        Regex("\"password\"\\s*:\\s*\"[^\"]+\"", RegexOption.IGNORE_CASE) to "\"password\":\"[REDACTED]\"",
        Regex("password=[^&\\s]+", RegexOption.IGNORE_CASE) to "password=[REDACTED]",
        Regex("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+") to "[REDACTED_EMAIL]",
        Regex("\\b\\d{8}\\b") to "[REDACTED_DNI]"
    )

    fun initialize(reporter: CrashReporter? = null) {
        this.activeReporter = reporter
    }

    fun sanitize(input: String?): String {
        if (input == null) return ""
        var sanitized: String = input
        for (pair in SENSITIVE_PATTERNS) {
            sanitized = pair.first.replace(sanitized, pair.second)
        }
        return sanitized
    }

    override fun logBreadcrumb(tag: String, message: String) {
        val safeTag = sanitize(tag)
        val safeMessage = sanitize(message)
        println("[SAFE-TELEMETRY] [$safeTag] $safeMessage")
        activeReporter?.logBreadcrumb(safeTag, safeMessage)
    }

    override fun recordException(throwable: Throwable, context: Map<String, String>) {
        val fallbackName = throwable::class.simpleName ?: "UnknownException"
        val safeMessage = sanitize(throwable.message ?: fallbackName)
        val safeContext = context.mapValues { sanitize(it.value) }
        println("[SAFE-TELEMETRY-ERROR] $fallbackName: $safeMessage | Context: $safeContext")
        activeReporter?.recordException(throwable, safeContext)
    }

    override fun setCustomKey(key: String, value: String) {
        val safeKey = sanitize(key)
        val safeValue = sanitize(value)
        activeReporter?.setCustomKey(safeKey, safeValue)
    }
}
