package com.gymvip.mobile.presentation.screen.membership

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.domain.model.MemberCheckInResult
import com.gymvip.mobile.domain.repository.MemberRepository
import com.gymvip.mobile.presentation.components.BarcodeScanFormat
import com.gymvip.mobile.presentation.components.CameraBarcodeScanner
import com.gymvip.mobile.presentation.permission.rememberNativePermissionRequester
import com.gymvip.mobile.presentation.theme.DangerRed
import com.gymvip.mobile.presentation.theme.GoldOnPrimary
import com.gymvip.mobile.presentation.theme.GoldPrimary
import com.gymvip.mobile.presentation.theme.LocalGymVipSystemTheme
import com.gymvip.mobile.presentation.theme.SuccessGreen
import com.gymvip.mobile.presentation.theme.vipOutlinedTextFieldColors
import kotlinx.coroutines.launch

sealed interface CheckInScanUiState {
    data object Idle : CheckInScanUiState
    data object Checking : CheckInScanUiState
    data class Success(val result: MemberCheckInResult) : CheckInScanUiState
    data class AlreadyChecked(val result: MemberCheckInResult) : CheckInScanUiState
    data class Failed(val message: String, val status: String) : CheckInScanUiState
}

@Composable
fun MemberCheckInScannerScreen(
    memberRepository: MemberRepository,
    onBack: () -> Unit
) {
    val followsSystem = LocalGymVipSystemTheme.current
    val accent = if (followsSystem) MaterialTheme.colorScheme.primary else GoldPrimary
    val onAccent = if (followsSystem) MaterialTheme.colorScheme.onPrimary else GoldOnPrimary
    val coroutineScope = rememberCoroutineScope()

    var cameraGranted by remember { mutableStateOf(false) }
    var isTorchEnabled by remember { mutableStateOf(false) }
    val permissionRequester = rememberNativePermissionRequester { results ->
        cameraGranted = results["android.permission.CAMERA"] ?: results["android.Manifest.permission.CAMERA"] ?: false
    }
    LaunchedEffect(Unit) {
        cameraGranted = permissionRequester.isCameraGranted()
    }

    var manualCodeInput by remember { mutableStateOf("") }
    var uiState by remember { mutableStateOf<CheckInScanUiState>(CheckInScanUiState.Idle) }
    var showManualInput by remember { mutableStateOf(false) }

    fun processChallenge(challenge: String) {
        val trimmed = challenge.trim()
        if (trimmed.isBlank()) return

        coroutineScope.launch {
            uiState = CheckInScanUiState.Checking
            val result = memberRepository.checkInWithReceptionQr(trimmed)
            result.fold(
                onSuccess = { res ->
                    when (res.status) {
                        "ATTENDANCE_RECORDED" -> uiState = CheckInScanUiState.Success(res)
                        "ALREADY_REGISTERED" -> uiState = CheckInScanUiState.AlreadyChecked(res)
                        else -> uiState = CheckInScanUiState.Failed(
                            message = res.message.ifBlank { "No fue posible registrar el ingreso." },
                            status = res.status
                        )
                    }
                },
                onFailure = { err ->
                    uiState = CheckInScanUiState.Failed(
                        message = err.message ?: "Error de comunicación con el gimnasio. Verifica tu conexión.",
                        status = "NETWORK_ERROR"
                    )
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
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Volver",
                    tint = accent
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Registrar Asistencia",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Control de acceso en recepción",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
            }
        }

        when (val state = uiState) {
            is CheckInScanUiState.Checking -> {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = accent,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Validando código de recepción...",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Verificando vigencia de membresía y turno de acceso.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            is CheckInScanUiState.Success -> {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SuccessGreen))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = "Ingreso Aprobado",
                                tint = SuccessGreen,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "¡Ingreso Registrado con Éxito!",
                            style = MaterialTheme.typography.titleLarge,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = state.result.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        state.result.memberName?.let { name ->
                            Text(
                                text = "Socio: $name",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        state.result.memberCode?.let { code ->
                            Text(
                                text = "Código: $code",
                                style = MaterialTheme.typography.bodySmall,
                                color = accent
                            )
                        }

                        state.result.attendanceDate?.let { date ->
                            Text(
                                text = "Fecha: $date",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                contentColor = onAccent
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Entendido / Volver al Inicio", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            is CheckInScanUiState.AlreadyChecked -> {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(accent))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Asistencia previa",
                                tint = accent,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Asistencia Ya Registrada",
                            style = MaterialTheme.typography.titleLarge,
                            color = accent,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = state.result.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                contentColor = onAccent
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Volver al Inicio", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            is CheckInScanUiState.Failed -> {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DangerRed))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(DangerRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = "Ingreso Denegado",
                                tint = DangerRed,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No se pudo registrar el ingreso",
                            style = MaterialTheme.typography.titleLarge,
                            color = DangerRed,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    uiState = CheckInScanUiState.Idle
                                    manualCodeInput = ""
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reintentar")
                            }

                            Button(
                                onClick = onBack,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accent,
                                    contentColor = onAccent
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("Volver")
                            }
                        }
                    }
                }
            }

            is CheckInScanUiState.Idle -> {
                // Viewfinder / Instrucciones
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ESCANEO DE ASISTENCIA EN RECEPCIÓN",
                            style = MaterialTheme.typography.labelMedium,
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Apunta la cámara de tu teléfono al código QR que se muestra en la pantalla del mostrador de recepción.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Visor de Cámara Real con CameraX y Google ML Kit
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.Black)
                                .border(2.dp, accent, RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (cameraGranted) {
                                CameraBarcodeScanner(
                                    modifier = Modifier.fillMaxSize(),
                                    scanFormat = BarcodeScanFormat.QR_CODE,
                                    torchEnabled = isTorchEnabled,
                                    isScanningEnabled = (uiState is CheckInScanUiState.Idle),
                                    onBarcodeScanned = { qr ->
                                        processChallenge(qr)
                                    }
                                )

                                // Botón flotante para encender/apagar linterna
                                IconButton(
                                    onClick = { isTorchEnabled = !isTorchEnabled },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp)
                                        .size(40.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isTorchEnabled) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                                        contentDescription = "Linterna",
                                        tint = if (isTorchEnabled) accent else Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.QrCodeScanner,
                                        contentDescription = "Visor QR",
                                        tint = accent,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Permiso de cámara requerido para escanear el QR del mostrador",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DangerRed,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        if (!cameraGranted) {
                            Button(
                                onClick = { permissionRequester.requestAppPermissions() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accent,
                                    contentColor = onAccent
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(46.dp)
                            ) {
                                Icon(Icons.Outlined.CameraAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Conceder Permiso de Cámara", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Alternativa manual o por soporte
                        OutlinedButton(
                            onClick = { showManualInput = !showManualInput },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Text(
                                if (showManualInput) "Ocultar entrada de respaldo" else "¿Problemas con la cámara? Ingresar código"
                            )
                        }

                        AnimatedVisibility(
                            visible = showManualInput,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                                OutlinedTextField(
                                    colors = vipOutlinedTextFieldColors(),
                                    value = manualCodeInput,
                                    onValueChange = { manualCodeInput = it },
                                    label = { Text("Código de desafío QR") },
                                    placeholder = { Text("Ej. gymvip:challenge:v1:...") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { processChallenge(manualCodeInput) }),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = { processChallenge(manualCodeInput) },
                                    enabled = manualCodeInput.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accent,
                                        contentColor = onAccent
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(44.dp)
                                ) {
                                    Text("Validar e Ingresar", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
