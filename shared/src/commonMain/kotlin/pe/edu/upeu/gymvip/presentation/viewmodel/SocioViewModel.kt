package pe.edu.upeu.gymvip.presentation.viewmodel

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import pe.edu.upeu.gymvip.domain.error.AppError
import pe.edu.upeu.gymvip.domain.model.ResumenSocio
import pe.edu.upeu.gymvip.domain.repository.MemberRepository

data class SocioUiState(val datos: ResumenSocio? = null, val cargando: Boolean = true,
    val error: String? = null, val sinConexion: Boolean = false,
    val mostrarAviso: Boolean = false, val ahora: Instant = Clock.System.now())

/** Inicio, membresía y pagos comparten una sola carga y los mismos estados. */
class SocioViewModel(private val repository: MemberRepository, private val accountId: String,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    private val clock: Clock = Clock.System) {
    private val _state = MutableStateFlow(SocioUiState(ahora = clock.now()))
    val state = _state.asStateFlow()
    private var carga: Job? = null
    init {
        repository.observeSummary(accountId).onEach { data -> _state.update { it.copy(datos = data) } }.launchIn(scope)
        scope.launch { while (isActive) { delay(60_000); _state.update { it.copy(ahora = clock.now()) } } }
        refresh()
    }
    fun refresh() {
        if (carga?.isActive == true) return
        _state.update { it.copy(cargando = true, error = null) }
        carga = scope.launch {
            val result = repository.refreshSummary(accountId)
            val aviso = if (result.isSuccess) try { repository.takeExpiryNotice(accountId) } catch (_: AppError) { false } else false
            val noAutorizado = result.exceptionOrNull() is AppError.Unauthorized || result.exceptionOrNull() is AppError.Forbidden
            _state.update { it.copy(datos = if (noAutorizado) null else result.getOrNull() ?: it.datos, cargando = false,
                error = result.exceptionOrNull()?.message, sinConexion = result.exceptionOrNull() is AppError.Network,
                mostrarAviso = it.mostrarAviso || aviso, ahora = clock.now()) }
        }
    }
    fun cerrarAviso() { _state.update { it.copy(mostrarAviso = false) } }
    fun onCleared() { scope.cancel() }
}
