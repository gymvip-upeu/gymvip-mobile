package pe.edu.upeu.gymvip.domain.model

enum class MembershipStatus {
    VIGENTE,
    PROXIMA_A_VENCER,
    VENCIDA,
    SIN_MEMBRESIA,
    CANCELADA,
    PENDIENTE,
    DESCONOCIDO
}

data class MembershipSummary(
    val id: String,
    val accountId: String,
    val planName: String,
    val startDate: String,
    val endDate: String,
    val status: MembershipStatus,
    val daysRemaining: Int,
    val lastSyncedAt: String,
    val durationDays: Int? = null,
    val benefits: String? = null,
    val conditions: String? = null,
    val backendReason: String? = null
) {
    val isAccessAllowed: Boolean get() = status == MembershipStatus.VIGENTE || status == MembershipStatus.PROXIMA_A_VENCER
}

data class MembershipPeriod(
    val id: String,
    val planName: String,
    val startDate: String,
    val endDate: String,
    val totalDays: Int
)
