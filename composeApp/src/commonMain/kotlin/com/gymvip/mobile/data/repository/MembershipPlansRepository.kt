package com.gymvip.mobile.data.repository

import com.gymvip.mobile.data.local.GymVipDatabase
import com.gymvip.mobile.data.remote.api.MemberApi
import com.gymvip.mobile.data.remote.dto.AvailableMembershipPlanDto
import com.gymvip.mobile.domain.error.AppError
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class MembershipPlansResult(val plans: List<AvailableMembershipPlanDto>, val isCached: Boolean = false)

/** Consulta comercial independiente del catálogo de suplementos y de la gestión de planes. */
class MembershipPlansRepository(private val api: MemberApi, private val database: GymVipDatabase) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(accountId: String): Result<MembershipPlansResult> {
        require(accountId.isNotBlank())
        val remote = api.getAvailablePlans()
        if (remote.isSuccess) {
            val plans = remote.getOrThrow().sortedBy { it.durationDays }
            database.gymVipDatabaseQueries.upsertPreference(accountId, CACHE_KEY, json.encodeToString(plans))
            return Result.success(MembershipPlansResult(plans))
        }
        val error = remote.exceptionOrNull() ?: AppError.Unknown(IllegalStateException("Sin respuesta"))
        // Una sesión inválida o un permiso denegado nunca se ocultan mediante datos guardados.
        if (error is AppError.Network || error is AppError.Server) {
            val cached = database.gymVipDatabaseQueries.selectPreference(accountId, CACHE_KEY).executeAsOneOrNull()
            val plans = cached?.let { runCatching { json.decodeFromString<List<AvailableMembershipPlanDto>>(it) }.getOrNull() }
            if (plans != null) return Result.success(MembershipPlansResult(plans, isCached = true))
        }
        return Result.failure(error)
    }

    private companion object { const val CACHE_KEY = "membership_available_plans_v1" }
}
