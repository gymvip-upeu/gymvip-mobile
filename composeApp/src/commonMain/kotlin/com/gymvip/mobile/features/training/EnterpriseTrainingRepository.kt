package com.gymvip.mobile.features.training

import com.gymvip.mobile.data.local.GymVipDatabase
import com.gymvip.mobile.domain.repository.ConfigRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Repositorio feature-first offline-first para Sedes, Rutinas, Ejercicios,
 * Registro de Sesiones Offline con cola idempotente y Reservas de Clases.
 */
class EnterpriseTrainingRepository(
    private val api: EnterpriseTrainingApi,
    private val database: GymVipDatabase,
    private val configRepository: ConfigRepository
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun readPref(scope: String, key: String): String? =
        database.gymVipDatabaseQueries.selectPreference(scope, key).executeAsOneOrNull()

    private fun writePref(scope: String, key: String, value: String) {
        database.gymVipDatabaseQueries.upsertPreference(scope, key, value)
    }

    internal fun readActiveWorkout(accountId: String): ActiveWorkoutDraft? =
        readPref(accountId, "active_workout_json")?.takeIf { it.isNotBlank() }?.let {
            runCatching { json.decodeFromString<ActiveWorkoutDraft>(it) }.getOrNull()
        }

    internal fun persistActiveWorkout(accountId: String, draft: ActiveWorkoutDraft?) {
        writePref(accountId, "active_workout_json", draft?.let { json.encodeToString(it) }.orEmpty())
    }

    internal fun hasQueuedWorkout(accountId: String, syncId: String): Boolean =
        loadPendingWorkouts(accountId).any { it.clientSyncId == syncId }

    suspend fun fetchGyms(query: String? = null, district: String? = null, scopeId: String = "__public__"): Result<List<PublishedGymDto>> {
        val remote = api.listPublishedGyms(query, district)
        return remote.onSuccess { list ->
            runCatching { writePref(scopeId, "cache_gyms_json", json.encodeToString(list)) }
        }.recoverCatching { err ->
            val cached = readPref(scopeId, "cache_gyms_json")
            if (!cached.isNullOrBlank()) json.decodeFromString<List<PublishedGymDto>>(cached)
            else throw err
        }
    }

    suspend fun selectPreferredBranch(branch: PublishedBranchDto) {
        configRepository.setPreferredBranch(branch.id, branch.name)
        runCatching {
            api.updatePreferences(UserPreferencesDto(preferredBranchId = branch.id))
        }
    }

    suspend fun toggleFavoriteGym(accountId: String, gymId: String): Set<String> {
        val current = getFavoriteGymIds(accountId).toMutableSet()
        if (!current.add(gymId)) current.remove(gymId)
        writePref(accountId, "favorite_gym_ids_json", json.encodeToString(current.toList()))
        return current
    }

    fun getFavoriteGymIds(accountId: String): Set<String> {
        val raw = readPref(accountId, "favorite_gym_ids_json") ?: return emptySet()
        return runCatching { json.decodeFromString<List<String>>(raw).toSet() }.getOrDefault(emptySet())
    }

    suspend fun fetchExercises(scopeId: String, muscleGroup: String? = null): Result<List<ExerciseDto>> {
        val remote = api.listExercises(muscleGroup)
        return remote.onSuccess { list ->
            runCatching { writePref(scopeId, "cache_exercises_json", json.encodeToString(list)) }
        }.recoverCatching { err ->
            val cached = readPref(scopeId, "cache_exercises_json")
            if (!cached.isNullOrBlank()) {
                val all = json.decodeFromString<List<ExerciseDto>>(cached)
                if (muscleGroup.isNullOrBlank()) all else all.filter { it.muscleGroup.contains(muscleGroup, ignoreCase = true) }
            } else throw err
        }
    }

    suspend fun fetchRoutines(scopeId: String): Result<List<WorkoutRoutineDto>> {
        val remote = api.listRoutines()
        return remote.onSuccess { list ->
            runCatching { writePref(scopeId, "cache_routines_json", json.encodeToString(list)) }
        }.recoverCatching { err ->
            val cached = readPref(scopeId, "cache_routines_json")
            if (!cached.isNullOrBlank()) json.decodeFromString<List<WorkoutRoutineDto>>(cached)
            else throw err
        }
    }

    /**
     * Guarda localmente en SQLite primero y sincroniza con el backend usando clientSyncId idempotente.
     * Si no hay conexión, deja la sesión en cola local para sincronización posterior sin pérdida de datos.
     */
    suspend fun saveAndSyncWorkoutSession(accountId: String, payload: WorkoutSessionPayload): Pair<Boolean, String> {
        val pending = loadPendingWorkouts(accountId).toMutableList()
        if (pending.none { it.clientSyncId == payload.clientSyncId }) {
            pending.add(payload)
            writePref(accountId, "pending_workout_sync_json", json.encodeToString(pending))
        }

        val localHistory = loadLocalSessions(accountId).toMutableList()
        if (localHistory.none { it.id == payload.clientSyncId }) {
            val vol = payload.sets.sumOf { it.reps * it.weightKg }
            localHistory.add(
                0,
                WorkoutSessionSummaryDto(
                    id = payload.clientSyncId,
                    routineTitle = payload.routineTitle,
                    durationSeconds = payload.durationSeconds,
                    totalVolumeKg = vol,
                    completedAt = payload.completedAt
                )
            )
            writePref(accountId, "local_workout_sessions_json", json.encodeToString(localHistory))
        }

        val syncedCount = syncPendingWorkouts(accountId)
        return if (syncedCount > 0) {
            true to "Entrenamiento registrado y sincronizado con el servidor."
        } else {
            false to "Entrenamiento guardado localmente en el dispositivo. Se sincronizará automáticamente al recuperar conexión."
        }
    }

    suspend fun syncPendingWorkouts(accountId: String): Int {
        val pending = loadPendingWorkouts(accountId)
        if (pending.isEmpty()) return 0
        val remaining = mutableListOf<WorkoutSessionPayload>()
        var synced = 0
        for (item in pending) {
            val res = api.recordSession(item)
            if (res.isSuccess) synced++ else remaining.add(item)
        }
        writePref(accountId, "pending_workout_sync_json", json.encodeToString(remaining))
        return synced
    }

    suspend fun fetchProgress(accountId: String): WorkoutProgressDto {
        syncPendingWorkouts(accountId)
        val remote = api.getProgress().getOrNull()
        val local = loadLocalSessions(accountId)
        return if (remote != null) {
            val merged = (remote.recentSessions + local.filter { l -> remote.recentSessions.none { it.completedAt == l.completedAt && it.routineTitle == l.routineTitle } })
            remote.copy(
                totalSessions = maxOf(remote.totalSessions, merged.size),
                recentSessions = merged.take(30)
            )
        } else {
            WorkoutProgressDto(
                totalSessions = local.size,
                totalVolumeKg = local.sumOf { it.totalVolumeKg },
                recentSessions = local,
                personalRecords = emptyList()
            )
        }
    }

    suspend fun listClasses(): Result<List<GymClassDto>> = api.listClasses()
    suspend fun reserveClass(classId: String) = api.reserveClass(classId)
    suspend fun cancelClass(classId: String) = api.cancelClassReservation(classId)

    suspend fun syncShowGuidesToServer(showGuides: Boolean) {
        runCatching {
            api.updatePreferences(
                UserPreferencesDto(
                    showGuides = showGuides,
                    experienceMode = if (showGuides) "GUIDED" else "COMPACT"
                )
            )
        }
    }

    fun pendingWorkoutCount(accountId: String): Int = loadPendingWorkouts(accountId).size

    private fun loadPendingWorkouts(accountId: String): List<WorkoutSessionPayload> {
        val raw = readPref(accountId, "pending_workout_sync_json") ?: return emptyList()
        return runCatching { json.decodeFromString<List<WorkoutSessionPayload>>(raw) }.getOrDefault(emptyList())
    }

    private fun loadLocalSessions(accountId: String): List<WorkoutSessionSummaryDto> {
        val raw = readPref(accountId, "local_workout_sessions_json") ?: return emptyList()
        return runCatching { json.decodeFromString<List<WorkoutSessionSummaryDto>>(raw) }.getOrDefault(emptyList())
    }
}
