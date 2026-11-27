package com.gymvip.mobile.features.training

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.data.remote.api.getPlatformHttpEngine
import com.gymvip.mobile.presentation.screen.catalog.decodeCatalogThumbnail
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CancellationException
import kotlinx.io.readByteArray

private const val MAX_IMAGE_BYTES = 2_097_152L

internal fun isExerciseMediaUrl(raw: String): Boolean = runCatching {
    val url = Url(raw)
    url.protocol == URLProtocol.HTTPS && url.host.isNotBlank() &&
        url.user == null && url.password == null
}.getOrDefault(false)

@Composable
internal fun ExerciseImage(exercise: ExerciseDto) {
    val url = exercise.imageUrl?.takeIf(::isExerciseMediaUrl) ?: return
    val bitmap by produceState<ImageBitmap?>(null, url) {
        value = try {
            HttpClient(getPlatformHttpEngine()) {
                followRedirects = false
                install(HttpTimeout) { requestTimeoutMillis = 8_000 }
            }.use { client ->
                client.prepareGet(url).execute { response ->
                    if (!response.status.isSuccess() || response.contentType()?.contentType != ContentType.Image.Any.contentType) {
                        return@execute null
                    }
                    val bytes = response.bodyAsChannel().readRemaining(MAX_IMAGE_BYTES + 1).readByteArray()
                    if (bytes.size <= MAX_IMAGE_BYTES) decodeCatalogThumbnail(bytes) else null
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = exercise.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().heightIn(max = 210.dp)
        )
    }
}
