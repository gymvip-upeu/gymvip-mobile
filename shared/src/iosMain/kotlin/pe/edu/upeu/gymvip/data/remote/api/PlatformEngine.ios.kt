package pe.edu.upeu.gymvip.data.remote.api

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

actual fun getPlatformHttpEngine(): HttpClientEngine {
    return Darwin.create()
}
