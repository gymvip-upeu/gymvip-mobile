package pe.edu.upeu.gymvip.data.remote.api

import pe.edu.upeu.gymvip.data.remote.dto.*
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
    suspend fun getWorkspace(): Result<MemberWorkspaceDto> = resultadoLlamada {
        val response = client.get("$baseUrl/api/v1/member-app/me")
        if (response.status == HttpStatusCode.OK) {
            response.body<MemberWorkspaceDto>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    /**
     * Perfil del socio autenticado desde /api/v1/member-app/me.
     */
    suspend fun getProfile(accountId: String = ""): Result<MemberProfileDto> = resultadoLlamada {
        val workspace = getWorkspace().getOrThrow()
        if (workspace.client == null) throw pe.edu.upeu.gymvip.domain.error.AppError.Server(502, "El servidor no devolvió el perfil del socio.")
        workspace.toProfileDto()
    }

    suspend fun getMembership(accountId: String = ""): Result<MembershipSummaryDto> =
        resultadoLlamada { getWorkspace().getOrThrow().toMembershipDto() }

    suspend fun getPayments(accountId: String = ""): Result<List<PaymentSummaryDto>> =
        resultadoLlamada { getWorkspace().getOrThrow().toPaymentsDto() }

    /**
     * Generación de credencial QR dinámica (GET /api/v1/member-app/qr, HU-13).
     * TTL: 60 segundos con firma temporal para validación en mostrador.
     */
    suspend fun getDynamicQr(): Result<DynamicQrResponseDto> = resultadoLlamada {
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
    suspend fun getAttendance(): Result<List<MemberAttendanceDto>> = resultadoLlamada {
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
    suspend fun getSessionStatus(): Result<MemberSessionStatusDto> = resultadoLlamada {
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
    suspend fun changePassword(request: PasswordChangeRequestDto): Result<PasswordChangeResponseDto> = resultadoLlamada {
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
    suspend fun getFaqs(): Result<List<FaqDto>> = resultadoLlamada {
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
    suspend fun getServices(): Result<List<GymServiceDto>> = resultadoLlamada {
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
    suspend fun getInbox(): Result<List<InboxItemDto>> = resultadoLlamada {
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
    suspend fun markInboxAsRead(itemId: String): Result<Unit> = resultadoLlamada {
        val response = client.post("$baseUrl/api/v1/member-app/inbox/$itemId/read")
        if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.NoContent) {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    /**
     * Consulta el estado de consentimientos legales del socio (GET /api/v1/member-app/consents, HU-05).
     */
    suspend fun getConsents(): Result<ConsentStatusDto> = resultadoLlamada {
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
    suspend fun submitConsents(request: ConsentSubmitRequestDto): Result<ConsentStatusDto> = resultadoLlamada {
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
}
