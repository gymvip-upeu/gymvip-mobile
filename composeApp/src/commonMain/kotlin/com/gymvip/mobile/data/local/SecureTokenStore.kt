package com.gymvip.mobile.data.local

interface SecureTokenStore {
    suspend fun saveTokens(accessToken: String, refreshToken: String?, accountId: String)
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun getAccountId(): String?
    suspend fun clear()
}

expect class PlatformSecureTokenStore : SecureTokenStore
