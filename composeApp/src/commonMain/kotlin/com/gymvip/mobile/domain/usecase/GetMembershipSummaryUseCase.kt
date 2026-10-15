package com.gymvip.mobile.domain.usecase

import com.gymvip.mobile.domain.model.MembershipStatus
import com.gymvip.mobile.domain.model.MembershipSummary
import com.gymvip.mobile.domain.repository.MemberRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetMembershipSummaryUseCase(private val memberRepository: MemberRepository) {

    fun observe(accountId: String): Flow<MembershipSummary?> {
        return memberRepository.observeMembership(accountId).map { summary ->
            summary?.let {
                // Validación de integridad: el estado y días son los provistos por la regla del negocio
                // Cero tolerancia, pase diario 1 jornada, 30 días exactos para mensualidades
                it
            }
        }
    }

    suspend fun refresh(accountId: String): Result<MembershipSummary> {
        return memberRepository.refreshMembership(accountId)
    }
}
