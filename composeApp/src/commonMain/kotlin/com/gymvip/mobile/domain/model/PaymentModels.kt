package com.gymvip.mobile.domain.model

enum class PaymentMethod {
    YAPE,
    PLIN,
    EFECTIVO,
    TARJETA,
    OTRO
}

data class PaymentSummary(
    val id: String,
    val accountId: String,
    val membershipId: String?,
    val amount: MoneyPen,
    val paymentMethod: PaymentMethod,
    val externalReference: String?,
    val paidAt: String,
    val notes: String?,
    val lastSyncedAt: String
)
