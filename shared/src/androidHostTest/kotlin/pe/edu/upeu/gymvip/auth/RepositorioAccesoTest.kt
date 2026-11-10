package pe.edu.upeu.gymvip.auth

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.request.get
import io.ktor.http.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.gymvip.data.local.*
import pe.edu.upeu.gymvip.data.remote.api.*
import pe.edu.upeu.gymvip.data.repository.AuthRepositoryImpl
import pe.edu.upeu.gymvip.domain.repository.AuthState
import kotlin.test.*

class RepositorioAccesoTest {
    private val headers = headersOf(HttpHeaders.ContentType, "application/json")
    private val profile = """{"client":{"id":"client-1","email":"socio@example.test","firstName":"Valeria","lastName":"Rojas","status":"ACTIVO"}}"""
    private val consent = """{"termsAccepted":true,"privacyAcknowledged":true,"whatsappConsent":true,"documentVersion":"borrador-mobile-v1"}"""

    private fun fixture(handler: MockRequestHandler): Fixture {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GymVipDatabase.Schema.create(driver)
        val store = AlmacenSesionFake()
        val session = SesionLocal(store, GymVipDatabase(driver))
        val client = GymVipHttpClient.create(MockEngine(handler), store, false, "https://api.test",
            onUnauthorized = { session.cerrar("Tu sesión expiró", it) })
        val repository = AuthRepositoryImpl(AuthApi(client, "https://api.test"), MemberApi(client, "https://api.test"), session)
        return Fixture(driver, store, session, client, repository)
    }
    private data class Fixture(val driver: JdbcSqliteDriver, val store: AlmacenSesionFake, val session: SesionLocal,
        val client: HttpClient, val repo: AuthRepositoryImpl) : AutoCloseable {
        override fun close() { client.close(); driver.close() }
    }

    @Test fun primerIngresoNoCargaPerfilYCambiaConTokenNuevo() = runTest {
        var loginCount = 0
        var changed = false
        val paths = mutableListOf<String>()
        fixture { request ->
            val path = request.url.encodedPath
            paths += path
            val json = when (path) {
                "/api/v1/auth/login" -> {
                    loginCount++
                    assertNull(request.headers[HttpHeaders.Authorization])
                    """{"token":"token-$loginCount","id":"account-1","email":"socio@example.test","roles":["CLIENTE"]}"""
                }
                "/api/v1/member-app/session" -> """{"mustChangePassword":${!changed}}"""
                "/api/v1/member-app/consents" -> { assertEquals("Bearer token-1", request.headers[HttpHeaders.Authorization]); consent }
                "/api/v1/member-app/password" -> {
                    assertEquals("Bearer token-1", request.headers[HttpHeaders.Authorization]); changed = true
                    """{"changed":true,"requiresLogin":true}"""
                }
                "/api/v1/member-app/me" -> {
                    assertEquals("Bearer token-2", request.headers[HttpHeaders.Authorization]); profile
                }
                else -> error("Ruta inesperada: $path")
            }
            respond(json, headers = headers)
        }.use { f ->
            assertTrue(f.repo.login("socio@example.test", "Temporal12345").isSuccess)
            assertIs<AuthState.RequiresPasswordChange>(f.repo.getAuthState().first())
            assertFalse(paths.contains("/api/v1/member-app/me"))
            assertTrue(f.store.pendiente)
            assertTrue(f.repo.activateAccount("Temporal12345", "MiNuevaClave123", true).isSuccess)
            assertIs<AuthState.Authenticated>(f.repo.getAuthState().first())
            assertEquals(2, loginCount)
            assertEquals("client-1", f.store.account)
            assertFalse(f.store.pendiente)
            assertEquals(2, paths.count { it == "/api/v1/member-app/consents" })
        }
    }

    @Test fun rolDesconocidoSeRechazaYRevocaSesionSinGuardarToken() = runTest {
        var revoked = false
        fixture { request ->
            if (request.url.encodedPath.endsWith("/logout")) {
                assertEquals("Bearer staff-token", request.headers[HttpHeaders.Authorization]); revoked = true
                respond("", HttpStatusCode.NoContent)
            } else respond("""{"token":"staff-token","id":"staff","email":"staff@example.test","roles":["INVITADO"]}""", headers = headers)
        }.use { f ->
            val result = f.repo.login("staff@example.test", "Password12345")
            assertTrue(result.isFailure); assertTrue(result.exceptionOrNull()!!.message!!.contains("rol habilitado"))
            assertNull(f.store.token); assertTrue(revoked)
            assertIs<AuthState.Unauthenticated>(f.repo.getAuthState().first())
        }
    }

    @Test fun perfilFallidoNuncaInventaSocioAutenticado() = runTest {
        fixture { request -> when {
            request.url.encodedPath.endsWith("/login") -> respond("""{"token":"t","id":"a","email":"socio@example.test","roles":["CLIENTE"]}""", headers = headers)
            request.url.encodedPath.endsWith("/session") -> respond("""{"mustChangePassword":false}""", headers = headers)
            else -> respond("{}", HttpStatusCode.ServiceUnavailable, headers)
        } }.use { f ->
            assertTrue(f.repo.login("socio@example.test", "Password12345").isFailure)
            assertIs<AuthState.Unauthenticated>(f.repo.getAuthState().first()); assertNull(f.store.token)
        }
    }

