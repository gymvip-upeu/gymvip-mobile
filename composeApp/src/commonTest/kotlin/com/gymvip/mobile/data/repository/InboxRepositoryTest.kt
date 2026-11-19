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
import kotlin.test.*

class InboxRepositoryTest {
    @Test fun failedReadKeepsNoticeUnreadAndCanBeRetried() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GymVipDatabase.Schema.create(driver)
        val database = GymVipDatabase(driver)
        database.gymVipDatabaseQueries.insertOrReplaceInboxItem("notice", "member", "Vencimiento", "Renueva en recepción", "RECORDATORIO", "2026-11-19T12:00:00Z", 0L)
        var offline = true
        val client = GymVipHttpClient.create(MockEngine {
            respond("{}", if (offline) HttpStatusCode.ServiceUnavailable else HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"))
        }, enableLogging = false)
        try {
            val repository = InboxRepositoryImpl(MemberApi(client, "http://test"), database)
            assertTrue(repository.markAsRead("member", "notice").isFailure)
            assertEquals(0L, database.gymVipDatabaseQueries.selectInboxByAccountId("member").executeAsOne().isRead)
            offline = false
            assertTrue(repository.markAsRead("member", "notice").isSuccess)
            assertEquals(1L, database.gymVipDatabaseQueries.selectInboxByAccountId("member").executeAsOne().isRead)
        } finally { client.close(); driver.close() }
    }
}
