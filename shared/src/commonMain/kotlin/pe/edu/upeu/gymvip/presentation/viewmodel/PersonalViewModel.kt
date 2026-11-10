package pe.edu.upeu.gymvip.presentation.viewmodel

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import pe.edu.upeu.gymvip.domain.model.*
import pe.edu.upeu.gymvip.domain.repository.GestionPersonalRepository

enum class SeccionPersonal(val titulo: String) { INICIO("Inicio"), SOCIOS("Socios"), PLANES("Planes"), CUENTAS("Cuentas"), PERFIL("Perfil") }
data class PersonalUiState(val cargando: Boolean = false, val guardando: Boolean = false,
    val resumen: ResumenPersonal? = null, val pagina: PaginaSocios? = null, val ficha: FichaGestion? = null,
    val planes: List<PlanGestion> = emptyList(), val cuentas: List<CuentaPersonal> = emptyList(),
    val error: String? = null, val mensaje: String? = null)

class PersonalViewModel(private val repository: GestionPersonalRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)) {
    private val _state = MutableStateFlow(PersonalUiState())
    val state = _state.asStateFlow()
    private var carga: Job? = null
    fun cargar(seccion: SeccionPersonal, busqueda: String = "", pagina: Int = 0) {
        carga?.cancel()
        carga = scope.launch {
            _state.update { it.copy(cargando = true, error = null) }
            val result = when (seccion) {
                SeccionPersonal.INICIO -> repository.resumen().map { v -> _state.update { it.copy(resumen = v) } }
                SeccionPersonal.SOCIOS -> repository.socios(busqueda, pagina).map { v -> _state.update { it.copy(pagina = v, ficha = null) } }
                SeccionPersonal.PLANES -> repository.planes().map { v -> _state.update { it.copy(planes = v) } }
                SeccionPersonal.CUENTAS -> repository.cuentas().map { v -> _state.update { it.copy(cuentas = v) } }
                SeccionPersonal.PERFIL -> Result.success(Unit)
            }
            _state.update { it.copy(cargando = false, error = result.exceptionOrNull()?.message) }
        }
    }
    fun ficha(id: String) {
        carga?.cancel()
        carga = scope.launch {
            _state.update { it.copy(cargando = true, ficha = null, error = null) }
            val result = repository.ficha(id)
            _state.update { it.copy(cargando = false, ficha = result.getOrNull(), error = result.exceptionOrNull()?.message) }
        }
    }
    fun guardarSocio(socio: SocioGestion, nuevo: Boolean, guardado: (SocioGestion) -> Unit) {
        if (_state.value.guardando) return
        _state.update { it.copy(guardando = true, error = null, mensaje = null) }
        scope.launch {
            val result = repository.guardarSocio(socio, nuevo)
            _state.update { it.copy(guardando = false, error = result.exceptionOrNull()?.message,
                mensaje = if (result.isSuccess) "Datos del socio guardados." else null) }
            result.getOrNull()?.let(guardado)
        }
    }
    fun guardarPlan(plan: PlanGestion, nuevo: Boolean, guardado: () -> Unit) {
        if (_state.value.guardando) return
        _state.update { it.copy(guardando = true, error = null, mensaje = null) }
        scope.launch {
            val result = repository.guardarPlan(plan, nuevo)
            _state.update { it.copy(guardando = false, error = result.exceptionOrNull()?.message,
                mensaje = if (result.isSuccess) "Plan guardado." else null) }
            if (result.isSuccess) guardado()
        }
    }
    fun asistencia(id: String) {
        if (_state.value.guardando) return
        _state.update { it.copy(guardando = true, error = null, mensaje = null) }
        scope.launch {
            val result = repository.asistencia(id)
            _state.update { it.copy(guardando = false, error = result.exceptionOrNull()?.message, mensaje = result.getOrNull()) }
            if (result.isSuccess) ficha(id)
        }
    }
    fun limpiarMensajes() { _state.update { it.copy(error = null, mensaje = null) } }
    fun onCleared() { scope.cancel() }
}
