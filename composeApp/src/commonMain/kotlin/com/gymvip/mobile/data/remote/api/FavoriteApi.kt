package com.gymvip.mobile.data.remote.api

import com.gymvip.mobile.data.remote.dto.FavoriteRequestDto
import com.gymvip.mobile.data.remote.dto.FavoriteResponseDto
import com.gymvip.mobile.data.remote.dto.FavoriteUpdateNoteDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

/**
 * Cliente Ktor para endpoints de favoritos del socio.
 * Conciliado formalmente con MemberAppController (/api/v1/member-app/favorites).
 */
class FavoriteApi(
    private val client: HttpClient,
    private val baseUrl: String = GymVipHttpClient.baseUrl
) {
    suspend fun getFavorites(): Result<List<FavoriteResponseDto>> = runCatching {
        val response = client.get("$baseUrl/api/v1/member-app/favorites")
        if (response.status == HttpStatusCode.OK) {
            response.body<List<FavoriteResponseDto>>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    suspend fun getFavoriteById(id: String): Result<FavoriteResponseDto> = runCatching {
        val response = client.get("$baseUrl/api/v1/member-app/favorites/$id")
        if (response.status == HttpStatusCode.OK) {
            response.body<FavoriteResponseDto>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    suspend fun addFavorite(request: FavoriteRequestDto): Result<FavoriteResponseDto> = runCatching {
        // Enviar notas y títulos compatibles con el backend
        val bodyPayload = if (request.notes.isBlank() && request.personalNote.isNotBlank()) {
            request.copy(notes = request.personalNote)
        } else {
            request
        }
        val response = client.post("$baseUrl/api/v1/member-app/favorites") {
            contentType(ContentType.Application.Json)
            setBody(bodyPayload)
        }
        if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
            response.body<FavoriteResponseDto>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    suspend fun updateNote(id: String, request: FavoriteUpdateNoteDto): Result<FavoriteResponseDto> = runCatching {
        val bodyPayload = if (request.notes.isBlank() && request.personalNote.isNotBlank()) {
            request.copy(notes = request.personalNote)
        } else {
            request
        }
        val response = client.put("$baseUrl/api/v1/member-app/favorites/$id") {
            contentType(ContentType.Application.Json)
            setBody(bodyPayload)
        }
        if (response.status == HttpStatusCode.OK) {
            response.body<FavoriteResponseDto>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    suspend fun deleteFavorite(id: String): Result<Unit> = runCatching {
        val response = client.delete("$baseUrl/api/v1/member-app/favorites/$id")
        if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.NoContent) {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }
}
