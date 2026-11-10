package pe.edu.upeu.gymvip.domain.model

data class SocioGestion(val id: String, val codigo: String, val nombres: String, val apellidos: String,
    val nacimiento: String, val telefono: String, val correo: String, val estado: String,
    val pais: String, val tipoDocumento: String, val documento: String, val version: Long = 0,
    val notas: String = "") { val nombreCompleto get() = "$nombres $apellidos".trim() }
data class PaginaSocios(val socios: List<SocioGestion>, val pagina: Int, val totalPaginas: Int, val total: Long)
data class PlanGestion(val id: String, val nombre: String, val descripcion: String, val dias: Int,
    val precio: Double, val beneficios: String, val condiciones: String, val estado: String)
data class CuentaPersonal(val id: String, val correo: String, val roles: Set<Rol>, val estado: String)
data class CoberturaGestion(val estado: String, val plan: String, val ultimoDia: String,
    val diasRestantes: Long, val permiteAsistencia: Boolean, val motivo: String)
data class PagoGestion(val fecha: String, val importe: Double, val medio: String, val referencia: String)
data class FichaGestion(val socio: SocioGestion, val cobertura: CoberturaGestion, val pagos: List<PagoGestion>)
data class ResumenPersonal(val socios: Long, val alertas: Long)
