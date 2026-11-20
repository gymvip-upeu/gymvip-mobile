package com.gymvip.mobile.data.remote.api

import com.gymvip.mobile.data.remote.dto.*
import com.gymvip.mobile.domain.error.AppError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

/**
 * Cliente Ktor conciliado formalmente con los endpoints de MemberAppController (/api/v1/member-app)
 * en Spring Boot 3.4.3 para la experiencia del socio.
 */
class MemberApi(
    private val client: HttpClient,
    baseUrl: String? = null
) {
    private val explicitBaseUrl: String? = baseUrl
    private val baseUrl: String get() = explicitBaseUrl ?: GymVipHttpClient.baseUrl

    /**
     * Consulta el espacio de trabajo unificado del socio autenticado (GET /api/v1/member-app/me).
     * Retorna cliente, vigencia/cobertura, periodos, pagos y asistencias en una sola invocación.
     */
    suspend fun getWorkspace(): Result<MemberWorkspaceDto> = GymVipHttpClient.safeApiCall {
        val response = client.get("$baseUrl/api/v1/member-app/me")
        if (response.status == HttpStatusCode.OK) {
            response.body<MemberWorkspaceDto>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    /**
     * Perfil del socio autenticado. Prioriza /api/v1/member-app/me y solo realiza fallback
     * a /api/v1/me/profile si el servidor no tiene implementado el endpoint unificado (404/501).
     */
    suspend fun getProfile(accountId: String = ""): Result<MemberProfileDto> = GymVipHttpClient.safeApiCall {
        val workspaceResult = getWorkspace()
        if (workspaceResult.isSuccess) {
            return@safeApiCall workspaceResult.getOrThrow().toProfileDto()
        }

        val err = workspaceResult.exceptionOrNull()
        val shouldFallback = err is AppError.NotFound || (err is AppError.Server && (err.statusCode == 404 || err.statusCode == 501))
        if (!shouldFallback) {
            throw err ?: AppError.Network("Error al consultar datos de perfil")
        }

        val fallbackResponse = client.get("$baseUrl/api/v1/me/profile")
        if (fallbackResponse.status == HttpStatusCode.OK) {
            fallbackResponse.body<MemberProfileDto>()
        } else {
            throw GymVipHttpClient.parseResponseError(fallbackResponse)
        }
    }

    /**
     * Resumen de membresía y vigencia. Reutiliza /api/v1/member-app/me con fallback controlado.
     */
    suspend fun getMembership(accountId: String = ""): Result<MembershipSummaryDto> = GymVipHttpClient.safeApiCall {
        val workspaceResult = getWorkspace()
        if (workspaceResult.isSuccess) {
            return@safeApiCall workspaceResult.getOrThrow().toMembershipDto()
        }

        val err = workspaceResult.exceptionOrNull()
        val shouldFallback = err is AppError.NotFound || (err is AppError.Server && (err.statusCode == 404 || err.statusCode == 501))
        if (!shouldFallback) {
            throw err ?: AppError.Network("Error al consultar vigencia de membresía")
        }

        val fallbackResponse = client.get("$baseUrl/api/v1/me/membership")
        if (fallbackResponse.status == HttpStatusCode.OK) {
            fallbackResponse.body<MembershipSummaryDto>()
        } else {
            throw GymVipHttpClient.parseResponseError(fallbackResponse)
        }
    }

    /**
     * Historial de pagos. Reutiliza /api/v1/member-app/me con fallback controlado.
     */
    suspend fun getPayments(accountId: String = ""): Result<List<PaymentSummaryDto>> = GymVipHttpClient.safeApiCall {
        val workspaceResult = getWorkspace()
        if (workspaceResult.isSuccess) {
            return@safeApiCall workspaceResult.getOrThrow().toPaymentsDto()
        }

        val err = workspaceResult.exceptionOrNull()
        val shouldFallback = err is AppError.NotFound || (err is AppError.Server && (err.statusCode == 404 || err.statusCode == 501))
        if (!shouldFallback) {
            throw err ?: AppError.Network("Error al consultar historial de pagos")
        }

        val fallbackResponse = client.get("$baseUrl/api/v1/me/payments")
        if (fallbackResponse.status == HttpStatusCode.OK) {
            fallbackResponse.body<List<PaymentSummaryDto>>()
        } else {
            throw GymVipHttpClient.parseResponseError(fallbackResponse)
        }
    }

    /**
     * Generación de credencial QR dinámica (GET /api/v1/member-app/qr, HU-13).
     * TTL: 60 segundos con firma temporal para validación en mostrador.
     */
    suspend fun getDynamicQr(): Result<DynamicQrResponseDto> = GymVipHttpClient.safeApiCall {
        val response = client.get("$baseUrl/api/v1/member-app/qr")
        if (response.status == HttpStatusCode.OK) {
            response.body<DynamicQrResponseDto>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    /**
     * Historial de asistencias registradas en recepción (GET /api/v1/member-app/attendance).
     */
    suspend fun getAttendance(): Result<List<MemberAttendanceDto>> = GymVipHttpClient.safeApiCall {
        val response = client.get("$baseUrl/api/v1/member-app/attendance")
        if (response.status == HttpStatusCode.OK) {
            response.body<List<MemberAttendanceDto>>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    /**
     * Consulta si la sesión activa exige cambio forzoso de contraseña (GET /api/v1/member-app/session).
     */
    suspend fun getSessionStatus(): Result<MemberSessionStatusDto> = GymVipHttpClient.safeApiCall {
        val response = client.get("$baseUrl/api/v1/member-app/session")
        if (response.status == HttpStatusCode.OK) {
            response.body<MemberSessionStatusDto>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    /**
     * Cambio obligatorio de contraseña temporal (POST /api/v1/member-app/password, HU-05).
     */
    suspend fun changePassword(request: PasswordChangeRequestDto): Result<PasswordChangeResponseDto> = GymVipHttpClient.safeApiCall {
        val response = client.post("$baseUrl/api/v1/member-app/password") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status == HttpStatusCode.OK) {
            response.body<PasswordChangeResponseDto>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    /**
     * Consulta preguntas frecuentes oficiales (GET /api/v1/member-app/faqs, HU-28).
     */
    suspend fun getFaqs(): Result<List<FaqDto>> = GymVipHttpClient.safeApiCall {
        val response = client.get("$baseUrl/api/v1/member-app/faqs")
        if (response.status == HttpStatusCode.OK) {
            response.body<List<FaqDto>>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    /**
     * Consulta catálogo de servicios e instalaciones publicadas (GET /api/v1/member-app/services, HU-33).
     */
    suspend fun getServices(): Result<List<GymServiceDto>> = GymVipHttpClient.safeApiCall {
        val response = client.get("$baseUrl/api/v1/member-app/services")
        if (response.status == HttpStatusCode.OK) {
            response.body<List<GymServiceDto>>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    /**
     * Consulta bandeja de notificaciones y avisos del socio (GET /api/v1/member-app/inbox, HU-15, HU-34).
     */
    suspend fun getInbox(): Result<List<InboxItemDto>> = GymVipHttpClient.safeApiCall {
        val response = client.get("$baseUrl/api/v1/member-app/inbox")
        if (response.status == HttpStatusCode.OK) {
            response.body<List<InboxItemDto>>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    /**
     * Marca un aviso como leído (POST /api/v1/member-app/inbox/{id}/read, HU-34).
     */
    suspend fun markInboxAsRead(itemId: String): Result<Unit> = GymVipHttpClient.safeApiCall {
        val response = client.post("$baseUrl/api/v1/member-app/inbox/$itemId/read")
        if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.NoContent) {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    /**
     * Consulta el estado de consentimientos legales del socio (GET /api/v1/member-app/consents, HU-05).
     */
    suspend fun getConsents(): Result<ConsentStatusDto> = GymVipHttpClient.safeApiCall {
        val response = client.get("$baseUrl/api/v1/member-app/consents")
        if (response.status == HttpStatusCode.OK) {
            response.body<ConsentStatusDto>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    /**
     * Registra aceptación de términos, privacidad y consentimiento de comunicaciones (POST /api/v1/member-app/consents, HU-05, HU-15).
     */
    suspend fun submitConsents(request: ConsentSubmitRequestDto): Result<ConsentStatusDto> = GymVipHttpClient.safeApiCall {
        val response = client.post("$baseUrl/api/v1/member-app/consents") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status == HttpStatusCode.OK) {
            response.body<ConsentStatusDto>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    /**
     * Registra asistencia del socio escaneando el código QR de recepción (POST /api/v1/member-app/attendance/check-in).
     */
    suspend fun selfCheckIn(challenge: String, requestId: String? = null): Result<MemberCheckInResponseDto> = GymVipHttpClient.safeApiCall {
        val response = client.post("$baseUrl/api/v1/member-app/attendance/check-in") {
            contentType(ContentType.Application.Json)
            setBody(MemberSelfCheckInRequestDto(challenge = challenge, requestId = requestId))
        }
        if (response.status == HttpStatusCode.OK) {
            response.body<MemberCheckInResponseDto>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }
}
