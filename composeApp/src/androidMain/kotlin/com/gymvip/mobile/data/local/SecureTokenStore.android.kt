package com.gymvip.mobile.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

actual class PlatformSecureTokenStore(private val context: Context) : SecureTokenStore {

    private val memoryFallback = mutableMapOf<String, String>()

    private val prefs: SharedPreferences? by lazy {
        runCatching {
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
        }.getOrNull()
    }

    override val supportsSecureCredentialStorage: Boolean
        get() = prefs != null

    private fun writeValues(values: Map<String, String?>) {
        val securePrefs = prefs
        if (securePrefs == null) {
            values.forEach { (key, value) ->
                if (value == null) memoryFallback.remove(key) else memoryFallback[key] = value
            }
            return
        }

        val editor = securePrefs.edit()
        values.forEach { (key, value) ->
            if (value == null) editor.remove(key) else editor.putString(key, value)
        }
        if (!runCatching { editor.commit() }.getOrDefault(false)) {
            values.forEach { (key, value) ->
                if (value == null) memoryFallback.remove(key) else memoryFallback[key] = value
            }
        }
    }

    private fun readValue(key: String): String? {
        val stored = runCatching { prefs?.getString(key, null) }.getOrNull()
        return stored ?: memoryFallback[key]
    }

    private fun removeValues(vararg keys: String) {
        val securePrefs = prefs
        if (securePrefs != null) {
            val editor = securePrefs.edit()
            keys.forEach(editor::remove)
            runCatching { editor.commit() }
        }
        keys.forEach(memoryFallback::remove)
    }

    override suspend fun saveTokens(accessToken: String, refreshToken: String?, accountId: String) {
        writeValues(
            mapOf(
                KEY_ACCESS_TOKEN to accessToken,
                KEY_REFRESH_TOKEN to refreshToken,
                KEY_ACCOUNT_ID to accountId
            )
        )
    }

    override suspend fun getAccessToken(): String? = readValue(KEY_ACCESS_TOKEN)

    override suspend fun getRefreshToken(): String? = readValue(KEY_REFRESH_TOKEN)

    override suspend fun getAccountId(): String? = readValue(KEY_ACCOUNT_ID)

    override suspend fun clear() {
        removeValues(KEY_ACCESS_TOKEN, KEY_REFRESH_TOKEN, KEY_ACCOUNT_ID, KEY_ACCOUNT_ROLES)
    }

    override suspend fun saveRememberedIdentifier(identifier: String) {
        writeValues(mapOf(KEY_REMEMBERED_IDENTIFIER to identifier))
    }

    override suspend fun getRememberedIdentifier(): String? = readValue(KEY_REMEMBERED_IDENTIFIER)

    override suspend fun clearRememberedIdentifier() {
        removeValues(KEY_REMEMBERED_IDENTIFIER)
    }

    override suspend fun saveRememberedPassword(password: String) {
        writeValues(mapOf(KEY_REMEMBERED_PASSWORD to password))
    }

    override suspend fun getRememberedPassword(): String? = readValue(KEY_REMEMBERED_PASSWORD)

    override suspend fun clearRememberedPassword() {
        removeValues(KEY_REMEMBERED_PASSWORD)
    }

    override suspend fun saveAccountRoles(roles: List<String>) {
        writeValues(mapOf(KEY_ACCOUNT_ROLES to roles.joinToString(ROLE_SEPARATOR)))
    }

    override suspend fun getAccountRoles(): List<String> {
        return readValue(KEY_ACCOUNT_ROLES)
            ?.split(ROLE_SEPARATOR)
            ?.filter(String::isNotBlank)
            .orEmpty()
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "jwt_access_token"
        const val KEY_REFRESH_TOKEN = "jwt_refresh_token"
        const val KEY_ACCOUNT_ID = "auth_account_id"
        const val KEY_ACCOUNT_ROLES = "auth_account_roles"
        const val KEY_REMEMBERED_IDENTIFIER = "remembered_identifier"
        const val KEY_REMEMBERED_PASSWORD = "remembered_password"
        const val ROLE_SEPARATOR = "\u001F"
    }
}