    @Test fun consentimientoNoPersistidoImpideCambiarPassword() = runTest {
        var passwordRequests = 0
        fixture { request ->
            val result = when {
                request.url.encodedPath.endsWith("/login") -> """{"token":"t","id":"a","email":"socio@example.test","roles":["CLIENTE"]}"""
                request.url.encodedPath.endsWith("/session") -> """{"mustChangePassword":true}"""
                request.url.encodedPath.endsWith("/password") -> { passwordRequests++; "{}" }
                else -> """{"termsAccepted":false}"""
            }
            respond(result, headers = headers)
        }.use { f ->
            f.repo.login("socio@example.test", "Temporal12345").getOrThrow()
            assertTrue(f.repo.activateAccount("Temporal12345", "MiNuevaClave123", true).isFailure)
            assertEquals(0, passwordRequests)
            assertIs<AuthState.RequiresPasswordChange>(f.repo.getAuthState().first())
        }
    }

    @Test fun restauracionRespetaCambioObligatorioY401LimpiaSesion() = runTest {
        var expired = false
        fixture { request -> if (expired) respond("{}", HttpStatusCode.Unauthorized, headers)
            else if (request.url.encodedPath.endsWith("/auth/me"))
                respond("""{"email":"socio@example.test","roles":[{"authority":"ROLE_CLIENTE"}]}""", headers = headers)
            else respond("""{"mustChangePassword":true}""", headers = headers)
        }.use { f ->
            f.store.saveTokens("restaurado", null, "a")
            f.store.saveSessionMetadata("socio@example.test", true)
            assertTrue(f.repo.checkExistingSession())
            assertIs<AuthState.RequiresPasswordChange>(f.repo.getAuthState().first())
            expired = true
            f.client.get("https://api.test/api/v1/member-app/session")
            assertNull(f.store.token)
            assertIs<AuthState.Unauthenticated>(f.repo.getAuthState().first())
        }
    }

    @Test fun personalEntraSinConsultarPerfilDeSocioYRestauracionActualizaRol() = runTest {
        var role = "ADMIN"
        val paths = mutableListOf<String>()
        fixture { request ->
            paths += request.url.encodedPath
            val json = if (request.url.encodedPath.endsWith("/login"))
                """{"token":"staff-token","id":"staff","email":"staff@example.test","roles":["ADMIN"]}"""
            else """{"email":"staff@example.test","roles":[{"authority":"ROLE_$role"}]}"""
            respond(json, headers = headers)
        }.use { f ->
            assertTrue(f.repo.login("staff@example.test", "Password12345").isSuccess)
            assertTrue(assertIs<AuthState.StaffAuthenticated>(f.repo.getAuthState().first()).usuario.esAdministrador)
            role = "RECEPCION"
            assertTrue(f.repo.checkExistingSession())
            assertFalse(assertIs<AuthState.StaffAuthenticated>(f.repo.getAuthState().first()).usuario.esAdministrador)
            assertTrue(paths.none { it.contains("/member-app/") })
            assertEquals("staff", f.store.account)
        }
    }

    @Test fun recepcionNoPuedeEditarPlanesNiConsultarCuentasInclusoFueraDeUi() = runTest {
        var requests = 0
        fixture { _ -> requests++; respond("{}", headers = headers) }.use { f ->
            f.session.estado.value = AuthState.StaffAuthenticated(pe.edu.upeu.gymvip.domain.model.UsuarioPersonal(
                "recepcion", "recepcion@example.test", setOf(pe.edu.upeu.gymvip.domain.model.Rol.RECEPCION)))
            val repository = pe.edu.upeu.gymvip.data.repository.GestionPersonalRepositoryRest(PersonalApi(f.client, "https://api.test"), f.session)
            assertIs<pe.edu.upeu.gymvip.domain.error.AppError.Forbidden>(repository.cuentas().exceptionOrNull())
            assertIs<pe.edu.upeu.gymvip.domain.error.AppError.Forbidden>(repository.guardarPlan(
                pe.edu.upeu.gymvip.domain.model.PlanGestion("", "Plan", "", 30, 90.0, "", "", "ACTIVO"), true).exceptionOrNull())
            assertEquals(0, requests)
        }
    }

    @Test fun socioNoAccedeAlRepositorioDePersonal() = runTest {
        var requests = 0
        fixture { _ -> requests++; respond("{}", headers = headers) }.use { f ->
            val repository = pe.edu.upeu.gymvip.data.repository.GestionPersonalRepositoryRest(PersonalApi(f.client, "https://api.test"), f.session)
            assertIs<pe.edu.upeu.gymvip.domain.error.AppError.Forbidden>(repository.socios("", 0).exceptionOrNull())
            assertEquals(0, requests)
        }
    }
}
