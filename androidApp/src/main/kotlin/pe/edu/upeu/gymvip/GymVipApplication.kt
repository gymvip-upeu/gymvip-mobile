package pe.edu.upeu.gymvip

import android.app.Application
import pe.edu.upeu.gymvip.data.local.DatabaseDriverFactory
import pe.edu.upeu.gymvip.data.local.PlatformSecureTokenStore
import pe.edu.upeu.gymvip.di.AppContainer

class GymVipApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppConfig.debug = BuildConfig.DEBUG
        AppConfig.versionName = BuildConfig.VERSION_NAME
        AppConfig.apiBaseUrl = if (BuildConfig.DEBUG) BuildConfig.API_BASE_URL_DEBUG else AppConfig.API_BASE_URL_PROD
        val driverFactory = DatabaseDriverFactory(this)
        val secureStore = PlatformSecureTokenStore(this)
        AppContainer.initialize(driverFactory, secureStore)
    }
}
