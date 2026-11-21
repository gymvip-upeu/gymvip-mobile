package com.gymvip.mobile.notification

import com.google.firebase.messaging.FirebaseMessaging
import com.gymvip.mobile.di.AppContainer
import com.gymvip.mobile.telemetry.SafeCrashLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual suspend fun syncPushRegistration() {
    val store = AppContainer.secureTokenStore
    if (store.getAccessToken().isNullOrBlank() || !store.getAccountRoles().contains("CLIENTE")) return
    val token = suspendCancellableCoroutine<String?> { continuation ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
            .addOnFailureListener { if (continuation.isActive) continuation.resume(null) }
    }
    if (token.isNullOrBlank()) return
    repeat(3) { attempt ->
        val result = AppContainer.memberApi.registerDevice(token)
        if (result.isSuccess) return
        if (attempt < 2) delay((attempt + 1) * 2_000L)
    }
    SafeCrashLogger.logBreadcrumb("FCM", "Registro de dispositivo pendiente de reintento")
}
