package com.gymvip.mobile.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MemberProfileDto(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val documentType: String,
    val documentNumber: String,
    val phone: String? = null,
    val status: String = "ACTIVO"
)

@Serializable
data class MembershipSummaryDto(
    val id: String,
    val clientId: String? = null,
    val planName: String,
    val startDate: String,
    val endDate: String,
    val status: String,
    val daysRemaining: Int = 0
)

@Serializable
data class PaymentSummaryDto(
    val id: String,
    val membershipId: String? = null,
    val amountPen: Double,
    val paymentMethod: String,
    val externalReference: String? = null,
    val paidAt: String,
    val notes: String? = null
)

@Serializable
data class ProductDto(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val pricePen: Double,
    val isPromotion: Boolean = false,
    val promotionDiscountPercent: Int? = null,
    val imageUrl: String? = null
)

@Serializable
data class FavoriteRequestDto(
    val productId: String,
    val personalNote: String
)

@Serializable
data class FavoriteUpdateNoteDto(
    val personalNote: String
)

@Serializable
data class FavoriteResponseDto(
    val id: String,
    val accountId: String? = null,
    val productId: String,
    val productName: String,
    val productCategory: String,
    val pricePen: Double,
    val personalNote: String,
    val createdAt: String,
    val updatedAt: String
)
