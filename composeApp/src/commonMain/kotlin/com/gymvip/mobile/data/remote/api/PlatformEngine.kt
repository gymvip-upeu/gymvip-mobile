package com.gymvip.mobile.data.remote.api

import io.ktor.client.engine.HttpClientEngine

expect fun getPlatformHttpEngine(): HttpClientEngine
