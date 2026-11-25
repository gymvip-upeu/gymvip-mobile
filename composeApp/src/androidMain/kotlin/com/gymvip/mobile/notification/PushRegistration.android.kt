package com.gymvip.mobile.notification

import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Base64
import com.google.firebase.messaging.FirebaseMessaging
import com.gymvip.mobile.BuildConfig
import com.gymvip.mobile.di.AppContainer
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.security.MessageDigest
import kotlin.coroutines.resume

object PushDeviceRuntime {
    private lateinit var context: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    internal val mutex = Mutex()
    internal val displayLock = Any()
    private val prefs get() = context.getSharedPreferences("push_registration", Context.MODE_PRIVATE)
    fun initialize(value: Context) {
        context = value.applicationContext
        context.getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { scope.launch { syncPushRegistration() } }
        })
    }
    internal fun registeredToken(): String? = prefs.getString("token", null)
    internal fun blocked(tag: String): Boolean = prefs.getString("blocked", null) == tag
    internal fun matchesBinding(account: String, tag: String): Boolean = !blocked(tag) &&
        prefs.getString("account", null) == account && prefs.getString("session", null) == tag
    internal fun save(token: String, account: String, tag: String) {
        synchronized(displayLock) {
            if (!blocked(tag)) prefs.edit().putString("token", token).putString("account", account).putString("session", tag).commit()
        }
    }
    internal fun clear(tag: String?) {
        synchronized(displayLock) {
            prefs.edit().clear().putString("blocked", tag).commit()
            context.getSystemService(NotificationManager::class.java).cancelAll()
            PushNavigation.clear()
        }
    }
    internal fun sessionTag(jwt: String): String? = runCatching {
        val payload = String(Base64.decode(jwt.split('.')[1], Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8)
        val jti = JSONObject(payload).getString("jti")
        MessageDigest.getInstance("SHA-256").digest(jti.toByteArray()).joinToString("") { "%02x".format(it) }
    }.getOrNull()
    internal suspend fun owns(account: String, tag: String): Boolean {
        if (!AppContainer.isInitialized) return false
        val store = AppContainer.secureTokenStore
        val jwt = store.getAccessToken() ?: return false
        val expires = runCatching {
            JSONObject(String(Base64.decode(jwt.split('.')[1], Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8)).getLong("exp")
        }.getOrDefault(0)
        if (expires <= System.currentTimeMillis() / 1000) return false
        return store.getAccountId() == account && store.getAccountRoles().contains("CLIENTE") &&
            sessionTag(jwt) == tag && matchesBinding(account, tag)
    }
}

actual suspend fun syncPushRegistration() {
    if (!BuildConfig.FIREBASE_ENABLED || !AppContainer.isInitialized) return
    PushDeviceRuntime.mutex.withLock {
        val store = AppContainer.secureTokenStore
        val jwt = store.getAccessToken() ?: return
        val account = store.getAccountId() ?: return
        if (!store.getAccountRoles().contains("CLIENTE")) return
        val tag = PushDeviceRuntime.sessionTag(jwt) ?: return
        if (PushDeviceRuntime.blocked(tag)) return
        val messaging = runCatching { FirebaseMessaging.getInstance() }.getOrNull() ?: return
        val token = withTimeoutOrNull(8_000) {
            suspendCancellableCoroutine<String?> { continuation ->
                messaging.token.addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
                    .addOnFailureListener { if (continuation.isActive) continuation.resume(null) }
            }
        } ?: return
        repeat(3) { attempt ->
            if (store.getAccessToken() != jwt || store.getAccountId() != account) return
            val result = withTimeoutOrNull(8_000) { AppContainer.memberApi.registerDevice(token) }
            if (result?.isSuccess == true) {
                if (store.getAccessToken() == jwt) {
                    val previous = PushDeviceRuntime.registeredToken()
                    PushDeviceRuntime.save(token, account, tag)
                    if (previous != null && previous != token) withTimeoutOrNull(3_000) { AppContainer.memberApi.unregisterDevice(previous) }
                }
                return
            }
            if (attempt < 2) delay((attempt + 1) * 2_000L)
        }
    }
}

actual suspend fun clearPushRegistration() {
    if (!AppContainer.isInitialized) return
    val token = PushDeviceRuntime.registeredToken()
    val tag = AppContainer.secureTokenStore.getAccessToken()?.let(PushDeviceRuntime::sessionTag)
    PushDeviceRuntime.clear(tag) // Immediate, before waiting for any registration or network call.
    withTimeoutOrNull(6_000) { PushDeviceRuntime.mutex.withLock {
        if (token != null) withTimeoutOrNull(3_000) { AppContainer.memberApi.unregisterDevice(token) }
        if (BuildConfig.FIREBASE_ENABLED) withTimeoutOrNull(3_000) {
            suspendCancellableCoroutine<Unit> { continuation ->
                val messaging = runCatching { FirebaseMessaging.getInstance() }.getOrNull()
                if (messaging == null) continuation.resume(Unit)
                else messaging.deleteToken().addOnCompleteListener { if (continuation.isActive) continuation.resume(Unit) }
            }
        }
    } }
}
