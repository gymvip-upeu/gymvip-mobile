package pe.edu.upeu.gymvip.domain

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import pe.edu.upeu.gymvip.domain.error.AppError
import pe.edu.upeu.gymvip.domain.model.*
import pe.edu.upeu.gymvip.domain.repository.ComunidadRepository
import pe.edu.upeu.gymvip.presentation.viewmodel.ComunidadViewModel
import kotlin.test.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ComunidadViewModelTest {
    private class Repo : ComunidadRepository {
        val data = MutableStateFlow(DatosComunidad())
        var error: Throwable? = null
        var espera: CompletableDeferred<Unit>? = null
        var consultasPrivadas = 0
        override fun observar() = data
        override suspend fun catalogo(filtro: FiltroCatalogo, pagina: Int): Result<PaginaComercial<Suplemento>> {
            espera?.await()
            return error?.let { Result.failure(it) } ?: Result.success(PaginaComercial(emptyList(), pagina, 0, 0))
        }
        override suspend fun promociones(estado: String, productoId: String?, pagina: Int) = Result.success(PaginaComercial<Oferta>(emptyList(), pagina, 0, 0))
        override suspend fun producto(id: String) = Result.failure<Suplemento>(AppError.NotFound())
        override suspend fun oferta(id: String) = Result.failure<Oferta>(AppError.NotFound())
        override suspend fun imagen(producto: String, imagen: String) = Result.failure<ByteArray>(AppError.NotFound())
        override suspend fun favoritos(): Result<List<FavoritoSocio>> { consultasPrivadas++; return Result.success(data.value.favoritos) }
        override suspend fun avisos(): Result<List<InboxItem>> { consultasPrivadas++; return Result.success(data.value.avisos) }
        override suspend fun preguntas(): Result<List<Faq>> { consultasPrivadas++; return Result.success(data.value.preguntas) }
        override suspend fun guardarFavorito(producto: Suplemento, nota: String) = Result.success(Unit)
        override suspend fun editarNota(id: String, nota: String): Result<Unit> { espera?.await(); return error?.let { Result.failure(it) } ?: Result.success(Unit) }
        override suspend fun quitarFavorito(id: String) = Result.success(Unit)
        override suspend fun leerAviso(id: String): Result<Unit> = error?.let { Result.failure(it) } ?: Result.success(Unit).also {
            data.value = data.value.copy(avisos = data.value.avisos.map { if (it.id == id) it.copy(isRead = true) else it })
        }
    }
    @Test fun catalogoTransitaPorCargaVacioYErrorConReintento() = runTest {
        val repo = Repo(); val vm = ComunidadViewModel(repo, false, backgroundScope)
        vm.cargarCatalogo(); assertTrue(vm.state.value.cargandoCatalogo)
        runCurrent(); assertTrue(vm.state.value.catalogo!!.contenido.isEmpty()); assertNull(vm.state.value.errorCatalogo)
        repo.error = AppError.Network("Sin conexión")
        vm.cargarCatalogo(); runCurrent()
        assertNotNull(vm.state.value.catalogo); assertNotNull(vm.state.value.errorCatalogo); assertFalse(vm.state.value.cargandoCatalogo)
        repo.error = null; vm.cargarCatalogo(); runCurrent(); assertNull(vm.state.value.errorCatalogo)
    }
    @Test fun cambiarFiltroNoMuestraResultadosDeLaConsultaAnterior() = runTest {
        val repo = Repo(); val vm = ComunidadViewModel(repo, false, backgroundScope)
        vm.cargarCatalogo(); runCurrent()
        repo.espera = CompletableDeferred()
        vm.cargarCatalogo(FiltroCatalogo(consulta = "Otra marca")); runCurrent()
        assertNull(vm.state.value.catalogo)
        assertEquals("Otra marca", vm.state.value.filtro.consulta)
    }
    @Test fun panelPersonalNoCargaDatosPrivadosDeSocio() = runTest {
        val repo = Repo(); val vm = ComunidadViewModel(repo, false, backgroundScope)
        vm.cargarAvisos(); vm.cargarFavoritos(); vm.cargarFaq(); runCurrent()
        assertEquals(0, repo.consultasPrivadas)
    }
    @Test fun carritoLocalDisponibleParaPersonalYSeparadoPorSesion() = runTest {
        val repo = Repo()
        val item = Suplemento("p1", "Creatina", "Marca", "CREATINA", "Pote", "500 g", "", "Descripción", "",
            MoneyPen(14990), true, true, emptyList())
        val personal = ComunidadViewModel(repo, false, backgroundScope)
        personal.agregarAlCarrito(item, item.precio)
        assertEquals(1, personal.state.value.carrito.cantidad)
        val socio = ComunidadViewModel(repo, true, backgroundScope)
        socio.agregarAlCarrito(item, item.precio)
        assertEquals(1, socio.state.value.carrito.cantidad)
        assertEquals(0, ComunidadViewModel(repo, true, backgroundScope).state.value.carrito.cantidad)
    }
    @Test fun falloAlLeerConservaAvisoSinLeerYPublicaError() = runTest {
        val repo = Repo().apply {
            data.value = DatosComunidad(avisos = listOf(InboxItem("a1", "socio", "Aviso", "Mensaje", InboxCategory.COMUNICADO, "2026-11-09T12:00:00Z", false)))
            error = AppError.Server(500, "Intenta de nuevo")
        }
        val vm = ComunidadViewModel(repo, true, backgroundScope); runCurrent()
        vm.leer("a1"); runCurrent()
        assertFalse(vm.state.value.datos.avisos.single().isRead)
        assertNotNull(vm.state.value.errorAccion)
        repo.error = null; vm.leer("a1"); runCurrent()
        assertTrue(vm.state.value.datos.avisos.single().isRead)
    }
    @Test fun falloDeNotaNoCierraEditorNiPermiteDobleEnvio() = runTest {
        val repo = Repo().apply { error = AppError.Network("Sin conexión"); espera = CompletableDeferred() }
        val vm = ComunidadViewModel(repo, true, backgroundScope); runCurrent()
        var cierres = 0
        vm.nota("f1", "Nota") { cierres++ }; vm.nota("f1", "Otra") { cierres++ }
        assertTrue(vm.state.value.guardando)
        runCurrent(); repo.espera!!.complete(Unit); runCurrent()
        assertEquals(0, cierres); assertFalse(vm.state.value.guardando); assertNotNull(vm.state.value.errorAccion)
    }
}
