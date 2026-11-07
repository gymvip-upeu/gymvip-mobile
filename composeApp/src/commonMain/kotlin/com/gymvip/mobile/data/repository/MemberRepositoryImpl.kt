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
import kotlinx.datetime.Clock

class MemberRepositoryImpl(
    private val memberApi: MemberApi,
    private val database: GymVipDatabase
) : MemberRepository {

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
        val result = memberApi.getProfile(accountId)
        return result.map { dto ->
            val now = Clock.System.now().toString()
            val domain = (dto as com.gymvip.mobile.data.remote.dto.MemberProfileDto).toDomain(now)
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
        val result = memberApi.getMembership(accountId)
        return result.map { dto ->
            val now = Clock.System.now().toString()
            val domain = (dto as com.gymvip.mobile.data.remote.dto.MembershipSummaryDto).toDomain(accountId, now)
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
        val result = memberApi.getPayments(accountId)
        return result.map { list ->
            val now = Clock.System.now().toString()
            val domainList = list.map { (it as com.gymvip.mobile.data.remote.dto.PaymentSummaryDto).toDomain(accountId, now) }
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

    override suspend fun clearLocalData(accountId: String) {
        database.gymVipDatabaseQueries.transaction {
            database.gymVipDatabaseQueries.deleteProfileByAccountId(accountId)
            database.gymVipDatabaseQueries.deleteMembershipByAccountId(accountId)
            database.gymVipDatabaseQueries.deletePaymentsByAccountId(accountId)
        }
    }
}
