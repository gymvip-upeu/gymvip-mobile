package pe.edu.upeu.gymvip.auth

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pe.edu.upeu.gymvip.data.local.*
import pe.edu.upeu.gymvip.data.remote.api.*
import pe.edu.upeu.gymvip.data.remote.dto.*
import pe.edu.upeu.gymvip.data.repository.ComunidadRepositoryRest
import pe.edu.upeu.gymvip.domain.error.AppError
import pe.edu.upeu.gymvip.domain.model.*
import pe.edu.upeu.gymvip.domain.repository.AuthState
import kotlin.test.*

class ComunidadRepositoryTest {
    private val headers = headersOf(HttpHeaders.ContentType, "application/json")
    private val json = Json { ignoreUnknownKeys = true }
    private val producto = SuplementoDto("p1", "Proteína", "VIP", "PROTEINA", "Bolsa", "1 kg", description = "Descripción real",
        pricePen = 189.9, availability = "DISPONIBLE", published = true)
    private val favorito = FavoritoSocioDto("f1", "p1", "Proteína", "PROTEINA", "Mi nota")
    private fun aviso(account: String = "socio", leido: Boolean = false) = InboxItemDto("a1", account, "Aviso", "Mensaje", createdAt = "2026-11-09T12:00:00", isRead = leido)
    private fun oferta(estado: String) = OfertaDto("o1", producto, "Oferta", "Detalle", "Hasta agotar stock", 189.9, 159.9, "2026-11-01", "2026-11-30", estado)
    private fun fixture(handler: MockRequestHandler): Fixture {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GymVipDatabase.Schema.create(driver)
        val store = AlmacenSesionFake().apply { token = "token-1"; account = "socio" }
        val profile = MemberProfile("socio", "socio@example.test", "Valeria", "Rojas", "DNI", "00000000", null, AccountStatus.ACTIVO, "2026-11-09T12:00:00Z")
        val session = SesionLocal(store, GymVipDatabase(driver)).apply { estado.value = AuthState.Authenticated(profile) }
        val client = GymVipHttpClient.create(MockEngine(handler), store, false, "https://api.test", onUnauthorized = { session.cerrar(tokenEsperado = it) })
        return Fixture(driver, store, session, client, ComunidadRepositoryRest(ComunidadApi(client, "https://api.test"), session))
    }
    private data class Fixture(val driver: JdbcSqliteDriver, val store: AlmacenSesionFake, val session: SesionLocal,
        val client: HttpClient, val repo: ComunidadRepositoryRest) : AutoCloseable {
        override fun close() { client.close(); driver.close() }
        fun personal() { session.estado.value = AuthState.StaffAuthenticated(UsuarioPersonal("admin", "admin@example.test", setOf(Rol.ADMIN))) }
    }
    @Test fun filtrosSeEnvianSegunContratoYPreciosNoSonPorcentajes() = runTest {
        fixture { r ->
            assertEquals("Bearer token-1", r.headers[HttpHeaders.Authorization])
            assertEquals("/api/v1/catalog/supplements", r.url.encodedPath)
            assertEquals("VIP", r.url.parameters["query"])
            assertEquals("PROTEINA", r.url.parameters["category"])
            assertEquals("150.50", r.url.parameters["minPrice"])
            assertEquals("true", r.url.parameters["withPromotion"])
            assertEquals("2", r.url.parameters["page"])
            respond(Json.encodeToString(PaginaComercialDto(listOf(producto), 2, 3, 25)), headers = headers)
        }.use { f ->
            val page = f.repo.catalogo(FiltroCatalogo(consulta = "VIP", categoria = "PROTEINA", minimo = "150.50", enPromocion = true), 2).getOrThrow()
            assertEquals(18990L, page.contenido.single().precio.cents)
            assertEquals(25L, page.total)
            assertEquals(15990L, oferta("VIGENTE").dominio().precioOferta.cents)
        }
    }
    @Test fun rangoInvalidoSeRechazaAntesDeLaRed() = runTest {
        var calls = 0
        fixture { calls++; respond("{}", headers = headers) }.use { f ->
            listOf(FiltroCatalogo(minimo = "NaN"), FiltroCatalogo(maximo = "-1"), FiltroCatalogo(minimo = "10", maximo = "2")).forEach {
                assertIs<AppError.Validation>(f.repo.catalogo(it, 0).exceptionOrNull())
            }
            assertEquals(0, calls)
        }
    }
    @Test fun socioNuncaVeProductoSinPublicarPeroPersonalSi() = runTest {
        fixture { respond(Json.encodeToString(PaginaComercialDto(listOf(producto.copy(published = false)), 0, 1, 1)), headers = headers) }.use { f ->
            assertTrue(f.repo.catalogo(FiltroCatalogo(), 0).getOrThrow().contenido.isEmpty())
            f.personal()
            assertEquals(1, f.repo.catalogo(FiltroCatalogo(), 0).getOrThrow().contenido.size)
        }
    }
    @Test fun finalizadasNoReetiquetaVigentesQueDevuelvaServidor() = runTest {
        fixture { r ->
            assertEquals("p1", r.url.parameters["supplementId"])
            respond(Json.encodeToString(PaginaComercialDto(listOf(oferta("VIGENTE")), 0, 1, 1)), headers = headers)
        }.use { f -> assertTrue(f.repo.promociones("FINALIZADA", "p1", 0).getOrThrow().contenido.isEmpty()) }
    }
    @Test fun favoritoSnakeCaseYNotaOpcionalDelServidor() {
        val dto = json.decodeFromString<FavoritoSocioDto>("""{"id":"f1","product_id":"p1","product_title":"Proteína","product_category":"PROTEINA","created_at_utc":"2026-11-09T12:00:00"}""")
        assertEquals("p1", dto.dominio().productoId)
        assertEquals("", dto.dominio().nota)
    }
    @Test fun favoritoSoloCambiaTrasConfirmacionDeLectura() = runTest {
        var guardado = false
        fixture { r ->
            if (r.method == HttpMethod.Post) { guardado = true; respond("{\"saved\":true}", headers = headers) }
            else respond(Json.encodeToString(if (guardado) listOf(favorito.copy(notes = "")) else emptyList()), headers = headers)
        }.use { f ->
            f.repo.favoritos().getOrThrow()
            f.repo.guardarFavorito(producto.dominio(), "").getOrThrow()
            assertEquals("f1", f.repo.observar().first().favoritos.single().id)
        }
    }
    @Test fun postExitosoSinPersistenciaNoInventaFavorito() = runTest {
        fixture { r -> respond(if (r.method == HttpMethod.Get) "[]" else "{\"saved\":true}", headers = headers) }.use { f ->
            assertIs<AppError.Server>(f.repo.guardarFavorito(producto.dominio(), "").exceptionOrNull())
            assertTrue(f.session.comunidad.value.favoritos.isEmpty())
        }
    }
    @Test fun editarYEliminarSeConfirmanContraLaListaReal() = runTest {
        var lista = listOf(favorito)
        fixture { r ->
            when (r.method) {
                HttpMethod.Put -> { lista = lista.map { it.copy(notes = "Nueva nota") }; respond("{\"updated\":true}", headers = headers) }
                HttpMethod.Delete -> { lista = emptyList(); respond("{\"deleted\":true}", headers = headers) }
                else -> respond(Json.encodeToString(lista), headers = headers)
            }
        }.use { f ->
            f.repo.favoritos().getOrThrow()
            f.repo.editarNota("f1", "Nueva nota").getOrThrow()
            assertEquals("Nueva nota", f.session.comunidad.value.favoritos.single().nota)
            f.repo.quitarFavorito("f1").getOrThrow()
            assertTrue(f.session.comunidad.value.favoritos.isEmpty())
        }
    }
    @Test fun notaLargaYFavoritoAjenoNoEnviaMutaciones() = runTest {
        var calls = 0
        fixture { calls++; respond("[]", headers = headers) }.use { f ->
            assertIs<AppError.Validation>(f.repo.guardarFavorito(producto.dominio(), "a".repeat(251)).exceptionOrNull())
            assertIs<AppError.NotFound>(f.repo.editarNota("ajeno", "nota").exceptionOrNull())
            assertIs<AppError.NotFound>(f.repo.quitarFavorito("ajeno").exceptionOrNull())
            assertEquals(0, calls)
        }
    }
    @Test fun falloAlMarcarLeidoMantieneContadorYContenido() = runTest {
        fixture { r -> respond(if (r.method == HttpMethod.Get) Json.encodeToString(listOf(aviso())) else "{\"read\":true}", headers = headers) }.use { f ->
            f.repo.avisos().getOrThrow()
            assertIs<AppError.Server>(f.repo.leerAviso("a1").exceptionOrNull())
            assertFalse(f.session.comunidad.value.avisos.single().isRead)
            assertEquals("Mensaje", f.session.comunidad.value.avisos.single().message)
        }
    }
    @Test fun lecturaConfirmadaActualizaContadorCompartido() = runTest {
        var leido = false
        fixture { r ->
            if (r.method == HttpMethod.Post) { leido = true; respond("{}", headers = headers) }
            else respond(Json.encodeToString(listOf(aviso(leido = leido))), headers = headers)
        }.use { f ->
            f.repo.avisos().getOrThrow(); f.repo.leerAviso("a1").getOrThrow()
            assertTrue(f.repo.observar().first().avisos.single().isRead)
        }
    }
    @Test fun avisoDeOtraCuentaNoSePublica() = runTest {
        fixture { respond(Json.encodeToString(listOf(aviso("ajeno"))), headers = headers) }.use { f ->
            assertIs<AppError.Forbidden>(f.repo.avisos().exceptionOrNull())
            assertTrue(f.session.comunidad.value.avisos.isEmpty())
        }
    }
    @Test fun personalNoConsultaRutasPrivadasDeSocio() = runTest {
        var calls = 0
        fixture { calls++; respond("[]", headers = headers) }.use { f ->
            f.personal()
            assertIs<AppError.Forbidden>(f.repo.favoritos().exceptionOrNull())
            assertIs<AppError.Forbidden>(f.repo.avisos().exceptionOrNull())
            assertIs<AppError.Forbidden>(f.repo.preguntas().exceptionOrNull())
            assertIs<AppError.Forbidden>(f.repo.guardarFavorito(producto.dominio(), "").exceptionOrNull())
            assertEquals(0, calls)
        }
    }
    @Test fun forbiddenDeCatalogoNoSeOcultaConDatosDemo() = runTest {
        fixture { respond("{}", HttpStatusCode.Forbidden, headers) }.use { f ->
            assertIs<AppError.Forbidden>(f.repo.catalogo(FiltroCatalogo(), 0).exceptionOrNull())
            assertIs<AuthState.Authenticated>(f.session.estado.value)
        }
    }
    @Test fun respuestaTardiaTrasLogoutNoRepueblaFavoritos() = runTest {
        val inicio = CompletableDeferred<Unit>(); val permitir = CompletableDeferred<Unit>()
        fixture { inicio.complete(Unit); permitir.await(); respond(Json.encodeToString(listOf(favorito)), headers = headers) }.use { f ->
            val carga = async { f.repo.favoritos() }
            inicio.await(); f.session.cerrar(); permitir.complete(Unit)
            assertTrue(carga.await().isFailure)
            assertEquals(DatosComunidad(), f.session.comunidad.value)
        }
    }
    @Test fun cambioDeTokenDescartaDatosDeSesionAnterior() = runTest {
        val inicio = CompletableDeferred<Unit>(); val permitir = CompletableDeferred<Unit>()
        fixture { inicio.complete(Unit); permitir.await(); respond(Json.encodeToString(listOf(aviso())), headers = headers) }.use { f ->
            val carga = async { f.repo.avisos() }
            inicio.await(); f.store.token = "token-2"; permitir.complete(Unit)
            assertTrue(carga.await().isFailure)
            assertEquals("token-2", f.store.token)
            assertTrue(f.session.comunidad.value.avisos.isEmpty())
        }
    }
    @Test fun mutacionEnEsperaNoSeEjecutaConLaSiguienteSesion() = runTest {
        val inicio = CompletableDeferred<Unit>(); val permitir = CompletableDeferred<Unit>()
        var escrituras = 0
        fixture { r ->
            if (r.method != HttpMethod.Get) escrituras++
            inicio.complete(Unit); permitir.await()
            respond(Json.encodeToString(listOf(favorito)), headers = headers)
        }.use { f ->
            val carga = async { f.repo.favoritos() }; inicio.await()
            val escritura = async(start = CoroutineStart.UNDISPATCHED) { f.repo.guardarFavorito(producto.dominio(), "") }
            f.store.token = "token-2"; permitir.complete(Unit)
            assertTrue(carga.await().isFailure)
            assertTrue(escritura.await().isFailure)
            assertEquals(0, escrituras)
        }
    }
    @Test fun cambioDeSesionAntesDelEnvioNoUsaLasCredencialesNuevas() = runTest {
        var escrituras = 0
        fixture { escrituras++; respond("{}", headers = headers) }.use { f ->
            val apiAnterior = ComunidadApi(f.client, "https://api.test").conSesion("token-anterior")
            val result = resultadoLlamada { apiAnterior.guardarFavorito(NuevoFavoritoDto("p1", "Proteína", "PROTEINA", "")) }
            assertIs<AppError.Forbidden>(result.exceptionOrNull())
            assertEquals(0, escrituras)
            assertEquals("token-1", f.store.token)
        }
    }
    @Test fun expiracionBorraTodoElEstadoPrivado() = runTest {
        var expiro = false
        fixture { if (expiro) respond("{}", HttpStatusCode.Unauthorized, headers) else respond(Json.encodeToString(listOf(favorito)), headers = headers) }.use { f ->
            f.repo.favoritos().getOrThrow(); expiro = true
            assertIs<AppError.Unauthorized>(f.repo.favoritos().exceptionOrNull())
            assertNull(f.store.token)
            assertEquals(DatosComunidad(), f.session.comunidad.value)
        }
    }
    @Test fun faqRealOrdenadaSinRellenoDeDemostracion() = runTest {
        fixture { r ->
            assertEquals("/api/v1/member-app/faqs", r.url.encodedPath)
            respond("""[{"id":"2","question":"B","answer":"Respuesta B","category":"SUPLEMENTOS","orderIndex":2},{"id":"1","question":"A","answer":"Respuesta A","category":"GENERAL","orderIndex":1}]""", headers = headers)
        }.use { f -> assertEquals(listOf("1", "2"), f.repo.preguntas().getOrThrow().map { it.id }) }
    }
}
