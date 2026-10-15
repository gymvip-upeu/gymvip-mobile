package com.gymvip.mobile.data.mapper

import com.gymvip.mobile.data.remote.dto.MemberProfileDto
import com.gymvip.mobile.data.remote.dto.MembershipSummaryDto
import com.gymvip.mobile.data.remote.dto.PaymentSummaryDto
import com.gymvip.mobile.domain.model.AccountStatus
import com.gymvip.mobile.domain.model.MembershipStatus
import com.gymvip.mobile.domain.model.PaymentMethod
import kotlin.test.Test
import kotlin.test.assertEquals

class DtoMapperTest {

    @Test
    fun testMemberProfileDtoMapping() {
        val dto = MemberProfileDto(
            id = "c1d2e3f4",
            email = "socio@vipcenterfit.pe",
            firstName = "Carlos",
            lastName = "Mendoza",
            documentType = "DNI",
            documentNumber = "72819201",
            phone = "+51987654321",
            status = "ACTIVO"
        )

        val domain = dto.toDomain("2026-10-15T10:00:00Z")
        assertEquals("c1d2e3f4", domain.accountId)
        assertEquals("Carlos Mendoza", domain.fullName)
        assertEquals(AccountStatus.ACTIVO, domain.status)
    }

    @Test
    fun testMembershipSummaryDtoMapping() {
        val dto = MembershipSummaryDto(
            id = "m1n2o3p4",
            clientId = "c1d2e3f4",
            planName = "Plan VIP Mensual",
            startDate = "2026-10-11",
            endDate = "2026-11-10",
            status = "VIGENTE",
            daysRemaining = 26
        )

        val domain = dto.toDomain("c1d2e3f4", "2026-10-15T10:00:00Z")
        assertEquals("m1n2o3p4", domain.id)
        assertEquals("Plan VIP Mensual", domain.planName)
        assertEquals(MembershipStatus.VIGENTE, domain.status)
        assertEquals(26, domain.daysRemaining)
    }

    @Test
    fun testPaymentSummaryDtoMappingWithMinorUnits() {
        val dto = PaymentSummaryDto(
            id = "pay_001",
            membershipId = "m1n2o3p4",
            amountPen = 120.00,
            paymentMethod = "YAPE",
            externalReference = "OPE-992812",
            paidAt = "2026-10-11T10:35:00Z",
            notes = "Pago presencial en mostrador"
        )

        val domain = dto.toDomain("c1d2e3f4", "2026-10-15T10:00:00Z")
        assertEquals(12000L, domain.amount.cents)
        assertEquals("S/ 120.00", domain.amount.amountInSoles)
        assertEquals(PaymentMethod.YAPE, domain.paymentMethod)
    }
}
