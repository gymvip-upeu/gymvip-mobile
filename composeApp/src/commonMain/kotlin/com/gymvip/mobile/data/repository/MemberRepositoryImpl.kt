package com.gymvip.mobile.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.gymvip.mobile.data.local.GymVipDatabase
import com.gymvip.mobile.data.mapper.toDomain
import com.gymvip.mobile.data.remote.api.MemberApi
import com.gymvip.mobile.domain.model.MemberProfile
import com.gymvip.mobile.domain.model.MembershipSummary
import com.gymvip.mobile.domain.model.PaymentSummary
import com.gymvip.mobile.domain.repository.MemberRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

class MemberRepositoryImpl(
    private val memberApi: MemberApi,
    private val database: GymVipDatabase
) : MemberRepository {

    private val syncMutex = Mutex()
    private var lastWorkspaceSyncTime: Long = 0L
    private val syncCooldownMillis: Long = 5_000L // 5 segundos de reutilización coherente

    private suspend fun syncWorkspaceIfStale(accountId: String): Result<Unit> = syncMutex.withLock {
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        if (nowMillis - lastWorkspaceSyncTime < syncCooldownMillis) {
            return Result.success(Unit)
        }

        val workspaceResult = memberApi.getWorkspace()
        return workspaceResult.map { workspace ->
            val nowStr = Clock.System.now().toString()
            val profile = workspace.toProfileDto().toDomain(nowStr).copy(accountId = accountId)
            val membership = workspace.toMembershipDto().toDomain(accountId, nowStr)
            val payments = workspace.toPaymentsDto().map { it.toDomain(accountId, nowStr) }

            database.gymVipDatabaseQueries.transaction {
                database.gymVipDatabaseQueries.insertOrReplaceProfile(
                    accountId = profile.accountId,
                    email = profile.email,
                    firstName = profile.firstName,
                    lastName = profile.lastName,
                    documentType = profile.documentType,
                    documentNumber = profile.documentNumber,
                    phone = profile.phone,
                    status = profile.status.name,
                    lastSyncedAt = profile.lastSyncedAt
                )

                database.gymVipDatabaseQueries.insertOrReplaceMembership(
                    id = membership.id,
                    accountId = accountId,
                    planName = membership.planName,
                    startDate = membership.startDate,
                    endDate = membership.endDate,
                    status = membership.status.name,
                    daysRemaining = membership.daysRemaining.toLong(),
                    lastSyncedAt = membership.lastSyncedAt
                )

                database.gymVipDatabaseQueries.deletePaymentsByAccountId(accountId)
                payments.forEach { payment ->
                    database.gymVipDatabaseQueries.insertOrReplacePayment(
                        id = payment.id,
                        accountId = accountId,
                        membershipId = payment.membershipId,
                        amountCents = payment.amount.cents,
                        paymentMethod = payment.paymentMethod.name,
                        externalReference = payment.externalReference,
                        paidAt = payment.paidAt,
                        notes = payment.notes,
                        lastSyncedAt = payment.lastSyncedAt
                    )
                }
            }
            lastWorkspaceSyncTime = nowMillis
        }
    }

    override fun observeProfile(accountId: String): Flow<MemberProfile?> {
        return database.gymVipDatabaseQueries
            .selectProfileByAccountId(accountId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toDomain() }
    }

    override fun observeMembership(accountId: String): Flow<MembershipSummary?> {
        return database.gymVipDatabaseQueries
            .selectMembershipByAccountId(accountId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toDomain() }
    }

    override fun observePayments(accountId: String): Flow<List<PaymentSummary>> {
        return database.gymVipDatabaseQueries
            .selectPaymentsByAccountId(accountId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun refreshProfile(accountId: String): Result<MemberProfile> {
        val syncResult = syncWorkspaceIfStale(accountId)
        if (syncResult.isSuccess) {
            val local = database.gymVipDatabaseQueries.selectProfileByAccountId(accountId).executeAsOneOrNull()
            if (local != null) {
                return Result.success(local.toDomain())
            }
        }

        // Si falló la sincronización unificada o no hay dato local, recurrir al método puntual
        val result = memberApi.getProfile(accountId)
        return result.map { dto ->
            val now = Clock.System.now().toString()
            val domain = dto.toDomain(now).copy(accountId = accountId)
            database.gymVipDatabaseQueries.insertOrReplaceProfile(
                accountId = domain.accountId,
                email = domain.email,
                firstName = domain.firstName,
                lastName = domain.lastName,
                documentType = domain.documentType,
                documentNumber = domain.documentNumber,
                phone = domain.phone,
                status = domain.status.name,
                lastSyncedAt = domain.lastSyncedAt
            )
            domain
        }
    }

    override suspend fun refreshMembership(accountId: String): Result<MembershipSummary> {
        val syncResult = syncWorkspaceIfStale(accountId)
        if (syncResult.isSuccess) {
            val local = database.gymVipDatabaseQueries.selectMembershipByAccountId(accountId).executeAsOneOrNull()
            if (local != null) {
                return Result.success(local.toDomain())
            }
        }

        val result = memberApi.getMembership(accountId)
        return result.map { dto ->
            val now = Clock.System.now().toString()
            val domain = dto.toDomain(accountId, now)
            database.gymVipDatabaseQueries.insertOrReplaceMembership(
                id = domain.id,
                accountId = accountId,
                planName = domain.planName,
                startDate = domain.startDate,
                endDate = domain.endDate,
                status = domain.status.name,
                daysRemaining = domain.daysRemaining.toLong(),
                lastSyncedAt = domain.lastSyncedAt
            )
            domain
        }
    }

    override suspend fun refreshPayments(accountId: String): Result<List<PaymentSummary>> {
        val syncResult = syncWorkspaceIfStale(accountId)
        if (syncResult.isSuccess) {
            val local = database.gymVipDatabaseQueries.selectPaymentsByAccountId(accountId).executeAsList()
            if (local.isNotEmpty()) {
                return Result.success(local.map { it.toDomain() })
            }
        }

        val result = memberApi.getPayments(accountId)
        return result.map { list ->
            val now = Clock.System.now().toString()
            val domainList = list.map { it.toDomain(accountId, now) }
            database.gymVipDatabaseQueries.transaction {
                database.gymVipDatabaseQueries.deletePaymentsByAccountId(accountId)
                domainList.forEach { payment ->
                    database.gymVipDatabaseQueries.insertOrReplacePayment(
                        id = payment.id,
                        accountId = accountId,
                        membershipId = payment.membershipId,
                        amountCents = payment.amount.cents,
                        paymentMethod = payment.paymentMethod.name,
                        externalReference = payment.externalReference,
                        paidAt = payment.paidAt,
                        notes = payment.notes,
                        lastSyncedAt = payment.lastSyncedAt
                    )
                }
            }
            domainList
        }
    }

    override suspend fun checkInWithReceptionQr(challenge: String): Result<com.gymvip.mobile.domain.model.MemberCheckInResult> {
        return memberApi.selfCheckIn(challenge).map { dto ->
            com.gymvip.mobile.domain.model.MemberCheckInResult(
                status = dto.status,
                recorded = dto.recorded,
                message = dto.message,
                attendanceDate = dto.attendanceDate,
                checkedAtUtc = dto.checkedAtUtc,
                memberName = dto.memberName,
                memberCode = dto.memberCode
            )
        }
    }

    override suspend fun requestAccountDeletion(reason: String): Result<com.gymvip.mobile.data.remote.dto.AccountDeletionResultDto> {
        return memberApi.requestAccountDeletion(reason)
    }

    override suspend fun clearLocalData(accountId: String) {
        database.gymVipDatabaseQueries.transaction {
            database.gymVipDatabaseQueries.deleteProfileByAccountId(accountId)
            database.gymVipDatabaseQueries.deleteMembershipByAccountId(accountId)
            database.gymVipDatabaseQueries.deletePaymentsByAccountId(accountId)
        }
    }
}
