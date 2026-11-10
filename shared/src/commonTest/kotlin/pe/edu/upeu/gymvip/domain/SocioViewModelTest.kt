package pe.edu.upeu.gymvip.domain

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import pe.edu.upeu.gymvip.domain.error.AppError
import pe.edu.upeu.gymvip.domain.model.*
import pe.edu.upeu.gymvip.domain.repository.MemberRepository
import pe.edu.upeu.gymvip.presentation.viewmodel.SocioViewModel
import kotlin.test.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SocioViewModelTest {
    private val data = ResumenSocio(MemberProfile("socio", "", "Valeria", "Rojas", "DNI", "", null,
        AccountStatus.ACTIVO, "2026-11-09T12:00:00Z"), null, emptyList(), emptyList(), "2026-11-09T12:00:00Z")
    private class Fake(initial: ResumenSocio) : MemberRepository {
        val values = MutableStateFlow<ResumenSocio?>(null)
        var result = Result.success(initial)
        var gate: CompletableDeferred<Unit>? = null
        var calls = 0
        override fun observeSummary(accountId: String): Flow<ResumenSocio?> = values
        override suspend fun refreshSummary(accountId: String): Result<ResumenSocio> {
            calls++; gate?.await()
            result.getOrNull()?.let { values.value = it }
            return result
        }
        override suspend fun takeExpiryNotice(accountId: String) = false
        override fun observeProfile(accountId: String) = values.map { it?.perfil }
        override fun observeMembership(accountId: String) = values.map { it?.membresia }
        override fun observePayments(accountId: String) = values.map { it?.pagos.orEmpty() }
        override suspend fun refreshProfile(accountId: String) = refreshSummary(accountId).map { it.perfil }
        override suspend fun refreshMembership(accountId: String) = refreshSummary(accountId).map { it.membresia }
        override suspend fun refreshPayments(accountId: String) = refreshSummary(accountId).map { it.pagos }
        override suspend fun clearLocalData(accountId: String) { values.value = null }
    }
    @Test fun cargaInicialUnicaYContenidoInclusoSinMembresia() = runTest {
        val fake = Fake(data).apply { gate = CompletableDeferred() }
        val vm = SocioViewModel(fake, "socio", backgroundScope)
        assertTrue(vm.state.value.cargando)
        runCurrent(); vm.refresh(); runCurrent()
        assertEquals(1, fake.calls)
        fake.gate!!.complete(Unit); runCurrent()
        assertFalse(vm.state.value.cargando); assertEquals(data, vm.state.value.datos)
        assertNull(vm.state.value.error); assertNull(vm.state.value.datos!!.membresia)
    }
    @Test fun distingueOfflineDeErrorDeServidorYMantieneDatos() = runTest {
        val fake = Fake(data)
        val vm = SocioViewModel(fake, "socio", backgroundScope)
        runCurrent()
        fake.result = Result.failure(AppError.Network("Sin conexión")); vm.refresh(); runCurrent()
        assertTrue(vm.state.value.sinConexion); assertEquals(data, vm.state.value.datos)
        fake.result = Result.failure(AppError.Server(503, "No disponible")); vm.refresh(); runCurrent()
        assertFalse(vm.state.value.sinConexion); assertEquals(data, vm.state.value.datos)
        assertNotNull(vm.state.value.error)
        fake.result = Result.success(data); vm.refresh(); runCurrent()
        assertNull(vm.state.value.error); assertFalse(vm.state.value.cargando)
    }
    @Test fun sesionExpiradaNoConservaDatosEnUi() = runTest {
        val fake = Fake(data)
        val vm = SocioViewModel(fake, "socio", backgroundScope)
        runCurrent()
        fake.result = Result.failure(AppError.Unauthorized("Tu sesión expiró")); vm.refresh(); runCurrent()
        assertNull(vm.state.value.datos); assertFalse(vm.state.value.sinConexion)
    }
}
