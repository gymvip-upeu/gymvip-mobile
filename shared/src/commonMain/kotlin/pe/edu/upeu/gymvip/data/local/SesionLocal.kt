package pe.edu.upeu.gymvip.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.gymvip.domain.repository.AuthState

class SesionLocal(val store: SecureTokenStore, val database: GymVipDatabase) {
    val estado = MutableStateFlow<AuthState>(AuthState.Idle)
    private val mutex = Mutex()
    val resumenSocio = MutableStateFlow<pe.edu.upeu.gymvip.domain.model.ResumenSocio?>(null)
    var avisoVencimientoMostrado = false
    val comunidad = MutableStateFlow(pe.edu.upeu.gymvip.domain.model.DatosComunidad())

    suspend fun <T> conSesion(token: String? = null, soloSocio: Boolean = false, block: () -> T): T = mutex.withLock {
        val actual = estado.value
        if (actual !is AuthState.Authenticated && actual !is AuthState.StaffAuthenticated ||
            soloSocio && actual !is AuthState.Authenticated || token != null && store.getAccessToken() != token)
            throw pe.edu.upeu.gymvip.domain.error.AppError.Forbidden("La sesión no permite esta operación.")
        block()
    }

    suspend fun <T> comoSocio(account: String, tokenEsperado: String? = null, block: () -> T): T = mutex.withLock {
        val auth = estado.value as? AuthState.Authenticated
        if (auth?.profile?.accountId != account || (tokenEsperado != null && store.getAccessToken() != tokenEsperado))
            throw pe.edu.upeu.gymvip.domain.error.AppError.Forbidden("La sesión no permite consultar estos datos.")
        block()
    }

    suspend fun cerrar(message: String? = null, tokenEsperado: String? = null, publicar: Boolean = true) = mutex.withLock {
        if (tokenEsperado != null && store.getAccessToken() != tokenEsperado) return@withLock
        val account = store.getAccountId()
        store.clear()
        resumenSocio.value = null
        avisoVencimientoMostrado = false
        comunidad.value = pe.edu.upeu.gymvip.domain.model.DatosComunidad()
        if (publicar) estado.value = AuthState.Unauthenticated(message)
        if (account != null) limpiarDatos(account)
    }

    fun limpiarDatos(account: String) {
        database.gymVipDatabaseQueries.transaction {
            val q = database.gymVipDatabaseQueries
            q.deleteProfileByAccountId(account)
            q.deleteMembershipByAccountId(account)
            q.deletePaymentsByAccountId(account)
            q.deleteInboxByAccountId(account)
            q.deleteFavoritesByAccountId(account)
            q.deleteSyncQueueByAccountId(account)
            q.deleteConsentByAccountId(account)
        }
    }
}
