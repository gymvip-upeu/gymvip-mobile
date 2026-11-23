package com.gymvip.mobile.notification

/** Refreshes the device token after authentication, even if Firebase did not rotate it. */
expect suspend fun syncPushRegistration()
expect suspend fun clearPushRegistration()
