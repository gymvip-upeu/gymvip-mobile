package pe.edu.upeu.gymvip.data.mapper

import pe.edu.upeu.gymvip.data.remote.dto.*
import pe.edu.upeu.gymvip.domain.model.AccountStatus
import pe.edu.upeu.gymvip.domain.model.MembershipStatus
import pe.edu.upeu.gymvip.domain.model.PaymentMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun testMemberWorkspaceDtoMapping() {
        val workspace = MemberWorkspaceDto(
            client = MemberClientDto(
                id = "cli-001",
                memberCode = "VIP-001",
                firstName = "Valeria",
                lastName = "Mendoza",
                email = "valeria@gymvip.local",
                status = "ACTIVO",
                documents = listOf(MemberClientDocumentDto("doc-1", "PE", "DNI", "72819201", true))
            ),
            coverage = MemberCoverageDto(
                clientId = "cli-001",
                planName = "VIP Mensual",
                startDate = "2026-10-01",
                lastIncludedDate = "2026-10-31",
                state = "VIGENTE",
                daysRemaining = 20
            ),
            periods = listOf(
                MemberPeriodDto(id = "per-1", plan_name = "VIP Mensual", duration_days = 30, price_pen = 120.0)
            ),
            payments = listOf(
                MemberPaymentDto(id = "pay-1", period_id = "per-1", amount_pen = 120.0, payment_method = "EFECTIVO", payment_timestamp_utc = "2026-10-01T10:00:00Z")
            )
        )

        val profile = workspace.toProfileDto()
        assertEquals("cli-001", profile.id)
        assertEquals("Valeria", profile.firstName)
        assertEquals("72819201", profile.documentNumber)

        val membership = workspace.toMembershipDto()
        assertEquals("VIP Mensual", membership.planName)
        assertEquals("VIGENTE", membership.status)
        assertEquals(20, membership.daysRemaining)

        val payments = workspace.toPaymentsDto()
        assertEquals(1, payments.size)
        assertEquals(120.0, payments[0].amountPen)
    }
}
