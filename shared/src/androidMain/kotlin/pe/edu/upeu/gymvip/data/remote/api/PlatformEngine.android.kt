package pe.edu.upeu.gymvip.data.remote.api

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO

actual fun getPlatformHttpEngine(): HttpClientEngine {
    return CIO.create()
}
