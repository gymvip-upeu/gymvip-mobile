package com.gymvip.mobile.data.repository

import com.gymvip.mobile.data.local.GymVipDatabase
import com.gymvip.mobile.data.local.SecureTokenStore
import com.gymvip.mobile.data.mapper.toDomain
import com.gymvip.mobile.data.remote.api.AuthApi
import com.gymvip.mobile.data.remote.api.MemberApi
import com.gymvip.mobile.data.remote.dto.ActivationRequestDto
import com.gymvip.mobile.data.remote.dto.FirstPasswordRequestDto
import com.gymvip.mobile.data.remote.dto.LoginRequestDto
import com.gymvip.mobile.data.remote.dto.LoginResponseDto
import com.gymvip.mobile.data.remote.dto.MfaLoginVerifyRequestDto
import com.gymvip.mobile.data.remote.dto.PasswordResetConfirmRequestDto
import com.gymvip.mobile.data.remote.dto.PasswordResetRequestDto
import com.gymvip.mobile.domain.error.AppError
import com.gymvip.mobile.domain.model.AccountStatus
import com.gymvip.mobile.domain.model.MemberProfile
import com.gymvip.mobile.domain.model.normalizedAccountRoles
import com.gymvip.mobile.domain.repository.AuthRepository
import com.gymvip.mobile.domain.repository.AuthState
import com.gymvip.mobile.domain.repository.DirectorySessionStarted
import com.gymvip.mobile.domain.repository.MfaChallengePendingException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock

