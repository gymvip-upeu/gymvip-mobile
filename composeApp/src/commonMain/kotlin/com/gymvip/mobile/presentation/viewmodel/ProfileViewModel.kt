package com.gymvip.mobile.presentation.viewmodel

import com.gymvip.mobile.domain.model.MemberProfile
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
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

class ProfileViewModel(
    private val memberRepository: MemberRepository,
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

    fun saveProfile(firstName: String, lastName: String, phone: String, onSaved: () -> Unit) {
        scope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            val result = memberRepository.updateOwnProfile(accountId, firstName.trim(), lastName.trim(), phone.trim())
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isSaving = false, profile = it)
                    onSaved()
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isSaving = false,
                        errorMessage = it.message ?: "No se pudieron guardar los cambios.")
                }
            )
        }
    }

    fun onCleared() {
        scope.cancel()
    }
}
