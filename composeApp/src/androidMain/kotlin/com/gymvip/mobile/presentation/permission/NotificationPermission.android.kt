package com.gymvip.mobile.presentation.permission

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner

@Composable
actual fun rememberNotificationPermission(): NotificationPermission {
    val context = LocalContext.current
    val view = LocalView.current
    val preferences = remember(context) {
        context.getSharedPreferences("notification_permission", Context.MODE_PRIVATE)
    }
    fun currentStatus(): NotificationPermissionStatus = when {
        context.getSystemService(NotificationManager::class.java).areNotificationsEnabled() ->
            NotificationPermissionStatus.ENABLED
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !preferences.getBoolean("requested", false) -> NotificationPermissionStatus.CAN_REQUEST
        else -> NotificationPermissionStatus.DISABLED
    }

    var status by remember(context) { mutableStateOf(currentStatus()) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        preferences.edit().putBoolean("requested", true).apply()
        status = currentStatus()
    }
    val settings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        status = currentStatus()
    }
    DisposableEffect(view, context) {
        val lifecycle = view.findViewTreeLifecycleOwner()?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) status = currentStatus()
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }

    return NotificationPermission(
        status = status,
        request = {
            if (status == NotificationPermissionStatus.CAN_REQUEST &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ) {
                request.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        openSettings = {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            }
            settings.launch(intent)
        }
    )
}
