package com.gymvip.mobile.domain.repository

import com.gymvip.mobile.domain.model.MemberProfile
import kotlinx.coroutines.flow.Flow

class DirectorySessionStarted : IllegalStateException()

sealed class AuthState {
    data object DirectoryOnly : AuthState()
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class Authenticated(val profile: MemberProfile, val isCachedOffline: Boolean = false) : AuthState()
    data class PasswordChangeRequired(val accountId: String, val email: String, val sharedAuthEndpoint: Boolean = false) : AuthState()
    data class MfaChallengeRequired(val challengeToken: String, val email: String, val message: String) : AuthState()
    data object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

class MfaChallengePendingException(
    val challengeToken: String,
    val email: String,
    override val message: String
) : Exception(message)

interface AuthRepository {
    fun getAuthState(): Flow<AuthState>
    suspend fun login(email: String, password: String): Result<MemberProfile>
    suspend fun verifyMfaLogin(challengeToken: String, code: String): Result<MemberProfile> =
        Result.failure(IllegalStateException("Verificación MFA no disponible"))
    suspend fun logout(): Result<Unit>
    suspend fun createFirstPassword(newPassword: String): Result<Unit> = Result.failure(IllegalStateException("Cambio no disponible"))
    suspend fun revokeOtherSessions(): Result<Unit>
    suspend fun activateAccount(token: String, newPassword: String): Result<Unit>
    suspend fun validateActiveSession() {}
    suspend fun requestOwnPasswordReset(): Result<String> = Result.failure(IllegalStateException("No disponible"))
    suspend fun requestPasswordReset(email: String): Result<String>
    suspend fun confirmPasswordReset(token: String, newPassword: String): Result<String>
    suspend fun getStoredAccountId(): String?
    suspend fun checkExistingSession(): Boolean
    suspend fun getRememberedIdentifier(): String?
    suspend fun saveRememberedIdentifier(identifier: String)
    suspend fun clearRememberedIdentifier()
    suspend fun getRememberedPassword(): String? = null
    suspend fun saveRememberedPassword(password: String) {}
    suspend fun clearRememberedPassword() {}
    suspend fun supportsSecureCredentialStorage(): Boolean = false
}