class AuthRepositoryImpl(
    private val authApi: AuthApi,
    private val memberApi: MemberApi,
    private val secureTokenStore: SecureTokenStore,
    private val database: GymVipDatabase
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    override fun getAuthState(): Flow<AuthState> = _authState.asStateFlow()

    override suspend fun checkExistingSession(): Boolean {
        val token = secureTokenStore.getAccessToken()
        val accountId = secureTokenStore.getAccountId()

        if (!token.isNullOrBlank() && !accountId.isNullOrBlank()) {
            val localProfile = database.gymVipDatabaseQueries.selectProfileByAccountId(accountId).executeAsOneOrNull()
            val currentUser = authApi.getCurrentUser()
            val serverProfile = currentUser.getOrNull()
            val authError = currentUser.exceptionOrNull()
            if (authError is AppError.Unauthorized || authError is AppError.Forbidden) {
                secureTokenStore.clear()
                _authState.value = AuthState.Unauthenticated
                return false
            }
            if (serverProfile?.mfaEnrollmentRequired == true) {
                secureTokenStore.clear()
                _authState.value = AuthState.Error("Configura la autenticación de dos factores en el portal web antes de ingresar a la app.")
                return false
            }
            if (serverProfile?.firstAccessPasswordRequired == true) {
                _authState.value = AuthState.PasswordChangeRequired(accountId, serverProfile.email, sharedAuthEndpoint = true)
                return true
            }
            if (serverProfile?.directoryOnly == true) {
                _authState.value = AuthState.DirectoryOnly
                return true
            }
            val serverRoles = serverProfile?.normalizedRoles().orEmpty()
            val accountRoles = serverRoles.ifEmpty { secureTokenStore.getAccountRoles().normalizedAccountRoles() }
            if (serverRoles.isNotEmpty()) secureTokenStore.saveAccountRoles(serverRoles)

            // Si la cuenta es administrativa (GERENTE, ADMIN o RECEPCION), no requiere consulta de socio
            if (accountRoles.isNotEmpty() && !accountRoles.contains("CLIENTE")) {
                if (serverProfile == null) {
                    _authState.value = AuthState.Error("No pudimos comprobar tu acceso. Reintenta cuando vuelva la conexión.")
                    return false
                }
                val now = Clock.System.now().toString()
                val profile = localProfile?.toDomain()?.copy(accountId = accountId, roles = accountRoles) ?: MemberProfile(
                    accountId = accountId,
                    email = secureTokenStore.getRememberedIdentifier().orEmpty().ifBlank { "admin@gymvip.local" },
                    firstName = when {
                        accountRoles.contains("GERENTE") -> "Gerente"
                        accountRoles.contains("ADMIN") -> "Administrador"
                        else -> "Recepción"
                    },
                    lastName = "GYM VIP",
                    documentType = "DNI",
                    documentNumber = "",
                    phone = null,
                    status = AccountStatus.ACTIVO,
                    lastSyncedAt = now,
                    roles = accountRoles
                )
                _authState.value = AuthState.Authenticated(profile, isCachedOffline = false)
                return true
            }

            // Intentar revalidación con backend si es CLIENTE
            val remoteResult = memberApi.getProfile(accountId)
            if (remoteResult.isSuccess) {
                val now = Clock.System.now().toString()
                val profile = remoteResult.getOrThrow().toDomain(now).copy(accountId = accountId, roles = accountRoles)
                database.gymVipDatabaseQueries.insertOrReplaceProfile(
                    accountId = profile.accountId,
                    email = profile.email,
                    firstName = profile.firstName,
                    lastName = profile.lastName,
                    documentType = profile.documentType,
                    documentNumber = profile.documentNumber,
                    phone = profile.phone,
                    status = profile.status.name,
                    lastSyncedAt = profile.lastSyncedAt
                )
                _authState.value = AuthState.Authenticated(profile, isCachedOffline = false)
                return true
            }

            // Si falló por 401 (token expirado/inválido), limpiar
            val err = remoteResult.exceptionOrNull()
            if (err is AppError.Unauthorized) {
                secureTokenStore.clear()
                _authState.value = AuthState.Unauthenticated
                return false
            }

            // Si falló por 403 (ej: contraseña temporal pendiente de cambio)
            if (err is AppError.Forbidden) {
                val sessionStatus = memberApi.getSessionStatus().getOrNull()
                if (sessionStatus?.mustChangePassword == true) {
                    val now = Clock.System.now().toString()
                    val profile = localProfile?.toDomain()?.copy(accountId = accountId, roles = accountRoles) ?: MemberProfile(
                        accountId = accountId,
                        email = sessionStatus.email ?: secureTokenStore.getRememberedIdentifier().orEmpty().ifBlank { "Socio VIP" },
                        firstName = "Socio",
                        lastName = "VIP",
                        documentType = "DNI",
                        documentNumber = "",
                        phone = null,
                        status = AccountStatus.ACTIVO,
                        lastSyncedAt = now,
                        roles = accountRoles
                    )
                    _authState.value = AuthState.PasswordChangeRequired(accountId, profile.email)
                    return true
                }
                secureTokenStore.clear()
                _authState.value = AuthState.Unauthenticated
                return false
            }

            // Si hay desconexión o timeout pero existe perfil local previamente sincronizado:
            if (localProfile != null) {
                _authState.value = AuthState.Authenticated(localProfile.toDomain().copy(accountId = accountId, roles = accountRoles), isCachedOffline = true)
                return true
            }

            // Si falló la red y no hay ningún perfil previamente persistido en caché para esta cuenta,
            // no asumimos sesión válida ni fabricamos perfiles provisionales no autorizados:
            secureTokenStore.clear()
            _authState.value = AuthState.Unauthenticated
            return false
        }
        _authState.value = AuthState.Unauthenticated
        return false
    }

    override suspend fun login(email: String, password: String): Result<MemberProfile> {
        _authState.value = AuthState.Loading
        val loginResult = authApi.login(LoginRequestDto(email.trim(), password))
        return processLoginDtoResult(loginResult)
    }

    override suspend fun verifyMfaLogin(challengeToken: String, code: String): Result<MemberProfile> {
        _authState.value = AuthState.Loading
        val verifyResult = authApi.verifyMfaLogin(MfaLoginVerifyRequestDto(challengeToken, code.trim()))
        return processLoginDtoResult(verifyResult)
    }

    private suspend fun processLoginDtoResult(loginResult: Result<LoginResponseDto>): Result<MemberProfile> {
        return loginResult.fold(
            onSuccess = { dto ->
                if (dto.mfaRequired && !dto.challengeToken.isNullOrBlank()) {
                    val prompt = dto.message ?: "Ingrese el código TOTP de 6 dígitos o código de recuperación."
                    _authState.value = AuthState.MfaChallengeRequired(dto.challengeToken, dto.email, prompt)
                    return@fold Result.failure(MfaChallengePendingException(dto.challengeToken, dto.email, prompt))
                }
                val token = dto.token?.takeIf { it.isNotBlank() }
                    ?: return@fold Result.failure(IllegalStateException("El servidor no devolvió un token de sesión válido."))
                if (dto.mfaEnrollmentRequired) {
                    val message = "Configura la autenticación de dos factores en el portal web antes de ingresar a la app."
                    _authState.value = AuthState.Error(message)
                    return@fold Result.failure(AppError.Forbidden(message))
                }
                val accountId = dto.resolvedAccountId
                secureTokenStore.saveTokens(
                    accessToken = token,
                    refreshToken = null,
                    accountId = accountId
                )
                val accountRoles = dto.roles.normalizedAccountRoles()
                secureTokenStore.saveAccountRoles(accountRoles)

                val now = Clock.System.now().toString()

                if (dto.firstAccessPasswordRequired) {
                    _authState.value = AuthState.PasswordChangeRequired(accountId, dto.email, sharedAuthEndpoint = true)
                    return@fold Result.failure(AppError.Forbidden("Crea tu nueva contraseña para continuar."))
                }

                if (dto.directoryOnly) {
                    _authState.value = AuthState.DirectoryOnly
                    return@fold Result.failure(DirectorySessionStarted())
                }

                // Si NO es socio (ej. GERENTE, ADMIN o RECEPCION):
                if (!accountRoles.contains("CLIENTE")) {
                    val profile = MemberProfile(
                        accountId = accountId,
                        email = dto.email,
                        firstName = when {
                            accountRoles.contains("GERENTE") -> "Gerente"
                            accountRoles.contains("ADMIN") -> "Administrador"
                            else -> "Recepción"
                        },
                        lastName = "GYM VIP",
                        documentType = "DNI",
                        documentNumber = "",
                        phone = null,
                        status = AccountStatus.ACTIVO,
                        lastSyncedAt = now,
                        roles = accountRoles
                    )
                    database.gymVipDatabaseQueries.insertOrReplaceProfile(
                        accountId = profile.accountId,
                        email = profile.email,
                        firstName = profile.firstName,
                        lastName = profile.lastName,
                        documentType = profile.documentType,
                        documentNumber = profile.documentNumber,
                        phone = profile.phone,
                        status = profile.status.name,
                        lastSyncedAt = profile.lastSyncedAt
                    )
                    _authState.value = AuthState.Authenticated(profile)
                    return@fold Result.success(profile)
                }

                // Si ES CLIENTE:
                val sessionStatusResult = memberApi.getSessionStatus()
                if (sessionStatusResult.isFailure) {
                    secureTokenStore.clear()
                    _authState.value = AuthState.Error("No pudimos comprobar tu acceso. Reintenta cuando vuelva la conexión.")
                    return@fold Result.failure(sessionStatusResult.exceptionOrNull()!!)
                }
                val mustChange = sessionStatusResult.getOrNull()?.mustChangePassword ?: false

                val profileResult = if (!mustChange) {
                    memberApi.getProfile(accountId)
                } else {
                    Result.failure(AppError.Forbidden("Debe cambiar su contraseña temporal antes de continuar"))
                }

                val profile = profileResult.getOrNull()?.toDomain(now)?.copy(accountId = accountId, roles = accountRoles) ?: MemberProfile(
                    accountId = accountId,
                    email = dto.email,
                    firstName = "Socio",
                    lastName = "VIP",
                    documentType = "DNI",
                    documentNumber = "",
                    phone = null,
                    status = AccountStatus.ACTIVO,
                    lastSyncedAt = now,
                    roles = accountRoles
                )

                if (mustChange) {
                    _authState.value = AuthState.PasswordChangeRequired(accountId, dto.email)
                    return@fold Result.success(profile)
                }

                database.gymVipDatabaseQueries.insertOrReplaceProfile(
                    accountId = profile.accountId,
                    email = profile.email,
                    firstName = profile.firstName,
                    lastName = profile.lastName,
                    documentType = profile.documentType,
                    documentNumber = profile.documentNumber,
                    phone = profile.phone,
                    status = profile.status.name,
                    lastSyncedAt = profile.lastSyncedAt
                )

                _authState.value = AuthState.Authenticated(profile)
                Result.success(profile)
            },
            onFailure = { error ->
                _authState.value = AuthState.Error(error.message ?: "Error al iniciar sesión")
                Result.failure(error)
            }
        )
    }

    override suspend fun logout(): Result<Unit> {
        val accountId = secureTokenStore.getAccountId()
        com.gymvip.mobile.notification.clearPushRegistration()
        try {
            authApi.logout()
        } catch (_: Exception) {
            // Continuar con limpieza local incluso si no hay red
        }

        if (accountId != null) {
            database.gymVipDatabaseQueries.transaction {
                database.gymVipDatabaseQueries.deleteProfileByAccountId(accountId)
                database.gymVipDatabaseQueries.deleteMembershipByAccountId(accountId)
                database.gymVipDatabaseQueries.deletePaymentsByAccountId(accountId)
                // Se retienen favoritos y sync_queue aislados por accountId para no perder operaciones
                // pendientes del socio silenciosamente tras desconexión o logout
            }
        }

        secureTokenStore.clear()
        _authState.value = AuthState.Unauthenticated
        return Result.success(Unit)
    }

    override suspend fun createFirstPassword(newPassword: String): Result<Unit> {
        val request = FirstPasswordRequestDto(newPassword)
        val access = _authState.value as? AuthState.PasswordChangeRequired
        val result = if (access?.sharedAuthEndpoint == true) {
            authApi.createFirstPassword(request)
        } else {
            memberApi.createFirstPassword(request)
        }
        if (result.isFailure) return Result.failure(result.exceptionOrNull()!!)
        com.gymvip.mobile.notification.clearPushRegistration()
        secureTokenStore.clear()
        _authState.value = AuthState.Unauthenticated
        return Result.success(Unit)
    }

    override suspend fun revokeOtherSessions(): Result<Unit> {
        return authApi.revokeOtherSessions()
    }

    override suspend fun activateAccount(token: String, newPassword: String): Result<Unit> {
        return authApi.activateAccount(ActivationRequestDto(token, newPassword))
    }

    override suspend fun validateActiveSession() {
        if (_authState.value !is AuthState.Authenticated) return
        val result = authApi.getCurrentUser()
        if (result.exceptionOrNull() is AppError.Unauthorized) {
            secureTokenStore.clear()
            _authState.value = AuthState.Unauthenticated
        }
    }

    override suspend fun requestOwnPasswordReset(): Result<String> = authApi.requestOwnPasswordReset().map { it.message }

    override suspend fun requestPasswordReset(email: String): Result<String> {
        val reqResult = authApi.requestPasswordReset(PasswordResetRequestDto(email = email.trim()))
        return reqResult.map { it.message }
    }

    override suspend fun confirmPasswordReset(token: String, newPassword: String): Result<String> {
        val confirmResult = authApi.confirmPasswordReset(
            PasswordResetConfirmRequestDto(
                token = token.trim(),
                newPassword = newPassword
            )
        )
        return confirmResult.map { resp ->
            clearRememberedPassword()
            clearRememberedIdentifier()
            resp.message
        }
    }

    override suspend fun getStoredAccountId(): String? {
        return secureTokenStore.getAccountId()
    }

    override suspend fun getRememberedIdentifier(): String? {
        return secureTokenStore.getRememberedIdentifier()
    }

    override suspend fun saveRememberedIdentifier(identifier: String) {
        secureTokenStore.saveRememberedIdentifier(identifier)
    }

    override suspend fun clearRememberedIdentifier() {
        secureTokenStore.clearRememberedIdentifier()
    }

    override suspend fun getRememberedPassword(): String? {
        return secureTokenStore.getRememberedPassword()
    }

    override suspend fun saveRememberedPassword(password: String) {
        secureTokenStore.saveRememberedPassword(password)
    }

    override suspend fun clearRememberedPassword() {
        secureTokenStore.clearRememberedPassword()
    }

    override suspend fun supportsSecureCredentialStorage(): Boolean {
        return secureTokenStore.supportsSecureCredentialStorage
    }
}
