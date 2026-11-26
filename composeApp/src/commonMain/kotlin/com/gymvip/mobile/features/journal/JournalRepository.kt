package com.gymvip.mobile.features.journal

import com.gymvip.mobile.data.local.GymVipDatabase
import com.gymvip.mobile.features.training.WorkoutRoutineDto
import com.gymvip.mobile.features.training.WorkoutSessionSummaryDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class JournalRepository(private val database: GymVipDatabase) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val key = "journal_entries_v1_json"
    private fun checkAccount(accountId: String) { require(accountId.isNotBlank()) { "Inicia sesión para abrir tu bitácora." } }
    private fun raw(accountId: String, key: String) = database.gymVipDatabaseQueries.selectPreference(accountId, key).executeAsOneOrNull()
    private fun entries(accountId: String): List<JournalEntry> {
        checkAccount(accountId)
        return raw(accountId, key)?.let { json.decodeFromString<List<JournalEntry>>(it) } ?: emptyList()
    }
    suspend fun list(accountId: String): List<JournalEntry> = withContext(Dispatchers.Default) { entries(accountId) }
    suspend fun save(accountId: String, entry: JournalEntry) = withContext(Dispatchers.Default) {
        checkAccount(accountId)
        require(validateJournal(entry).isEmpty()) { validateJournal(entry).joinToString("\n") }
        database.transaction {
            val all = entries(accountId)
            val old = all.firstOrNull { it.id == entry.id }
            val saved = entry.copy(clientSyncId = old?.clientSyncId ?: entry.clientSyncId, createdAt = old?.createdAt ?: entry.createdAt)
            database.gymVipDatabaseQueries.upsertPreference(accountId, key, json.encodeToString(all.filterNot { it.id == entry.id } + saved))
        }
    }
    suspend fun delete(accountId: String, id: String) = withContext(Dispatchers.Default) {
        checkAccount(accountId)
        database.transaction { database.gymVipDatabaseQueries.upsertPreference(accountId, key, json.encodeToString(entries(accountId).filterNot { it.id == id })) }
    }
    /** Lectura directa: nunca dispara fetchProgress/syncPendingWorkouts ni escribe en Entreno. */
    suspend fun sessions(accountId: String): List<JournalSession> = withContext(Dispatchers.Default) {
        checkAccount(accountId)
        val stored = raw(accountId, "local_workout_sessions_json") ?: return@withContext emptyList()
        json.decodeFromString<List<WorkoutSessionSummaryDto>>(stored).distinctBy { it.id }.mapNotNull { s ->
            val date = runCatching { Instant.parse(s.completedAt).toLocalDateTime(journalZone()).date }
                .recoverCatching { LocalDateTime.parse(s.completedAt).date }.getOrNull() ?: return@mapNotNull null
            JournalSession(s.id, date.toString(), s.routineTitle, s.durationSeconds.coerceAtLeast(0), s.totalVolumeKg)
        }
    }
    suspend fun routines(accountId: String): List<WorkoutRoutineDto> = withContext(Dispatchers.Default) {
        checkAccount(accountId)
        raw(accountId, "cache_routines_json")?.let { json.decodeFromString<List<WorkoutRoutineDto>>(it) } ?: emptyList()
    }
    suspend fun days(accountId: String): List<JournalDay> {
        val entries = list(accountId); val sessions = sessions(accountId)
        return (entries.map { it.date } + sessions.map { it.date }).distinct().sorted().map { d ->
            JournalDay(d, entries.filter { it.date == d }, sessions.filter { it.date == d })
        }
    }
}
