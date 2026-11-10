package pe.edu.upeu.gymvip.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import pe.edu.upeu.gymvip.data.local.Faq_item
import pe.edu.upeu.gymvip.data.local.GymVipDatabase
import pe.edu.upeu.gymvip.data.local.Inbox_item
import pe.edu.upeu.gymvip.data.remote.api.MemberApi
import pe.edu.upeu.gymvip.domain.model.Faq
import pe.edu.upeu.gymvip.domain.model.InboxCategory
import pe.edu.upeu.gymvip.domain.model.InboxItem
import pe.edu.upeu.gymvip.domain.repository.FaqRepository
import pe.edu.upeu.gymvip.domain.repository.InboxRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class FaqRepositoryImpl(
    private val memberApi: MemberApi,
    private val database: GymVipDatabase
) : FaqRepository {


    override fun observeFaqs(): Flow<List<Faq>> {
        return database.gymVipDatabaseQueries
            .selectAllFaqs()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list ->
                list.map {
                    Faq(
                        id = it.id,
                        question = it.question,
                        answer = it.answer,
                        category = it.category,
                        orderIndex = it.orderIndex.toInt()
                    )
                }
            }
    }

    override suspend fun refreshFaqs(): Result<List<Faq>> {
        val remoteResult = memberApi.getFaqs()
        return if (remoteResult.isSuccess) {
            val remoteList = remoteResult.getOrThrow()
            database.gymVipDatabaseQueries.transaction {
                database.gymVipDatabaseQueries.clearFaqs()
                remoteList.forEach { item ->
                    database.gymVipDatabaseQueries.insertOrReplaceFaq(
                        id = item.id,
                        question = item.question,
                        answer = item.answer,
                        category = item.category,
                        orderIndex = item.orderIndex.toLong()
                    )
                }
            }
            Result.success(remoteList.map { Faq(it.id, it.question, it.answer, it.category, it.orderIndex) })
        } else {
            val cached = database.gymVipDatabaseQueries.selectAllFaqs().executeAsList()
            if (cached.isNotEmpty()) {
                Result.success(cached.map { Faq(it.id, it.question, it.answer, it.category, it.orderIndex.toInt()) })
            } else {
                Result.failure(remoteResult.exceptionOrNull() ?: Exception("Error al cargar FAQs"))
            }
        }
    }
}

class InboxRepositoryImpl(
    private val memberApi: MemberApi,
    private val database: GymVipDatabase
) : InboxRepository {

    override fun observeInbox(accountId: String): Flow<List<InboxItem>> {
        return database.gymVipDatabaseQueries
            .selectInboxByAccountId(accountId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list ->
                list.map {
                    val cat = try { InboxCategory.valueOf(it.category) } catch (_: Exception) { InboxCategory.COMUNICADO }
                    InboxItem(
                        id = it.id,
                        accountId = it.accountId,
                        title = it.title,
                        message = it.message,
                        category = cat,
                        createdAt = it.createdAt,
                        isRead = it.isRead == 1L
                    )
                }
            }
    }

    override suspend fun markAsRead(accountId: String, itemId: String): Result<Unit> {
        database.gymVipDatabaseQueries.markInboxAsRead(itemId, accountId)
        val remote = memberApi.markInboxAsRead(itemId)
        return if (remote.isSuccess) Result.success(Unit) else Result.success(Unit)
    }

    override suspend fun refreshInbox(accountId: String): Result<List<InboxItem>> {
        val remoteResult = memberApi.getInbox()
        return if (remoteResult.isSuccess) {
            val remoteList = remoteResult.getOrThrow()
            database.gymVipDatabaseQueries.transaction {
                database.gymVipDatabaseQueries.deleteInboxByAccountId(accountId)
                remoteList.forEach { item ->
                    database.gymVipDatabaseQueries.insertOrReplaceInboxItem(
                        id = item.id,
                        accountId = accountId,
                        title = item.title,
                        message = item.message,
                        category = item.category,
                        createdAt = item.createdAt,
                        isRead = if (item.isRead) 1L else 0L
                    )
                }
            }
            Result.success(database.gymVipDatabaseQueries.selectInboxByAccountId(accountId).executeAsList().map {
                val cat = try { InboxCategory.valueOf(it.category) } catch (_: Exception) { InboxCategory.COMUNICADO }
                InboxItem(it.id, it.accountId, it.title, it.message, cat, it.createdAt, it.isRead == 1L)
            })
        } else {
            val cached = database.gymVipDatabaseQueries.selectInboxByAccountId(accountId).executeAsList()
            Result.success(cached.map {
                val cat = try { InboxCategory.valueOf(it.category) } catch (_: Exception) { InboxCategory.COMUNICADO }
                InboxItem(it.id, it.accountId, it.title, it.message, cat, it.createdAt, it.isRead == 1L)
            })
        }
    }

    override suspend fun clearInbox(accountId: String) {
        database.gymVipDatabaseQueries.deleteInboxByAccountId(accountId)
    }
}
