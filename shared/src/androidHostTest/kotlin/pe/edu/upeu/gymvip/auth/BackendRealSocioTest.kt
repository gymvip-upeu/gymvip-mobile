package pe.edu.upeu.gymvip.auth

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import pe.edu.upeu.gymvip.data.local.*
import pe.edu.upeu.gymvip.data.remote.api.*
import pe.edu.upeu.gymvip.data.repository.*
import pe.edu.upeu.gymvip.domain.error.AppError
import pe.edu.upeu.gymvip.domain.model.FiltroCatalogo
import pe.edu.upeu.gymvip.domain.repository.AuthState
import kotlin.test.*

/** Escrituras solo en el servidor QA local aislado, nunca en la instancia del gimnasio. */
class BackendRealSocioTest {
    @Test fun socioConsultaDatosPropiosYConfirmaFavoritosYLecturaSinGestionPersonal() {
        assumeTrue("Prueba optativa contra backend QA aislado", System.getenv("GYMVIP_LIVE_SOCIO_QA") == "1")
        val base = requireNotNull(System.getenv("GYMVIP_QA_BASE_URL"))
        require(base == "http://localhost:8082") { "Esta prueba solo admite el servidor QA aislado en 8082." }
        val email = requireNotNull(System.getenv("GYMVIP_QA_SOCIO_EMAIL"))
        val password = requireNotNull(System.getenv("GYMVIP_QA_SOCIO_PASSWORD"))
        val clientId = requireNotNull(System.getenv("GYMVIP_QA_SOCIO_ID"))
        val productId = requireNotNull(System.getenv("GYMVIP_QA_PRODUCTO_ID"))
        runBlocking {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            GymVipDatabase.Schema.create(driver)
            val store = AlmacenSesionFake()
            val session = SesionLocal(store, GymVipDatabase(driver))
            val client = GymVipHttpClient.create(tokenStore = store, enableLogging = false, apiBaseUrl = base)
            val auth = AuthRepositoryImpl(AuthApi(client, base), MemberApi(client, base), session)
            try {
                auth.login(email, password).getOrThrow()
                val user = assertIs<AuthState.Authenticated>(auth.getAuthState().first())
                assertEquals(clientId, user.profile.accountId)
                assertTrue(user.profile.lastName.contains("QA"), "La cuenta debe ser un escenario QA.")
                assertTrue(auth.checkExistingSession())
                val data = MemberRepositoryImpl(MemberApi(client, base), session.database, session).refreshSummary(clientId).getOrThrow()
                assertEquals(clientId, data.perfil.accountId)
                assertTrue(data.pagos.isNotEmpty())
                val repository = ComunidadRepositoryRest(ComunidadApi(client, base), session)
                val products = repository.catalogo(FiltroCatalogo(consulta = "QA"), 0).getOrThrow().contenido
                assertTrue(products.all { it.publicado })
                val product = repository.producto(productId).getOrThrow()
                assertTrue(product.nombre.endsWith("QA"))
                assertTrue(repository.imagen(product.id, product.imagenes.first()).getOrThrow().isNotEmpty())
                assertTrue(repository.promociones("VIGENTE", productId, 0).getOrThrow().contenido.any { it.vigente })
                repository.favoritos().getOrThrow()
                repository.guardarFavorito(product, "Nota QA").getOrThrow()
                val favorite = repository.favoritos().getOrThrow().single { it.productoId == productId }
                repository.editarNota(favorite.id, "Nota actualizada QA").getOrThrow()
                assertEquals("Nota actualizada QA", repository.favoritos().getOrThrow().single { it.id == favorite.id }.nota)
                repository.quitarFavorito(favorite.id).getOrThrow()
                assertTrue(repository.favoritos().getOrThrow().none { it.id == favorite.id })
                val notices = repository.avisos().getOrThrow()
                val notice = notices.first()
                repository.leerAviso(notice.id).getOrThrow()
                assertTrue(repository.avisos().getOrThrow().single { it.id == notice.id }.isRead)
                assertTrue(repository.preguntas().getOrThrow().isNotEmpty())
                assertIs<AppError.Forbidden>(resultadoLlamada { PersonalApi(client, base).socios("", 0) }.exceptionOrNull())
                assertIs<AppError.Forbidden>(resultadoLlamada { PersonalApi(client, base).cuentas() }.exceptionOrNull())
            } finally { auth.logout(); client.close(); driver.close() }
            assertNull(store.token)
            assertTrue(session.comunidad.value.favoritos.isEmpty())
        }
    }
}
