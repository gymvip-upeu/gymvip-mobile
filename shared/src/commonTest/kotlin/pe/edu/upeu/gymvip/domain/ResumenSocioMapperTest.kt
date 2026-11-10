package pe.edu.upeu.gymvip.domain

import kotlinx.datetime.Instant
import pe.edu.upeu.gymvip.data.mapper.*
import pe.edu.upeu.gymvip.data.remote.dto.*
import pe.edu.upeu.gymvip.domain.error.AppError
import pe.edu.upeu.gymvip.domain.model.*
import kotlin.test.*

class ResumenSocioMapperTest {
    private val ahora = Instant.parse("2026-11-09T12:00:00Z")
    private fun workspace(estado: String = "VIGENTE", dias: Long? = 21) = MemberWorkspaceDto(
        client = MemberClientDto("socio", firstName = "Valeria", lastName = "Rojas"),
        coverage = MemberCoverageDto(clientId = "socio", state = estado, planName = "VIP Mensual",
            startDate = "2026-10-31", endExclusive = "2026-11-30", daysRemaining = dias),
        periods = listOf(MemberPeriodDto("pagado", "VIP Mensual", 30, "2026-10-31", "2026-11-30", 99.9,
            "Acceso a sala", "Pago presencial", paid = true)))

    @Test fun reconoceEstadosRealesSinTratarDesconocidosComoActivos() {
        assertEquals(MembershipStatus.PROXIMA_A_VENCER, estadoMembresia("POR_VENCER"))
        assertEquals(MembershipStatus.VENCIDA, estadoMembresia("VENCIDO"))
        assertEquals(MembershipStatus.CANCELADA, estadoMembresia("CANCELADA"))
        assertEquals(MembershipStatus.PENDIENTE, estadoMembresia("PENDIENTE"))
        assertEquals(MembershipStatus.DESCONOCIDO, estadoMembresia("OTRO"))
    }
    @Test fun convierteFinExclusivoYNoTomaCondicionesDePeriodoFuturoSinPagar() {
        val source = workspace()
        val extra = MemberPeriodDto("futuro", "Plan nuevo", 365, "2027-01-01", "2028-01-01", benefits = "No corresponde")
        val result = source.copy(periods = listOf(extra) + source.periods).resumenSocio("socio", ahora)
        assertEquals("2026-11-29", result.membresia!!.endDate)
        assertEquals(30, result.membresia!!.durationDays)
        assertEquals("Acceso a sala", result.membresia!!.benefits)
        assertEquals("2027-12-31", result.renovaciones.first().ultimoDia)
    }
    @Test fun backendEsAutoridadDeDiasYEstado() {
        val result = workspace("POR_VENCER", 5).resumenSocio("socio", Instant.parse("2030-01-01T00:00:00Z"))
        assertEquals(5, result.membresia!!.daysRemaining)
        assertEquals(MembershipStatus.PROXIMA_A_VENCER, result.membresia!!.status)
    }
    @Test fun ultimoDiaCompletoDeLimaYFallbackSoloCuandoFaltanDias() {
        val source = workspace(dias = null).let { it.copy(coverage = it.coverage!!.copy(endExclusive = "2026-11-09")) }
        assertEquals(1, source.resumenSocio("socio", Instant.parse("2026-11-09T04:59:59Z")).membresia!!.daysRemaining)
        assertEquals(0, source.resumenSocio("socio", Instant.parse("2026-11-09T05:00:00Z")).membresia!!.daysRemaining)
        assertEquals("2026-11-08", source.resumenSocio("socio", ahora).membresia!!.endDate)
    }
    @Test fun sinCoberturaNoInventaMembresiaAunqueExistanPeriodos() {
        assertNull(workspace("SIN_MEMBRESIA").resumenSocio("socio", ahora).membresia)
    }
    @Test fun pagosOrdenadosYTransferenciaSeConservan() {
        val result = workspace("VENCIDO", -1).copy(payments = listOf(
            MemberPaymentDto("viejo", plan_name = "VIP Mensual", amount_pen = 99.9, payment_method = "YAPE", payment_timestamp_utc = "2026-10-01T13:00:00"),
            MemberPaymentDto("nuevo", plan_name = "VIP Mensual", amount_pen = 189.9, payment_method = "TRANSFERENCIA", notes = "Referencia en recepción",
                payment_timestamp_utc = "2026-11-09T02:20:00", status = "REEMPLAZADO")
        )).resumenSocio("socio", ahora)
        assertEquals(listOf("nuevo", "viejo"), result.pagos.map { it.id })
        assertEquals(PaymentMethod.TRANSFERENCIA, result.pagos[0].paymentMethod)
        assertEquals("S/ 189.90", result.pagos[0].amount.amountInSoles)
        assertEquals("8 nov 2026 · 21:20", fechaPagoSocio(result.pagos[0].paidAt))
        assertEquals("VIP Mensual", result.pagos[0].planName)
        assertEquals("REEMPLAZADO", result.pagos[0].state)
        assertFalse(result.membresia!!.isAccessAllowed)
        assertEquals(2, result.pagos.size)
    }
    @Test fun rechazaPerfilYCoberturaDeOtroSocio() {
        assertFailsWith<AppError.Forbidden> { workspace().resumenSocio("otro", ahora) }
        assertFailsWith<AppError.Forbidden> { workspace().copy(coverage = workspace().coverage!!.copy(clientId = "otro")).resumenSocio("socio", ahora) }
    }
    @Test fun periodosCanceladosNoAportanCondicionesVigentes() {
        val source = workspace("CANCELADA").let { it.copy(periods = it.periods.map { p -> p.copy(cancelled_at_utc = "2026-11-08T12:00:00") }) }
        val result = source.resumenSocio("socio", ahora)
        assertEquals(MembershipStatus.CANCELADA, result.membresia!!.status)
        assertNull(result.membresia!!.benefits)
        assertTrue(result.renovaciones[0].cancelado)
    }
    @Test fun fechasYSaludoNoDependenDeZonaDelDispositivo() {
        assertEquals("BUENAS NOCHES", saludoSocio(Instant.parse("2026-11-09T04:59:00Z")))
        assertEquals("BUENOS DÍAS", saludoSocio(Instant.parse("2026-11-09T12:00:00Z")))
        assertEquals("29 feb 2024", fechaCortaSocio("2024-02-29"))
        assertEquals("Hace 2 min", tiempoSincronizacion("2026-11-09T11:58:00Z", ahora))
    }
}
