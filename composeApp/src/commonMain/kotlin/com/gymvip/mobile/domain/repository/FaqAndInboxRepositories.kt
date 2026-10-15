package com.gymvip.mobile.domain.repository

import com.gymvip.mobile.domain.model.Faq
import com.gymvip.mobile.domain.model.InboxItem
import kotlinx.coroutines.flow.Flow

interface FaqRepository {
    fun observeFaqs(): Flow<List<Faq>>
    suspend fun refreshFaqs(): Result<List<Faq>>
}

interface InboxRepository {
    fun observeInbox(accountId: String): Flow<List<InboxItem>>
    suspend fun markAsRead(accountId: String, itemId: String): Result<Unit>
    suspend fun refreshInbox(accountId: String): Result<List<InboxItem>>
    suspend fun clearInbox(accountId: String)
}
