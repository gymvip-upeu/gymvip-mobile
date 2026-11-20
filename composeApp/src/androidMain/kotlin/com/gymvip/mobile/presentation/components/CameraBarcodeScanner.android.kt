package com.gymvip.mobile.presentation.components

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
actual fun CameraBarcodeScanner(
    modifier: Modifier,
    scanFormat: BarcodeScanFormat,
    torchEnabled: Boolean,
    isScanningEnabled: Boolean,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentOnScanned by rememberUpdatedState(onBarcodeScanned)
    val currentScanningEnabled by rememberUpdatedState(isScanningEnabled)

    var camera by remember { mutableStateOf<Camera?>(null) }
    var lastScannedValue by remember { mutableStateOf<String?>(null) }
    var lastScanTimestamp by remember { mutableStateOf(0L) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val barcodeScanner: BarcodeScanner = remember(scanFormat) {
        val options = when (scanFormat) {
            BarcodeScanFormat.QR_CODE -> {
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
            }
            BarcodeScanFormat.ALL_FORMATS -> {
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE,
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_CODE_128,
                        Barcode.FORMAT_CODE_39
                    )
                    .build()
            }
        }
        BarcodeScanning.getClient(options)
    }

    // Control dinámico de la linterna (torch)
    LaunchedEffect(torchEnabled, camera) {
        camera?.cameraControl?.enableTorch(torchEnabled)
    }

    DisposableEffect(barcodeScanner, cameraExecutor) {
        onDispose {
            barcodeScanner.close()
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(
                            imageProxy = imageProxy,
                            scanner = barcodeScanner,
                            isScanningEnabled = currentScanningEnabled,
                            onScanned = { rawCode ->
                                val now = System.currentTimeMillis()
                                // Antirrebote de 1.5 segundos para el mismo código o 600ms entre códigos distintos
                                val isSameCode = rawCode == lastScannedValue
                                val debounceLimit = if (isSameCode) 1500L else 600L
                                if (now - lastScanTimestamp > debounceLimit) {
                                    lastScannedValue = rawCode
                                    lastScanTimestamp = now
                                    ContextCompat.getMainExecutor(ctx).execute {
                                        currentOnScanned(rawCode)
                                    }
                                }
                            }
                        )
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )
    }
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    scanner: BarcodeScanner,
    isScanningEnabled: Boolean,
    onScanned: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null && isScanningEnabled) {
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue
                    if (!rawValue.isNullOrBlank()) {
                        onScanned(rawValue.trim())
                        break
                    }
                }
            }
            .addOnFailureListener {
                // Falla de procesamiento de frame puntual, continuar con el siguiente
            }
            .addOnCompleteListener {
                // OBLIGATORIO: cerrar imageProxy para evitar fuga y congelamiento del flujo CameraX
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}
