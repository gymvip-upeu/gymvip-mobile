package com.gymvip.mobile.data.repository

import com.gymvip.mobile.data.local.GymVipDatabase
import com.gymvip.mobile.data.local.SecureTokenStore
import com.gymvip.mobile.data.mapper.toDomain
import com.gymvip.mobile.data.remote.api.AuthApi
import com.gymvip.mobile.data.remote.api.MemberApi
import com.gymvip.mobile.data.remote.dto.ActivationRequestDto
import com.gymvip.mobile.data.remote.dto.LoginRequestDto
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
            if (localProfile != null) {
                _authState.value = AuthState.Authenticated(localProfile.toDomain())
                return true
            } else {
                // Perfil básico inicial si aún no sincronizó
                val fallbackProfile = MemberProfile(
                    accountId = accountId,
                    email = "",
                    firstName = "Socio",
                    lastName = "VIP",
                    documentType = "DNI",
                    documentNumber = "",
                    phone = null,
                    status = AccountStatus.ACTIVO,
                    lastSyncedAt = Clock.System.now().toString()
                )
                _authState.value = AuthState.Authenticated(fallbackProfile)
                return true
            }
        }
        _authState.value = AuthState.Unauthenticated
        return false
    }

    override suspend fun login(email: String, password: String): Result<MemberProfile> {
        _authState.value = AuthState.Loading
        val loginResult = authApi.login(LoginRequestDto(email, password))

        return loginResult.fold(
            onSuccess = { dto ->
                secureTokenStore.saveTokens(
                    accessToken = dto.token,
                    refreshToken = null,
                    accountId = dto.accountId
                )

                // Intentar obtener perfil completo del socio o guardar perfil básico
                val now = Clock.System.now().toString()
                val profileResult = memberApi.getProfile(dto.accountId)

                val profile = profileResult.getOrNull()?.toDomain(now) ?: MemberProfile(
                    accountId = dto.accountId,
                    email = dto.email,
                    firstName = "Socio",
                    lastName = "VIP",
                    documentType = "DNI",
                    documentNumber = "",
                    phone = null,
                    status = AccountStatus.ACTIVO,
                    lastSyncedAt = now
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
                database.gymVipDatabaseQueries.deleteFavoritesByAccountId(accountId)
                database.gymVipDatabaseQueries.deleteInboxByAccountId(accountId)
                database.gymVipDatabaseQueries.deleteSyncQueueByAccountId(accountId)
            }
        }

        secureTokenStore.clear()
        _authState.value = AuthState.Unauthenticated
        return Result.success(Unit)
    }

    override suspend fun activateAccount(token: String, newPassword: String): Result<Unit> {
        return authApi.activateAccount(ActivationRequestDto(token, newPassword))
    }

    override suspend fun getStoredAccountId(): String? {
        return secureTokenStore.getAccountId()
    }
}
