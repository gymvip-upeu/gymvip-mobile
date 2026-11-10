package pe.edu.upeu.gymvip.presentation.viewmodel

import pe.edu.upeu.gymvip.domain.model.MemberProfile
import pe.edu.upeu.gymvip.domain.repository.AuthRepository
import pe.edu.upeu.gymvip.domain.repository.MemberRepository
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

data class ProfileUiState(
    val profile: MemberProfile? = null,
    val isLoading: Boolean = false,
    val isLoggingOut: Boolean = false
)

class ProfileViewModel(
    private val memberRepository: MemberRepository,
    private val authRepository: AuthRepository,
    private val accountId: String,
    refreshOnInit: Boolean = true,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        memberRepository.observeProfile(accountId).onEach { prof ->
            _uiState.value = _uiState.value.copy(profile = prof)
        }.launchIn(scope)

        if (refreshOnInit) refresh()
    }

    fun refresh() {
        scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            memberRepository.refreshProfile(accountId)
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun logout() {
        scope.launch {
            _uiState.value = _uiState.value.copy(isLoggingOut = true)
            authRepository.logout()
        }
    }

    fun onCleared() {
        scope.cancel()
    }
}
