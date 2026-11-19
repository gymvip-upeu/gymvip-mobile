package com.gymvip.mobile.presentation.screen.catalog

import androidx.compose.ui.graphics.ImageBitmap

internal expect fun decodeCatalogThumbnail(bytes: ByteArray): ImageBitmap?
