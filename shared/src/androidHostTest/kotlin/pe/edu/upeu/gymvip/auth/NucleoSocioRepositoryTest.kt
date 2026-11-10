package pe.edu.upeu.gymvip.auth

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlinx.datetime.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pe.edu.upeu.gymvip.data.local.*
import pe.edu.upeu.gymvip.data.remote.api.*
import pe.edu.upeu.gymvip.data.remote.dto.*
import pe.edu.upeu.gymvip.data.repository.MemberRepositoryImpl
import pe.edu.upeu.gymvip.domain.error.AppError
import pe.edu.upeu.gymvip.domain.model.*
import pe.edu.upeu.gymvip.domain.repository.AuthState
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class NucleoSocioRepositoryTest {
    private val headers = headersOf(HttpHeaders.ContentType, "application/json")
    private val instante = Instant.parse("2026-11-09T12:00:00Z")
    private val profile = MemberProfile("socio", "socio@example.test", "Valeria", "Rojas", "DNI", "00000000", null, AccountStatus.ACTIVO, instante.toString())
    private fun payload(estado: String = "VIGENTE", id: String = "socio", pagos: Boolean = true): String = Json.encodeToString(MemberWorkspaceDto(
        client = MemberClientDto(id, firstName = "Valeria", lastName = "Rojas"),
        coverage = MemberCoverageDto(clientId = id, state = estado, planName = "VIP Mensual", startDate = "2026-10-31",
            lastIncludedDate = "2026-11-29", endExclusive = "2026-11-30", daysRemaining = 21),
        payments = if (pagos) listOf(MemberPaymentDto("pago", amount_pen = 99.9, payment_method = "PLIN", payment_timestamp_utc = "2026-11-01T12:00:00")) else emptyList()))

    private fun fixture(handler: MockRequestHandler): Fixture {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GymVipDatabase.Schema.create(driver)
        val database = GymVipDatabase(driver)
        val store = AlmacenSesionFake().apply { token = "token-1"; account = "socio"; email = "socio@example.test" }
        val session = SesionLocal(store, database).apply { estado.value = AuthState.Authenticated(profile) }
        val client = GymVipHttpClient.create(MockEngine(handler), store, false, "https://api.test", onUnauthorized = { session.cerrar("Tu sesión expiró", it) })
        val repo = MemberRepositoryImpl(MemberApi(client, "https://api.test"), database, session, object : Clock { override fun now() = instante })
        return Fixture(driver, database, store, session, client, repo)
    }
    private data class Fixture(val driver: JdbcSqliteDriver, val database: GymVipDatabase, val store: AlmacenSesionFake,
        val session: SesionLocal, val client: HttpClient, val repo: MemberRepositoryImpl) : AutoCloseable {
        override fun close() { client.close(); driver.close() }
    }
    @Test fun cargaUnaRespuestaCoherenteSinRutasSupuestas() = runTest {
        val paths = mutableListOf<String>()
        fixture { request -> paths += request.url.encodedPath; respond(payload(), headers = headers) }.use { f ->
            val data = f.repo.refreshSummary("socio").getOrThrow()
            assertEquals(listOf("/api/v1/member-app/me"), paths)
            assertEquals("Valeria", data.perfil.firstName)
            assertEquals(9990L, data.pagos.single().amount.cents)
            assertEquals(data, f.repo.observeSummary("socio").first())
            assertNull(f.repo.observeSummary("otra").first())
        }
    }
    @Test fun falloDeServidorConservaSoloCacheDeLaCuentaActual() = runTest {
        var falla = false
        fixture { _ -> if (falla) respond("{}", HttpStatusCode.ServiceUnavailable, headers) else respond(payload(), headers = headers) }.use { f ->
            val old = f.repo.refreshSummary("socio").getOrThrow()
            falla = true
            assertIs<AppError.Server>(f.repo.refreshSummary("socio").exceptionOrNull())
            assertEquals(old, f.repo.observeSummary("socio").first())
            assertNull(f.repo.observeSummary("otra").first())
        }
    }
    @Test fun respuestaVaciaBorraMembresiaYPagosPreviosAtomicamente() = runTest {
        var vacio = false
        fixture { _ -> respond(payload(if (vacio) "SIN_MEMBRESIA" else "VIGENTE", pagos = !vacio), headers = headers) }.use { f ->
            f.repo.refreshSummary("socio").getOrThrow()
            vacio = true
            val data = f.repo.refreshSummary("socio").getOrThrow()
            assertNull(data.membresia); assertTrue(data.pagos.isEmpty())
            assertNull(f.database.gymVipDatabaseQueries.selectMembershipByAccountId("socio").executeAsOneOrNull())
            assertTrue(f.database.gymVipDatabaseQueries.selectPaymentsByAccountId("socio").executeAsList().isEmpty())
        }
    }
    @Test fun unauthorizedLimpiaTodosLosDatosDelNucleo() = runTest {
        var expired = false
        fixture { _ -> if (expired) respond("{}", HttpStatusCode.Unauthorized, headers) else respond(payload(), headers = headers) }.use { f ->
            f.repo.refreshSummary("socio").getOrThrow()
            expired = true
            assertIs<AppError.Unauthorized>(f.repo.refreshSummary("socio").exceptionOrNull())
            assertNull(f.store.token); assertNull(f.session.resumenSocio.value)
            assertNull(f.database.gymVipDatabaseQueries.selectProfileByAccountId("socio").executeAsOneOrNull())
        }
    }
    @Test fun personalYCuentaAjenaNoPuedenConsultarElNucleoDeSocio() = runTest {
        var calls = 0
        fixture { _ -> calls++; respond(payload(), headers = headers) }.use { f ->
            assertIs<AppError.Forbidden>(f.repo.refreshSummary("otra").exceptionOrNull())
            f.session.estado.value = AuthState.StaffAuthenticated(UsuarioPersonal("admin", "admin@example.test", setOf(Rol.ADMIN)))
            assertIs<AppError.Forbidden>(f.repo.refreshSummary("socio").exceptionOrNull())
            assertEquals(0, calls)
            assertIs<AuthState.StaffAuthenticated>(f.session.estado.value)
        }
    }
    @Test fun respuestaConOtroPropietarioNuncaSeGuarda() = runTest {
        fixture { _ -> respond(payload(id = "otra"), headers = headers) }.use { f ->
            assertIs<AppError.Forbidden>(f.repo.refreshSummary("socio").exceptionOrNull())
            assertNull(f.session.resumenSocio.value)
            assertNull(f.database.gymVipDatabaseQueries.selectProfileByAccountId("otra").executeAsOneOrNull())
        }
    }
    @Test fun respuestaTardiaTrasLogoutNoRepueblaCache() = runTest {
        val inicio = CompletableDeferred<Unit>(); val permitir = CompletableDeferred<Unit>()
        fixture { _ -> inicio.complete(Unit); permitir.await(); respond(payload(), headers = headers) }.use { f ->
            val request = async { f.repo.refreshSummary("socio") }
            inicio.await(); f.session.cerrar(); permitir.complete(Unit)
            assertTrue(request.await().isFailure)
            assertNull(f.session.resumenSocio.value)
            assertNull(f.database.gymVipDatabaseQueries.selectProfileByAccountId("socio").executeAsOneOrNull())
        }
    }
    @Test fun respuestaDeTokenAnteriorNoCierraNuevaSesionDeMismoSocio() = runTest {
        val inicio = CompletableDeferred<Unit>(); val permitir = CompletableDeferred<Unit>()
        fixture { _ -> inicio.complete(Unit); permitir.await(); respond(payload(), headers = headers) }.use { f ->
            val request = async { f.repo.refreshSummary("socio") }
            inicio.await(); f.store.saveTokens("token-2", null, "socio"); permitir.complete(Unit)
            assertTrue(request.await().isFailure)
            assertEquals("token-2", f.store.token)
            assertIs<AuthState.Authenticated>(f.session.estado.value)
            assertNull(f.session.resumenSocio.value)
        }
    }
    @Test fun avisoDeVencimientoSoloUnaVezPorSesion() = runTest {
        fixture { _ -> respond(payload("POR_VENCER"), headers = headers) }.use { f ->
            f.repo.refreshSummary("socio").getOrThrow()
            assertTrue(f.repo.takeExpiryNotice("socio")); assertFalse(f.repo.takeExpiryNotice("socio"))
            f.session.cerrar()
            f.store.saveTokens("token-2", null, "socio"); f.session.estado.value = AuthState.Authenticated(profile)
            f.repo.refreshSummary("socio").getOrThrow()
            assertTrue(f.repo.takeExpiryNotice("socio"))
        }
    }
}
