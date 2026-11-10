package pe.edu.upeu.gymvip.auth

import io.ktor.client.engine.mock.*
import io.ktor.client.request.get
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import pe.edu.upeu.gymvip.data.remote.api.*
import pe.edu.upeu.gymvip.data.remote.dto.LoginRequestDto
import pe.edu.upeu.gymvip.domain.usecase.ReglasPassword
import kotlin.test.*

class AccesoTest {
    @Test fun reglasRealesDePassword() {
        assertNotNull(ReglasPassword.error("Temporal12345", "Abc12345", "Abc12345", true))
        assertNull(ReglasPassword.error("Temporal12345", "MiNuevaClave123", "MiNuevaClave123", true))
        assertNotNull(ReglasPassword.error("MiNuevaClave123", "MiNuevaClave123", "MiNuevaClave123", true))
        assertNotNull(ReglasPassword.error("Temporal12345", "minuevaclave123", "minuevaclave123", true))
        assertNotNull(ReglasPassword.error("Temporal12345", "MiNuevaClave123", "Distinta123456", true))
        assertNotNull(ReglasPassword.error("Temporal12345", "MiNuevaClave123", "MiNuevaClave123", false))
        val multibyte = "Aa1" + "ñ".repeat(35)
        assertNotNull(ReglasPassword.error("Temporal12345", multibyte, multibyte, true))
    }

    @Test fun tokenActualSeUsaSinCacheYNoSaleAOtroOrigen() = runTest {
        val store = AlmacenSesionFake()
        val headers = mutableListOf<String?>()
        val client = GymVipHttpClient.create(MockEngine { request ->
            headers += request.headers[HttpHeaders.Authorization]
            respond("{}", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }, store, false, "https://api.test")
        try {
            store.token = "primero"
            client.get("https://api.test/api/v1/member-app/session")
            store.token = "segundo"
            client.get("https://api.test/api/v1/member-app/session")
            client.get("https://api.test.ejemplo.com/private")
            client.get("http://api.test/private")
            client.get("https://api.test:8443/private")
            client.get("https://api.test/api/v1/auth/login")
            assertEquals(listOf("Bearer primero", "Bearer segundo", null, null, null, null), headers)
        } finally { client.close() }
    }

    @Test fun unauthorizedNotificaUnaVezSinRefreshInventado() = runTest {
        val store = AlmacenSesionFake().apply { token = "expirado" }
        var requests = 0
        var expired = 0
        val client = GymVipHttpClient.create(MockEngine {
            requests++; respond("{}", HttpStatusCode.Unauthorized)
        }, store, false, "https://api.test", onUnauthorized = { expired++ })
        try {
            client.get("https://api.test/api/v1/member-app/session")
            assertEquals(1, requests); assertEquals(1, expired)
        } finally { client.close() }
    }

    @Test fun loginNoMuestraMensajeSensibleDelServidor() = runTest {
        val client = GymVipHttpClient.create(MockEngine {
            respond("""{"message":"El correo existe pero la cuenta está bloqueada"}""", HttpStatusCode.Unauthorized,
                headersOf(HttpHeaders.ContentType, "application/json"))
        }, enableLogging = false)
        try {
            val result = AuthApi(client).login(LoginRequestDto("prueba@example.test", "incorrecta"))
            assertTrue(result.isFailure)
            assertFalse(result.exceptionOrNull()!!.message!!.contains("existe"))
        } finally { client.close() }
    }

    @Test fun cancelacionSePropaga() = runTest {
        assertFailsWith<CancellationException> { resultadoLlamada<Unit> { throw CancellationException("Cancelado") } }
    }
}
