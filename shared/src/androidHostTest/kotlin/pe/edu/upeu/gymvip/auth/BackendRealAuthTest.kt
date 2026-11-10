package pe.edu.upeu.gymvip.auth

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import pe.edu.upeu.gymvip.data.local.*
import pe.edu.upeu.gymvip.data.remote.api.*
import pe.edu.upeu.gymvip.data.repository.AuthRepositoryImpl
import pe.edu.upeu.gymvip.data.repository.GestionPersonalRepositoryRest
import pe.edu.upeu.gymvip.data.repository.ComunidadRepositoryRest
import pe.edu.upeu.gymvip.domain.model.FiltroCatalogo
import pe.edu.upeu.gymvip.data.mapper.resumenSocio
import kotlinx.datetime.Clock
import pe.edu.upeu.gymvip.domain.model.Rol
import pe.edu.upeu.gymvip.domain.repository.AuthState
import pe.edu.upeu.gymvip.domain.error.AppError
import kotlinx.coroutines.flow.first
import kotlin.test.*

/** Credenciales inyectadas solo en el proceso, nunca en archivos del proyecto. */
class BackendRealAuthTest {
    @Test fun administradorAccedeAGestionYCuentas() = verificarRol("ADMIN")
    @Test fun recepcionAccedeASociosSinPermisoDeCuentas() = verificarRol("RECEPCION")

    private fun verificarRol(rol: String) {
        assumeTrue("Prueba real optativa: requiere variables de entorno", System.getenv("GYMVIP_LIVE_AUTH") == "1")
        val email = requireNotNull(System.getenv("GYMVIP_TEST_" + rol + "_EMAIL"))
        val password = requireNotNull(System.getenv("GYMVIP_TEST_" + rol + "_PASSWORD"))
        val base = System.getenv("GYMVIP_TEST_BASE_URL") ?: "http://localhost:8081"
        runBlocking {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            GymVipDatabase.Schema.create(driver)
            val store = AlmacenSesionFake()
            val session = SesionLocal(store, GymVipDatabase(driver))
            val client = GymVipHttpClient.create(tokenStore = store, enableLogging = false, apiBaseUrl = base)
            val repository = AuthRepositoryImpl(AuthApi(client, base), MemberApi(client, base), session)
            try {
                val result = repository.login(email, password)
                assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
                val staff = assertIs<AuthState.StaffAuthenticated>(repository.getAuthState().first())
                assertTrue(Rol.valueOf(rol) in staff.usuario.roles)
                assertTrue(repository.checkExistingSession())
                assertIs<AuthState.StaffAuthenticated>(repository.getAuthState().first())
                val api = PersonalApi(client, base)
                val gestion = GestionPersonalRepositoryRest(api, session)
                assertTrue(gestion.resumen().isSuccess)
                val socios = gestion.socios("", 0).getOrThrow()
                socios.socios.firstOrNull()?.let { socio ->
                    // Mismo contrato workspace, leído con permisos de personal; no suplanta un token CLIENTE.
                    val data = api.workspace(socio.id).resumenSocio(socio.id, Clock.System.now())
                    assertEquals(socio.id, data.perfil.accountId)
                    assertTrue(data.pagos.all { it.accountId == socio.id })
                }
                assertTrue(gestion.planes().isSuccess)
                val comunidad = ComunidadRepositoryRest(ComunidadApi(client, base), session)
                val catalogo = comunidad.catalogo(FiltroCatalogo(), 0).getOrThrow()
                catalogo.contenido.firstOrNull()?.let { producto ->
                    assertEquals(producto.id, comunidad.producto(producto.id).getOrThrow().id)
                    producto.imagenes.firstOrNull()?.let { imagen -> assertTrue(comunidad.imagen(producto.id, imagen).getOrThrow().isNotEmpty()) }
                }
                val promociones = comunidad.promociones("VIGENTE", null, 0).getOrThrow()
                assertTrue(promociones.contenido.all { it.vigente })
                promociones.contenido.firstOrNull()?.let { assertEquals(it.id, comunidad.oferta(it.id).getOrThrow().id) }
                comunidad.promociones("FINALIZADA", null, 0).getOrThrow()
                assertIs<AppError.Forbidden>(comunidad.favoritos().exceptionOrNull())
                assertIs<AppError.Forbidden>(resultadoLlamada { ComunidadApi(client, base).avisos() }.exceptionOrNull())
                assertIs<AppError.Forbidden>(MemberApi(client, base).getWorkspace().exceptionOrNull())
                if (rol == "ADMIN") assertTrue(gestion.cuentas().isSuccess)
                else {
                    assertIs<AppError.Forbidden>(gestion.cuentas().exceptionOrNull())
                    // Comprobar además la denegación real del servidor, saltando el guard local.
                    assertIs<AppError.Forbidden>(resultadoLlamada { api.cuentas() }.exceptionOrNull())
                }
            } finally { repository.logout(); client.close(); driver.close() }
            assertNull(store.token)
        }
    }
}
