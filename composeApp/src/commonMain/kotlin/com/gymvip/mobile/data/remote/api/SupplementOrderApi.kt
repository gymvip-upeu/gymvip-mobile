package com.gymvip.mobile.data.remote.api

import com.gymvip.mobile.domain.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

@Serializable
private data class CartItemResponse(
    val supplementId: String, val name: String, val brand: String? = null,
    val presentation: String? = null, val flavor: String? = null,
    val unitPricePen: Double, val quantity: Int, val availableStock: Int? = null
)

@Serializable
private data class CartResponse(val items: List<CartItemResponse> = emptyList())

@Serializable
private data class UpdateCartRequest(val supplementId: String, val quantity: Int)

@Serializable
private data class CashCheckoutRequest(val paymentMethod: String, val customerNotes: String, val idempotencyKey: String)

@Serializable
private data class DigitalCheckoutRequest(val customerNotes: String, val idempotencyKey: String)

@Serializable
private data class PaymentConfigResponse(val culqiEnabled: Boolean = false)

@Serializable
private data class DigitalPaymentLinkResponse(
    val id: String, val orderId: String, val amountPen: Double,
    val status: String, val url: String, val expiresAt: String
)

@Serializable
private data class CancelOrderRequest(val reason: String)

@Serializable
private data class OrderResponse(
    val id: String, val orderNumber: String,
    val orderStatus: String, val paymentStatus: String, val fulfillmentStatus: String,
    val subtotalPen: Double, val discountPen: Double, val totalPen: Double,
    val paymentMethod: String, val paymentReference: String? = null,
    val reservationExpiresAtUtc: String? = null, val hasPickupToken: Boolean = false,
    val items: List<OrderItemResponse> = emptyList()
)

@Serializable
private data class OrderItemResponse(
    val productName: String, val productBrand: String, val productPresentation: String,
    val quantity: Int, val unitFinalPricePen: Double, val lineTotalPen: Double
)

@Serializable
private data class PickupTicketResponse(val pickupToken: String)

class SupplementOrderApi(private val client: HttpClient) {
    private val base get() = "${GymVipHttpClient.baseUrl}/api/v1/member-app"

    suspend fun cart(): Result<ShoppingCart> = GymVipHttpClient.safeApiCall {
        val response = client.get("$base/cart")
        if (!response.status.isSuccess()) throw GymVipHttpClient.parseResponseError(response)
        response.body<CartResponse>().toDomain()
    }

    suspend fun setQuantity(productId: String, quantity: Int): Result<ShoppingCart> = GymVipHttpClient.safeApiCall {
        val response = client.put("$base/cart") {
            contentType(ContentType.Application.Json)
            setBody(UpdateCartRequest(productId, quantity))
        }
        if (!response.status.isSuccess()) throw GymVipHttpClient.parseResponseError(response)
        response.body<CartResponse>().toDomain()
    }

    suspend fun clearCart(): Result<Unit> = GymVipHttpClient.safeApiCall {
        val response = client.delete("$base/cart")
        if (!response.status.isSuccess()) throw GymVipHttpClient.parseResponseError(response)
    }

    suspend fun checkoutCash(idempotencyKey: String, notes: String): Result<SupplementOrder> = GymVipHttpClient.safeApiCall {
        val response = client.post("$base/orders/checkout") {
            contentType(ContentType.Application.Json)
            setBody(CashCheckoutRequest("EFECTIVO_MOSTRADOR", notes, idempotencyKey))
        }
        if (!response.status.isSuccess()) throw GymVipHttpClient.parseResponseError(response)
        response.body<OrderResponse>().toDomain()
    }

    suspend fun paymentConfig(): Result<Boolean> = GymVipHttpClient.safeApiCall {
        val response = client.get("$base/orders/payment-config")
        if (!response.status.isSuccess()) throw GymVipHttpClient.parseResponseError(response)
        response.body<PaymentConfigResponse>().culqiEnabled
    }

    suspend fun checkoutDigital(idempotencyKey: String, notes: String): Result<DigitalPaymentLink> = GymVipHttpClient.safeApiCall {
        val response = client.post("$base/orders/checkout-culqi") {
            contentType(ContentType.Application.Json)
            setBody(DigitalCheckoutRequest(notes, idempotencyKey))
        }
        if (!response.status.isSuccess()) throw GymVipHttpClient.parseResponseError(response)
        response.body<DigitalPaymentLinkResponse>().toDomain()
    }

    suspend fun paymentLinkForOrder(orderId: String): Result<DigitalPaymentLink> = GymVipHttpClient.safeApiCall {
        val response = client.get("$base/orders/$orderId/payment-link")
        if (!response.status.isSuccess()) throw GymVipHttpClient.parseResponseError(response)
        response.body<DigitalPaymentLinkResponse>().toDomain()
    }

    suspend fun orders(): Result<List<SupplementOrder>> = GymVipHttpClient.safeApiCall {
        val response = client.get("$base/orders")
        if (!response.status.isSuccess()) throw GymVipHttpClient.parseResponseError(response)
        response.body<List<OrderResponse>>().map { order -> order.toDomain() }
    }

    suspend fun pickupToken(orderId: String): Result<String> = GymVipHttpClient.safeApiCall {
        val response = client.get("$base/orders/$orderId/pickup-ticket")
        if (!response.status.isSuccess()) throw GymVipHttpClient.parseResponseError(response)
        response.body<PickupTicketResponse>().pickupToken
    }

    suspend fun cancel(orderId: String, reason: String): Result<SupplementOrder> = GymVipHttpClient.safeApiCall {
        val response = client.post("$base/orders/$orderId/cancel") {
            contentType(ContentType.Application.Json)
            setBody(CancelOrderRequest(reason))
        }
        if (!response.status.isSuccess()) throw GymVipHttpClient.parseResponseError(response)
        response.body<OrderResponse>().toDomain()
    }

    private fun CartResponse.toDomain() = ShoppingCart(items.map { item ->
        val price = MoneyPen.fromDouble(item.unitPricePen)
        CartItem(
            Product(item.supplementId, item.name, "", ProductCategory.SUPLEMENTO, price,
                brand = item.brand, presentation = item.presentation, flavor = item.flavor,
                availability = when (item.availableStock) { null -> null; 0 -> "AGOTADO"; else -> "DISPONIBLE" }),
            price, item.quantity, item.availableStock
        )
    })

    private fun DigitalPaymentLinkResponse.toDomain() = DigitalPaymentLink(
        id, orderId, MoneyPen.fromDouble(amountPen), status, url, expiresAt
    )

    private fun OrderResponse.toDomain() = SupplementOrder(
        id, orderNumber,
        items.map { OrderItemSnapshot(it.productName, it.productBrand, it.productPresentation,
            it.quantity, MoneyPen.fromDouble(it.unitFinalPricePen), MoneyPen.fromDouble(it.lineTotalPen)) },
        MoneyPen.fromDouble(subtotalPen), MoneyPen.fromDouble(discountPen), MoneyPen.fromDouble(totalPen),
        enumValueOf(orderStatus), enumValueOf(paymentStatus), enumValueOf(fulfillmentStatus),
        paymentMethod, paymentReference, reservationExpiresAtUtc, null, hasPickupToken
    )
}
