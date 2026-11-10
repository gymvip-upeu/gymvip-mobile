package pe.edu.upeu.gymvip.data.local

interface SecureTokenStore {
    suspend fun saveTokens(accessToken: String, refreshToken: String?, accountId: String)
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun getAccountId(): String?
    suspend fun clear()
    suspend fun saveRememberedIdentifier(identifier: String)
    suspend fun getRememberedIdentifier(): String?
    suspend fun clearRememberedIdentifier()
    suspend fun saveSessionMetadata(email: String, mustChangePassword: Boolean)
    suspend fun getSessionEmail(): String?
    suspend fun mustChangePassword(): Boolean
    suspend fun hasSeenOnboarding(): Boolean
    suspend fun completeOnboarding()
    suspend fun getPreference(key: String): String? = null
    suspend fun setPreference(key: String, value: String) {}
}

expect class PlatformSecureTokenStore : SecureTokenStore
