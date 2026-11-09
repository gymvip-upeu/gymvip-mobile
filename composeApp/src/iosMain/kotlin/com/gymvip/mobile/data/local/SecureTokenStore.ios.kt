package com.gymvip.mobile.data.local

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

actual class PlatformSecureTokenStore : SecureTokenStore {
    private val mutex = Mutex()
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var accountId: String? = null
    private var rememberedIdentifier: String? = null

    override suspend fun saveTokens(accessToken: String, refreshToken: String?, accountId: String) {
        mutex.withLock {
            this.accessToken = accessToken
            this.refreshToken = refreshToken
            this.accountId = accountId
        }
    }

    override suspend fun getAccessToken(): String? = mutex.withLock { accessToken }

    override suspend fun getRefreshToken(): String? = mutex.withLock { refreshToken }

    override suspend fun getAccountId(): String? = mutex.withLock { accountId }

    override suspend fun clear() {
        mutex.withLock {
            accessToken = null
            refreshToken = null
            accountId = null
        }
    }

    override suspend fun saveRememberedIdentifier(identifier: String) {
        mutex.withLock { rememberedIdentifier = identifier }
    }

    override suspend fun getRememberedIdentifier(): String? = mutex.withLock { rememberedIdentifier }

    override suspend fun clearRememberedIdentifier() {
        mutex.withLock { rememberedIdentifier = null }
    }
}
