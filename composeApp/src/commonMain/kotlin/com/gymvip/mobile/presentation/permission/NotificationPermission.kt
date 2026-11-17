package com.gymvip.mobile.presentation.permission

import androidx.compose.runtime.Composable

enum class NotificationPermissionStatus {
    CAN_REQUEST,
    DISABLED,
    ENABLED,
    UNAVAILABLE
}

data class NotificationPermission(
    val status: NotificationPermissionStatus,
    val request: () -> Unit = {},
    val openSettings: () -> Unit = {}
)

@Composable
expect fun rememberNotificationPermission(): NotificationPermission
