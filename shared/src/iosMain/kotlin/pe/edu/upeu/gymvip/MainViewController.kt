package pe.edu.upeu.gymvip

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    AppConfig.debug = kotlin.native.Platform.isDebugBinary
    AppConfig.versionName = platform.Foundation.NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: ""
    AppConfig.apiBaseUrl = if (AppConfig.debug) AppConfig.API_IOS_LOCAL else AppConfig.API_BASE_URL_PROD
    pe.edu.upeu.gymvip.di.AppContainer.initialize(
        pe.edu.upeu.gymvip.data.local.DatabaseDriverFactory(),
        pe.edu.upeu.gymvip.data.local.PlatformSecureTokenStore()
    )
    return ComposeUIViewController { App() }
}
