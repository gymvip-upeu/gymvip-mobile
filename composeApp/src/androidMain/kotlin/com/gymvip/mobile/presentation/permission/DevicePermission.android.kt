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
actual fun rememberNotificationPermission(): DevicePermission = rememberAndroidPermission(
    preferencesName = "notification_permission",
    permissionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else null,
    isEnabled = { it.getSystemService(NotificationManager::class.java).areNotificationsEnabled() },
    settingsIntent = { context ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else appDetailsIntent(context)
    }
)

@Composable
internal actual fun rememberCameraPermission(): DevicePermission = rememberAndroidPermission(
    preferencesName = "camera_permission",
    permissionName = Manifest.permission.CAMERA,
    isEnabled = { it.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED },
    settingsIntent = ::appDetailsIntent
)

private fun appDetailsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))

@Composable
private fun rememberAndroidPermission(
    preferencesName: String,
    permissionName: String?,
    isEnabled: (Context) -> Boolean,
    settingsIntent: (Context) -> Intent
): DevicePermission {
    val context = LocalContext.current
    val view = LocalView.current
    val preferences = remember(context, preferencesName) {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }
    fun currentStatus(): DevicePermissionStatus = when {
        isEnabled(context) -> DevicePermissionStatus.ENABLED
        permissionName != null &&
            context.checkSelfPermission(permissionName) != PackageManager.PERMISSION_GRANTED &&
            !preferences.getBoolean("requested", false) -> DevicePermissionStatus.CAN_REQUEST
        else -> DevicePermissionStatus.DISABLED
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

    return DevicePermission(
        status = status,
        request = {
            if (status == DevicePermissionStatus.CAN_REQUEST && permissionName != null) {
                request.launch(permissionName)
            }
        },
        openSettings = { settings.launch(settingsIntent(context)) }
    )
}
