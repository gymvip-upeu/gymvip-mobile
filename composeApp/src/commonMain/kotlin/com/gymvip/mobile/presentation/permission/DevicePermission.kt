package com.gymvip.mobile.presentation.permission

import androidx.compose.runtime.Composable

enum class DevicePermissionStatus {
    CAN_REQUEST,
    DISABLED,
    ENABLED,
    UNAVAILABLE
}

data class DevicePermission(
    val status: DevicePermissionStatus,
    val request: () -> Unit = {},
    val openSettings: () -> Unit = {}
)

@Composable
expect fun rememberNotificationPermission(): DevicePermission

@Composable
internal expect fun rememberCameraPermission(): DevicePermission
