package com.gymvip.mobile.data.remote.api

import com.gymvip.mobile.data.local.SecureTokenStore
import com.gymvip.mobile.domain.error.AppError
import com.gymvip.mobile.util.isDevelopmentBuild
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.bearerAuth
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

object GymVipHttpClient {

    const val PRODUCTION_SERVER_URL = "https://gymvip-backend.onrender.com"

    private var _customBaseUrl: String? = null

    /**
     * URL base del backend central.
     * En compilaciones de producción (Release), está bloqueada estrictamente al servidor HTTPS aprobado.
     * En desarrollo (Debug), permite alternar entre emulador, host local o Render.
     */
    var baseUrl: String
        get() = if (!isDevelopmentBuild) PRODUCTION_SERVER_URL else (_customBaseUrl ?: PRODUCTION_SERVER_URL)
        set(value) {
            if (isDevelopmentBuild) {
                val clean = value.trim().removeSuffix("/")
                _customBaseUrl = clean
            }
        }

    /**
     * Valida si el destino de la petición corresponde al backend autorizado.
     * Previene fuga de tokens JWT a hosts de terceros no confiables.
     */
    fun isAuthorizedOrigin(requestUrl: Url, configuredBaseUrl: String = baseUrl): Boolean {
        if (!isDevelopmentBuild) {
            val official = Url(PRODUCTION_SERVER_URL)
            return requestUrl.protocol.name.equals("https", ignoreCase = true) &&
                    requestUrl.host.equals(official.host, ignoreCase = true)
        }
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
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 60_000
            }

            if (tokenStore != null) {
                val session = createClientPlugin("GymVipSession") {
                    onRequest { request, _ ->
                        val target = request.url.build()
                        val path = target.encodedPath
                        val publicRead = request.method == io.ktor.http.HttpMethod.Get &&
                            (path.startsWith("/api/v1/public/") ||
                                (path == "/api/v1/group-promotions" && target.parameters["onlyActive"] != "false") ||
                                (path.startsWith("/api/v1/gyms/") && path.endsWith("/reviews")))
                        if (!publicRead && isAuthorizedOrigin(target, baseUrl)) {
                            tokenStore.getAccessToken()?.let { token ->
                                if (token.isNotBlank()) {
                                    request.bearerAuth(token)
                                }
                            }
                        }
                    }
                }
                install(session)
            }

            val perfPlugin = createClientPlugin("GymVipPerfTracking") {
                val traceKey = io.ktor.util.AttributeKey<com.gymvip.mobile.telemetry.NetworkTraceHandle>("GymVipNetworkTrace")
                onRequest { request, _ ->
                    val url = request.url.build().toString()
                    val method = request.method.value
                    val trace = com.gymvip.mobile.telemetry.GlobalNetworkTracker.startTrace(url, method)
                    request.attributes.put(traceKey, trace)
                }
                onResponse { response ->
                    val trace = response.call.attributes.getOrNull(traceKey)
                    val len = try { response.headers[io.ktor.http.HttpHeaders.ContentLength]?.toLongOrNull() ?: 0L } catch (_: Throwable) { 0L }
                    trace?.stop(response.status.value, len)
                }
            }
            install(perfPlugin)

