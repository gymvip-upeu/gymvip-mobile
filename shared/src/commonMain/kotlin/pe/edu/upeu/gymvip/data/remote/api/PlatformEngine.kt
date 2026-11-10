package pe.edu.upeu.gymvip.data.remote.api

import io.ktor.client.engine.HttpClientEngine

expect fun getPlatformHttpEngine(): HttpClientEngine
