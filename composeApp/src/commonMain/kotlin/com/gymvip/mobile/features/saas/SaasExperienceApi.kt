package com.gymvip.mobile.features.saas

import com.gymvip.mobile.data.remote.api.GymVipHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class SaasExperienceApi(
    private val client: HttpClient,
    private val explicitBaseUrl: String? = null
) {
    private val baseUrl: String get() = explicitBaseUrl ?: GymVipHttpClient.baseUrl

    private suspend inline fun <reified T> execute(crossinline block: suspend () -> HttpResponse): Result<T> =
        GymVipHttpClient.safeApiCall {
            val response = block()
            if (!response.status.isSuccess()) {
                throw GymVipHttpClient.parseResponseError(response)
            }
            response.body()
        }

    suspend fun listPublicGymsShowcase(query: String? = null, district: String? = null): Result<List<PublicGymShowcaseDto>> =
        execute {
            client.get("$baseUrl/api/v1/public/gyms") {
                if (!query.isNullOrBlank()) parameter("q", query)
                if (!district.isNullOrBlank()) parameter("district", district)
            }
        }

    suspend fun listGymReviews(organizationId: String): Result<List<GymReviewDto>> =
        execute { client.get("$baseUrl/api/v1/gyms/$organizationId/reviews") }

    suspend fun createGymReview(organizationId: String, req: CreateGymReviewRequest): Result<GymReviewDto> =
        execute {
            client.post("$baseUrl/api/v1/gyms/$organizationId/reviews") {
                contentType(ContentType.Application.Json)
                setBody(req)
            }
        }

    suspend fun listGroupPromotions(organizationId: String? = null): Result<List<GroupPromotionDto>> =
        execute {
            client.get("$baseUrl/api/v1/group-promotions") {
                if (!organizationId.isNullOrBlank()) parameter("organizationId", organizationId)
                parameter("onlyActive", true)
            }
        }

    suspend fun createGroupEnrollment(req: CreateGroupEnrollmentRequest): Result<GroupEnrollmentDto> =
        execute {
            client.post("$baseUrl/api/v1/group-enrollments") {
                contentType(ContentType.Application.Json)
                setBody(req)
            }
        }

    suspend fun listMyGroupEnrollments(): Result<List<GroupEnrollmentDto>> =
        execute { client.get("$baseUrl/api/v1/group-enrollments/my") }

    suspend fun joinGroupEnrollment(invitationCode: String, acceptConsent: Boolean = true): Result<GroupEnrollmentDto> =
        execute {
            client.post("$baseUrl/api/v1/group-enrollments/$invitationCode/join") {
                contentType(ContentType.Application.Json)
                setBody(JoinGroupEnrollmentRequest(acceptConsent = acceptConsent))
            }
        }

    suspend fun getPhysicalProfile(): Result<PhysicalProfileDto> =
        execute { client.get("$baseUrl/api/v1/member-app/physical-profile") }

    suspend fun savePhysicalProfile(req: SavePhysicalProfileRequest): Result<PhysicalProfileDto> =
        execute {
            client.put("$baseUrl/api/v1/member-app/physical-profile") {
                contentType(ContentType.Application.Json)
                setBody(req)
            }
        }

    suspend fun deletePhysicalProfile(): Result<Unit> =
        GymVipHttpClient.safeApiCall {
            val response = client.delete("$baseUrl/api/v1/member-app/physical-profile")
            if (!response.status.isSuccess()) throw GymVipHttpClient.parseResponseError(response)
        }

    suspend fun listWeeklyPlans(): Result<List<WeeklyTrainingPlanDto>> =
        execute { client.get("$baseUrl/api/v1/member-app/weekly-plans") }

    suspend fun saveWeeklyPlan(req: SaveWeeklyPlanRequest): Result<WeeklyTrainingPlanDto> =
        execute {
            client.post("$baseUrl/api/v1/member-app/weekly-plans") {
                contentType(ContentType.Application.Json)
                setBody(req)
            }
        }

    suspend fun getNotificationPreferences(): Result<NotificationPreferencesDto> =
        execute { client.get("$baseUrl/api/v1/member-app/notification-preferences") }

    suspend fun updateNotificationPreferences(req: NotificationPreferencesDto): Result<NotificationPreferencesDto> =
        execute {
            client.put("$baseUrl/api/v1/member-app/notification-preferences") {
                contentType(ContentType.Application.Json)
                setBody(req)
            }
        }

    suspend fun getLatestAppRelease(currentVersionCode: Int = 200): Result<AppReleaseManifestDto> =
        execute {
            client.get("$baseUrl/api/v1/app/releases/latest") {
                parameter("platform", "ANDROID")
                parameter("channel", "PRODUCTION")
                parameter("currentVersionCode", currentVersionCode)
            }
        }
}
