package com.gymvip.mobile.presentation.screen.membership

import com.gymvip.mobile.domain.model.MembershipStatus
import com.gymvip.mobile.domain.model.MembershipSummary
import kotlin.test.*

class MembershipPresentationTest {
    private val membership = MembershipSummary("id", "account", "Mensual", "2026-11-01", "2026-11-30", MembershipStatus.VIGENTE, 0, "")

    @Test fun durationIncludesTheLastValidDayAndUsesTheRegisteredContract() {
        assertEquals(30, membershipDurationDays(membership))
        assertEquals(90, membershipDurationDays(membership.copy(durationDays = 90)))
        assertEquals(1, membershipDurationDays(membership.copy(endDate = "2026-11-01")))
        assertNull(membershipDurationDays(membership.copy(startDate = "")))
        assertNull(membershipDurationDays(membership.copy(endDate = "2026-10-01")))
    }

    @Test fun dateLabelsUseSpanishAndDoNotInventMissingDates() {
        assertEquals("30 nov 2026", membershipDate("2026-11-30"))
        assertEquals("No registrado", membershipDate(""))
    }
}
