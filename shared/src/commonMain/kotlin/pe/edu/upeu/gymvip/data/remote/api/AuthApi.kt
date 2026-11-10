package pe.edu.upeu.gymvip.data.remote.api

import pe.edu.upeu.gymvip.data.remote.dto.LoginRequestDto
import pe.edu.upeu.gymvip.data.remote.dto.LoginResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import pe.edu.upeu.gymvip.data.remote.dto.CurrentUserDto
import io.ktor.http.*

class AuthApi(private val client: HttpClient, private val baseUrl: String = GymVipHttpClient.baseUrl) {
    suspend fun currentUser(): Result<CurrentUserDto> = resultadoLlamada {
        val response = client.get("$baseUrl/api/v1/auth/me")
        if (response.status != HttpStatusCode.OK) throw GymVipHttpClient.parseResponseError(response)
        response.body<CurrentUserDto>()
    }
    suspend fun login(request: LoginRequestDto): Result<LoginResponseDto> = resultadoLlamada {
        val response = client.post("$baseUrl/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status != HttpStatusCode.OK) throw GymVipHttpClient.parseResponseError(response)
        response.body<LoginResponseDto>()
    }

    suspend fun logout(token: String? = null): Result<Unit> = resultadoLlamada {
        val response = client.post("$baseUrl/api/v1/auth/logout") { if (token != null) bearerAuth(token) }
        if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.NoContent)
            throw GymVipHttpClient.parseResponseError(response)
    }
}
