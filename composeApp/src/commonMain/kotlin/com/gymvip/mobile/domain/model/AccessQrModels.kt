package com.gymvip.mobile.domain.model

/**
 * Generador y validador de códigos QR dinámicos para control de acceso.
 * Diseñado para operar offline en torniquetes y en la App móvil (HU-13).
 */
data class DynamicAccessQr(
    val memberCode: String,
    val timestampSeconds: Long,
    val signatureHex: String,
    val payloadUri: String
)

object DynamicAccessQrGenerator {

    private const val VALIDITY_WINDOW_SECONDS = 60L
    private const val SCHEME = "gymvip://access/v2"

    /**
     * Genera un payload URI con sello temporal y firma verificable.
     */
    fun generateToken(memberCode: String, nowEpochSeconds: Long, secretKey: String = "VIP_PASS_2026"): DynamicAccessQr {
        val rawToHash = "$memberCode:$nowEpochSeconds:$secretKey"
        val signature = computeSimpleHash(rawToHash)
        val uri = "$SCHEME/$memberCode?ts=$nowEpochSeconds&sig=$signature"

        return DynamicAccessQr(
            memberCode = memberCode,
            timestampSeconds = nowEpochSeconds,
            signatureHex = signature,
            payloadUri = uri
        )
    }

    /**
     * Valida si un QR dinámico sigue dentro de la ventana de validez.
     */
    fun isTokenValid(tokenTimestampSeconds: Long, currentTimestampSeconds: Long): Boolean {
        val age = currentTimestampSeconds - tokenTimestampSeconds
        return age in -5..VALIDITY_WINDOW_SECONDS
    }

    private fun computeSimpleHash(input: String): String {
        var h = 1125899906842597L
        for (ch in input) {
            h = 31L * h + ch.code
        }
        return (h and 0xFFFFFFFFL).toString(16).padStart(8, '0')
    }
}
