package pe.edu.upeu.gymvip.data.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import pe.edu.upeu.gymvip.data.local.GymVipDatabase
import pe.edu.upeu.gymvip.data.remote.api.FavoriteApi
import pe.edu.upeu.gymvip.data.remote.api.GymVipHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncQueueManagerTest {

    private lateinit var database: GymVipDatabase

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GymVipDatabase.Schema.create(driver)
        database = GymVipDatabase(driver)
    }

    @Test
    fun testEnqueueAndDeduplicateCreateAndUpdate() = runTest {
        val mockEngine = MockEngine { respond("", HttpStatusCode.OK) }
        val client = GymVipHttpClient.create(engine = mockEngine, enableLogging = false)
        val api = FavoriteApi(client)
        val queueManager = SyncQueueManager(database, api)

        // 1. Encolar CREATE
        queueManager.enqueueFavoriteCreate("temp_1", "acc_1", "prod_1", "Nota inicial")
        val ops1 = database.gymVipDatabaseQueries.selectPendingOperations("acc_1").executeAsList()
        assertEquals(1, ops1.size)
        assertEquals("CREATE", ops1[0].operation)

        // 2. Encolar UPDATE sobre la misma entidad antes del envío
        queueManager.enqueueFavoriteUpdate("temp_1", "acc_1", "Nota modificada")
        val ops2 = database.gymVipDatabaseQueries.selectPendingOperations("acc_1").executeAsList()
        assertEquals(1, ops2.size) // No duplica la fila, optimiza el payload existente
        assertEquals("CREATE", ops2[0].operation)
    }

    @Test
    fun testEnqueueCreateThenDeleteCancelsOperationLocally() = runTest {
        val mockEngine = MockEngine { respond("", HttpStatusCode.OK) }
        val client = GymVipHttpClient.create(engine = mockEngine, enableLogging = false)
        val api = FavoriteApi(client)
        val queueManager = SyncQueueManager(database, api)

        // Insertar registro local provisional
        database.gymVipDatabaseQueries.insertOrReplaceFavorite(
            id = "temp_2",
            accountId = "acc_1",
            productId = "prod_2",
            productName = "Shaker",
            productCategory = "MERCHANDISING",
            productPriceCents = 4500L,
            personalNote = "Nota",
            createdAt = "2026-10-15T10:00:00Z",
            updatedAt = "2026-10-15T10:00:00Z",
            syncStatus = "PENDING_CREATE"
        )
        queueManager.enqueueFavoriteCreate("temp_2", "acc_1", "prod_2", "Nota")

        // Eliminar antes de sincronizar
        queueManager.enqueueFavoriteDelete("temp_2", "acc_1")

        // La cola se vacía y el registro local se elimina físicamente sin necesidad de llamada HTTP
        val ops = database.gymVipDatabaseQueries.selectPendingOperations("acc_1").executeAsList()
        assertEquals(0, ops.size)

        val local = database.gymVipDatabaseQueries.selectFavoriteById("temp_2", "acc_1").executeAsOneOrNull()
        assertEquals(null, local)
    }

    @Test
    fun testProcessQueueSuccessReconciliation() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """{
                    "id":"server_fav_999",
                    "accountId":"acc_1",
                    "productId":"prod_1",
                    "productName":"Proteína",
                    "productCategory":"SUPLEMENTO",
                    "pricePen":165.0,
                    "personalNote":"Nota de prueba",
                    "createdAt":"2026-10-15T10:00:00Z",
                    "updatedAt":"2026-10-15T10:00:00Z"
                }""",
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = GymVipHttpClient.create(engine = mockEngine, enableLogging = false)
        val api = FavoriteApi(client)
        val queueManager = SyncQueueManager(database, api)

        // Encolar y procesar
        queueManager.enqueueFavoriteCreate("temp_local_id", "acc_1", "prod_1", "Nota de prueba")
        val processed = queueManager.processQueue("acc_1").getOrThrow()

        assertEquals(1, processed)
        // La cola debe estar vacía
        val pending = database.gymVipDatabaseQueries.selectPendingOperations("acc_1").executeAsList()
        assertEquals(0, pending.size)

        // El registro debe existir con el ID emitido por el servidor
        val reconciled = database.gymVipDatabaseQueries.selectFavoriteById("server_fav_999", "acc_1").executeAsOneOrNull()
        assertEquals("server_fav_999", reconciled?.id)
        assertEquals("SYNCED", reconciled?.syncStatus)
    }
}
