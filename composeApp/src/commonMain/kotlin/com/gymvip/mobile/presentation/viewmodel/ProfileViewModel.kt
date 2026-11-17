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
    val isLoading: Boolean = false
)

private data class ProfileEdit(val firstName: String, val lastName: String, val phone: String?)

class ProfileViewModel(
    private val memberRepository: MemberRepository,
    private val accountId: String,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    private var localEdit: ProfileEdit? = null

    init {
        memberRepository.observeProfile(accountId).onEach { prof ->
            val edit = localEdit
            val displayedProfile = if (edit == null) prof else prof?.copy(
                firstName = edit.firstName, lastName = edit.lastName, phone = edit.phone
            )
            _uiState.value = _uiState.value.copy(profile = displayedProfile)
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

    fun editLocally(firstName: String, lastName: String, phone: String) {
        val profile = _uiState.value.profile ?: return
        val edit = ProfileEdit(firstName.trim(), lastName.trim(), phone.trim().ifBlank { null })
        localEdit = edit
        _uiState.value = _uiState.value.copy(
            profile = profile.copy(firstName = edit.firstName, lastName = edit.lastName, phone = edit.phone)
        )
    }

    fun onCleared() {
        scope.cancel()
    }
}
