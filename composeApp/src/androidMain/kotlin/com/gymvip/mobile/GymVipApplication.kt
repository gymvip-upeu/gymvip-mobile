package com.gymvip.mobile

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.gymvip.mobile.config.FirebaseRemoteConfigProvider
import com.gymvip.mobile.config.RuntimeSettings
import com.gymvip.mobile.config.SafeRemoteConfig
import com.gymvip.mobile.data.local.DatabaseDriverFactory
import com.gymvip.mobile.data.local.PlatformSecureTokenStore
import com.gymvip.mobile.data.remote.api.GymVipHttpClient
import com.gymvip.mobile.di.AppContainer
import com.gymvip.mobile.notification.GymVipFirebaseMessagingService
import com.gymvip.mobile.notification.PushDeviceRuntime
import com.gymvip.mobile.telemetry.FirebaseAnalyticsTracker
import com.gymvip.mobile.telemetry.FirebaseCrashReporter
import com.gymvip.mobile.telemetry.FirebaseNetworkTracker
import com.gymvip.mobile.telemetry.GlobalNetworkTracker
import com.gymvip.mobile.telemetry.SafeCrashLogger
import com.gymvip.mobile.telemetry.VipAnalyticsTracker
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

open class GymVipApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    protected open fun createClientEngine(): HttpClientEngine? = null

    override fun onCreate() {
        super.onCreate()

        // 1. Inicialización de Firebase (Crashlytics, Performance, Analytics, App Check y Remote Config)
        if (BuildConfig.FIREBASE_ENABLED && FirebaseApp.initializeApp(this) != null) {
            try {
                val appCheckFactory = if (BuildConfig.DEBUG) {
                    DebugAppCheckProviderFactory.getInstance()
                } else {
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                }
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(appCheckFactory)
                SafeCrashLogger.initialize(FirebaseCrashReporter())
                GlobalNetworkTracker.tracker = FirebaseNetworkTracker()
                VipAnalyticsTracker.tracker = FirebaseAnalyticsTracker(this)
                val remoteConfigProvider = FirebaseRemoteConfigProvider()
                SafeRemoteConfig.provider = remoteConfigProvider
                appScope.launch { remoteConfigProvider.fetchAndActivate() }
            } catch (e: Throwable) {
                println("[APP-INIT] Error configurando Firebase en inicio: ${e.message}")
            }
        }

        // 2. Inicialización de Cliente HTTP, Contenedores Locales y Push vinculado a sesión
        GymVipHttpClient.baseUrl = BuildConfig.DEFAULT_SERVER_URL
        RuntimeSettings.receptionWhatsAppNumber = BuildConfig.RECEPTION_WHATSAPP
        val driverFactory = DatabaseDriverFactory(this)
        val secureStore = PlatformSecureTokenStore(this)
        AppContainer.initialize(driverFactory, secureStore, createClientEngine())
        PushDeviceRuntime.initialize(this)
        GymVipFirebaseMessagingService.createNotificationChannel(this)
    }
}
