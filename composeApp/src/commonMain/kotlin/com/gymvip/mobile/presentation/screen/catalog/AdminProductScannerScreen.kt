package com.gymvip.mobile.presentation.screen.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.data.remote.dto.AdminSupplementDto
import com.gymvip.mobile.domain.repository.CatalogRepository
import com.gymvip.mobile.presentation.components.BarcodeScanFormat
import com.gymvip.mobile.presentation.components.CameraBarcodeScanner
import com.gymvip.mobile.presentation.permission.rememberNativePermissionRequester
import com.gymvip.mobile.presentation.theme.DangerRed
import com.gymvip.mobile.presentation.theme.GoldOnPrimary
import com.gymvip.mobile.presentation.theme.GoldPrimary
import com.gymvip.mobile.presentation.theme.LocalGymVipSystemTheme
import com.gymvip.mobile.presentation.theme.SuccessGreen
import kotlinx.coroutines.launch
import com.gymvip.mobile.presentation.theme.vipOutlinedTextFieldColors

@Composable
fun AdminProductScannerScreen(
    catalogRepository: CatalogRepository,
    userRoles: List<String>,
    onBack: () -> Unit
) {
    val followsSystem = LocalGymVipSystemTheme.current
    val accent = if (followsSystem) MaterialTheme.colorScheme.primary else GoldPrimary
    val accentForeground = if (followsSystem) MaterialTheme.colorScheme.onSurface else GoldPrimary
    val onAccent = if (followsSystem) MaterialTheme.colorScheme.onPrimary else GoldOnPrimary
    val coroutineScope = rememberCoroutineScope()
    val isAuthorized = userRoles.any {
        it.contains("ADMIN", ignoreCase = true) || it.contains("RECEPCION", ignoreCase = true) || it.contains("GERENTE", ignoreCase = true)
    }

    var cameraGranted by remember { mutableStateOf(false) }
    var isTorchEnabled by remember { mutableStateOf(false) }
    var showCameraView by remember { mutableStateOf(true) }
    val permissionRequester = rememberNativePermissionRequester { results ->
        cameraGranted = results["android.permission.CAMERA"] ?: results["android.Manifest.permission.CAMERA"] ?: false
    }
    LaunchedEffect(Unit) {
        cameraGranted = permissionRequester.isCameraGranted()
    }

    var scannedCodeInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var foundProduct by remember { mutableStateOf<AdminSupplementDto?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Estado para edición/asociación de nuevo código
    var isEditingBarcode by remember { mutableStateOf(false) }
    var newBarcodeInput by remember { mutableStateOf("") }

    fun performScan(code: String) {
        val trimmed = code.trim()
        if (trimmed.isBlank()) {
            errorMessage = "Ingresa un código de barras o SKU para buscar"
            return
        }
        // Seguridad: no abrir enlaces URL automáticamente
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            errorMessage = "El código detectado es un enlace URL ($trimmed). Por favor escanee el código de barras comercial o SKU del producto."
            return
        }

        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            successMessage = null
            val result = catalogRepository.scanSupplement(trimmed)
            isLoading = false
            result.fold(
                onSuccess = { prod ->
                    foundProduct = prod
                    isEditingBarcode = false
                    newBarcodeInput = prod.barcode.orEmpty()
                },
                onFailure = { err ->
                    foundProduct = null
                    errorMessage = err.message ?: "No se encontró ningún producto con el código '$trimmed'. Solo personal de administración o recepción puede asociar este código."
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Volver",
                    tint = accentForeground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Escáner de Productos",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Gestión de catálogo y códigos de barra",
                    style = MaterialTheme.typography.labelSmall,
                    color = accentForeground
                )
            }
        }

        if (!isAuthorized) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Acceso restringido",
                        style = MaterialTheme.typography.titleMedium,
                        color = DangerRed,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Esta funcionalidad requiere rol de Administrador o Personal de Recepción autorizado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Column
        }

        // Card de Búsqueda y Escáner (con soporte manual para Desktop y Android)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "LECTOR / BÚSQUEDA DE CÓDIGO",
                    style = MaterialTheme.typography.labelMedium,
                    color = accentForeground,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Ingresa o lee con pistola/cámara el código de barras o SKU del suplemento:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    colors = vipOutlinedTextFieldColors(),
                    value = scannedCodeInput,
                    onValueChange = {
                        scannedCodeInput = it
                        errorMessage = null
                    },
                    label = { Text("Código de barras / SKU") },
                    placeholder = { Text("Ej. 7751234567890 o PROD-WHEY-01") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.QrCodeScanner,
                            contentDescription = "Código",
                            tint = accentForeground
                        )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { performScan(scannedCodeInput) }),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { performScan(scannedCodeInput) },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = onAccent
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = onAccent,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(imageVector = Icons.Outlined.Search, contentDescription = "Buscar")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buscar Producto", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (!cameraGranted) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentForeground),
                        onClick = { permissionRequester.requestAppPermissions() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = "Cámara",
                            tint = accentForeground,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Habilitar Permiso de Cámara para Escaneo Óptico",
                            style = MaterialTheme.typography.bodySmall,
                            color = accentForeground,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CÁMARA / ESCÁNER ÓPTICO",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentForeground,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = { showCameraView = !showCameraView },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (showCameraView) Icons.Outlined.Videocam else Icons.Outlined.VideocamOff,
                                    contentDescription = "Alternar Visor",
                                    tint = accentForeground,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (showCameraView) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black)
                                .border(1.5.dp, accent, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            CameraBarcodeScanner(
                                modifier = Modifier.fillMaxSize(),
                                scanFormat = BarcodeScanFormat.ALL_FORMATS,
                                torchEnabled = isTorchEnabled,
                                isScanningEnabled = !isLoading,
                                onBarcodeScanned = { code ->
                                    scannedCodeInput = code
                                    performScan(code)
                                }
                            )

                            // Botón de Linterna (Torch)
                            IconButton(
                                onClick = { isTorchEnabled = !isTorchEnabled },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(36.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isTorchEnabled) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                                    contentDescription = "Linterna",
                                    tint = if (isTorchEnabled) accent else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mensaje de Error
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DangerRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = errorMessage.orEmpty(),
                    color = DangerRed,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Mensaje de Éxito
        if (successMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SuccessGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Éxito",
                        tint = SuccessGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = successMessage.orEmpty(),
                        color = SuccessGreen,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Resultado: Detalle del Producto Encontrado
        val product = foundProduct
        if (product != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PRODUCTO ENCONTRADO",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentForeground,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (product.displayAvailable) SuccessGreen.copy(alpha = 0.2f) else DangerRed.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (product.displayAvailable) "DISPONIBLE" else "AGOTADO",
                                color = if (product.displayAvailable) SuccessGreen else DangerRed,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    if (!product.brand.isNullOrBlank()) {
                        Text(
                            text = "Marca: ${product.brand}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Precio: S/. ${"%.2f".format(product.displayPrice)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = accentForeground,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Stock: ${product.stock ?: 0} unidades",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (!product.category.isNullOrBlank()) {
                        Text(
                            text = "Categoría: ${product.category}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!product.flavor.isNullOrBlank()) {
                        Text(
                            text = "Sabor: ${product.flavor}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Código de barras actual:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = product.barcode.takeIf { !it.isNullOrBlank() } ?: "Sin código asignado",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (product.barcode.isNullOrBlank()) DangerRed else accentForeground,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            IconButton(onClick = {
                                isEditingBarcode = !isEditingBarcode
                                newBarcodeInput = product.barcode.orEmpty()
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Editar código",
                                    tint = accentForeground
                                )
                            }
                        }
                    }

                    // Panel de Edición/Vinculación de Código de Barras
                    if (isEditingBarcode) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            colors = vipOutlinedTextFieldColors(),
                            value = newBarcodeInput,
                            onValueChange = { newBarcodeInput = it },
                            label = { Text("Nuevo código de barras") },
                            placeholder = { Text("Escanea o escribe el nuevo código") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.QrCode,
                                    contentDescription = "Código",
                                    tint = accentForeground
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = accentForeground),
                                onClick = { isEditingBarcode = false },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancelar")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val newCode = newBarcodeInput.trim()
                                    if (newCode.isBlank()) {
                                        errorMessage = "El código no puede estar vacío"
                                        return@Button
                                    }
                                    coroutineScope.launch {
                                        isLoading = true
                                        errorMessage = null
                                        val res = catalogRepository.updateSupplementBarcode(product.id, newCode)
                                        isLoading = false
                                        res.fold(
                                            onSuccess = { updated ->
                                                foundProduct = updated
                                                isEditingBarcode = false
                                                successMessage = "¡Código de barras actualizado a '$newCode' exitosamente!"
                                            },
                                            onFailure = { err ->
                                                errorMessage = err.message ?: "Error al actualizar código de barras"
                                            }
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accent,
                                    contentColor = onAccent
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Vincular Código", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

