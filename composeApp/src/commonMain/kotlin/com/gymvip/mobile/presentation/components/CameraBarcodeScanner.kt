package com.gymvip.mobile.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class BarcodeScanFormat {
    /**
     * Solo códigos QR (utilizado para control de acceso y asistencia en recepción)
     */
    QR_CODE,

    /**
     * Todos los formatos comerciales soportados: EAN-13, EAN-8, UPC-A, UPC-E, Code 128, Code 39 y QR
     */
    ALL_FORMATS
}

/**
 * Componente nativo de escaneo por cámara de códigos de barras y QR.
 * En Android utiliza CameraX y Google ML Kit Barcode Scanning.
 */
@Composable
expect fun CameraBarcodeScanner(
    modifier: Modifier = Modifier,
    scanFormat: BarcodeScanFormat = BarcodeScanFormat.QR_CODE,
    torchEnabled: Boolean = false,
    isScanningEnabled: Boolean = true,
    onBarcodeScanned: (String) -> Unit
)
