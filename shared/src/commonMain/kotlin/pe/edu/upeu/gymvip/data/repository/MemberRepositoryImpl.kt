package pe.edu.upeu.gymvip.data.repository

import pe.edu.upeu.gymvip.data.local.GymVipDatabase
import pe.edu.upeu.gymvip.data.local.SesionLocal
import pe.edu.upeu.gymvip.data.mapper.resumenSocio
import pe.edu.upeu.gymvip.data.remote.api.*
import pe.edu.upeu.gymvip.domain.error.AppError
import pe.edu.upeu.gymvip.domain.model.*
import pe.edu.upeu.gymvip.domain.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

class MemberRepositoryImpl(private val memberApi: MemberApi, private val database: GymVipDatabase,
    private val session: SesionLocal, private val clock: Clock = Clock.System) : MemberRepository {
    private val refreshMutex = Mutex()
    override fun observeSummary(accountId: String): Flow<ResumenSocio?> =
        combine(session.resumenSocio, session.estado) { data, state ->
            if ((state as? AuthState.Authenticated)?.profile?.accountId == accountId && data?.perfil?.accountId == accountId) data else null
        }.distinctUntilChanged()
    override fun observeProfile(accountId: String): Flow<MemberProfile?> =
        combine(observeSummary(accountId), session.estado) { data, state ->
            data?.perfil ?: (state as? AuthState.Authenticated)?.profile?.takeIf { it.accountId == accountId }
        }.distinctUntilChanged()
    override fun observeMembership(accountId: String) = observeSummary(accountId).map { it?.membresia }
    override fun observePayments(accountId: String) = observeSummary(accountId).map { it?.pagos.orEmpty() }

    override suspend fun refreshSummary(accountId: String): Result<ResumenSocio> = refreshMutex.withLock {
        var tokenInicial: String? = null
        val result = resultadoLlamada {
            session.comoSocio(accountId) {}
            val token = session.store.getAccessToken() ?: throw AppError.Unauthorized("Tu sesión expiró.")
            tokenInicial = token
            val dto = memberApi.getWorkspace().getOrThrow()
            val data = dto.resumenSocio(accountId, clock.now())
            if (data.perfil.status != AccountStatus.ACTIVO) throw AppError.Forbidden("Tu cuenta no está habilitada. Consulta en recepción.")
            session.comoSocio(accountId, token) {
                // Confirmar propietario y token también después de la red; logout no puede repoblar la caché.
                database.gymVipDatabaseQueries.transaction {
                    val q = database.gymVipDatabaseQueries
                    val p = data.perfil
                    q.insertOrReplaceProfile(p.accountId, p.email, p.firstName, p.lastName, p.documentType,
                        p.documentNumber, p.phone, p.status.name, p.lastSyncedAt)
                    q.deleteMembershipByAccountId(accountId)
                    data.membresia?.let { m ->
                        q.insertOrReplaceMembership(m.id, accountId, m.planName, m.startDate, m.endDate,
                            m.status.name, m.daysRemaining.toLong(), m.lastSyncedAt)
                    }
                    q.deletePaymentsByAccountId(accountId)
                    data.pagos.forEach { payment ->
                        q.insertOrReplacePayment(payment.id, accountId, payment.membershipId, payment.amount.cents,
                            payment.paymentMethod.name, payment.externalReference, payment.paidAt, payment.notes, payment.lastSyncedAt)
                    }
                }
                session.resumenSocio.value = data
            }
            data
        }
        // Forbidden de otra cuenta/rol no debe cerrar la sesión de personal desde una llamada ajena.
        if (result.exceptionOrNull() is AppError.Forbidden &&
            tokenInicial != null && session.store.getAccessToken() == tokenInicial &&
            (session.estado.value as? AuthState.Authenticated)?.profile?.accountId == accountId) {
            session.cerrar("No tienes acceso a esta información. Inicia sesión de nuevo.")
        }
        result
    }
    override suspend fun refreshProfile(accountId: String) = refreshSummary(accountId).map { it.perfil }
    override suspend fun takeExpiryNotice(accountId: String): Boolean = session.comoSocio(accountId) {
        val estado = session.resumenSocio.value?.membresia?.status
        if (estado !in setOf(MembershipStatus.PROXIMA_A_VENCER, MembershipStatus.VENCIDA) || session.avisoVencimientoMostrado) false
        else { session.avisoVencimientoMostrado = true; true }
    }
    override suspend fun refreshMembership(accountId: String) = refreshSummary(accountId).map { it.membresia }
    override suspend fun refreshPayments(accountId: String) = refreshSummary(accountId).map { it.pagos }
    override suspend fun clearLocalData(accountId: String) {
        session.comoSocio(accountId) {
            if (session.resumenSocio.value?.perfil?.accountId == accountId) session.resumenSocio.value = null
            session.limpiarDatos(accountId)
        }
    }
}
