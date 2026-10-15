package com.gymvip.mobile.presentation.viewmodel

import com.gymvip.mobile.domain.model.MemberProfile
import com.gymvip.mobile.domain.repository.AuthRepository
import com.gymvip.mobile.domain.repository.MemberRepository
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
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        memberRepository.observeProfile(accountId).onEach { prof ->
            _uiState.value = _uiState.value.copy(profile = prof)
        }.launchIn(scope)

        refresh()
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
