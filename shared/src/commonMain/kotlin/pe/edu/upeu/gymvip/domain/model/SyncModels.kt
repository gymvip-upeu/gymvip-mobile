package pe.edu.upeu.gymvip.domain.model

enum class SyncAction {
    CREATE,
    UPDATE,
    DELETE
}

data class SyncOperation(
    val id: String,
    val accountId: String,
    val entityType: String,
    val entityId: String,
    val operation: SyncAction,
    val payloadJson: String,
    val baseVersion: Int = 1,
    val attempts: Int = 0,
    val createdAt: String,
    val lastError: String? = null
)
