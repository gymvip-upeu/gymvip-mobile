package com.gymvip.mobile.presentation.permission

import androidx.compose.runtime.Composable

interface NativePermissionRequester {
    fun requestAppPermissions()
    fun isCameraGranted(): Boolean
    fun isNotificationGranted(): Boolean
}

@Composable
expect fun rememberNativePermissionRequester(
    onPermissionsResult: (grantedPermissions: Map<String, Boolean>) -> Unit = {}
): NativePermissionRequester

