package com.gymvip.mobile.data.remote

import com.gymvip.mobile.data.remote.api.AuthApi
import com.gymvip.mobile.data.remote.api.CatalogApi
import com.gymvip.mobile.data.remote.api.FavoriteApi
import com.gymvip.mobile.data.remote.api.GymVipHttpClient
import com.gymvip.mobile.data.remote.api.MemberApi
import com.gymvip.mobile.data.remote.dto.ConsentSubmitRequestDto
import com.gymvip.mobile.data.remote.dto.FavoriteRequestDto
import com.gymvip.mobile.data.remote.dto.FavoriteUpdateNoteDto
import com.gymvip.mobile.data.remote.dto.LoginRequestDto
import com.gymvip.mobile.data.remote.dto.PasswordResetConfirmRequestDto
import com.gymvip.mobile.data.remote.dto.PasswordResetRequestDto
import com.gymvip.mobile.domain.error.AppError
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KtorMockEngineTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun testLoginSuccess200() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"token":"jwt_sample_token_123","accountId":"acc_vip_1","email":"socio@vip.pe","roles":["CLIENTE"]}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }
        val client = GymVipHttpClient.create(engine = mockEngine, enableLogging = false)
        val api = AuthApi(client, baseUrl = "https://mock.gymvip.pe")

        val result = api.login(LoginRequestDto("socio@vip.pe", "Password123!"))
        assertTrue(result.isSuccess)
        assertEquals("jwt_sample_token_123", result.getOrNull()?.token)
        assertEquals("acc_vip_1", result.getOrNull()?.accountId)
    }

    @Test
    fun testLoginUnauthorized401() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"status":401,"error":"Unauthorized","message":"Credenciales inválidas"}""",
                status = HttpStatusCode.Unauthorized,
                headers = jsonHeaders
            )
        }
        val client = GymVipHttpClient.create(engine = mockEngine, enableLogging = false)
        val api = AuthApi(client, baseUrl = "https://mock.gymvip.pe")

        val result = api.login(LoginRequestDto("socio@vip.pe", "wrongpass"))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.Unauthorized)
    }

    @Test
    fun testFavoriteCreate201() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """{
                    "id":"fav_serv_123",
                    "accountId":"acc_1",
                    "productId":"prod_whey",
                    "productName":"Proteína Whey",
                    "productCategory":"SUPLEMENTO",
                    "pricePen":165.0,
                    "personalNote":"Nota de prueba",
                    "createdAt":"2026-10-15T10:00:00Z",
                    "updatedAt":"2026-10-15T10:00:00Z"
                }""",
                status = HttpStatusCode.Created,
                headers = jsonHeaders
            )
        }
        val client = GymVipHttpClient.create(engine = mockEngine, enableLogging = false)
        val api = FavoriteApi(client, baseUrl = "https://mock.gymvip.pe")

        val result = api.addFavorite(FavoriteRequestDto("prod_whey", "Nota de prueba"))
        assertTrue(result.isSuccess)
        assertEquals("fav_serv_123", result.getOrNull()?.id)
        assertEquals(165.0, result.getOrNull()?.pricePen)
    }

    @Test
    fun testFavoriteConflict409() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"status":409,"code":"DOCUMENT_ALREADY_EXISTS","message":"El producto ya está en favoritos"}""",
                status = HttpStatusCode.Conflict,
                headers = jsonHeaders
            )
        }
        val client = GymVipHttpClient.create(engine = mockEngine, enableLogging = false)
        val api = FavoriteApi(client, baseUrl = "https://mock.gymvip.pe")

        val result = api.addFavorite(FavoriteRequestDto("prod_whey", "Nota"))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.Conflict)
    }

    @Test
    fun testFavoriteDelete204NoContent() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = "",
                status = HttpStatusCode.NoContent,
                headers = jsonHeaders
            )
        }
        val client = GymVipHttpClient.create(engine = mockEngine, enableLogging = false)
        val api = FavoriteApi(client, baseUrl = "https://mock.gymvip.pe")

        val result = api.deleteFavorite("fav_serv_123")
        assertTrue(result.isSuccess)
    }

    @Test
    fun testServerError500() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"status":500,"error":"Internal Server Error","message":"Error inesperado"}""",
                status = HttpStatusCode.InternalServerError,
                headers = jsonHeaders
            )
        }
        val client = GymVipHttpClient.create(engine = mockEngine, enableLogging = false)
        val api = FavoriteApi(client, baseUrl = "https://mock.gymvip.pe")

        val result = api.deleteFavorite("fav_serv_123")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.Server)
    }

    @Test
    fun testGetFaqsSuccess200() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """[{"id":"faq-1","question":"¿Horario?","answer":"6am a 10pm","category":"General","orderIndex":1}]""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }
        val client = GymVipHttpClient.create(engine = mockEngine, enableLogging = false)
        val api = MemberApi(client, baseUrl = "https://mock.gymvip.pe")

        val result = api.getFaqs()
        assertTrue(result.isSuccess)
        val list = result.getOrThrow()
        assertEquals(1, list.size)
        assertEquals("faq-1", list[0].id)
    }

    @Test
    fun testGetServicesSuccess200() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """[{"id":"serv-1","name":"Musculación","description":"Pesas","schedule":"6am-10pm","location":"Piso 1","isIncluded":true}]""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }
        val client = GymVipHttpClient.create(engine = mockEngine, enableLogging = false)
        val api = MemberApi(client, baseUrl = "https://mock.gymvip.pe")

        val result = api.getServices()
        assertTrue(result.isSuccess)
        val list = result.getOrThrow()
        assertEquals("serv-1", list[0].id)
        assertTrue(list[0].isIncluded)
    }

    @Test
    fun testConsentsGetAndSubmit200() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"termsAccepted":true,"privacyAcknowledged":true,"whatsappConsent":true,"documentVersion":"v1.0","acceptedAt":"2026-11-09T10:00:00Z"}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }
        val client = GymVipHttpClient.create(engine = mockEngine, enableLogging = false)
        val api = MemberApi(client, baseUrl = "https://mock.gymvip.pe")

        val getRes = api.getConsents()
        assertTrue(getRes.isSuccess)
        assertTrue(getRes.getOrThrow().termsAccepted)

        val submitRes = api.submitConsents(ConsentSubmitRequestDto(true, true, true, "v1.0"))
        assertTrue(submitRes.isSuccess)
        assertTrue(submitRes.getOrThrow().whatsappConsent)
    }

    @Test
    fun testInboxGetAndMarkRead() = runTest {
        val mockEngine = MockEngine { request ->
            if (request.url.encodedPath.contains("/read")) {
                respond(content = "", status = HttpStatusCode.OK, headers = jsonHeaders)
            } else {
                respond(
                    content = """[{"id":"msg-1","title":"Bienvenido","message":"Hola","category":"COMUNICADO","createdAt":"2026-11-09T08:00:00Z","isRead":false}]""",
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders
                )
            }
        }
        val client = GymVipHttpClient.create(engine = mockEngine, enableLogging = false)
        val api = MemberApi(client, baseUrl = "https://mock.gymvip.pe")

        val inbox = api.getInbox()
        assertTrue(inbox.isSuccess)
        assertEquals(1, inbox.getOrThrow().size)

        val mark = api.markInboxAsRead("msg-1")
        assertTrue(mark.isSuccess)
    }

    @Test
    fun testDynamicQrSuccess200() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """{
                    "token":"gymvip://access/v1?cid=123&code=VIP-01&nonce=abc&exp=1800000000",
                    "memberCode":"VIP-01",
                    "memberName":"Valeria Mendoza",
                    "expiresAtUtc":"2026-11-09T12:01:00Z",
                    "ttlSeconds":60,
                    "disclaimer":"No es autorización de ingreso."
                }""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }
        val client = GymVipHttpClient.create(engine = mockEngine, enableLogging = false)
        val api = MemberApi(client, baseUrl = "https://mock.gymvip.pe")

        val qrRes = api.getDynamicQr()
        assertTrue(qrRes.isSuccess)
        val qr = qrRes.getOrThrow()
        assertEquals("VIP-01", qr.memberCode)
        assertEquals(60, qr.ttlSeconds)
        assertTrue(qr.token.startsWith("gymvip://access/v1"))
    }

    @Test
    fun testPasswordResetRequest200() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"message":"Si el correo está registrado en GYM VIP, se ha enviado un enlace/código de recuperación válido por 15 minutos."}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }
        val client = GymVipHttpClient.create(engine = mockEngine, enableLogging = false)
        val api = AuthApi(client, baseUrl = "https://mock.gymvip.pe")

        val result = api.requestPasswordReset(PasswordResetRequestDto("valeria@vipcenterfit.pe"))
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().message.contains("15 minutos"))
    }

    @Test
    fun testPasswordResetConfirm200() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"message":"Contraseña actualizada exitosamente. Todas las sesiones activas han sido invalidadas."}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }
        val client = GymVipHttpClient.create(engine = mockEngine, enableLogging = false)
        val api = AuthApi(client, baseUrl = "https://mock.gymvip.pe")

        val result = api.confirmPasswordReset(PasswordResetConfirmRequestDto("tok_abc_123", "NewSecretPass123!"))
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().message.contains("actualizada exitosamente"))
    }

    @Test
    fun testPasswordResetConfirmExpired400() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"status":400,"code":"TOKEN_EXPIRED","message":"El token de recuperación ha expirado"}""",
                status = HttpStatusCode.BadRequest,
                headers = jsonHeaders
            )
        }
        val client = GymVipHttpClient.create(engine = mockEngine, enableLogging = false)
        val api = AuthApi(client, baseUrl = "https://mock.gymvip.pe")

        val result = api.confirmPasswordReset(PasswordResetConfirmRequestDto("expired_token", "NewSecretPass123!"))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.Validation)
    }

    @Test
    fun testScanSupplementSuccess200() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """{
                    "id":"supp-01",
                    "name":"Proteína Whey 2kg",
                    "brand":"Optimum",
                    "category":"PROTEINA",
                    "price":249.90,
                    "stock":10,
                    "isAvailable":true,
                    "barcode":"7751234567890"
                }""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }
        val client = GymVipHttpClient.create(engine = mockEngine, enableLogging = false)
        val api = CatalogApi(client, baseUrl = "https://mock.gymvip.pe")

        val result = api.scanSupplement("7751234567890")
        assertTrue(result.isSuccess)
        val supp = result.getOrThrow()
        assertEquals("supp-01", supp.id)
        assertEquals("7751234567890", supp.barcode)
        assertEquals(249.90, supp.price)
    }

    @Test
    fun testUpdateSupplementBarcodeSuccess200() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """{
                    "id":"supp-01",
                    "name":"Proteína Whey 2kg",
                    "brand":"Optimum",
                    "category":"PROTEINA",
                    "price":249.90,
                    "stock":10,
                    "isAvailable":true,
                    "barcode":"7759998887776"
                }""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }
        val client = GymVipHttpClient.create(engine = mockEngine, enableLogging = false)
        val api = CatalogApi(client, baseUrl = "https://mock.gymvip.pe")

        val result = api.updateSupplementBarcode("supp-01", "7759998887776")
        assertTrue(result.isSuccess)
        val supp = result.getOrThrow()
        assertEquals("7759998887776", supp.barcode)
    }

    private class FakeTestTokenStore(private val token: String?) : com.gymvip.mobile.data.local.SecureTokenStore {
        override suspend fun saveTokens(accessToken: String, refreshToken: String?, accountId: String) {}
        override suspend fun getAccessToken(): String? = token
        override suspend fun getRefreshToken(): String? = null
        override suspend fun getAccountId(): String? = null
        override suspend fun clear() {}
        override suspend fun saveRememberedIdentifier(identifier: String) {}
        override suspend fun getRememberedIdentifier(): String? = null
        override suspend fun clearRememberedIdentifier() {}
    }

    @Test
    fun testIsAuthorizedOriginStrictValidation() {
        val base = "https://gymvip-backend.onrender.com"
        assertTrue(GymVipHttpClient.isAuthorizedOrigin(Url("https://gymvip-backend.onrender.com/api/v1/test"), base))
        assertFalse(GymVipHttpClient.isAuthorizedOrigin(Url("http://gymvip-backend.onrender.com/api/v1/test"), base))
        assertFalse(GymVipHttpClient.isAuthorizedOrigin(Url("https://evil-gymvip.onrender.com/api/v1/test"), base))
        assertFalse(GymVipHttpClient.isAuthorizedOrigin(Url("https://gymvip-backend.onrender.com:8443/api/v1/test"), base))
        assertFalse(GymVipHttpClient.isAuthorizedOrigin(Url("https://gymvip-backend.onrender.com.attacker.com/api/v1/test"), base))
    }

    @Test
    fun testBearerTokenAttachedOnlyToAuthorizedOrigin() = runTest {
        var capturedAuthHeader: String? = null
        val mockEngine = MockEngine { request ->
            capturedAuthHeader = request.headers[HttpHeaders.Authorization]
            respond(content = "{}", status = HttpStatusCode.OK, headers = jsonHeaders)
        }

        GymVipHttpClient.baseUrl = "https://gymvip-backend.onrender.com"
        val client = GymVipHttpClient.create(
            engine = mockEngine,
            tokenStore = FakeTestTokenStore("valid_secret_token_xyz"),
            enableLogging = false
        )

        // Request to authorized origin
        client.get("https://gymvip-backend.onrender.com/api/v1/secure-data")
        assertEquals("Bearer valid_secret_token_xyz", capturedAuthHeader)

        // Request to unauthorized external origin (must NOT receive Bearer token)
        capturedAuthHeader = null
        client.get("https://attacker-gymvip.com/api/v1/steal")
        assertNull(capturedAuthHeader)
    }

    @Test
    fun testNetworkExceptionMappingShowsSafeUserMessages() {
        val timeoutErr = GymVipHttpClient.mapThrowableToAppError(
            io.ktor.client.plugins.HttpRequestTimeoutException("https://gymvip-backend.onrender.com", 60_000L)
        )
        assertTrue(timeoutErr is AppError.Network)
        assertTrue(timeoutErr.messageDetail.contains("tardó en responder"))
        assertFalse(timeoutErr.messageDetail.contains("https://"))
        assertFalse(timeoutErr.messageDetail.contains("60000"))

        val dnsErr = GymVipHttpClient.mapThrowableToAppError(
            java.net.UnknownHostException("gymvip-backend.onrender.com")
        )
        assertTrue(dnsErr is AppError.Network)
        assertFalse(dnsErr.messageDetail.contains("onrender.com"))

        val sslErr = GymVipHttpClient.mapThrowableToAppError(
            javax.net.ssl.SSLException("Handshake failed")
        )
        assertTrue(sslErr is AppError.Network)
        assertTrue(sslErr.messageDetail.contains("conexión segura"))
    }

    @Test
    fun testSafeApiCallPreservesCancellationException() = runTest {
        var threwCancellation = false
        try {
            GymVipHttpClient.safeApiCall<String> {
                throw kotlinx.coroutines.CancellationException("Job was cancelled")
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            threwCancellation = true
            assertEquals("Job was cancelled", e.message)
        }
        assertTrue(threwCancellation, "CancellationException debe ser relanzada y no capturada como AppError")
    }
}
