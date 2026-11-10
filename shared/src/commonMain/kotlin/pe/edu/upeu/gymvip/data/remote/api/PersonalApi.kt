package pe.edu.upeu.gymvip.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import pe.edu.upeu.gymvip.data.remote.dto.*

class PersonalApi(private val client: HttpClient, private val base: String = GymVipHttpClient.baseUrl) {
    private suspend inline fun <reified T> get(path: String, query: Map<String, String> = emptyMap()): T {
        val response = client.get("$base/api/v1/$path") { query.forEach { (k,v) -> parameter(k,v) } }
        if (response.status.value !in 200..299) throw GymVipHttpClient.parseResponseError(response)
        return response.body()
    }
    suspend fun socios(query: String, page: Int) = get<PaginaSociosDto>("clients", mapOf("query" to query, "page" to page.toString(), "size" to "20"))
    suspend fun alertas() = get<TotalesGestionDto>("coverage", mapOf("state" to "ALERTAS", "size" to "1"))
    suspend fun socio(id: String) = get<SocioGestionDto>("clients/$id")
    suspend fun cobertura(id: String) = get<CoberturaGestionDto>("coverage/clients/$id")
    suspend fun workspace(id: String) = get<MemberWorkspaceDto>("members/$id")
    suspend fun planes() = get<List<PlanGestionDto>>("plans")
    suspend fun cuentas() = get<List<CuentaGestionDto>>("accounts")
    suspend fun guardarSocio(id: String?, body: GuardarSocioDto): SocioGestionDto {
        val response = client.request("$base/api/v1/clients" + if (id == null) "" else "/$id") {
            method = if (id == null) HttpMethod.Post else HttpMethod.Put
            contentType(ContentType.Application.Json); setBody(body)
        }
        if (response.status.value !in 200..299) throw GymVipHttpClient.parseResponseError(response)
        return response.body()
    }
    suspend fun guardarPlan(id: String?, body: GuardarPlanDto) {
        val response = client.request("$base/api/v1/plans" + if (id == null) "" else "/$id") {
            method = if (id == null) HttpMethod.Post else HttpMethod.Put
            contentType(ContentType.Application.Json); setBody(body)
        }
        if (response.status.value !in 200..299) throw GymVipHttpClient.parseResponseError(response)
    }
    suspend fun asistencia(id: String): String {
        val response = client.post("$base/api/v1/members/$id/attendance")
        if (response.status.value !in 200..299) throw GymVipHttpClient.parseResponseError(response)
        return response.body<AsistenciaGestionDto>().message
    }
}
