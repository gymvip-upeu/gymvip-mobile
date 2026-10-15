package com.gymvip.mobile.domain

import com.gymvip.mobile.domain.model.MembershipStatus
import com.gymvip.mobile.domain.model.MembershipSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MembershipValidationTest {

    @Test
    fun testMembershipAccessAllowedWhenVigente() {
        val summary = MembershipSummary(
            id = "mem_1",
            accountId = "acc_1",
            planName = "Plan VIP Mensual",
            startDate = "2026-10-15",
            endDate = "2026-11-14",
            status = MembershipStatus.VIGENTE,
            daysRemaining = 30,
            lastSyncedAt = "2026-10-15T10:00:00Z"
        )

        assertTrue(summary.isAccessAllowed)
        assertEquals(30, summary.daysRemaining)
    }

    @Test
    fun testMembershipAccessAllowedWhenProximaAVencer() {
        val summary = MembershipSummary(
            id = "mem_2",
            accountId = "acc_1",
            planName = "Plan VIP Mensual",
            startDate = "2026-09-15",
            endDate = "2026-10-15",
            status = MembershipStatus.PROXIMA_A_VENCER,
            daysRemaining = 2,
            lastSyncedAt = "2026-10-13T10:00:00Z"
        )

        assertTrue(summary.isAccessAllowed)
        assertEquals(2, summary.daysRemaining)
    }

    @Test
    fun testMembershipAccessDeniedWhenVencida() {
        val summary = MembershipSummary(
            id = "mem_3",
            accountId = "acc_1",
            planName = "Plan VIP Mensual",
            startDate = "2026-08-01",
            endDate = "2026-08-31",
            status = MembershipStatus.VENCIDA,
            daysRemaining = 0,
            lastSyncedAt = "2026-10-15T10:00:00Z"
        )

        assertFalse(summary.isAccessAllowed)
        assertEquals(0, summary.daysRemaining)
    }
}
