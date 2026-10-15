package com.gymvip.mobile

import android.app.Application
import com.gymvip.mobile.data.local.DatabaseDriverFactory
import com.gymvip.mobile.data.local.PlatformSecureTokenStore
import com.gymvip.mobile.di.AppContainer

class GymVipApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val driverFactory = DatabaseDriverFactory(this)
        val secureStore = PlatformSecureTokenStore(this)
        AppContainer.initialize(driverFactory, secureStore)
    }
}
