package com.gymvip.mobile.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.gymvip.mobile.data.local.Faq_item
import com.gymvip.mobile.data.local.GymVipDatabase
import com.gymvip.mobile.data.local.Inbox_item
import com.gymvip.mobile.domain.model.Faq
import com.gymvip.mobile.domain.model.InboxCategory
import com.gymvip.mobile.domain.model.InboxItem
import com.gymvip.mobile.domain.repository.FaqRepository
import com.gymvip.mobile.domain.repository.InboxRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class FaqRepositoryImpl(private val database: GymVipDatabase) : FaqRepository {

    init {
        seedInitialFaqsIfNeeded()
    }

    private fun seedInitialFaqsIfNeeded() {
        val count = database.gymVipDatabaseQueries.selectAllFaqs().executeAsList().size
        if (count == 0) {
            val faqs = listOf(
                Faq("faq_1", "¿Cómo funciona la vigencia de mi membresía mensual?", "La membresía mensual tiene una duración estricta de 30 días calendario continuos desde su inicio, con cero tolerancia de días de gracia.", "Membresías", 1),
                Faq("faq_2", "¿Puedo pagar o renovar mi plan desde la aplicación móvil?", "No. Por política de seguridad y operación autorizada, todo pago y renovación se efectúa exclusivamente en el mostrador de recepción.", "Pagos", 2),
                Faq("faq_3", "¿Puedo ver mis pagos anteriores si mi membresía ya venció?", "Sí. Su historial de pagos y perfil permanecen accesibles en la app mientras su cuenta de socio se mantenga habilitada.", "Historial", 3),
                Faq("faq_4", "¿Qué horarios de atención tiene el gimnasio?", "Lunes a viernes de 6:00 a.m. a 10:00 p.m., sábados de 7:00 a.m. a 8:00 p.m. y domingos de 8:00 a.m. a 2:00 p.m.", "Instalaciones", 4),
                Faq("faq_5", "¿Cómo solicito mi activación si es mi primera vez?", "Tras registrarse en recepción, el personal le entregará un comprobante con su código de activación presencial para configurar su contraseña personal.", "Cuenta", 5)
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
        return Result.success(database.gymVipDatabaseQueries.selectAllFaqs().executeAsList().map {
            Faq(it.id, it.question, it.answer, it.category, it.orderIndex.toInt())
        })
    }
}

class InboxRepositoryImpl(private val database: GymVipDatabase) : InboxRepository {

    override fun observeInbox(accountId: String): Flow<List<InboxItem>> {
        seedInitialInboxIfNeeded(accountId)
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

    private fun seedInitialInboxIfNeeded(accountId: String) {
        val existing = database.gymVipDatabaseQueries.selectInboxByAccountId(accountId).executeAsList()
        if (existing.isEmpty()) {
            val now = Clock.System.now().toString()
            database.gymVipDatabaseQueries.transaction {
                database.gymVipDatabaseQueries.insertOrReplaceInboxItem(
                    id = "msg_welcome_$accountId",
                    accountId = accountId,
                    title = "¡Bienvenido a GYM VIP!",
                    message = "Su cuenta digital de socio ha sido configurada. Puede consultar el estado de su membresía e historial de pagos.",
                    category = InboxCategory.COMUNICADO.name,
                    createdAt = now,
                    isRead = 0L
                )
                database.gymVipDatabaseQueries.insertOrReplaceInboxItem(
                    id = "msg_reminder_$accountId",
                    accountId = accountId,
                    title = "Recordatorio de higiene y toalla",
                    message = "Por respeto a todos los socios, el uso de toalla personal es obligatorio en todas las zonas de pesas y máquinas.",
                    category = InboxCategory.RECORDATORIO.name,
                    createdAt = now,
                    isRead = 0L
                )
            }
        }
    }

    override suspend fun markAsRead(accountId: String, itemId: String): Result<Unit> {
        database.gymVipDatabaseQueries.markInboxAsRead(itemId, accountId)
        return Result.success(Unit)
    }

    override suspend fun refreshInbox(accountId: String): Result<List<InboxItem>> {
        return Result.success(database.gymVipDatabaseQueries.selectInboxByAccountId(accountId).executeAsList().map {
            val cat = try { InboxCategory.valueOf(it.category) } catch (_: Exception) { InboxCategory.COMUNICADO }
            InboxItem(it.id, it.accountId, it.title, it.message, cat, it.createdAt, it.isRead == 1L)
        })
    }

    override suspend fun clearInbox(accountId: String) {
        database.gymVipDatabaseQueries.deleteInboxByAccountId(accountId)
    }
}
