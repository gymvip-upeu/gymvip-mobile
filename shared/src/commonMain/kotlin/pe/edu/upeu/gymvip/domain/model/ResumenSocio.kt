package pe.edu.upeu.gymvip.domain.model

data class RenovacionSocio(val id: String, val plan: String, val inicio: String?, val ultimoDia: String?,
    val importe: MoneyPen?, val pagado: Boolean, val cancelado: Boolean)

/** Una única instantánea del servidor para no mezclar datos de distintas recargas. */
data class ResumenSocio(val perfil: MemberProfile, val membresia: MembershipSummary?,
    val pagos: List<PaymentSummary>, val renovaciones: List<RenovacionSocio>, val actualizadoEn: String)
