package pe.edu.upeu.gymvip.domain.repository

import pe.edu.upeu.gymvip.domain.model.MemberProfile
import kotlinx.coroutines.flow.Flow

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class RequiresPasswordChange(val email: String) : AuthState()
    data class Authenticated(val profile: MemberProfile, val isCachedOffline: Boolean = false) : AuthState()
    data class StaffAuthenticated(val usuario: pe.edu.upeu.gymvip.domain.model.UsuarioPersonal) : AuthState()
    data class Unauthenticated(val message: String? = null) : AuthState()
    data class Error(val message: String) : AuthState()
}

interface AuthRepository {
    fun getAuthState(): Flow<AuthState>
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun logout(): Result<Unit>
    suspend fun activateAccount(currentPassword: String, newPassword: String, acceptsTerms: Boolean): Result<Unit>
    suspend fun getStoredAccountId(): String?
    suspend fun checkExistingSession(): Boolean
    suspend fun getRememberedIdentifier(): String?
    suspend fun saveRememberedIdentifier(identifier: String)
    suspend fun clearRememberedIdentifier()
    suspend fun hasSeenOnboarding(): Boolean
    suspend fun completeOnboarding()
}
