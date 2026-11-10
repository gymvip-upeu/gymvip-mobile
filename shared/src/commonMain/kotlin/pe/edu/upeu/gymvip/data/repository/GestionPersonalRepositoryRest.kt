package pe.edu.upeu.gymvip.data.repository

import kotlinx.datetime.LocalDate
import pe.edu.upeu.gymvip.data.local.SesionLocal
import pe.edu.upeu.gymvip.data.remote.api.*
import pe.edu.upeu.gymvip.data.remote.dto.*
import pe.edu.upeu.gymvip.domain.error.AppError
import pe.edu.upeu.gymvip.domain.model.*
import pe.edu.upeu.gymvip.domain.repository.*

class GestionPersonalRepositoryRest(private val api: PersonalApi, private val session: SesionLocal) : GestionPersonalRepository {
    private fun permiso(admin: Boolean = false) {
        val user = (session.estado.value as? AuthState.StaffAuthenticated)?.usuario
            ?: throw AppError.Forbidden("Esta operación requiere una sesión de personal.")
        if (admin && !user.esAdministrador) throw AppError.Forbidden("Esta operación es exclusiva de administrador.")
    }
    override suspend fun resumen() = resultadoLlamada {
        permiso(); ResumenPersonal(api.socios("", 0).totalElements, api.alertas().totalElements)
    }
    override suspend fun socios(busqueda: String, pagina: Int) = resultadoLlamada {
        permiso(); val page = api.socios(busqueda.trim(), pagina.coerceAtLeast(0))
        PaginaSocios(page.content.map { it.dominio() }, page.number, page.totalPages, page.totalElements)
    }
    override suspend fun ficha(id: String) = resultadoLlamada {
        permiso(); val socio = api.socio(id).dominio(); val c = api.cobertura(id)
        val pagos = api.workspace(id).payments.map { PagoGestion(it.payment_timestamp_utc.orEmpty(), it.amount_pen,
            it.payment_method.orEmpty(), it.external_reference.orEmpty()) }
        FichaGestion(socio, CoberturaGestion(c.state, c.planName.orEmpty(), c.lastIncludedDate.orEmpty(), c.daysRemaining ?: 0,
            c.allowed, c.reason.orEmpty()), pagos)
    }
    override suspend fun guardarSocio(socio: SocioGestion, nuevo: Boolean) = resultadoLlamada {
        permiso()
        if (socio.nombres.isBlank() || socio.apellidos.isBlank() || socio.documento.isBlank())
            throw AppError.Validation("Completa nombres, apellidos y documento.")
        if (nuevo && runCatching { LocalDate.parse(socio.nacimiento) }.isFailure)
            throw AppError.Validation("Ingresa la fecha de nacimiento como AAAA-MM-DD.")
        api.guardarSocio(if (nuevo) null else socio.id, GuardarSocioDto(socio.nombres.trim(), socio.apellidos.trim(), socio.pais,
            socio.tipoDocumento, socio.documento.trim(), socio.telefono.trim().ifBlank { null }, socio.correo.trim().ifBlank { null },
            if (nuevo) socio.nacimiento else null, if (nuevo) null else socio.version, if (nuevo) null else socio.notas)).dominio()
    }
    override suspend fun asistencia(id: String) = resultadoLlamada { permiso(); api.asistencia(id) }
    override suspend fun planes() = resultadoLlamada { permiso(); api.planes().map { it.dominio() } }
    override suspend fun guardarPlan(plan: PlanGestion, nuevo: Boolean) = resultadoLlamada {
        permiso(admin = true)
        if (plan.nombre.isBlank() || plan.dias < 1 || !plan.precio.isFinite() || plan.precio <= 0)
            throw AppError.Validation("Completa el nombre, duración y un precio mayor a cero.")
        api.guardarPlan(if (nuevo) null else plan.id, GuardarPlanDto(plan.nombre.trim(), plan.descripcion,
            plan.dias, plan.precio, plan.beneficios, plan.condiciones))
    }
    override suspend fun cuentas() = resultadoLlamada {
        permiso(admin = true); api.cuentas().map { CuentaPersonal(it.id, it.email, Rol.reconocidos(it.roles), it.status) }
    }
}
