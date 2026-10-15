package com.gymvip.mobile.domain.usecase

import com.gymvip.mobile.domain.model.Faq
import com.gymvip.mobile.domain.model.InboxItem
import com.gymvip.mobile.domain.repository.FaqRepository
import com.gymvip.mobile.domain.repository.InboxRepository
import kotlinx.coroutines.flow.Flow

class GetFaqsUseCase(private val faqRepository: FaqRepository) {
    fun observe(): Flow<List<Faq>> = faqRepository.observeFaqs()
    suspend fun refresh(): Result<List<Faq>> = faqRepository.refreshFaqs()
}

class GetInboxUseCase(private val inboxRepository: InboxRepository) {
    fun observe(accountId: String): Flow<List<InboxItem>> = inboxRepository.observeInbox(accountId)
    suspend fun refresh(accountId: String): Result<List<InboxItem>> = inboxRepository.refreshInbox(accountId)
    suspend fun markAsRead(accountId: String, itemId: String): Result<Unit> = inboxRepository.markAsRead(accountId, itemId)
}
