package pe.edu.upeu.gymvip.domain.usecase

import pe.edu.upeu.gymvip.domain.model.Faq
import pe.edu.upeu.gymvip.domain.model.InboxItem
import pe.edu.upeu.gymvip.domain.repository.FaqRepository
import pe.edu.upeu.gymvip.domain.repository.InboxRepository
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
