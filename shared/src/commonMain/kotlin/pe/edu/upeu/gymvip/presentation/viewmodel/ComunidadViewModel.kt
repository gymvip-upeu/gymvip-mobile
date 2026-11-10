package pe.edu.upeu.gymvip.presentation.viewmodel

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import pe.edu.upeu.gymvip.domain.model.*
import pe.edu.upeu.gymvip.domain.repository.ComunidadRepository

data class ComunidadUiState(val datos: DatosComunidad = DatosComunidad(),
    val catalogo: PaginaComercial<Suplemento>? = null, val promociones: PaginaComercial<Oferta>? = null,
    val destacadas: List<Oferta> = emptyList(), val filtro: FiltroCatalogo = FiltroCatalogo(),
    val ofertasEnCatalogo: Map<String, Oferta> = emptyMap(), val errorPrecios: String? = null,
    val estadoPromos: String = "VIGENTE", val productoPromo: String? = null,
    val cargandoCatalogo: Boolean = false, val cargandoPromos: Boolean = false,
    val cargandoAvisos: Boolean = false, val cargandoFavoritos: Boolean = false, val cargandoFaq: Boolean = false,
    val errorCatalogo: String? = null, val errorPromos: String? = null, val errorAvisos: String? = null,
    val errorFavoritos: String? = null, val errorFaq: String? = null, val errorAccion: String? = null,
    val sinRedCatalogo: Boolean = false, val sinRedPromos: Boolean = false, val sinRedAvisos: Boolean = false,
    val sinRedFavoritos: Boolean = false, val sinRedFaq: Boolean = false,
    val guardando: Boolean = false, val productoId: String? = null, val ofertaId: String? = null,
    val producto: Suplemento? = null, val oferta: Oferta? = null, val cargandoDetalle: Boolean = false,
    val errorDetalle: String? = null, val ofertaProducto: Oferta? = null)

