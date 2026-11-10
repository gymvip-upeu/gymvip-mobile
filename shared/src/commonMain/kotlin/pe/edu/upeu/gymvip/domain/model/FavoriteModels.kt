package pe.edu.upeu.gymvip.domain.model

enum class SyncStatus {
    SYNCED,
    PENDING_CREATE,
    PENDING_UPDATE,
    PENDING_DELETE,
    CONFLICT
}

data class Favorite(
    val id: String,
    val accountId: String,
    val productId: String,
    val productName: String,
    val productCategory: ProductCategory,
    val productPrice: MoneyPen,
    val personalNote: String,
    val createdAt: String,
    val updatedAt: String,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)
