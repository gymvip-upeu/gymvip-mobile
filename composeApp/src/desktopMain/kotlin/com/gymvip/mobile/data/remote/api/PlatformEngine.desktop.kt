package com.gymvip.mobile.data.remote.api

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

actual fun getPlatformHttpEngine(): HttpClientEngine {
    return OkHttp.create()
}

