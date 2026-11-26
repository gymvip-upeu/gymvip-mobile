package com.gymvip.mobile.presentation.viewmodel

import com.gymvip.mobile.domain.error.AppError
import com.gymvip.mobile.domain.model.MembershipSummary
import com.gymvip.mobile.domain.model.PaymentSummary
import com.gymvip.mobile.domain.usecase.GetMembershipSummaryUseCase
import com.gymvip.mobile.domain.usecase.GetPaymentHistoryUseCase
import com.gymvip.mobile.presentation.component.ConnectionNoticeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class MembershipUiState(
    val isLoading: Boolean = false,
    val membership: MembershipSummary? = null,
    val payments: List<PaymentSummary> = emptyList(),
    val selectedSubTab: Int = 0, // 0 = Resumen de Membresía, 1 = Historial de Pagos
    val isOffline: Boolean = false,
    val noticeType: ConnectionNoticeType = ConnectionNoticeType.NONE,
    val errorMessage: String? = null,
    val plansVisible: Boolean = false,
    val plansLoading: Boolean = false,
    val plans: List<com.gymvip.mobile.data.remote.dto.AvailableMembershipPlanDto> = emptyList(),
    val plansCached: Boolean = false,
    val plansError: String? = null
)

class MembershipViewModel(
    private val getMembershipSummaryUseCase: GetMembershipSummaryUseCase,
    private val getPaymentHistoryUseCase: GetPaymentHistoryUseCase,
    private val accountId: String,
    private val isStaff: Boolean = false,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    private val plansRepository: com.gymvip.mobile.data.repository.MembershipPlansRepository? = null
) {
    private val _uiState = MutableStateFlow(MembershipUiState())
    val uiState: StateFlow<MembershipUiState> = _uiState.asStateFlow()

    init {
        observeData()
        refresh()
    }

    private fun observeData() {
        getMembershipSummaryUseCase.observe(accountId).onEach { summary ->
            _uiState.value = _uiState.value.copy(membership = summary)
        }.launchIn(scope)

        getPaymentHistoryUseCase.observe(accountId).onEach { list ->
            _uiState.value = _uiState.value.copy(payments = list)
        }.launchIn(scope)
    }

    fun setSubTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedSubTab = tabIndex)
    }

    fun showPlans() {
        if (isStaff) return
        if (_uiState.value.plansLoading) {
            _uiState.value = _uiState.value.copy(plansVisible = true)
            return
        }
        _uiState.value = _uiState.value.copy(plansVisible = true, plansLoading = true, plansError = null)
        scope.launch {
            val result = plansRepository?.load(accountId)
            val data = result?.getOrNull()
            _uiState.value = _uiState.value.copy(
                plansLoading = false,
                plans = data?.plans ?: emptyList(),
                plansCached = data?.isCached ?: false,
                plansError = if (data != null) null else result?.exceptionOrNull()?.message
                    ?: "No se pudieron consultar los planes. Inténtelo nuevamente."
            )
        }
    }

    fun hidePlans() { _uiState.value = _uiState.value.copy(plansVisible = false) }

    fun refresh() {
        if (isStaff) {
            _uiState.value = _uiState.value.copy(isLoading = false, isOffline = false, errorMessage = null)
            return
        }
        scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val memResult = getMembershipSummaryUseCase.refresh(accountId)
            val payResult = getPaymentHistoryUseCase.refresh(accountId)

            if (memResult.isFailure && payResult.isFailure) {
                val error = memResult.exceptionOrNull() ?: payResult.exceptionOrNull()
                val notice = when (error) {
                    is AppError.Network -> ConnectionNoticeType.OFFLINE_NETWORK
                    is AppError.Server -> ConnectionNoticeType.SERVER_ERROR
                    is AppError.Unauthorized -> ConnectionNoticeType.SESSION_EXPIRED
                    else -> ConnectionNoticeType.ACCESS_DENIED
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isOffline = notice == ConnectionNoticeType.OFFLINE_NETWORK || notice == ConnectionNoticeType.SERVER_ERROR,
                    noticeType = notice,
                    errorMessage = error?.message ?: "No se pudo actualizar la membresía."
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isOffline = false,
                    noticeType = ConnectionNoticeType.NONE,
                    errorMessage = null
                )
            }
        }
    }

    fun onCleared() {
        scope.cancel()
    }
}
