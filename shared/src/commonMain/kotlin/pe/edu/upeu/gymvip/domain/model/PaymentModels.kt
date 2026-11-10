package pe.edu.upeu.gymvip.domain.model

enum class PaymentMethod {
    YAPE,
    PLIN,
    EFECTIVO,
    TARJETA,
    TRANSFERENCIA,
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
    val lastSyncedAt: String,
    val planName: String? = null,
    val state: String? = null,
    val originalMethod: String? = null
)
