package com.gymvip.mobile.features.training

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.gymvip.mobile.data.local.GymVipDatabase
import com.gymvip.mobile.data.remote.api.GymVipHttpClient
import com.gymvip.mobile.data.repository.ConfigRepositoryImpl
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EnterpriseTrainingAndPreferencesTest {

    private fun createDb(): GymVipDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GymVipDatabase.Schema.create(driver)
        return GymVipDatabase(driver)
    }

    @Test
    fun guidePreferencePersistsPerAccountAndResetsCleanly() = runTest {
        val db = createDb()
        val config = ConfigRepositoryImpl(db)
        config.setActiveAccount("acc-1")

        assertTrue(config.getShowGuides().first())
        config.setShowGuides(false)
        assertFalse(config.getShowGuides().first())

        config.setActiveAccount("acc-2")
        assertTrue(config.getShowGuides().first())

        config.setActiveAccount("acc-1")
        assertFalse(config.getShowGuides().first())

        config.resetGuidesAndTutorials()
        assertTrue(config.getShowGuides().first())
    }

    @Test
    fun offlineWorkoutSavesLocallyWhenOfflineAndSyncsWhenOnline() = runTest {
        val db = createDb()
        val config = ConfigRepositoryImpl(db)
        config.setActiveAccount("acc-train")

        var online = false
        val mockEngine = MockEngine { request ->
            if (!online) {
                throw IllegalStateException("Sin conexión")
            }
            if (request.url.encodedPath.endsWith("/api/v1/training/sessions")) {
                respond(
                    content = """{"id":"srv-1","deduplicated":false}""",
                    status = HttpStatusCode.Created,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                respond(
                    content = """{"totalSessions":1,"totalVolumeKg":400.0,"recentSessions":[],"personalRecords":[]}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }
        val api = EnterpriseTrainingApi(GymVipHttpClient.create(engine = mockEngine))
        val repo = EnterpriseTrainingRepository(api, db, config)

        val payload = WorkoutSessionPayload(
            routineId = "r-1",
            routineTitle = "Fuerza Base",
            durationSeconds = 1800,
            notes = "Offline test",
            clientSyncId = "sync-offline-123",
            startedAt = "2026-11-25T10:00:00",
            completedAt = "2026-11-25T10:30:00",
            sets = listOf(WorkoutSetPayload("ex-1", "Sentadilla", 1, 10, 40.0))
        )

        val (syncedInitially, _) = repo.saveAndSyncWorkoutSession("acc-train", payload)
        assertFalse(syncedInitially)
        assertEquals(1, repo.pendingWorkoutCount("acc-train"))

        val offlineProgress = repo.fetchProgress("acc-train")
        assertEquals(1, offlineProgress.totalSessions)
        assertEquals(400.0, offlineProgress.totalVolumeKg)

        online = true
        val flushed = repo.syncPendingWorkouts("acc-train")
        assertEquals(1, flushed)
        assertEquals(0, repo.pendingWorkoutCount("acc-train"))
    }
}
