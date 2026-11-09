package com.gymvip.mobile.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

actual class PlatformSecureTokenStore(private val context: Context) : SecureTokenStore {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "gymvip_secure_tokens",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            context.getSharedPreferences("gymvip_secure_tokens_fallback", Context.MODE_PRIVATE)
        }
    }

    override suspend fun saveTokens(accessToken: String, refreshToken: String?, accountId: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_ACCOUNT_ID, accountId)
            .apply()
    }

    override suspend fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    override suspend fun getRefreshToken(): String? {
        return prefs.getString(KEY_REFRESH_TOKEN, null)
    }

    override suspend fun getAccountId(): String? {
        return prefs.getString(KEY_ACCOUNT_ID, null)
    }

    override suspend fun clear() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_ACCOUNT_ID)
            .apply()
    }

    override suspend fun saveRememberedIdentifier(identifier: String) {
        prefs.edit().putString(KEY_REMEMBERED_IDENTIFIER, identifier).apply()
    }

    override suspend fun getRememberedIdentifier(): String? {
        return prefs.getString(KEY_REMEMBERED_IDENTIFIER, null)
    }

    override suspend fun clearRememberedIdentifier() {
        prefs.edit().remove(KEY_REMEMBERED_IDENTIFIER).apply()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "jwt_access_token"
        private const val KEY_REFRESH_TOKEN = "jwt_refresh_token"
        private const val KEY_ACCOUNT_ID = "auth_account_id"
        private const val KEY_REMEMBERED_IDENTIFIER = "remembered_identifier"
    }
}
