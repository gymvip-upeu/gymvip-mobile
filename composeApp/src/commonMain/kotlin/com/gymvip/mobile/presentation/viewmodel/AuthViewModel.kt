package com.gymvip.mobile.presentation.viewmodel

import com.gymvip.mobile.domain.model.MemberProfile
import com.gymvip.mobile.domain.repository.AuthRepository
import com.gymvip.mobile.domain.repository.AuthState
import com.gymvip.mobile.domain.usecase.ActivateAccountUseCase
import com.gymvip.mobile.domain.usecase.LoginUseCase
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

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isCachedOffline: Boolean = false,
    val authenticatedProfile: MemberProfile? = null,
    val rememberedIdentifier: String? = null,
    val rememberedPassword: String? = null,
    val secureCredentialStorageAvailable: Boolean = false,
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

    init {
        scope.launch {
            val rememberedIdentifier = authRepository.getRememberedIdentifier()
            val secureStorageAvailable = authRepository.supportsSecureCredentialStorage()
            val rememberedPassword = if (secureStorageAvailable) {
                authRepository.getRememberedPassword()
            } else {
                null
            }
            _uiState.value = _uiState.value.copy(
                rememberedIdentifier = rememberedIdentifier,
                rememberedPassword = rememberedPassword,
                secureCredentialStorageAvailable = secureStorageAvailable
            )
        }

        authRepository.getAuthState().onEach { state ->
            when (state) {
                is AuthState.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                }
                is AuthState.Authenticated -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        isCachedOffline = state.isCachedOffline,
                        authenticatedProfile = state.profile,
                        errorMessage = null
                    )
                }
                is AuthState.Unauthenticated -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = false,
                        isCachedOffline = false,
                        authenticatedProfile = null
                    )
                }
                is AuthState.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = state.message
                    )
                }
                AuthState.Idle -> Unit
            }
        }.launchIn(scope)

        checkSession()
    }

    fun checkSession() {
        scope.launch {
            authRepository.checkExistingSession()
        }
    }

    fun login(email: String, pass: String, rememberCredentials: Boolean = false) {
        scope.launch {
            if (!rememberCredentials) {
                authRepository.clearRememberedIdentifier()
                authRepository.clearRememberedPassword()
                _uiState.value = _uiState.value.copy(
                    rememberedIdentifier = null,
                    rememberedPassword = null
                )
            }

            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = loginUseCase(email, pass)
            result.fold(
                onSuccess = {
                    if (rememberCredentials) {
                        authRepository.saveRememberedIdentifier(email.trim())
                        val canStorePasswordSecurely = authRepository.supportsSecureCredentialStorage()
                        if (canStorePasswordSecurely) {
                            authRepository.saveRememberedPassword(pass)
                        } else {
                            authRepository.clearRememberedPassword()
                        }
                        _uiState.value = _uiState.value.copy(
                            rememberedIdentifier = email.trim(),
                            rememberedPassword = if (canStorePasswordSecurely) pass else null,
                            secureCredentialStorageAvailable = canStorePasswordSecurely
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Error al autenticar"
                    )
                }
            )
        }
    }

    fun activateAccount(token: String, newPass: String, confirmPass: String) {
        scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            val result = activateAccountUseCase(token, newPass, confirmPass)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "¡Cuenta activada con éxito! Ya puede iniciar sesión con su nueva contraseña."
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Error en activación"
                    )
                }
            )
        }
    }

    fun requestPasswordReset(email: String, onComplete: (Result<String>) -> Unit = {}) {
        val trimmed = email.trim()
        if (!trimmed.contains("@") || trimmed.endsWith("@")) {
            val err = "Ingresa un correo electrónico válido"
            _uiState.value = _uiState.value.copy(errorMessage = err)
            onComplete(Result.failure(IllegalArgumentException(err)))
            return
        }

        scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            val result = authRepository.requestPasswordReset(trimmed)
            result.fold(
                onSuccess = { msg ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = msg,
                        errorMessage = null
                    )
                    onComplete(Result.success(msg))
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Error al solicitar restablecimiento"
                    )
                    onComplete(Result.failure(error))
                }
            )
        }
    }

    fun confirmPasswordReset(
        token: String,
        newPass: String,
        confirmPass: String,
        onComplete: (Result<String>) -> Unit = {}
    ) {
        val trimmedToken = token.trim()
        if (trimmedToken.isBlank()) {
            val err = "Ingresa el código o token de recuperación"
            _uiState.value = _uiState.value.copy(errorMessage = err)
            onComplete(Result.failure(IllegalArgumentException(err)))
            return
        }
        if (newPass.length < 8) {
            val err = "La nueva contraseña debe tener al menos 8 caracteres"
            _uiState.value = _uiState.value.copy(errorMessage = err)
            onComplete(Result.failure(IllegalArgumentException(err)))
            return
        }
        if (newPass != confirmPass) {
            val err = "Las contraseñas no coinciden"
            _uiState.value = _uiState.value.copy(errorMessage = err)
            onComplete(Result.failure(IllegalArgumentException(err)))
            return
        }

        scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            val result = authRepository.confirmPasswordReset(trimmedToken, newPass)
            result.fold(
                onSuccess = { msg ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = msg,
                        errorMessage = null,
                        rememberedIdentifier = null,
                        rememberedPassword = null
                    )
                    onComplete(Result.success(msg))
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Error al actualizar contraseña"
                    )
                    onComplete(Result.failure(error))
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    fun logout() {
        scope.launch {
            authRepository.logout()
        }
    }

    fun onCleared() {
        scope.cancel()
    }
}