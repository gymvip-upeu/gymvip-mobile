package com.gymvip.mobile.presentation.viewmodel

import com.gymvip.mobile.domain.error.AppError
import com.gymvip.mobile.domain.model.MemberProfile
import com.gymvip.mobile.domain.model.MembershipSummary
import com.gymvip.mobile.domain.repository.ConfigRepository
import com.gymvip.mobile.domain.repository.MemberRepository
import com.gymvip.mobile.domain.usecase.GetMembershipSummaryUseCase
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

data class HomeUiState(
    val isLoading: Boolean = false,
    val profile: MemberProfile? = null,
    val membership: MembershipSummary? = null,
    val remoteNoticeBanner: String? = null,
    val isOffline: Boolean = false,
    val noticeType: ConnectionNoticeType = ConnectionNoticeType.NONE,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val memberRepository: MemberRepository,
    private val getMembershipSummaryUseCase: GetMembershipSummaryUseCase,
    private val configRepository: ConfigRepository,
    private val accountId: String,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeData()
        fetchRemoteConfig()
        refreshAll()
    }

    private fun observeData() {
        memberRepository.observeProfile(accountId).onEach { prof ->
            _uiState.value = _uiState.value.copy(profile = prof)
        }.launchIn(scope)

        getMembershipSummaryUseCase.observe(accountId).onEach { mem ->
            _uiState.value = _uiState.value.copy(membership = mem)
        }.launchIn(scope)
    }

    private fun fetchRemoteConfig() {
        scope.launch {
            val banner = configRepository.fetchNoticeBanner()
            _uiState.value = _uiState.value.copy(remoteNoticeBanner = banner)
        }
    }

    fun refreshAll() {
        scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val profResult = memberRepository.refreshProfile(accountId)
            val memResult = getMembershipSummaryUseCase.refresh(accountId)

            if (profResult.isFailure && memResult.isFailure) {
                val ex = profResult.exceptionOrNull() ?: memResult.exceptionOrNull()
                val (noticeType, message) = when (ex) {
                    is AppError.Unauthorized -> ConnectionNoticeType.SESSION_EXPIRED to "Sesión expirada. Inicie sesión nuevamente para actualizar sus datos."
                    is AppError.Server -> ConnectionNoticeType.SERVER_ERROR to "El servidor de GYM VIP no responde temporalmente. Mostrando datos locales de su última sesión."
                    is AppError.Network -> ConnectionNoticeType.OFFLINE_NETWORK to "Sin conexión a internet. Mostrando datos locales de su última sesión."
                    else -> ConnectionNoticeType.OFFLINE_NETWORK to "Modo sin conexión. Mostrando datos locales de su última sesión."
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isOffline = true,
                    noticeType = noticeType,
                    errorMessage = message
                )
            } else {
                // Al reconectar y sincronizar exitosamente, ocultar automáticamente el banner
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
