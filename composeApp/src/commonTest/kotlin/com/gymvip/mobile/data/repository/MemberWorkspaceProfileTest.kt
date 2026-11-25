package com.gymvip.mobile.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.gymvip.mobile.data.local.GymVipDatabase
import com.gymvip.mobile.data.remote.api.GymVipHttpClient
import com.gymvip.mobile.data.remote.api.MemberApi
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class MemberWorkspaceProfileTest {
    @Test fun cachesProfileUnderAuthenticatedAccountInsteadOfClientId() = runTest {
        val driver=JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GymVipDatabase.Schema.create(driver)
        val db=GymVipDatabase(driver)
        val client=GymVipHttpClient.create(MockEngine {
            respond("""{"client":{"id":"client-id","firstName":"Sofia","lastName":"Demo","email":"socia@example.test"}}""",
                HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
        },enableLogging=false)
        try {
            val result=MemberRepositoryImpl(MemberApi(client,"http://test"),db).refreshProfile("account-id")
            assertTrue(result.isSuccess)
            assertEquals("account-id",result.getOrThrow().accountId)
            assertEquals("Sofia",db.gymVipDatabaseQueries.selectProfileByAccountId("account-id").executeAsOne().firstName)
            assertNull(db.gymVipDatabaseQueries.selectProfileByAccountId("client-id").executeAsOneOrNull())
        } finally { client.close();driver.close() }
    }
}
