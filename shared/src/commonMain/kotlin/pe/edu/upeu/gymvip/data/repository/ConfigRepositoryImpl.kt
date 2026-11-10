package pe.edu.upeu.gymvip.data.repository

import pe.edu.upeu.gymvip.data.local.SecureTokenStore
import pe.edu.upeu.gymvip.domain.repository.ConfigRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ConfigRepositoryImpl(private val store: SecureTokenStore,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)) : ConfigRepository {
    private val theme = MutableStateFlow("DARK")
    private val expiry = MutableStateFlow(true)
    private val mutex = Mutex()
    private val initial = scope.async {
        mutex.withLock {
            store.getPreference("theme")?.takeIf { it in setOf("DARK", "LIGHT", "SYSTEM") }?.let { theme.value = it }
            expiry.value = store.getPreference("expiry_popups") != "false"
        }
    }
    override suspend fun fetchNoticeBanner(): String? = null
    override suspend fun isFeatureEnabled(featureKey: String, defaultValue: Boolean) = defaultValue
    override fun getThemePreference(): Flow<String> = theme.asStateFlow()
    override suspend fun setThemePreference(theme: String) {
        require(theme in setOf("DARK", "LIGHT", "SYSTEM"))
        initial.await()
        mutex.withLock { store.setPreference("theme", theme); this.theme.value = theme }
    }
    override fun getExpiryPopups(): Flow<Boolean> = expiry.asStateFlow()
    override suspend fun setExpiryPopups(enabled: Boolean) {
        initial.await()
        mutex.withLock { store.setPreference("expiry_popups", enabled.toString()); expiry.value = enabled }
    }
}
