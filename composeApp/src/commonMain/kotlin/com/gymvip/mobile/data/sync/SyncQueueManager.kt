package com.gymvip.mobile.data.sync

import com.gymvip.mobile.data.local.GymVipDatabase
import com.gymvip.mobile.data.remote.api.FavoriteApi
import com.gymvip.mobile.data.remote.dto.FavoriteRequestDto
import com.gymvip.mobile.data.remote.dto.FavoriteUpdateNoteDto
import com.gymvip.mobile.domain.error.AppError
import com.gymvip.mobile.domain.model.SyncAction
import com.gymvip.mobile.domain.model.SyncStatus
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SyncQueueManager(
    private val database: GymVipDatabase,
    private val favoriteApi: FavoriteApi
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun enqueueFavoriteCreate(
        tempId: String,
        accountId: String,
        productId: String,
        note: String
    ) {
        val now = Clock.System.now().toString()
        val payload = """{"productId":"$productId","personalNote":"$note"}"""
        val opId = "op_${tempId}"

        database.gymVipDatabaseQueries.transaction {
            database.gymVipDatabaseQueries.insertSyncOperation(
                id = opId,
                accountId = accountId,
                entityType = "FAVORITE",
                entityId = tempId,
                operation = SyncAction.CREATE.name,
                payloadJson = payload,
                baseVersion = 1,
                attempts = 0,
                createdAt = now,
                lastError = null
            )
        }
    }

    fun enqueueFavoriteUpdate(
        favoriteId: String,
        accountId: String,
        newNote: String
    ) {
        val now = Clock.System.now().toString()
        val opId = "op_upd_${favoriteId}"

        database.gymVipDatabaseQueries.transaction {
            // Optimización: si ya existe una operación CREATE pendiente para esta entidad, actualizamos el payload de ese CREATE directamente
            val pendingOps = database.gymVipDatabaseQueries.selectPendingOperations(accountId).executeAsList()
            val existingCreate = pendingOps.find { it.entityId == favoriteId && it.operation == SyncAction.CREATE.name }

            if (existingCreate != null) {
                val parsed = json.parseToJsonElement(existingCreate.payloadJson).jsonObject
                val productId = parsed["productId"]?.jsonPrimitive?.content ?: ""
                val updatedPayload = """{"productId":"$productId","personalNote":"$newNote"}"""
                database.gymVipDatabaseQueries.deleteSyncOperation(existingCreate.id)
                database.gymVipDatabaseQueries.insertSyncOperation(
                    id = existingCreate.id,
                    accountId = accountId,
                    entityType = "FAVORITE",
                    entityId = favoriteId,
                    operation = SyncAction.CREATE.name,
                    payloadJson = updatedPayload,
                    baseVersion = existingCreate.baseVersion,
                    attempts = existingCreate.attempts,
                    createdAt = existingCreate.createdAt,
                    lastError = null
                )
            } else {
                val payload = """{"personalNote":"$newNote"}"""
                database.gymVipDatabaseQueries.insertSyncOperation(
                    id = opId,
                    accountId = accountId,
                    entityType = "FAVORITE",
                    entityId = favoriteId,
                    operation = SyncAction.UPDATE.name,
                    payloadJson = payload,
                    baseVersion = 1,
                    attempts = 0,
                    createdAt = now,
                    lastError = null
                )
            }
        }
    }

    fun enqueueFavoriteDelete(
        favoriteId: String,
        accountId: String
    ) {
        val now = Clock.System.now().toString()
        val opId = "op_del_${favoriteId}"

        database.gymVipDatabaseQueries.transaction {
            // Optimización: si el ítem estaba solo en PENDING_CREATE local y aún no subió al servidor,
            // cancelamos ambas operaciones y borramos físicamente de SQLite sin llamada HTTP
            val pendingOps = database.gymVipDatabaseQueries.selectPendingOperations(accountId).executeAsList()
            val existingCreate = pendingOps.find { it.entityId == favoriteId && it.operation == SyncAction.CREATE.name }

            if (existingCreate != null) {
                database.gymVipDatabaseQueries.deleteSyncOperation(existingCreate.id)
                database.gymVipDatabaseQueries.deleteFavoritePhysical(favoriteId, accountId)
            } else {
                database.gymVipDatabaseQueries.insertSyncOperation(
                    id = opId,
                    accountId = accountId,
                    entityType = "FAVORITE",
                    entityId = favoriteId,
                    operation = SyncAction.DELETE.name,
                    payloadJson = "{}",
                    baseVersion = 1,
                    attempts = 0,
                    createdAt = now,
                    lastError = null
                )
            }
        }
    }

    suspend fun processQueue(accountId: String): Result<Int> = runCatching {
        var processedCount = 0
        val operations = database.gymVipDatabaseQueries.selectPendingOperations(accountId).executeAsList()

        for (op in operations) {
            val action = try {
                SyncAction.valueOf(op.operation)
            } catch (_: Exception) {
                database.gymVipDatabaseQueries.deleteSyncOperation(op.id)
                continue
            }

            try {
                when (action) {
                    SyncAction.CREATE -> {
                        val parsed = json.parseToJsonElement(op.payloadJson).jsonObject
                        val productId = parsed["productId"]?.jsonPrimitive?.content ?: ""
                        val note = parsed["personalNote"]?.jsonPrimitive?.content ?: ""

                        val response = favoriteApi.addFavorite(FavoriteRequestDto(productId, note)).getOrThrow()

                        // Reconciliación: actualizar ID local si el servidor generó uno nuevo
                        database.gymVipDatabaseQueries.transaction {
                            if (op.entityId != response.id) {
                                database.gymVipDatabaseQueries.deleteFavoritePhysical(op.entityId, accountId)
                            }
                            database.gymVipDatabaseQueries.insertOrReplaceFavorite(
                                id = response.id,
                                accountId = accountId,
                                productId = response.productId,
                                productName = response.productName,
                                productCategory = response.productCategory,
                                productPriceCents = (response.pricePen * 100.0).toLong(),
                                personalNote = response.personalNote,
                                createdAt = response.createdAt,
                                updatedAt = response.updatedAt,
                                syncStatus = SyncStatus.SYNCED.name
                            )
                            database.gymVipDatabaseQueries.deleteSyncOperation(op.id)
                        }
                        processedCount++
                    }

                    SyncAction.UPDATE -> {
                        val parsed = json.parseToJsonElement(op.payloadJson).jsonObject
                        val note = parsed["personalNote"]?.jsonPrimitive?.content ?: ""

                        val response = favoriteApi.updateNote(op.entityId, FavoriteUpdateNoteDto(note)).getOrThrow()

                        database.gymVipDatabaseQueries.transaction {
                            database.gymVipDatabaseQueries.insertOrReplaceFavorite(
                                id = response.id,
                                accountId = accountId,
                                productId = response.productId,
                                productName = response.productName,
                                productCategory = response.productCategory,
                                productPriceCents = (response.pricePen * 100.0).toLong(),
                                personalNote = response.personalNote,
                                createdAt = response.createdAt,
                                updatedAt = response.updatedAt,
                                syncStatus = SyncStatus.SYNCED.name
                            )
                            database.gymVipDatabaseQueries.deleteSyncOperation(op.id)
                        }
                        processedCount++
                    }

                    SyncAction.DELETE -> {
                        favoriteApi.deleteFavorite(op.entityId).getOrThrow()
                        database.gymVipDatabaseQueries.transaction {
                            database.gymVipDatabaseQueries.deleteFavoritePhysical(op.entityId, accountId)
                            database.gymVipDatabaseQueries.deleteSyncOperation(op.id)
                        }
                        processedCount++
                    }
                }
            } catch (e: Exception) {
                if (e is AppError.Conflict) {
                    // Conflicto 409 detectado: marcar ítem local como CONFLICT para intervención visible del usuario
                    database.gymVipDatabaseQueries.transaction {
                        database.gymVipDatabaseQueries.markFavoritePendingDelete(
                            updatedAt = Clock.System.now().toString(),
                            id = op.entityId,
                            accountId = accountId
                        )
                        database.gymVipDatabaseQueries.deleteSyncOperation(op.id)
                    }
                } else if (e is AppError.Unauthorized) {
                    // Detener procesamiento si la sesión expiró para no reintentar en bucle
                    throw e
                } else {
                    val nextAttempt = op.attempts + 1
                    database.gymVipDatabaseQueries.updateSyncOperationAttempt(
                        attempts = nextAttempt,
                        lastError = e.message ?: "Error desconocido",
                        id = op.id
                    )
                    // Si superó reintentos o es error permanente de red, propagar y detener lote
                    throw e
                }
            }
        }
        processedCount
    }
}
