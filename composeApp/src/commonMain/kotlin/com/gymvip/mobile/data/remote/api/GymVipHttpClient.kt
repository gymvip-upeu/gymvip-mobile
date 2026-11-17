package com.gymvip.mobile.data.remote.api

import com.gymvip.mobile.data.local.SecureTokenStore
import com.gymvip.mobile.domain.error.AppError
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.bearerAuth
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object GymVipHttpClient {

    // Configuración de URL base: Android Emulador: http://10.0.2.2:8080, PC/Render/Dispositivo físico según entorno
    var baseUrl: String = "https://gymvip-backend.onrender.com"

    fun isAuthorizedOrigin(requestUrl: Url, configuredBaseUrl: String = baseUrl): Boolean {
        val server = Url(configuredBaseUrl)
        return requestUrl.protocol == server.protocol &&
                requestUrl.host.equals(server.host, ignoreCase = true) &&
                requestUrl.port == server.port
    }

    fun create(
        engine: HttpClientEngine = getPlatformHttpEngine(),
        tokenStore: SecureTokenStore? = null,
        enableLogging: Boolean = true
    ): HttpClient {
        val jsonConfig = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            prettyPrint = false
        }

        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(jsonConfig)
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 15_000
            }

            if (tokenStore != null) {
                val session = createClientPlugin("GymVipSession") {
                    onRequest { request, _ ->
                        val target = request.url.build()
                        if (isAuthorizedOrigin(target, baseUrl)) {
                            tokenStore.getAccessToken()?.let { request.bearerAuth(it) }
                        }
                    }
                }
                install(session)
            }

            if (enableLogging) {
                install(Logging) {
                    level = LogLevel.INFO
                    logger = object : Logger {
                        override fun log(message: String) {
                            // Sanitizar tokens y credenciales sensibles para evitar filtrado en logs
                            val sanitized = message
                                .replace(Regex("Bearer [A-Za-z0-9-_=.]+"), "Bearer [REDACTED]")
                                .replace(Regex("\"password\"\\s*:\\s*\"[^\"]+\""), "\"password\":\"[REDACTED]\"")
                            println("[GYM-VIP-HTTP] $sanitized")
                        }
                    }
                }
            }
        }
    }

    suspend fun parseResponseError(response: HttpResponse): AppError {
        val status = response.status
        val body = try { response.bodyAsText() } catch (_: Exception) { "" }

        return when (status) {
            HttpStatusCode.Unauthorized -> AppError.Unauthorized("Credenciales inválidas o sesión expirada")
            HttpStatusCode.Forbidden -> AppError.Forbidden("No cuenta con permisos de socio para esta acción")
            HttpStatusCode.NotFound -> AppError.NotFound("El recurso solicitado no fue encontrado")
            HttpStatusCode.Conflict -> AppError.Conflict("CONFLICT", "Conflicto con el estado actual del servidor: $body")
            HttpStatusCode.BadRequest -> AppError.Validation("Datos de solicitud inválidos: $body")
            HttpStatusCode.InternalServerError, HttpStatusCode.BadGateway, HttpStatusCode.ServiceUnavailable -> {
                AppError.Server(status.value, "Error temporal del servidor del gimnasio (${status.value})")
            }
            else -> AppError.Server(status.value, "Respuesta inesperada del servidor: ${status.value}")
        }
    }
}
