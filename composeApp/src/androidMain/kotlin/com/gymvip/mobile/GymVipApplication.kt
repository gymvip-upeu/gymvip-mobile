package com.gymvip.mobile

import android.app.Application
import com.gymvip.mobile.config.FirebaseRemoteConfigProvider
import com.gymvip.mobile.config.SafeRemoteConfig
import com.gymvip.mobile.data.local.DatabaseDriverFactory
import com.gymvip.mobile.data.local.PlatformSecureTokenStore
import com.gymvip.mobile.data.remote.api.GymVipHttpClient
import com.gymvip.mobile.di.AppContainer
import com.gymvip.mobile.notification.GymVipFirebaseMessagingService
import com.gymvip.mobile.telemetry.FirebaseCrashReporter
import com.gymvip.mobile.telemetry.FirebaseNetworkTracker
import com.gymvip.mobile.telemetry.GlobalNetworkTracker
import com.gymvip.mobile.telemetry.SafeCrashLogger

open class GymVipApplication : Application() {
    protected open fun createClientEngine(): io.ktor.client.engine.HttpClientEngine? = null
    override fun onCreate() {
        super.onCreate()

        // 1. Inicialización de Observabilidad y Telemetría Segura (Crashlytics & Perf)
        if (BuildConfig.FIREBASE_ENABLED && com.google.firebase.FirebaseApp.initializeApp(this) != null) try {
            SafeCrashLogger.initialize(FirebaseCrashReporter())
            GlobalNetworkTracker.tracker = FirebaseNetworkTracker()
            SafeRemoteConfig.provider = FirebaseRemoteConfigProvider()
            GymVipFirebaseMessagingService.createNotificationChannel(this)
        } catch (e: Throwable) {
            println("[APP-INIT] Error configurando Firebase en inicio: ${e.message}")
        }

        // 2. Inicialización de Cliente HTTP y Contenedores Locales
        GymVipHttpClient.baseUrl = BuildConfig.DEFAULT_SERVER_URL
        com.gymvip.mobile.config.RuntimeSettings.receptionWhatsAppNumber = BuildConfig.RECEPTION_WHATSAPP
        val driverFactory = DatabaseDriverFactory(this)
        val secureStore = PlatformSecureTokenStore(this)
        AppContainer.initialize(driverFactory, secureStore, createClientEngine())
        com.gymvip.mobile.notification.PushDeviceRuntime.initialize(this)
        GymVipFirebaseMessagingService.createNotificationChannel(this)
    }
}
