package com.gymvip.mobile.features.journal

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.gymvip.mobile.data.local.GymVipDatabase
import com.gymvip.mobile.features.training.WorkoutSessionSummaryDto
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

class JournalTest {
    @Test fun exportNeverUsesImageFromPreviousPrivacySettings() {
        val card = JournalShareCard("2026-11-25", listOf("Nombre visible"), "Minimal", true, null)
        val rendered = JournalRenderedCard(card, byteArrayOf(1, 2, 3))
        assertContentEquals(byteArrayOf(1, 2, 3), journalExportBytes(card, rendered))
        assertNull(journalExportBytes(card.copy(lines = emptyList()), rendered))
        assertNull(journalExportBytes(card.copy(story = false), rendered))
        assertNull(journalExportBytes(card, null))
    }

    @Test fun stopwatchPreservesPartialSecondsAndRoundsOnlyWhenStopped() {
        assertEquals(61_500L, timerElapsedMillis(1000, 500, 62_000))
        assertEquals(500L, timerElapsedMillis(null, 500, 100_000))
        assertEquals(500L, timerElapsedMillis(1000, 500, 900))
        assertEquals(0, timerDurationMinutes(0))
        assertEquals(1, timerDurationMinutes(500))
        assertEquals(1, timerDurationMinutes(60_000))
        assertEquals(2, timerDurationMinutes(60_001))
    }

