package pe.edu.upeu.gymvip.data.remote

import pe.edu.upeu.gymvip.data.remote.api.AuthApi
import pe.edu.upeu.gymvip.data.remote.api.FavoriteApi
import pe.edu.upeu.gymvip.data.remote.api.GymVipHttpClient
import pe.edu.upeu.gymvip.data.remote.api.MemberApi
import pe.edu.upeu.gymvip.data.remote.dto.ConsentSubmitRequestDto
import pe.edu.upeu.gymvip.data.remote.dto.FavoriteRequestDto
import pe.edu.upeu.gymvip.data.remote.dto.FavoriteUpdateNoteDto
import pe.edu.upeu.gymvip.data.remote.dto.LoginRequestDto
import pe.edu.upeu.gymvip.domain.error.AppError
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
