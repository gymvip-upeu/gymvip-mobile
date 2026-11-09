package com.gymvip.mobile.domain.repository

import com.gymvip.mobile.domain.model.MemberProfile
import kotlinx.coroutines.flow.Flow

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class Authenticated(val profile: MemberProfile, val isCachedOffline: Boolean = false) : AuthState()
    data object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

interface AuthRepository {
    fun getAuthState(): Flow<AuthState>
    suspend fun login(email: String, password: String): Result<MemberProfile>
    suspend fun logout(): Result<Unit>
    suspend fun activateAccount(token: String, newPassword: String): Result<Unit>
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
