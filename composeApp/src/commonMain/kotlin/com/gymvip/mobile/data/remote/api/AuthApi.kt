package com.gymvip.mobile.data.remote.api

import com.gymvip.mobile.data.remote.dto.CurrentUserDto
import com.gymvip.mobile.data.remote.dto.ActivationRequestDto
import com.gymvip.mobile.data.remote.dto.LoginRequestDto
import com.gymvip.mobile.data.remote.dto.LoginResponseDto
import com.gymvip.mobile.data.remote.dto.PasswordResetRequestDto
import com.gymvip.mobile.data.remote.dto.PasswordResetResponseDto
import com.gymvip.mobile.data.remote.dto.PasswordResetConfirmRequestDto
import com.gymvip.mobile.data.remote.dto.PasswordResetConfirmResponseDto
import com.gymvip.mobile.domain.error.AppError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class AuthApi(
    private val client: HttpClient,
    private val baseUrl: String = GymVipHttpClient.baseUrl
) {
    suspend fun login(request: LoginRequestDto): Result<LoginResponseDto> = runCatching {
        val response = client.post("$baseUrl/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status == HttpStatusCode.OK) {
            response.body<LoginResponseDto>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    suspend fun getCurrentUser(): Result<CurrentUserDto> = runCatching {
        val response = client.get("$baseUrl/api/v1/auth/me")
        if (response.status == HttpStatusCode.OK) {
            response.body<CurrentUserDto>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    suspend fun logout(): Result<Unit> = runCatching {
        val response = client.post("$baseUrl/api/v1/auth/logout")
        if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.NoContent) {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    suspend fun activateAccount(request: ActivationRequestDto): Result<Unit> = runCatching {
        val response = client.post("$baseUrl/api/v1/auth/activate") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.NoContent) {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    suspend fun requestPasswordReset(request: PasswordResetRequestDto): Result<PasswordResetResponseDto> = runCatching {
        val response = client.post("$baseUrl/api/v1/auth/password-reset/request") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status == HttpStatusCode.OK) {
            response.body<PasswordResetResponseDto>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    suspend fun confirmPasswordReset(request: PasswordResetConfirmRequestDto): Result<PasswordResetConfirmResponseDto> = runCatching {
        val response = client.post("$baseUrl/api/v1/auth/password-reset/confirm") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status == HttpStatusCode.OK) {
            response.body<PasswordResetConfirmResponseDto>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }
}
