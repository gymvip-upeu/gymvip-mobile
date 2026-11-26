package com.gymvip.mobile.features.training

import com.gymvip.mobile.data.remote.api.GymVipHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonElement

class EnterpriseTrainingApi(
    private val client: HttpClient,
    private val explicitBaseUrl: String? = null
) {
    private val baseUrl: String get() = explicitBaseUrl ?: GymVipHttpClient.baseUrl

    private suspend inline fun <reified T> execute(crossinline request: suspend () -> HttpResponse): Result<T> =
        GymVipHttpClient.safeApiCall {
            val response = request()
            if (!response.status.isSuccess()) {
                throw GymVipHttpClient.parseResponseError(response)
            }
            response.body()
        }

    suspend fun listPublishedGyms(query: String? = null, district: String? = null): Result<List<PublishedGymDto>> =
        execute {
            client.get("$baseUrl/api/v1/public/gyms") {
                if (!query.isNullOrBlank()) parameter("q", query)
                if (!district.isNullOrBlank()) parameter("district", district)
            }
        }

    suspend fun listExercises(muscleGroup: String? = null): Result<List<ExerciseDto>> =
        execute {
            client.get("$baseUrl/api/v1/training/exercises") {
                if (!muscleGroup.isNullOrBlank()) parameter("muscleGroup", muscleGroup)
            }
        }

    suspend fun listRoutines(): Result<List<WorkoutRoutineDto>> =
        execute {
            client.get("$baseUrl/api/v1/training/routines")
        }

    suspend fun recordSession(payload: WorkoutSessionPayload): Result<Map<String, JsonElement>> =
        execute {
            client.post("$baseUrl/api/v1/training/sessions") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
        }

    suspend fun getProgress(): Result<WorkoutProgressDto> =
        execute {
            client.get("$baseUrl/api/v1/training/progress")
        }

    suspend fun listClasses(): Result<List<GymClassDto>> =
        execute {
            client.get("$baseUrl/api/v1/training/classes")
        }

    suspend fun reserveClass(classId: String): Result<Map<String, JsonElement>> =
        execute {
            client.post("$baseUrl/api/v1/training/classes/$classId/reserve")
        }

    suspend fun cancelClassReservation(classId: String): Result<Map<String, JsonElement>> =
        execute {
            client.post("$baseUrl/api/v1/training/classes/$classId/cancel")
        }

    suspend fun getPreferences(): Result<UserPreferencesDto> =
        execute {
            client.get("$baseUrl/api/v1/preferences")
        }

    suspend fun updatePreferences(dto: UserPreferencesDto): Result<UserPreferencesDto> =
        execute {
            client.put("$baseUrl/api/v1/preferences") {
                contentType(ContentType.Application.Json)
                setBody(dto)
            }
        }
}
