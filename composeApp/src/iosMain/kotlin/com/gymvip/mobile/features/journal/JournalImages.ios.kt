package com.gymvip.mobile.features.journal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap

@Composable
actual fun rememberJournalImageService(onMessage: (String) -> Unit): JournalImageService = remember(onMessage) {
    object : JournalImageService {
        override suspend fun render(card: JournalShareCard): ByteArray? = null
        override suspend fun save(bytes: ByteArray) = "No disponible en esta plataforma"
        override suspend fun share(bytes: ByteArray) = "No disponible en esta plataforma"
        override fun pickPhoto(onResult: (String?) -> Unit) { onResult(null); onMessage("No disponible en esta plataforma") }
        override fun takePhoto(onResult: (String?) -> Unit) { onResult(null); onMessage("No disponible en esta plataforma") }
    }
}
actual fun decodeJournalPreview(bytes: ByteArray): ImageBitmap? = null
