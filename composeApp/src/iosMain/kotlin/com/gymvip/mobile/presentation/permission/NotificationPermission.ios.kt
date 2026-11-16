package com.gymvip.mobile.presentation.permission

import androidx.compose.runtime.Composable

@Composable
actual fun rememberNotificationPermission(): NotificationPermission =
    NotificationPermission(NotificationPermissionStatus.UNAVAILABLE)
