package com.gymvip.mobile.data.local

interface SecureTokenStore {
    suspend fun saveTokens(accessToken: String, refreshToken: String?, accountId: String)
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun getAccountId(): String?
    suspend fun clear()
    suspend fun saveRememberedIdentifier(identifier: String)
    suspend fun getRememberedIdentifier(): String?
    suspend fun clearRememberedIdentifier()
    suspend fun saveRememberedPassword(password: String) {}
    suspend fun getRememberedPassword(): String? = null
    suspend fun clearRememberedPassword() {}
    suspend fun saveAccountRoles(roles: List<String>) {}
    suspend fun getAccountRoles(): List<String> = emptyList()
    val supportsSecureCredentialStorage: Boolean get() = false
}

expect class PlatformSecureTokenStore : SecureTokenStore
