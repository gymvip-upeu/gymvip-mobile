package pe.edu.upeu.gymvip.domain.usecase

import pe.edu.upeu.gymvip.domain.model.PaymentSummary
import pe.edu.upeu.gymvip.domain.repository.MemberRepository
import kotlinx.coroutines.flow.Flow

class GetPaymentHistoryUseCase(private val memberRepository: MemberRepository) {

    /**
     * Devuelve el historial de pagos de solo lectura en PEN.
     * Accesible sin importar si la membresía está VENCIDA, siempre que la cuenta de usuario esté activa.
     */
    fun observe(accountId: String): Flow<List<PaymentSummary>> {
        return memberRepository.observePayments(accountId)
    }

    suspend fun refresh(accountId: String): Result<List<PaymentSummary>> {
        return memberRepository.refreshPayments(accountId)
    }
}
