package com.gymvip.mobile.data.remote

import com.gymvip.mobile.data.remote.api.AuthApi
import com.gymvip.mobile.data.remote.api.FavoriteApi
import com.gymvip.mobile.data.remote.api.GymVipHttpClient
import com.gymvip.mobile.data.remote.dto.FavoriteRequestDto
import com.gymvip.mobile.data.remote.dto.FavoriteUpdateNoteDto
import com.gymvip.mobile.data.remote.dto.LoginRequestDto
import com.gymvip.mobile.domain.error.AppError
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
}
