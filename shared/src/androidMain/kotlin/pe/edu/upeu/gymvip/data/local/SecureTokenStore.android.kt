package pe.edu.upeu.gymvip.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

actual class PlatformSecureTokenStore(context: Context) : SecureTokenStore {
    // Fallar de forma cerrada si el almacén seguro no está disponible; sin fallback sin cifrar.
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context, "gymvip_secure_tokens",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    private val appPrefs = context.getSharedPreferences("gymvip_preferences", Context.MODE_PRIVATE)
    override suspend fun saveTokens(accessToken: String, refreshToken: String?, accountId: String) {
        check(prefs.edit().putString("jwt_access_token", accessToken).putString("jwt_refresh_token", refreshToken)
            .putString("auth_account_id", accountId).commit()) { "No se pudo guardar la sesión." }
    }
    override suspend fun getAccessToken() = prefs.getString("jwt_access_token", null)
    override suspend fun getRefreshToken() = prefs.getString("jwt_refresh_token", null)
    override suspend fun getAccountId() = prefs.getString("auth_account_id", null)
    override suspend fun saveSessionMetadata(email: String, mustChangePassword: Boolean) {
        check(prefs.edit().putString("session_email", email).putBoolean("must_change_password", mustChangePassword).commit())
    }
    override suspend fun getSessionEmail() = prefs.getString("session_email", null)
    override suspend fun mustChangePassword() = prefs.getBoolean("must_change_password", true)
    override suspend fun clear() {
        check(prefs.edit().remove("jwt_access_token").remove("jwt_refresh_token").remove("auth_account_id")
            .remove("session_email").remove("must_change_password").commit())
    }
    override suspend fun saveRememberedIdentifier(identifier: String) { prefs.edit().putString("remembered_identifier", identifier).apply() }
    override suspend fun getRememberedIdentifier() = prefs.getString("remembered_identifier", null)
    override suspend fun clearRememberedIdentifier() { prefs.edit().remove("remembered_identifier").apply() }
    override suspend fun hasSeenOnboarding() = appPrefs.getBoolean("onboarding_seen", false)
    override suspend fun completeOnboarding() { appPrefs.edit().putBoolean("onboarding_seen", true).apply() }
    override suspend fun getPreference(key: String) = appPrefs.getString("pref_$key", null)
    override suspend fun setPreference(key: String, value: String) {
        check(appPrefs.edit().putString("pref_$key", value).commit()) { "No se pudo guardar la preferencia." }
    }
}