    @Test fun brokenTrainingCachesDoNotBlockOrOverwriteManualJournal() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            GymVipDatabase.Schema.create(driver)
            val db = GymVipDatabase(driver); val repo = JournalRepository(db)
            repo.save("a", entry())
            db.gymVipDatabaseQueries.upsertPreference("a", "local_workout_sessions_json", "invalid sessions")
            db.gymVipDatabaseQueries.upsertPreference("a", "cache_routines_json", "invalid routines")
            val snapshot = repo.snapshot("a")
            assertEquals(1, snapshot.days.size)
            assertEquals(2, snapshot.warnings.size)
            assertTrue(snapshot.routines.isEmpty())
            repo.save("a", entry().copy(notes = "Sigo registrando"))
            assertEquals("invalid sessions", db.gymVipDatabaseQueries.selectPreference("a", "local_workout_sessions_json").executeAsOne())
            assertEquals("invalid routines", db.gymVipDatabaseQueries.selectPreference("a", "cache_routines_json").executeAsOne())
            db.gymVipDatabaseQueries.upsertPreference("a", "journal_entries_v1_json", "invalid journal")
            assertFailsWith<Exception> { repo.save("a", entry(id = "other")) }
            assertEquals("invalid journal", db.gymVipDatabaseQueries.selectPreference("a", "journal_entries_v1_json").executeAsOne())
        } finally { driver.close() }
    }

    @Test fun duplicateSyncIdsAreRejectedWithoutLosingExistingRecord() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            GymVipDatabase.Schema.create(driver)
            val repo = JournalRepository(GymVipDatabase(driver))
            repo.save("a", entry())
            assertFailsWith<IllegalArgumentException> { repo.save("a", entry(id = "two").copy(clientSyncId = "one")) }
            assertEquals(1, repo.list("a").size)
            assertTrue(validateJournal(entry().copy(id = ""), LocalDate.parse("2026-11-25")).isNotEmpty())
        } finally { driver.close() }
    }
    @Test fun restoredServerRolesKeepClientAccessWithoutChangingAssignedPermissions() {
        val dto = Json.decodeFromString<com.gymvip.mobile.data.remote.dto.CurrentUserDto>("""{"roles":[{"authority":"ROLE_CLIENTE"},"CLIENTE"]}""")
        assertEquals(listOf("CLIENTE"), dto.normalizedRoles())
        val staff = Json.decodeFromString<com.gymvip.mobile.data.remote.dto.CurrentUserDto>("""{"roles":[{"authority":"ROLE_ADMIN"}]}""")
        assertEquals(listOf("ADMIN"), staff.normalizedRoles())
    }
    private fun entry(date: String = "2026-11-25", id: String = "one") = JournalEntry(id,id,date,"09:00",45,"Fuerza",exercises = listOf(JournalExercise("Press",3,10,20.0)),createdAt="2026-11-25T14:00:00Z",updatedAt="2026-11-25T14:00:00Z")
    @Test fun streakCrossesMonthAndAllowsRestToday() {
        val dates = setOf("2026-10-30","2026-10-31","2026-11-01").map(LocalDate::parse).toSet()
        assertEquals(3, journalStreak(dates,LocalDate.parse("2026-11-02")))
        assertEquals(0, journalStreak(dates,LocalDate.parse("2026-11-03")))
        assertEquals(1, journalStreak(dates - LocalDate.parse("2026-10-31"),LocalDate.parse("2026-11-01")))
    }
    @Test fun statsCombineSessionsAndManualWithoutCountingDayTwice() {
        val days = listOf(JournalDay("2026-11-25",listOf(entry()),listOf(JournalSession("s","2026-11-25","Rutina",900,200.0))),JournalDay("2026-10-25",listOf(entry("2026-10-25")),emptyList()))
        assertEquals(JournalMonthStats(1,60,60,800.0),journalStats(days,LocalDate.parse("2026-11-01")))
        assertEquals(800.0,journalWeeks(days).last().volume)
    }
    @Test fun epleyAndTimelineUseBestRecordPerDay() {
        assertEquals(40.0,estimatedOneRepMax(30.0,10),0.001)
        assertEquals(0.0,estimatedOneRepMax(Double.NaN,10))
        val values = strengthTimeline(listOf(entry(), entry(id="two").copy(exercises=listOf(JournalExercise("Press",1,10,30.0)))),"Press")
        assertEquals(1,values.size); assertEquals(40.0,values.single().estimatedMax,0.001)
    }
    @Test fun validationRejectsFutureBadTimeAndInvalidExercise() {
        val today=LocalDate.parse("2026-11-25")
        assertTrue(validateJournal(entry(),today).isEmpty())
        assertTrue(validateJournal(entry("2026-11-26"),today).isNotEmpty())
        assertTrue(validateJournal(entry().copy(startTime="30:80",durationMinutes=0,energy=6,notes="x".repeat(1001),exercises=listOf(JournalExercise("",0,0,Double.NaN))),today).size >= 5)
    }
    @Test fun repositoryIsolatesAccountsAndDoesNotModifyTrainingOrSchema() = runTest {
        val driver=JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            GymVipDatabase.Schema.create(driver)
            val db=GymVipDatabase(driver); val repo=JournalRepository(db)
            val sessionJson=Json.encodeToString(listOf(WorkoutSessionSummaryDto("s","Rutina",900,100.0,"2026-11-25T10:00:00")))
            db.gymVipDatabaseQueries.upsertPreference("a","local_workout_sessions_json",sessionJson)
            db.gymVipDatabaseQueries.upsertPreference("a","pending_workout_sync_json","[]")
            repo.save("a",entry()); assertEquals(1,repo.list("a").size); assertTrue(repo.list("b").isEmpty())
            repo.save("a",entry().copy(notes="Editado",clientSyncId="changed"))
            assertEquals("one",repo.list("a").single().clientSyncId)
            assertEquals(1,repo.sessions("a").size); assertTrue(repo.sessions("b").isEmpty())
            repo.days("a"); repo.days("a")
            assertEquals(sessionJson,db.gymVipDatabaseQueries.selectPreference("a","local_workout_sessions_json").executeAsOne())
            assertEquals("[]",db.gymVipDatabaseQueries.selectPreference("a","pending_workout_sync_json").executeAsOne())
            repo.delete("b","one"); assertEquals(1,repo.list("a").size)
            repo.delete("a","one"); assertTrue(repo.list("a").isEmpty())
            assertEquals(1,repo.sessions("a").size)
        } finally { driver.close() }
    }
    @Test fun shareDefaultsExcludeNameAndPrivateNotes() {
        val e=entry().copy(notes="privado",whatIDid="privado también",howIFelt="Con energía")
        val day=JournalDay(e.date,listOf(e),emptyList())
        val lines=journalShareLines(day,listOf(day),setOf("time","type","volume","feeling","streak","record"),"Nombre privado")
        assertFalse(lines.any { it.contains("privado",true) }); assertTrue(lines.any { it.contains("Con energía") })
        assertEquals(listOf("Nombre"),journalShareLines(day,listOf(day),setOf("name"),"Nombre"))
    }
}
