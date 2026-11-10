package pe.edu.upeu.gymvip.domain.repository

import pe.edu.upeu.gymvip.domain.model.MemberProfile
import pe.edu.upeu.gymvip.domain.model.MembershipSummary
import pe.edu.upeu.gymvip.domain.model.PaymentSummary
import kotlinx.coroutines.flow.Flow

interface MemberRepository {
    fun observeSummary(accountId: String): Flow<pe.edu.upeu.gymvip.domain.model.ResumenSocio?>
    suspend fun refreshSummary(accountId: String): Result<pe.edu.upeu.gymvip.domain.model.ResumenSocio>
    suspend fun takeExpiryNotice(accountId: String): Boolean
    fun observeProfile(accountId: String): Flow<MemberProfile?>
    fun observeMembership(accountId: String): Flow<MembershipSummary?>
    fun observePayments(accountId: String): Flow<List<PaymentSummary>>

    suspend fun refreshProfile(accountId: String): Result<MemberProfile>
    suspend fun refreshMembership(accountId: String): Result<MembershipSummary?>
    suspend fun refreshPayments(accountId: String): Result<List<PaymentSummary>>
    suspend fun clearLocalData(accountId: String)
}
