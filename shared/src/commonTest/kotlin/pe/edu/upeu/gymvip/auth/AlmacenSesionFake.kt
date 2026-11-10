package pe.edu.upeu.gymvip.auth

import pe.edu.upeu.gymvip.data.local.SecureTokenStore

class AlmacenSesionFake : SecureTokenStore {
    var token: String? = null
    var account: String? = null
    var email: String? = null
    var pendiente = true
    var recordado: String? = null
    var onboarding = false
    override suspend fun saveTokens(accessToken: String, refreshToken: String?, accountId: String) { token = accessToken; account = accountId }
    override suspend fun getAccessToken() = token
    override suspend fun getRefreshToken(): String? = null
    override suspend fun getAccountId() = account
    override suspend fun clear() { token = null; account = null; email = null; pendiente = true }
    override suspend fun saveRememberedIdentifier(identifier: String) { recordado = identifier }
    override suspend fun getRememberedIdentifier() = recordado
    override suspend fun clearRememberedIdentifier() { recordado = null }
    override suspend fun saveSessionMetadata(email: String, mustChangePassword: Boolean) { this.email = email; pendiente = mustChangePassword }
    override suspend fun getSessionEmail() = email
    override suspend fun mustChangePassword() = pendiente
    override suspend fun hasSeenOnboarding() = onboarding
    override suspend fun completeOnboarding() { onboarding = true }
}