            if (enableLogging) {
                install(Logging) {
                    level = if (isDevelopmentBuild) LogLevel.INFO else LogLevel.NONE
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

    /**
     * Mapea respuestas HTTP distinguiendo 401, 403, 404, 409, 400 y errores 5xx.
     * Nunca convierte errores de servidor en modo sin conexión.
     */
    suspend fun parseResponseError(response: HttpResponse): AppError {
        val status = response.status
        val body = try { response.bodyAsText() } catch (_: Exception) { "" }

        val problem = runCatching { kotlinx.serialization.json.Json.parseToJsonElement(body) as? kotlinx.serialization.json.JsonObject }.getOrNull()
        val message = (problem?.get("message") as? kotlinx.serialization.json.JsonPrimitive)
            ?.content?.takeIf { it.isNotBlank() && !it.contains("://") && !it.contains("/api/") }
            ?.take(240)
        val code = (problem?.get("code") as? kotlinx.serialization.json.JsonPrimitive)?.content
        return when (status) {
            HttpStatusCode.Unauthorized -> AppError.Unauthorized("Credenciales inválidas o sesión expirada")
            HttpStatusCode.Forbidden -> {
                if (body.contains("contraseña", ignoreCase = true) || body.contains("temporal", ignoreCase = true)) {
                    AppError.Forbidden("Debe cambiar su contraseña temporal antes de continuar")
                } else {
                    AppError.Forbidden("No tienes permiso para realizar esta acción.")
                }
            }
            HttpStatusCode.NotFound -> AppError.NotFound("El recurso solicitado no fue encontrado en el servidor")
            HttpStatusCode.Conflict -> AppError.Conflict(code ?: "CONFLICT", message ?: "Los datos cambiaron. Actualiza e inténtalo nuevamente.")
            HttpStatusCode.BadRequest -> AppError.Validation(message ?: "Datos de solicitud inválidos.")
            HttpStatusCode.GatewayTimeout, HttpStatusCode.RequestTimeout -> {
                AppError.Server(status.value, "Tiempo de espera agotado. El servidor se está iniciando, por favor reintente.")
            }
            HttpStatusCode.InternalServerError, HttpStatusCode.BadGateway, HttpStatusCode.ServiceUnavailable -> {
                AppError.Server(status.value, "El servicio del gimnasio no está disponible. Vuelve a intentarlo en unos minutos.")
            }
            else -> AppError.Server(status.value, "No se pudo completar la solicitud. Inténtalo de nuevo.")
        }
    }

    /**
     * Mapea excepciones de bajo nivel distinguiendo DNS, TLS, Timeout y Conexión.
     * Conserva CancellationException relanzándola inmediatamente.
     */
    fun mapThrowableToAppError(e: Throwable): AppError {
        if (e is CancellationException) throw e
        if (e is AppError) return e
        if (e is kotlinx.serialization.SerializationException || e is io.ktor.serialization.JsonConvertException) {
            return AppError.Sync("No se pudo interpretar la información recibida del gimnasio. Intenta actualizar nuevamente.")
        }

        val msg = e.message ?: ""
        val className = e::class.simpleName ?: ""

        return when {
            className.contains("Timeout", ignoreCase = true) || msg.contains("timed out", ignoreCase = true) -> {
                AppError.Network("El gimnasio tardó en responder. Revisa tu conexión y vuelve a intentarlo.")
            }
            className.contains("UnknownHost", ignoreCase = true) || msg.contains("Unable to resolve host", ignoreCase = true) -> {
                AppError.Network("El gimnasio no está disponible por ahora. Vuelve a intentarlo en unos minutos.")
            }
            className.contains("SSL", ignoreCase = true) || className.contains("Cert", ignoreCase = true) -> {
                AppError.Network("No se pudo establecer una conexión segura con el gimnasio.")
            }
            className.contains("Connect", ignoreCase = true) || msg.contains("Connection refused", ignoreCase = true) -> {
                AppError.Network("No pudimos conectar con el gimnasio. Revisa tu conexión y vuelve a intentarlo.")
            }
            else -> {
                AppError.Network("No pudimos conectar con el gimnasio. Revisa tu conexión y vuelve a intentarlo.")
            }
        }
    }

    /**
     * Ejecuta una llamada de red segura garantizando que CancellationException
     * no sea capturada erróneamente ni tragada en los flujos de corrutinas.
     */
    suspend inline fun <T> safeApiCall(crossinline block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: AppError) {
            Result.failure(e)
        } catch (e: Throwable) {
            Result.failure(mapThrowableToAppError(e))
        }
    }
}
