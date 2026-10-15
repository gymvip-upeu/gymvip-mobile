package com.gymvip.mobile.domain.model

data class Faq(
    val id: String,
    val question: String,
    val answer: String,
    val category: String,
    val orderIndex: Int
)

enum class InboxCategory {
    RECORDATORIO,
    VENCIMIENTO,
    COMUNICADO,
    PROMOCION
}

data class InboxItem(
    val id: String,
    val accountId: String,
    val title: String,
    val message: String,
    val category: InboxCategory,
    val createdAt: String,
    val isRead: Boolean
)
