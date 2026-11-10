package pe.edu.upeu.gymvip.data.remote.api

import pe.edu.upeu.gymvip.AppConfig
import pe.edu.upeu.gymvip.data.local.SecureTokenStore
import pe.edu.upeu.gymvip.data.remote.dto.ErrorResponseDto
import pe.edu.upeu.gymvip.domain.error.AppError
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

object GymVipHttpClient {
    val tokenEsperado = io.ktor.util.AttributeKey<String>("GymVipTokenEsperado")
    var baseUrl: String
        get() = AppConfig.apiBaseUrl
        set(value) { AppConfig.apiBaseUrl = value }

    fun create(
        engine: HttpClientEngine = getPlatformHttpEngine(),
        tokenStore: SecureTokenStore? = null,
        enableLogging: Boolean = AppConfig.debug,
        apiBaseUrl: String = baseUrl,
        onUnauthorized: suspend (String) -> Unit = {}
    ): HttpClient {
        val origin = Url(apiBaseUrl)
        val sessionPlugin = createClientPlugin("SesionGymVip") {
            onRequest { request, _ ->
                val destination = request.url.build()
                val ownOrigin = destination.host == origin.host && destination.port == origin.port && destination.protocol == origin.protocol
                if (!ownOrigin || request.url.encodedPath == "/api/v1/auth/login") request.headers.remove(HttpHeaders.Authorization)
                if (ownOrigin && request.url.encodedPath != "/api/v1/auth/login") {
                    val token = tokenStore?.getAccessToken()
                    val esperado = request.attributes.getOrNull(tokenEsperado)
                    if (esperado != null && esperado != token) throw AppError.Forbidden("La sesión cambió. Vuelve a intentar la operación.")
                    token?.let { request.headers[HttpHeaders.Authorization] = "Bearer $it" }
                }
            }
            onResponse { response ->
                if (response.status == HttpStatusCode.Unauthorized) {
                    response.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")?.let { onUnauthorized(it) }
                }
            }
        }
        return HttpClient(engine) {
            followRedirects = false
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
            install(HttpTimeout) { requestTimeoutMillis = 30_000; connectTimeoutMillis = 30_000; socketTimeoutMillis = 30_000 }
            install(sessionPlugin)
            if (enableLogging) install(Logging) {
                level = LogLevel.HEADERS
                sanitizeHeader { it.equals(HttpHeaders.Authorization, true) || it.equals("X-Auth-Token", true) }
                logger = Logger.DEFAULT
            }
        }
    }

    suspend fun parseResponseError(response: HttpResponse): AppError {
        val dto = try {
            Json { ignoreUnknownKeys = true }.decodeFromString<ErrorResponseDto>(response.bodyAsText())
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { null }
        return when (response.status.value) {
            400, 422 -> AppError.Validation(dto?.message ?: "Revisa los datos ingresados.")
            401 -> AppError.Unauthorized("Correo o contraseña incorrectos, o acceso no habilitado. Consulta en recepción.")
            403 -> AppError.Forbidden("No tienes acceso a esta información.")
            404 -> AppError.NotFound("No se encontró la información solicitada.")
            409 -> AppError.Conflict(dto?.code ?: "CONFLICT", dto?.message ?: "La información cambió. Intenta de nuevo.")
            423 -> AppError.Forbidden("Tu cuenta está inactiva. Acércate a recepción.")
            429 -> AppError.Validation("Demasiados intentos. Espera unos minutos antes de volver a ingresar.")
            else -> AppError.Server(response.status.value, "El servidor no pudo completar la operación. Intenta de nuevo.")
        }
    }
}
