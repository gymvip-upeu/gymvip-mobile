package com.gymvip.mobile.data.remote.api

import com.gymvip.mobile.data.remote.dto.MemberProfileDto
import com.gymvip.mobile.data.remote.dto.MembershipSummaryDto
import com.gymvip.mobile.data.remote.dto.PaymentSummaryDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode

class MemberApi(
    private val client: HttpClient,
    private val baseUrl: String = GymVipHttpClient.baseUrl
) {
    suspend fun getProfile(accountId: String): Result<MemberProfileDto> = runCatching {
        val response = client.get("$baseUrl/api/v1/me/profile")
        if (response.status == HttpStatusCode.OK) {
            response.body<MemberProfileDto>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    suspend fun getMembership(accountId: String): Result<MembershipSummaryDto> = runCatching {
        val response = client.get("$baseUrl/api/v1/me/membership")
        if (response.status == HttpStatusCode.OK) {
            response.body<MembershipSummaryDto>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }

    suspend fun getPayments(accountId: String): Result<List<PaymentSummaryDto>> = runCatching {
        val response = client.get("$baseUrl/api/v1/me/payments")
        if (response.status == HttpStatusCode.OK) {
            response.body<List<PaymentSummaryDto>>()
        } else {
            throw GymVipHttpClient.parseResponseError(response)
        }
    }
}
