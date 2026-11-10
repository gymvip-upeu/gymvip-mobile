package pe.edu.upeu.gymvip.domain.model

import kotlinx.datetime.*
import pe.edu.upeu.gymvip.AppConfig

private val meses = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
val zonaSocio: TimeZone get() = TimeZone.of(AppConfig.ZONA_HORARIA)
fun fechaCortaSocio(valor: String?): String {
    val fecha = valor?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return "Sin fecha registrada"
    return "${fecha.dayOfMonth} ${meses[fecha.monthNumber - 1]} ${fecha.year}"
}
fun instantePago(valor: String): Instant? = runCatching { Instant.parse(valor) }.getOrNull()
    ?: runCatching { LocalDateTime.parse(valor).toInstant(TimeZone.UTC) }.getOrNull()
fun fechaPagoSocio(valor: String): String {
    val local = instantePago(valor)?.toLocalDateTime(zonaSocio) ?: return "Fecha no disponible"
    return fechaCortaSocio(local.date.toString()) + " · " + local.hour.toString().padStart(2, '0') + ":" + local.minute.toString().padStart(2, '0')
}
fun fechaAvisoSocio(valor: String, ahora: Instant = Clock.System.now()): String {
    val local = instantePago(valor)?.toLocalDateTime(zonaSocio) ?: return "Fecha no disponible"
    val hoy = ahora.toLocalDateTime(zonaSocio).date
    val dia = when (local.date) {
        hoy -> "Hoy"
        hoy.minus(1, DateTimeUnit.DAY) -> "Ayer"
        else -> "${local.dayOfMonth} ${meses[local.monthNumber - 1]}" + if (local.year != hoy.year) " ${local.year}" else ""
    }
    return dia + " · " + local.hour.toString().padStart(2, '0') + ":" + local.minute.toString().padStart(2, '0')
}
fun saludoSocio(ahora: Instant = Clock.System.now()): String = when (ahora.toLocalDateTime(zonaSocio).hour) {
    in 5..11 -> "BUENOS DÍAS"
    in 12..18 -> "BUENAS TARDES"
    else -> "BUENAS NOCHES"
}
fun tiempoSincronizacion(instante: String, ahora: Instant = Clock.System.now()): String {
    val anterior = runCatching { Instant.parse(instante) }.getOrNull() ?: return "Fecha no disponible"
    val minutos = ((ahora - anterior).inWholeMinutes).coerceAtLeast(0)
    return when {
        minutos < 1 -> "Ahora"
        minutos < 60 -> "Hace $minutos min"
        minutos < 1440 -> "Hace ${minutos / 60} h"
        else -> "Hace ${minutos / 1440} días"
    }
}
fun PaymentMethod.etiqueta(): String = when (this) {
    PaymentMethod.YAPE -> "Yape"
    PaymentMethod.PLIN -> "Plin"
    PaymentMethod.EFECTIVO -> "Efectivo"
    PaymentMethod.TRANSFERENCIA -> "Transferencia"
    PaymentMethod.TARJETA -> "Tarjeta"
    PaymentMethod.OTRO -> "Otro medio"
}
