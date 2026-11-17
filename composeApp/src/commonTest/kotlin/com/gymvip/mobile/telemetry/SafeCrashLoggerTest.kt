package com.gymvip.mobile.telemetry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SafeCrashLoggerTest {

    @Test
    fun testSanitizeRedactsSensitiveTokensAndPasswords() {
        val raw = "Error al autenticar: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0 y {\"password\":\"MiClaveSecreta123!\"}"
        val sanitized = SafeCrashLogger.sanitize(raw)

        assertFalse(sanitized.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"))
        assertFalse(sanitized.contains("MiClaveSecreta123!"))
        assertTrue(sanitized.contains("[REDACTED_JWT]"))
        assertTrue(sanitized.contains("[REDACTED]"))
    }

    @Test
    fun testSanitizeRedactsPiiEmailsAndDni() {
        val raw = "Fallo en socio@vip.pe con documento 72345678 en la operación"
        val sanitized = SafeCrashLogger.sanitize(raw)

        assertFalse(sanitized.contains("socio@vip.pe"))
        assertFalse(sanitized.contains("72345678"))
        assertTrue(sanitized.contains("[REDACTED_EMAIL]"))
        assertTrue(sanitized.contains("[REDACTED_DNI]"))
    }

    @Test
    fun testMockReporterReceivesSanitizedBreadcrumbs() {
        var recordedTag = ""
        var recordedMessage = ""

        val mockReporter = object : CrashReporter {
            override fun logBreadcrumb(tag: String, message: String) {
                recordedTag = tag
                recordedMessage = message
            }
            override fun recordException(throwable: Throwable, context: Map<String, String>) {}
            override fun setCustomKey(key: String, value: String) {}
        }

        SafeCrashLogger.initialize(mockReporter)
        SafeCrashLogger.logBreadcrumb("AUTH", "Login fallido para admin@gymvip.pe con password=SuperSecret456!")

        assertEquals("AUTH", recordedTag)
        assertTrue(recordedMessage.contains("[REDACTED_EMAIL]"))
        assertTrue(recordedMessage.contains("password=[REDACTED]"))
        assertFalse(recordedMessage.contains("admin@gymvip.pe"))
        assertFalse(recordedMessage.contains("SuperSecret456!"))
    }
}
