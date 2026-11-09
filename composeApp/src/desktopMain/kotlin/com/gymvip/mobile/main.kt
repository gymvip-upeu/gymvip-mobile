package com.gymvip.mobile

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.gymvip.mobile.data.local.DatabaseDriverFactory
import com.gymvip.mobile.data.local.PlatformSecureTokenStore
import com.gymvip.mobile.di.AppContainer

fun main() = application {
    val driverFactory = DatabaseDriverFactory()
    val secureStore = PlatformSecureTokenStore()
    AppContainer.initialize(driverFactory, secureStore)

    val windowState = rememberWindowState(width = 420.dp, height = 860.dp)
    Window(
        onCloseRequest = ::exitApplication,
        title = "GYM VIP — App Móvil",
        state = windowState,
        resizable = true
    ) {
        App()
    }
}

