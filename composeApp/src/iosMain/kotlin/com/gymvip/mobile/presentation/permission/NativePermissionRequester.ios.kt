package com.gymvip.mobile.presentation.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberNativePermissionRequester(
    onPermissionsResult: (grantedPermissions: Map<String, Boolean>) -> Unit
): NativePermissionRequester {
    return remember {
        object : NativePermissionRequester {
            override fun requestAppPermissions() {
                onPermissionsResult(mapOf("CAMERA" to true, "POST_NOTIFICATIONS" to true))
            }

            override fun isCameraGranted(): Boolean = true

            override fun isNotificationGranted(): Boolean = true
        }
    }
}

