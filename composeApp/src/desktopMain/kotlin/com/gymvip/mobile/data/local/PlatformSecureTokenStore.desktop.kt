package com.gymvip.mobile.data.local

import java.util.prefs.Preferences

actual class PlatformSecureTokenStore : SecureTokenStore {
    private val prefs = Preferences.userNodeForPackage(PlatformSecureTokenStore::class.java)

    override suspend fun saveTokens(accessToken: String, refreshToken: String?, accountId: String) {
        prefs.put("access_token", accessToken)
        if (refreshToken != null) {
            prefs.put("refresh_token", refreshToken)
        } else {
            prefs.remove("refresh_token")
        }
        prefs.put("account_id", accountId)
    }

    override suspend fun getAccessToken(): String? = prefs.get("access_token", null)

    override suspend fun getRefreshToken(): String? = prefs.get("refresh_token", null)

    override suspend fun getAccountId(): String? = prefs.get("account_id", null)

    override suspend fun clear() {
        prefs.remove("access_token")
        prefs.remove("refresh_token")
        prefs.remove("account_id")
    }

    override suspend fun saveRememberedIdentifier(identifier: String) {
        prefs.put("remembered_identifier", identifier)
    }

    override suspend fun getRememberedIdentifier(): String? = prefs.get("remembered_identifier", null)

    override suspend fun clearRememberedIdentifier() {
        prefs.remove("remembered_identifier")
    }
}

