package com.gymvip.mobile.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

@Serializable
data class MemberSignupRequest(
    val firstName: String,
    val lastName: String,
    val dni: String,
    val birthDate: String,
    val email: String,
    val phone: String,
    val acceptedTerms: Boolean
) {
    override fun toString() = "MemberSignupRequest(datos omitidos)"
}

@Serializable
data class MemberSignupChallenge(val challengeId: String, val expiresInMinutes: Int)

@Serializable
private data class Availability(val enabled: Boolean)

@Serializable
private data class SignupVerification(val emailCode: String, val whatsappCode: String) {
    override fun toString() = "SignupVerification(códigos omitidos)"
}

class MemberSignupApi(private val client: HttpClient) {
    private val endpoint get() = "${GymVipHttpClient.baseUrl}/api/v1/public/member-signups"

    suspend fun isAvailable(): Result<Boolean> = runCatching {
        val response = client.get("$endpoint/availability")
        if (response.status != HttpStatusCode.OK) throw GymVipHttpClient.parseResponseError(response)
        response.body<Availability>().enabled
    }

    suspend fun start(request: MemberSignupRequest): Result<MemberSignupChallenge> = runCatching {
        val response = client.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status != HttpStatusCode.Accepted) throw GymVipHttpClient.parseResponseError(response)
        response.body<MemberSignupChallenge>()
    }

    suspend fun verify(challengeId: String, emailCode: String, whatsappCode: String): Result<Unit> = runCatching {
        val response = client.post("$endpoint/$challengeId/verify") {
            contentType(ContentType.Application.Json)
            setBody(SignupVerification(emailCode, whatsappCode))
        }
        if (response.status != HttpStatusCode.Accepted) throw GymVipHttpClient.parseResponseError(response)
    }
}
