package pe.edu.upeu.gymvip.domain.model

enum class AccountStatus {
    ACTIVO,
    INACTIVO,
    BLOQUEADO
}

data class MemberProfile(
    val accountId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val documentType: String,
    val documentNumber: String,
    val phone: String?,
    val status: AccountStatus,
    val lastSyncedAt: String,
    val memberCode: String? = null
) {
    val fullName: String get() = "$firstName $lastName"
}
