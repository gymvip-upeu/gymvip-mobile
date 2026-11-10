package pe.edu.upeu.gymvip.data.mapper

import kotlinx.datetime.*
import pe.edu.upeu.gymvip.data.remote.dto.*
import pe.edu.upeu.gymvip.domain.error.AppError
import pe.edu.upeu.gymvip.domain.model.*

fun estadoMembresia(valor: String?): MembershipStatus = when (valor?.uppercase()) {
    "VIGENTE" -> MembershipStatus.VIGENTE
    "POR_VENCER", "PROXIMA_A_VENCER" -> MembershipStatus.PROXIMA_A_VENCER
    "VENCIDO", "VENCIDA" -> MembershipStatus.VENCIDA
    "CANCELADA" -> MembershipStatus.CANCELADA
    "PENDIENTE" -> MembershipStatus.PENDIENTE
    "SIN_MEMBRESIA" -> MembershipStatus.SIN_MEMBRESIA
    else -> MembershipStatus.DESCONOCIDO
}

private fun fecha(valor: String?): LocalDate? = valor?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
private fun incluido(finExclusivo: String?): String? = fecha(finExclusivo)?.minus(DatePeriod(days = 1))?.toString()

fun MemberWorkspaceDto.resumenSocio(account: String, ahora: Instant): ResumenSocio {
    if (client?.id != account || (coverage?.clientId != null && coverage.clientId != account))
        throw AppError.Forbidden("El servidor devolvió información de otra cuenta.")
    val c = coverage ?: throw AppError.Server(502, "No se recibió el estado de membresía del servidor.")
    val perfil = toProfileDto().toDomain(ahora.toString())
    val estado = estadoMembresia(c.state)
    val ultimoDia = c.lastIncludedDate ?: incluido(c.endExclusive)
    // La cobertura puede unir varias renovaciones. Solo tomar condiciones de un periodo pagado
    // cuyo fin coincide con el bloque elegido por el servidor, nunca del primero de una lista.
    val periodo = periods.filter { it.paid && it.cancelled_at_utc == null && it.end_date == c.endExclusive }
        .maxByOrNull { it.start_date.orEmpty() }
    val restantes = c.daysRemaining?.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())?.toInt()
        ?: fecha(c.endExclusive)?.let { ahora.toLocalDateTime(zonaSocio).date.daysUntil(it) } ?: 0
    val membresia = if (estado == MembershipStatus.SIN_MEMBRESIA) null else MembershipSummary(
        id = periodo?.id ?: "coverage-$account", accountId = account, planName = c.planName ?: "Plan no informado",
        startDate = c.startDate.orEmpty(), endDate = ultimoDia.orEmpty(), status = estado, daysRemaining = restantes,
        lastSyncedAt = ahora.toString(), durationDays = periodo?.duration_days,
        benefits = periodo?.benefits, conditions = periodo?.conditions, backendReason = c.reason
    )
    if (payments.map { it.id }.distinct().size != payments.size || payments.any { !it.amount_pen.isFinite() || it.amount_pen < 0 })
        throw AppError.Server(502, "El servidor devolvió pagos inconsistentes.")
    val pagos = payments.map { p ->
        val medio = when (p.payment_method.uppercase()) {
            "TRANSFERENCIA", "TRANSFERENCIA_BANCARIA", "TRANSFER", "BANK_TRANSFER" -> PaymentMethod.TRANSFERENCIA
            else -> PaymentMethod.entries.firstOrNull { it.name == p.payment_method.uppercase() } ?: PaymentMethod.OTRO
        }
        PaymentSummary(p.id, account, p.period_id, MoneyPen.fromDouble(p.amount_pen), medio,
            p.external_reference, p.payment_timestamp_utc.orEmpty(), p.notes, ahora.toString(),
            p.plan_name, p.status, p.payment_method)
    }.sortedWith(compareByDescending<PaymentSummary> { instantePago(it.paidAt)?.epochSeconds ?: Long.MIN_VALUE }.thenByDescending { it.id })
    val renovaciones = periods.sortedByDescending { it.start_date.orEmpty() }.map {
        RenovacionSocio(it.id, it.plan_name ?: "Plan no informado", it.start_date, incluido(it.end_date),
            it.price_pen?.let(MoneyPen::fromDouble), it.paid, it.cancelled_at_utc != null)
    }
    return ResumenSocio(perfil, membresia, pagos, renovaciones, ahora.toString())
}
