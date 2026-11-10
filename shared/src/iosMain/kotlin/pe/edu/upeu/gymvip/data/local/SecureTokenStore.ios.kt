package pe.edu.upeu.gymvip.data.local

import platform.Foundation.NSUserDefaults

/** Persistencia MVP según el prompt. Migración a Keychain pendiente antes de distribución. */
actual class PlatformSecureTokenStore : SecureTokenStore {
    private val prefs = NSUserDefaults(suiteName = "pe.edu.upeu.gymvip.session")
    override suspend fun saveTokens(accessToken: String, refreshToken: String?, accountId: String) {
        prefs.setObject(accessToken, forKey = "access_token")
        prefs.setObject(refreshToken, forKey = "refresh_token")
        prefs.setObject(accountId, forKey = "account_id")
    }
    override suspend fun getAccessToken() = prefs.stringForKey("access_token")
    override suspend fun getRefreshToken() = prefs.stringForKey("refresh_token")
    override suspend fun getAccountId() = prefs.stringForKey("account_id")
    override suspend fun clear() {
        listOf("access_token", "refresh_token", "account_id", "session_email", "must_change_password")
            .forEach { prefs.removeObjectForKey(it) }
    }
    override suspend fun saveRememberedIdentifier(identifier: String) { prefs.setObject(identifier, forKey = "remembered_identifier") }
    override suspend fun getRememberedIdentifier() = prefs.stringForKey("remembered_identifier")
    override suspend fun clearRememberedIdentifier() { prefs.removeObjectForKey("remembered_identifier") }
    override suspend fun saveSessionMetadata(email: String, mustChangePassword: Boolean) {
        prefs.setObject(email, forKey = "session_email")
        prefs.setBool(mustChangePassword, forKey = "must_change_password")
    }
    override suspend fun getSessionEmail() = prefs.stringForKey("session_email")
    override suspend fun mustChangePassword() = prefs.objectForKey("must_change_password") == null || prefs.boolForKey("must_change_password")
    override suspend fun hasSeenOnboarding() = prefs.boolForKey("onboarding_seen")
    override suspend fun completeOnboarding() { prefs.setBool(true, forKey = "onboarding_seen") }
    override suspend fun getPreference(key: String) = prefs.stringForKey("pref_$key")
    override suspend fun setPreference(key: String, value: String) { prefs.setObject(value, forKey = "pref_$key") }
}