class ComunidadViewModel(val repository: ComunidadRepository, val esSocio: Boolean,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)) {
    private val _state = MutableStateFlow(ComunidadUiState())
    val state = _state.asStateFlow()
    private var catalogJob: Job? = null
    private var promoJob: Job? = null
    private var detailJob: Job? = null
    init {
        repository.observar().onEach { data -> _state.update { it.copy(datos = data) } }.launchIn(scope)
        if (esSocio) { cargarAvisos(); if (pe.edu.upeu.gymvip.AppConfig.FEATURE_FAVORITOS) cargarFavoritos(); scope.launch {
            val result = repository.promociones("VIGENTE", null, 0)
            _state.update { it.copy(destacadas = result.getOrNull()?.contenido.orEmpty().filter(Oferta::vigente).take(4)) }
        } }
    }
    fun cargarCatalogo(filtro: FiltroCatalogo = state.value.filtro, pagina: Int = 0) {
        catalogJob?.cancel()
        _state.update { it.copy(cargandoCatalogo = true, errorCatalogo = null, filtro = filtro,
            ofertasEnCatalogo = it.ofertasEnCatalogo.takeIf { _ -> it.filtro == filtro && it.catalogo?.pagina == pagina }.orEmpty(), errorPrecios = null,
            catalogo = it.catalogo.takeIf { p -> it.filtro == filtro && p?.pagina == pagina }) }
        catalogJob = scope.launch {
            val result = repository.catalogo(filtro, pagina)
            _state.update { it.copy(cargandoCatalogo = false, catalogo = result.getOrNull() ?: it.catalogo, errorCatalogo = result.exceptionOrNull()?.message,
                ofertasEnCatalogo = if (result.isSuccess) emptyMap() else it.ofertasEnCatalogo,
                sinRedCatalogo = result.exceptionOrNull() is pe.edu.upeu.gymvip.domain.error.AppError.Network) }
            result.getOrNull()?.let { page ->
                val limite = Semaphore(4)
                val precios = coroutineScope { page.contenido.map { p -> async {
                    limite.withPermit { p.id to repository.promociones("VIGENTE", p.id, 0) }
                } }.awaitAll() }
                val ofertas = precios.mapNotNull { (id, r) -> r.getOrNull()?.contenido?.firstOrNull { it.vigente && it.producto.id == id }?.let { id to it } }.toMap()
                _state.update { it.copy(ofertasEnCatalogo = ofertas, errorPrecios = if (precios.any { p -> p.second.isFailure })
                    "No se pudieron consultar algunas promociones. Se muestran los precios regulares de esos productos." else null) }
            }
        }
    }
    fun cargarPromos(estado: String = state.value.estadoPromos, producto: String? = state.value.productoPromo, pagina: Int = 0) {
        promoJob?.cancel()
        _state.update { it.copy(cargandoPromos = true, errorPromos = null, estadoPromos = estado, productoPromo = producto,
            promociones = it.promociones.takeIf { p -> it.estadoPromos == estado && it.productoPromo == producto && p?.pagina == pagina }) }
        promoJob = scope.launch {
            val result = repository.promociones(estado, producto, pagina)
            _state.update { it.copy(cargandoPromos = false, promociones = result.getOrNull() ?: it.promociones, errorPromos = result.exceptionOrNull()?.message,
                sinRedPromos = result.exceptionOrNull() is pe.edu.upeu.gymvip.domain.error.AppError.Network) }
        }
    }
    fun abrirProducto(id: String) {
        detailJob?.cancel(); _state.update { it.copy(productoId = id, ofertaId = null, producto = null, oferta = null, cargandoDetalle = true, errorDetalle = null) }
        detailJob = scope.launch {
            val r = repository.producto(id)
            val ofertas = if (r.isSuccess) repository.promociones("VIGENTE", id, 0).getOrNull()?.contenido else null
            _state.update { it.copy(cargandoDetalle = false, producto = r.getOrNull(), ofertaProducto = ofertas?.firstOrNull { o -> o.vigente && o.producto.id == id }, errorDetalle = r.exceptionOrNull()?.message) }
        }
    }
    fun abrirOferta(id: String) {
        detailJob?.cancel(); _state.update { it.copy(ofertaId = id, productoId = null, producto = null, oferta = null, cargandoDetalle = true, errorDetalle = null) }
        detailJob = scope.launch { val r = repository.oferta(id); _state.update { it.copy(cargandoDetalle = false, oferta = r.getOrNull(), errorDetalle = r.exceptionOrNull()?.message) } }
    }
    fun cerrarDetalle() { detailJob?.cancel(); _state.update { it.copy(productoId = null, ofertaId = null, producto = null, oferta = null, errorDetalle = null, cargandoDetalle = false) } }
    fun cargarFavoritos() { if (!esSocio || state.value.cargandoFavoritos) return
        _state.update { it.copy(cargandoFavoritos = true, errorFavoritos = null) }; scope.launch {
        val r = repository.favoritos()
        _state.update { it.copy(cargandoFavoritos = false, errorFavoritos = r.exceptionOrNull()?.message,
            sinRedFavoritos = r.exceptionOrNull() is pe.edu.upeu.gymvip.domain.error.AppError.Network) }
    } }
    fun alternarFavorito(producto: Suplemento) = accion {
        val existente = state.value.datos.favoritos.firstOrNull { it.productoId == producto.id }
        if (existente == null) repository.guardarFavorito(producto, "") else repository.quitarFavorito(existente.id)
    }
    fun nota(id: String, texto: String, terminado: () -> Unit) = accion {
        val r = repository.editarNota(id, texto); if (r.isSuccess) terminado(); r
    }
    fun quitar(id: String) = accion { repository.quitarFavorito(id) }
    fun cargarAvisos() { if (!esSocio || state.value.cargandoAvisos) return
        _state.update { it.copy(cargandoAvisos = true, errorAvisos = null) }; scope.launch {
        val r = repository.avisos()
        _state.update { it.copy(cargandoAvisos = false, errorAvisos = r.exceptionOrNull()?.message,
            sinRedAvisos = r.exceptionOrNull() is pe.edu.upeu.gymvip.domain.error.AppError.Network) }
    } }
    fun leer(id: String) { if (state.value.datos.avisos.any { it.id == id && !it.isRead }) accion { repository.leerAviso(id) } }
    fun cargarFaq() { if (!esSocio || state.value.cargandoFaq) return
        _state.update { it.copy(cargandoFaq = true, errorFaq = null) }; scope.launch {
        val r = repository.preguntas()
        _state.update { it.copy(cargandoFaq = false, errorFaq = r.exceptionOrNull()?.message,
            sinRedFaq = r.exceptionOrNull() is pe.edu.upeu.gymvip.domain.error.AppError.Network) }
    } }
    private fun accion(block: suspend () -> Result<Unit>) {
        if (!esSocio || state.value.guardando) return
        _state.update { it.copy(guardando = true, errorAccion = null) }
        scope.launch { val r = block(); _state.update { it.copy(guardando = false, errorAccion = r.exceptionOrNull()?.message) } }
    }
    fun limpiarErrorAccion() { _state.update { it.copy(errorAccion = null) } }
    fun onCleared() { scope.cancel() }
}
