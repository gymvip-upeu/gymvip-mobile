package com.gymvip.mobile.domain.repository

import com.gymvip.mobile.domain.model.MemberProfile
import com.gymvip.mobile.domain.model.MembershipSummary
import com.gymvip.mobile.domain.model.PaymentSummary
import kotlinx.coroutines.flow.Flow

interface MemberRepository {
    fun observeProfile(accountId: String): Flow<MemberProfile?>
    fun observeMembership(accountId: String): Flow<MembershipSummary?>
    fun observePayments(accountId: String): Flow<List<PaymentSummary>>

    suspend fun refreshProfile(accountId: String): Result<MemberProfile>
    suspend fun refreshMembership(accountId: String): Result<MembershipSummary>
    suspend fun refreshPayments(accountId: String): Result<List<PaymentSummary>>
    suspend fun clearLocalData(accountId: String)
}
