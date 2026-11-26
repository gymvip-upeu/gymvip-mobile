package com.gymvip.mobile.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.gymvip.mobile.data.local.GymVipDatabase
import com.gymvip.mobile.data.remote.api.GymVipHttpClient
import com.gymvip.mobile.data.remote.api.MemberApi
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FaqRepositoryTest {
    @Test
    fun failedRefreshReportsErrorWhilePreservingCachedAnswer() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GymVipDatabase.Schema.create(driver)
        val database = GymVipDatabase(driver)
        database.gymVipDatabaseQueries.insertOrReplaceFaq("published", "Pregunta", "Respuesta aprobada", "Cuenta", 1)
        val client = GymVipHttpClient.create(
            MockEngine { respond("{}", HttpStatusCode.ServiceUnavailable) },
            enableLogging = false
        )
        try {
            val repository = FaqRepositoryImpl(MemberApi(client, "http://test"), database)
            assertTrue(repository.refreshFaqs().isFailure)
            assertEquals("Respuesta aprobada", database.gymVipDatabaseQueries.selectAllFaqs().executeAsOne().answer)
        } finally {
            client.close()
            driver.close()
        }
    }

    @Test
    fun emptyPublishedCatalogRemovesPreviouslyCachedAnswer() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GymVipDatabase.Schema.create(driver)
        val database = GymVipDatabase(driver)
        database.gymVipDatabaseQueries.insertOrReplaceFaq("deactivated", "Pregunta", "Respuesta anterior", "Cuenta", 1)
        val client = GymVipHttpClient.create(
            MockEngine {
                respond("[]", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            },
            enableLogging = false
        )
        try {
            val repository = FaqRepositoryImpl(MemberApi(client, "http://test"), database)
            assertTrue(repository.refreshFaqs().isSuccess)
            assertTrue(database.gymVipDatabaseQueries.selectAllFaqs().executeAsList().isEmpty())
        } finally {
            client.close()
            driver.close()
        }
    }
}
