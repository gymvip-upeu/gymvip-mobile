package com.gymvip.mobile.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.gymvip.mobile.data.local.Faq_item
import com.gymvip.mobile.data.local.GymVipDatabase
import com.gymvip.mobile.data.local.Inbox_item
import com.gymvip.mobile.data.remote.api.MemberApi
import com.gymvip.mobile.domain.model.Faq
import com.gymvip.mobile.domain.model.InboxCategory
import com.gymvip.mobile.domain.model.InboxItem
import com.gymvip.mobile.domain.repository.FaqRepository
import com.gymvip.mobile.domain.repository.InboxRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class FaqRepositoryImpl(
    private val memberApi: MemberApi,
    private val database: GymVipDatabase
) : FaqRepository {

    init {
        seedFallbackFaqsIfEmpty()
    }

    private fun seedFallbackFaqsIfEmpty() {
        val count = database.gymVipDatabaseQueries.selectAllFaqs().executeAsList().size
        if (count == 0) {
            val faqs = listOf(
                Faq("faq_1", "¿Cómo funciona la vigencia de mi membresía mensual?", "La membresía mensual tiene una duración estricta de 30 días calendario continuos desde su inicio, con cero tolerancia de días de gracia.", "Membresías", 1),
                Faq("faq_2", "¿Puedo pagar o renovar mi plan desde la aplicación móvil?", "No. Por política de seguridad y operación autorizada, todo pago y renovación se efectúa exclusivamente en el mostrador de recepción.", "Pagos", 2),
                Faq("faq_3", "¿Puedo ver mis pagos anteriores si mi membresía ya venció?", "Sí. Su historial de pagos y perfil permanecen accesibles en la app mientras su cuenta de socio se mantenga habilitada.", "Historial", 3),
                Faq("faq_4", "¿Qué horarios de atención tiene el gimnasio?", "Lunes a viernes de 6:00 a.m. a 10:00 p.m., sábados de 7:00 a.m. a 8:00 p.m. y domingos de 8:00 a.m. a 2:00 p.m.", "Instalaciones", 4),
                Faq("faq_5", "¿Cómo solicito mi activación si es mi primera vez?", "Tras registrarse en recepción, el personal le entregará un comprobante con su código de activación presencial para configurar su contraseña personal.", "Cuenta", 5),
                Faq("faq_6", "¿Cómo accedo a los suplementos y promociones?", "Consulte la sección Catálogo en la app móvil. Todas las compras y canjes se concretan directamente en recepción.", "Catálogo", 6)
            )
            database.gymVipDatabaseQueries.transaction {
                faqs.forEach { faq ->
                    database.gymVipDatabaseQueries.insertOrReplaceFaq(
                        id = faq.id,
                        question = faq.question,
                        answer = faq.answer,
                        category = faq.category,
                        orderIndex = faq.orderIndex.toLong()
                    )
                }
            }
        }
    }

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
