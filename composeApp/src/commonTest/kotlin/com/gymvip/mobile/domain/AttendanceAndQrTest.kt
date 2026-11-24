package com.gymvip.mobile.domain

import com.gymvip.mobile.domain.model.AttendanceStreakCalculator
import com.gymvip.mobile.domain.model.DynamicAccessQrGenerator
import com.gymvip.mobile.domain.model.StreakBadge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AttendanceAndQrTest {

    @Test
    fun testAttendanceStreakCalculation() {
        val dates = listOf("2026-11-01", "2026-11-02", "2026-11-03", "2026-11-05")
        val streak = AttendanceStreakCalculator.calculateStreak(dates)

        assertEquals(4, streak.monthlyTotalDays)
        assertTrue(streak.currentStreakDays >= 1)
        assertEquals(StreakBadge.BRONCE, streak.badgeLevel)
    }

    @Test
    fun testAttendanceStreakGoldBadge() {
        val dates = (1..15).map { "2026-11-${it.toString().padStart(2, '0')}" }
        val streak = AttendanceStreakCalculator.calculateStreak(dates)

        assertEquals(15, streak.monthlyTotalDays)
        assertEquals(StreakBadge.ORO, streak.badgeLevel)
        assertEquals(15f / 16f, streak.goalCompletionRatio)
    }

    @Test
    fun testDynamicAccessQrGenerationAndValidity() {
        val memberCode = "MAR-001234"
        val now = 1795000000L

        val qr = DynamicAccessQrGenerator.generateToken(memberCode, now)
        assertTrue(qr.payloadUri.startsWith("gymvip://access/v2/MAR-001234"))
        assertTrue(qr.payloadUri.contains("sig="))

        // Válido dentro de 30 segundos
        assertTrue(DynamicAccessQrGenerator.isTokenValid(now, now + 30))

        // Válido a los 59 segundos
        assertTrue(DynamicAccessQrGenerator.isTokenValid(now, now + 59))

        // Expirado a los 65 segundos
        assertFalse(DynamicAccessQrGenerator.isTokenValid(now, now + 65))
    }
}
