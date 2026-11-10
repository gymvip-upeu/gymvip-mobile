package pe.edu.upeu.gymvip.presentation.viewmodel

import pe.edu.upeu.gymvip.domain.model.MembershipSummary
import pe.edu.upeu.gymvip.domain.model.PaymentSummary
import pe.edu.upeu.gymvip.domain.usecase.GetMembershipSummaryUseCase
import pe.edu.upeu.gymvip.domain.usecase.GetPaymentHistoryUseCase
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
    val errorMessage: String? = null
)

class MembershipViewModel(
    private val getMembershipSummaryUseCase: GetMembershipSummaryUseCase,
    private val getPaymentHistoryUseCase: GetPaymentHistoryUseCase,
    private val accountId: String,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
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

    fun refresh() {
        scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val memResult = getMembershipSummaryUseCase.refresh(accountId)
            val payResult = getPaymentHistoryUseCase.refresh(accountId)

            if (memResult.isFailure && payResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isOffline = true,
                    errorMessage = "No se pudo actualizar con el servidor. Mostrando información local archivada."
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isOffline = false,
                    errorMessage = null
                )
            }
        }
    }

    fun onCleared() {
        scope.cancel()
    }
}
