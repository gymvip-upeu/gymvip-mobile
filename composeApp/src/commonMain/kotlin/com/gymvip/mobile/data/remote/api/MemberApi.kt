package com.gymvip.mobile.data.remote.api

import com.gymvip.mobile.data.remote.dto.*
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
    private val baseUrl: String = GymVipHttpClient.baseUrl
) {
    /**
     * Consulta el espacio de trabajo unificado del socio autenticado (GET /api/v1/member-app/me).
     * Retorna cliente, vigencia/cobertura, periodos, pagos y asistencias en una sola invocación.
     */
    suspend fun getWorkspace(): Result<MemberWorkspaceDto> = runCatching {
        val response = client.get("$baseUrl/api/v1/member-app/me")
        if (response.status == HttpStatusCode.OK) {
            response.body<MemberWorkspaceDto>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    /**
     * Perfil del socio autenticado. Prioriza /api/v1/member-app/me y mantiene fallback a /api/v1/me/profile.
     */
    suspend fun getProfile(accountId: String = ""): Result<MemberProfileDto> = runCatching {
        val workspaceResult = runCatching {
            val response = client.get("$baseUrl/api/v1/member-app/me")
            if (response.status == HttpStatusCode.OK) {
                response.body<MemberWorkspaceDto>().toProfileDto()
            } else {
                throw GymVipHttpClient.parseResponseError(response)
            }
        }

        if (workspaceResult.isSuccess) {
            workspaceResult.getOrThrow()
        } else {
            val fallbackResponse = client.get("$baseUrl/api/v1/me/profile")
            if (fallbackResponse.status == HttpStatusCode.OK) {
                fallbackResponse.body<MemberProfileDto>()
            } else {
                throw workspaceResult.exceptionOrNull() ?: GymVipHttpClient.parseResponseError(fallbackResponse)
            }
        }
    }

    /**
     * Resumen de membresía y vigencia. Prioriza /api/v1/member-app/me y mantiene fallback a /api/v1/me/membership.
     */
    suspend fun getMembership(accountId: String = ""): Result<MembershipSummaryDto> = runCatching {
        val workspaceResult = runCatching {
            val response = client.get("$baseUrl/api/v1/member-app/me")
            if (response.status == HttpStatusCode.OK) {
                response.body<MemberWorkspaceDto>().toMembershipDto()
            } else {
                throw GymVipHttpClient.parseResponseError(response)
            }
        }

        if (workspaceResult.isSuccess) {
            workspaceResult.getOrThrow()
        } else {
            val fallbackResponse = client.get("$baseUrl/api/v1/me/membership")
            if (fallbackResponse.status == HttpStatusCode.OK) {
                fallbackResponse.body<MembershipSummaryDto>()
            } else {
                throw workspaceResult.exceptionOrNull() ?: GymVipHttpClient.parseResponseError(fallbackResponse)
            }
        }
    }

    /**
     * Historial de pagos. Prioriza /api/v1/member-app/me y mantiene fallback a /api/v1/me/payments.
     */
    suspend fun getPayments(accountId: String = ""): Result<List<PaymentSummaryDto>> = runCatching {
        val workspaceResult = runCatching {
            val response = client.get("$baseUrl/api/v1/member-app/me")
            if (response.status == HttpStatusCode.OK) {
                response.body<MemberWorkspaceDto>().toPaymentsDto()
            } else {
                throw GymVipHttpClient.parseResponseError(response)
            }
        }

        if (workspaceResult.isSuccess) {
            workspaceResult.getOrThrow()
        } else {
            val fallbackResponse = client.get("$baseUrl/api/v1/me/payments")
            if (fallbackResponse.status == HttpStatusCode.OK) {
                fallbackResponse.body<List<PaymentSummaryDto>>()
            } else {
                throw workspaceResult.exceptionOrNull() ?: GymVipHttpClient.parseResponseError(fallbackResponse)
            }
        }
    }

    /**
     * Generación de credencial QR dinámica (GET /api/v1/member-app/qr, HU-13).
     * TTL: 60 segundos con firma temporal para validación en mostrador.
     */
    suspend fun getDynamicQr(): Result<DynamicQrResponseDto> = runCatching {
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
    suspend fun getAttendance(): Result<List<MemberAttendanceDto>> = runCatching {
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
    suspend fun getSessionStatus(): Result<MemberSessionStatusDto> = runCatching {
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
    suspend fun changePassword(request: PasswordChangeRequestDto): Result<PasswordChangeResponseDto> = runCatching {
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
}
