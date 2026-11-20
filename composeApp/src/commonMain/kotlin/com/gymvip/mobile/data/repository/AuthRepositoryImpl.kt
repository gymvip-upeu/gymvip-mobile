package com.gymvip.mobile.data.repository

import com.gymvip.mobile.data.local.GymVipDatabase
import com.gymvip.mobile.data.local.SecureTokenStore
import com.gymvip.mobile.data.mapper.toDomain
import com.gymvip.mobile.data.remote.api.AuthApi
import com.gymvip.mobile.data.remote.api.MemberApi
import com.gymvip.mobile.data.remote.dto.ActivationRequestDto
import com.gymvip.mobile.data.remote.dto.LoginRequestDto
import com.gymvip.mobile.data.remote.dto.PasswordResetConfirmRequestDto
import com.gymvip.mobile.data.remote.dto.PasswordResetRequestDto
import com.gymvip.mobile.domain.error.AppError
import com.gymvip.mobile.domain.model.AccountStatus
import com.gymvip.mobile.domain.model.MemberProfile
import com.gymvip.mobile.domain.repository.AuthRepository
import com.gymvip.mobile.domain.repository.AuthState
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
            val serverRoles = authApi.getCurrentUser().getOrNull()?.normalizedRoles().orEmpty()
            val accountRoles = serverRoles.ifEmpty { secureTokenStore.getAccountRoles() }
            if (serverRoles.isNotEmpty()) secureTokenStore.saveAccountRoles(serverRoles)

            // Si la cuenta es administrativa (ADMIN o RECEPCION), no requiere consulta de socio
            if (accountRoles.isNotEmpty() && !accountRoles.contains("CLIENTE")) {
                val now = Clock.System.now().toString()
                val profile = localProfile?.toDomain()?.copy(accountId = accountId, roles = accountRoles) ?: MemberProfile(
                    accountId = accountId,
                    email = secureTokenStore.getRememberedIdentifier().orEmpty().ifBlank { "admin@gymvip.local" },
                    firstName = if (accountRoles.contains("ADMIN")) "Administrador" else "Recepción",
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
                        email = secureTokenStore.getRememberedIdentifier().orEmpty().ifBlank { "socio@gymvip.local" },
                        firstName = "Socio",
                        lastName = "VIP",
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

        return loginResult.fold(
            onSuccess = { dto ->
                val accountId = dto.resolvedAccountId
                secureTokenStore.saveTokens(
                    accessToken = dto.token,
                    refreshToken = null,
                    accountId = accountId
                )
                val accountRoles = dto.roles.map { it.trim().uppercase() }.filter(String::isNotBlank).distinct()
                secureTokenStore.saveAccountRoles(accountRoles)

                val now = Clock.System.now().toString()

                // Si NO es socio (ej. ADMIN o RECEPCION):
                if (!accountRoles.contains("CLIENTE")) {
                    val profile = MemberProfile(
                        accountId = accountId,
                        email = dto.email,
                        firstName = if (accountRoles.contains("ADMIN")) "Administrador" else "Recepción",
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

    override suspend fun revokeOtherSessions(): Result<Unit> {
        return authApi.revokeOtherSessions()
    }

    override suspend fun activateAccount(token: String, newPassword: String): Result<Unit> {
        return authApi.activateAccount(ActivationRequestDto(token, newPassword))
    }

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
