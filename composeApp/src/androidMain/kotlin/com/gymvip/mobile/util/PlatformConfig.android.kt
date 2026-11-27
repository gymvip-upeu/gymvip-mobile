package com.gymvip.mobile.util

import com.gymvip.mobile.BuildConfig

actual val isDevelopmentBuild: Boolean = BuildConfig.DEBUG
actual val installedAndroidVersionCode: Int? = BuildConfig.VERSION_CODE
