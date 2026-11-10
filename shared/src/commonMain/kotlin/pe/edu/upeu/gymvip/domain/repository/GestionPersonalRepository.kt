package pe.edu.upeu.gymvip.domain.repository

import pe.edu.upeu.gymvip.domain.model.*

interface GestionPersonalRepository {
    suspend fun resumen(): Result<ResumenPersonal>
    suspend fun socios(busqueda: String, pagina: Int): Result<PaginaSocios>
    suspend fun ficha(id: String): Result<FichaGestion>
    suspend fun guardarSocio(socio: SocioGestion, nuevo: Boolean): Result<SocioGestion>
    suspend fun asistencia(id: String): Result<String>
    suspend fun planes(): Result<List<PlanGestion>>
    suspend fun guardarPlan(plan: PlanGestion, nuevo: Boolean): Result<Unit>
    suspend fun cuentas(): Result<List<CuentaPersonal>>
}
