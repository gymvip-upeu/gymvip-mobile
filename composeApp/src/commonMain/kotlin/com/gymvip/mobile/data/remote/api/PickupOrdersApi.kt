package com.gymvip.mobile.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

class PickupOrdersApi(private val client: HttpClient) {
    private val url get() = "${GymVipHttpClient.baseUrl}/api/v1/member-app/store/orders"

    suspend fun list(): PickupOrders = client.get(url).checkedBody()

    suspend fun create(request: PickupRequest): PickupOrder =
        client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.checkedBody()

    suspend fun refresh(id: String): PickupOrder = client.post("$url/$id/refresh").checkedBody()

    private suspend inline fun <reified T> HttpResponse.checkedBody(): T {
        if (!status.isSuccess()) throw GymVipHttpClient.parseResponseError(this)
        return body()
    }
}

@Serializable
data class PickupRequest(val requestId: String, val productId: String, val quantity: Int,
                         val expectedUnitPricePen: Double)

@Serializable
data class PickupOrders(val sandboxEnabled: Boolean, val orders: List<PickupOrder>)

@Serializable
data class PickupOrder(val id: String, val productName: String, val quantity: Int,
                       val totalPen: Double, val status: String, val checkoutUrl: String? = null)
