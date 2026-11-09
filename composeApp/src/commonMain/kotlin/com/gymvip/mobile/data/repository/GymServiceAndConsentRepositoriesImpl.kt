package com.gymvip.mobile.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.gymvip.mobile.data.local.GymVipDatabase
import com.gymvip.mobile.data.remote.api.MemberApi
import com.gymvip.mobile.data.remote.dto.ConsentSubmitRequestDto
import com.gymvip.mobile.domain.model.GymService
import com.gymvip.mobile.domain.model.MemberConsent
import com.gymvip.mobile.domain.repository.GymServiceRepository
import com.gymvip.mobile.domain.repository.LegalConsentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GymServiceRepositoryImpl(
    private val memberApi: MemberApi,
    private val database: GymVipDatabase
) : GymServiceRepository {

    override fun observeServices(): Flow<List<GymService>> {
        return database.gymVipDatabaseQueries
            .selectAllGymServices()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list ->
                list.map {
                    GymService(
                        id = it.id,
                        name = it.name,
                        description = it.description,
                        schedule = it.schedule,
                        location = it.location,
                        isIncluded = it.isIncluded == 1L
                    )
                }
            }
    }

    override suspend fun refreshServices(): Result<List<GymService>> {
        val remote = memberApi.getServices()
        return if (remote.isSuccess) {
            val list = remote.getOrThrow()
            database.gymVipDatabaseQueries.transaction {
                database.gymVipDatabaseQueries.clearGymServices()
                list.forEach { s ->
                    database.gymVipDatabaseQueries.insertOrReplaceGymService(
                        id = s.id,
                        name = s.name,
                        description = s.description,
                        schedule = s.schedule,
                        location = s.location,
                        isIncluded = if (s.isIncluded) 1L else 0L
                    )
                }
            }
            Result.success(list.map { GymService(it.id, it.name, it.description, it.schedule, it.location, it.isIncluded) })
        } else {
            val cached = database.gymVipDatabaseQueries.selectAllGymServices().executeAsList()
            if (cached.isNotEmpty()) {
                Result.success(cached.map { GymService(it.id, it.name, it.description, it.schedule, it.location, it.isIncluded == 1L) })
            } else {
                Result.failure(remote.exceptionOrNull() ?: Exception("Error al cargar servicios"))
            }
        }
    }
}

class LegalConsentRepositoryImpl(
    private val memberApi: MemberApi,
    private val database: GymVipDatabase
) : LegalConsentRepository {

    override fun observeConsent(accountId: String): Flow<MemberConsent?> {
        return database.gymVipDatabaseQueries
            .selectConsentByAccountId(accountId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { row ->
                row?.let {
                    MemberConsent(
                        accountId = it.accountId,
                        termsAccepted = it.termsAccepted == 1L,
                        privacyAcknowledged = it.privacyAcknowledged == 1L,
                        whatsappConsent = it.whatsappConsent == 1L,
                        documentVersion = it.documentVersion,
                        acceptedAt = it.acceptedAt
                    )
                }
            }
    }

    override suspend fun fetchConsent(accountId: String): Result<MemberConsent> {
        val remote = memberApi.getConsents()
        return if (remote.isSuccess) {
            val dto = remote.getOrThrow()
            val consent = MemberConsent(
                accountId = accountId,
                termsAccepted = dto.termsAccepted,
                privacyAcknowledged = dto.privacyAcknowledged,
                whatsappConsent = dto.whatsappConsent,
                documentVersion = dto.documentVersion,
                acceptedAt = dto.acceptedAt
            )
            database.gymVipDatabaseQueries.insertOrReplaceConsent(
                accountId = accountId,
                termsAccepted = if (dto.termsAccepted) 1L else 0L,
                privacyAcknowledged = if (dto.privacyAcknowledged) 1L else 0L,
                whatsappConsent = if (dto.whatsappConsent) 1L else 0L,
                documentVersion = dto.documentVersion,
                acceptedAt = dto.acceptedAt
            )
            Result.success(consent)
        } else {
            val local = database.gymVipDatabaseQueries.selectConsentByAccountId(accountId).executeAsOneOrNull()
            if (local != null) {
                Result.success(MemberConsent(local.accountId, local.termsAccepted == 1L, local.privacyAcknowledged == 1L, local.whatsappConsent == 1L, local.documentVersion, local.acceptedAt))
            } else {
                Result.failure(remote.exceptionOrNull() ?: Exception("Error al consultar consentimientos"))
            }
        }
    }

    override suspend fun submitConsent(
        accountId: String,
        termsAccepted: Boolean,
        privacyAcknowledged: Boolean,
        whatsappConsent: Boolean
    ): Result<MemberConsent> {
        val req = ConsentSubmitRequestDto(
            termsAccepted = termsAccepted,
            privacyAcknowledged = privacyAcknowledged,
            whatsappConsent = whatsappConsent,
            documentVersion = "v1.0"
        )
        val remote = memberApi.submitConsents(req)
        return if (remote.isSuccess) {
            val dto = remote.getOrThrow()
            val consent = MemberConsent(
                accountId = accountId,
                termsAccepted = dto.termsAccepted,
                privacyAcknowledged = dto.privacyAcknowledged,
                whatsappConsent = dto.whatsappConsent,
                documentVersion = dto.documentVersion,
                acceptedAt = dto.acceptedAt
            )
            database.gymVipDatabaseQueries.insertOrReplaceConsent(
                accountId = accountId,
                termsAccepted = if (dto.termsAccepted) 1L else 0L,
                privacyAcknowledged = if (dto.privacyAcknowledged) 1L else 0L,
                whatsappConsent = if (dto.whatsappConsent) 1L else 0L,
                documentVersion = dto.documentVersion,
                acceptedAt = dto.acceptedAt
            )
            Result.success(consent)
        } else {
            // Guardar localmente para offline si falla la conexión
            database.gymVipDatabaseQueries.insertOrReplaceConsent(
                accountId = accountId,
                termsAccepted = if (termsAccepted) 1L else 0L,
                privacyAcknowledged = if (privacyAcknowledged) 1L else 0L,
                whatsappConsent = if (whatsappConsent) 1L else 0L,
                documentVersion = "v1.0",
                acceptedAt = null
            )
            Result.failure(remote.exceptionOrNull() ?: Exception("Error al guardar consentimientos"))
        }
    }
}

