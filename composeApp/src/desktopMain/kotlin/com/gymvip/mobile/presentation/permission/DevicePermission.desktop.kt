package com.gymvip.mobile.presentation.permission

import androidx.compose.runtime.Composable

@Composable
actual fun rememberNotificationPermission(): DevicePermission =
    DevicePermission(DevicePermissionStatus.UNAVAILABLE)

@Composable
internal actual fun rememberCameraPermission(): DevicePermission =
    DevicePermission(DevicePermissionStatus.UNAVAILABLE)
