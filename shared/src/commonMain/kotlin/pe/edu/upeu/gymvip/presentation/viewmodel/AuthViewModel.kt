package pe.edu.upeu.gymvip.presentation.viewmodel

import pe.edu.upeu.gymvip.domain.model.MemberProfile
import pe.edu.upeu.gymvip.domain.repository.*
import pe.edu.upeu.gymvip.domain.usecase.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class AuthUiState(
    val isLoading: Boolean = false,
    val sessionChecked: Boolean = false,
    val onboardingSeen: Boolean = true,
    val mustChangePassword: Boolean = false,
    val needsTemporaryPassword: Boolean = true,
    val isAuthenticated: Boolean = false,
    val isCachedOffline: Boolean = false,
    val authenticatedProfile: MemberProfile? = null,
    val staffUser: pe.edu.upeu.gymvip.domain.model.UsuarioPersonal? = null,
    val rememberedIdentifier: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val activateAccountUseCase: ActivateAccountUseCase,
    private val authRepository: AuthRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    // Solo memoria efímera: nunca SavedState, disco o estado de UI.
    private var temporaryPassword: String? = null
    private var operation: Job? = null

    init {
        authRepository.getAuthState().onEach { state ->
            _uiState.update { old ->
                when (state) {
                    AuthState.Idle -> old
                    AuthState.Loading -> old.copy(isLoading = true, errorMessage = null)
                    is AuthState.Authenticated -> {
                        temporaryPassword = null
                        old.copy(isLoading = false, isAuthenticated = true, mustChangePassword = false,
                            authenticatedProfile = state.profile, staffUser = null, isCachedOffline = state.isCachedOffline, errorMessage = null)
                    }
                    is AuthState.StaffAuthenticated -> {
                        temporaryPassword = null
                        old.copy(isLoading = false, isAuthenticated = true, mustChangePassword = false,
                            authenticatedProfile = null, staffUser = state.usuario, isCachedOffline = false, errorMessage = null)
                    }
                    is AuthState.RequiresPasswordChange -> old.copy(isAuthenticated = false, isLoading = false,
                        mustChangePassword = true, authenticatedProfile = null, needsTemporaryPassword = temporaryPassword == null)
                    is AuthState.Unauthenticated -> {
                        temporaryPassword = null
                        old.copy(isAuthenticated = false, mustChangePassword = false, isLoading = false,
                            authenticatedProfile = null, staffUser = null, errorMessage = state.message)
                    }
                    is AuthState.Error -> old.copy(isLoading = false, errorMessage = state.message)
                }
            }
        }.launchIn(scope)
        operation = scope.launch {
            _uiState.update { it.copy(rememberedIdentifier = authRepository.getRememberedIdentifier(),
                onboardingSeen = authRepository.hasSeenOnboarding()) }
            authRepository.checkExistingSession()
            _uiState.update { it.copy(sessionChecked = true) }
        }
    }

    fun completeOnboarding(onComplete: () -> Unit) {
        scope.launch {
            authRepository.completeOnboarding()
            _uiState.update { it.copy(onboardingSeen = true) }
            onComplete()
        }
    }

    fun login(email: String, pass: String, rememberIdentifier: Boolean = false) {
        if (operation?.isActive == true) return
        operation = scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = loginUseCase(email, pass)
            if (result.isSuccess) {
                if (rememberIdentifier) authRepository.saveRememberedIdentifier(email.trim())
                else authRepository.clearRememberedIdentifier()
                if (authRepository.getAuthState().first() is AuthState.RequiresPasswordChange) temporaryPassword = pass
                _uiState.update { it.copy(rememberedIdentifier = if (rememberIdentifier) email.trim() else null,
                    needsTemporaryPassword = temporaryPassword == null) }
            }
            _uiState.update { it.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message) }
        }
    }

    fun activateAccount(current: String, newPass: String, confirmPass: String, acceptsTerms: Boolean) {
        if (operation?.isActive == true) return
        operation = scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = activateAccountUseCase(temporaryPassword ?: current, newPass, confirmPass, acceptsTerms)
            if (result.isSuccess) temporaryPassword = null
            _uiState.update { it.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message) }
        }
    }
    fun clearMessages() { _uiState.update { it.copy(errorMessage = null, successMessage = null) } }
    fun logout() {
        operation?.cancel()
        temporaryPassword = null
        operation = scope.launch { authRepository.logout() }
    }
    fun onCleared() { temporaryPassword = null; scope.cancel() }
}
