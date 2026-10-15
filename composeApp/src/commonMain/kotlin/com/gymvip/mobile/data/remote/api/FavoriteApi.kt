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

class FavoriteApi(
    private val client: HttpClient,
    private val baseUrl: String = GymVipHttpClient.baseUrl
) {
    suspend fun getFavorites(): Result<List<FavoriteResponseDto>> = runCatching {
        val response = client.get("$baseUrl/api/v1/me/favorites")
        if (response.status == HttpStatusCode.OK) {
            response.body<List<FavoriteResponseDto>>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    suspend fun getFavoriteById(id: String): Result<FavoriteResponseDto> = runCatching {
        val response = client.get("$baseUrl/api/v1/me/favorites/$id")
        if (response.status == HttpStatusCode.OK) {
            response.body<FavoriteResponseDto>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    suspend fun addFavorite(request: FavoriteRequestDto): Result<FavoriteResponseDto> = runCatching {
        val response = client.post("$baseUrl/api/v1/me/favorites") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
            response.body<FavoriteResponseDto>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    suspend fun updateNote(id: String, request: FavoriteUpdateNoteDto): Result<FavoriteResponseDto> = runCatching {
        val response = client.put("$baseUrl/api/v1/me/favorites/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status == HttpStatusCode.OK) {
            response.body<FavoriteResponseDto>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    suspend fun deleteFavorite(id: String): Result<Unit> = runCatching {
        val response = client.delete("$baseUrl/api/v1/me/favorites/$id")
        if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.NoContent) {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }
}
