package com.gymvip.mobile.presentation.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.data.remote.dto.AccountDeletionResultDto
import com.gymvip.mobile.di.AppContainer
import com.gymvip.mobile.domain.repository.AuthRepository
import com.gymvip.mobile.domain.repository.ConfigRepository
import com.gymvip.mobile.domain.repository.MemberRepository
import com.gymvip.mobile.presentation.theme.DangerRed
import com.gymvip.mobile.presentation.theme.SuccessGreen
import com.gymvip.mobile.presentation.theme.accentForeground
import com.gymvip.mobile.presentation.theme.vipOutlinedTextFieldColors
import com.gymvip.mobile.presentation.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

@Composable
internal fun AccountSecurityScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onChangePassword: () -> Unit,
    onAccountDeleted: () -> Unit = {},
    authRepository: AuthRepository = AppContainer.authRepository,
    memberRepository: MemberRepository = AppContainer.memberRepository,
    configRepository: ConfigRepository = AppContainer.configRepository
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var isRevokingSessions by remember { mutableStateOf(false) }
    var sessionRevocationMessage by remember { mutableStateOf<String?>(null) }
    var biometricEnabled by remember { mutableStateOf(false) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteReason by remember { mutableStateOf("") }
    var isDeletingAccount by remember { mutableStateOf(false) }
    var deletionResult by remember { mutableStateOf<AccountDeletionResultDto?>(null) }
    var deletionError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        biometricEnabled = configRepository.getBooleanPreference("biometric_lock_enabled", false)
    }

    SettingsPage(title = "Cuenta y seguridad", onBack = onBack) {
        // Sección 1: Credenciales de acceso
        SettingsSection("Credenciales") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Outlined.Email, contentDescription = null, tint = MaterialTheme.colorScheme.accentForeground)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Correo electrónico", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = state.profile?.email?.takeIf { it.isNotBlank() }
                            ?: if (state.isLoading) "Cargando correo…" else "Correo no disponible",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingsRow("Cambiar contraseña", Icons.Outlined.Lock, onChangePassword)
        }

        // Sección 2: Gestión de sesiones activas en el servidor
        SettingsSection("Sesiones y dispositivos") {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Outlined.Devices, contentDescription = null, tint = MaterialTheme.colorScheme.accentForeground)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cerrar otras sesiones activas", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Invalida de inmediato en el servidor todas las sesiones abiertas en otros teléfonos o navegadores, conservando activo únicamente este dispositivo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (sessionRevocationMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SuccessGreen.copy(alpha = 0.12f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = sessionRevocationMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = SuccessGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isRevokingSessions = true
                            sessionRevocationMessage = null
                            val res = authRepository.revokeOtherSessions()
                            isRevokingSessions = false
                            sessionRevocationMessage = if (res.isSuccess) {
                                "Otras sesiones cerradas exitosamente en el servidor."
                            } else {
                                "No se pudieron cerrar otras sesiones. Verifique su conexión."
                            }
                        }
                    },
                    enabled = !isRevokingSessions,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.accentForeground)
                ) {
                    if (isRevokingSessions) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.padding(start = 8.dp))
                        Text("Revocando sesiones en servidor...")
                    } else {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.padding(start = 8.dp))
                        Text("Cerrar sesiones en otros dispositivos")
                    }
                }
            }
        }

        // Sección 3: Seguridad local del dispositivo (Biometría desacoplada)
        SettingsSection("Protección local en este dispositivo") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Outlined.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.accentForeground)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Bloqueo biométrico de aplicación", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Solicitar huella o biometría local al abrir la app. Esta protección es exclusiva de este teléfono y está desacoplada de la sesión en el servidor.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = biometricEnabled,
                    onCheckedChange = { isChecked ->
                        biometricEnabled = isChecked
                        scope.launch {
                            configRepository.setBooleanPreference("biometric_lock_enabled", isChecked)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    )
                )
            }
        }

        // Sección 4: Protección de Datos Personales (LPDP Ley 29733)
        SettingsSection("Privacidad y Ley N° 29733") {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Outlined.PersonRemove, contentDescription = null, tint = DangerRed)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Solicitar supresión de datos y cuenta",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = DangerRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Conforme a la Ley de Protección de Datos Personales (Ley N° 29733), puede ejercer su derecho de cancelación. Al registrar la solicitud, sus accesos y credenciales QR se revocarán de inmediato y se emitirá un código de expediente formal con plazo legal de atención de 10 días hábiles. Los comprobantes de pago se conservan bajo exigencia tributaria SUNAT.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                ) {
                    Text("Ejercer derecho de supresión de cuenta")
                }
            }
        }
    }

    // Diálogo de confirmación para solicitud de eliminación de cuenta
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeletingAccount) showDeleteDialog = false },
            icon = { Icon(Icons.Outlined.Security, contentDescription = null, tint = DangerRed) },
            title = {
                Text(
                    text = "Confirmar solicitud de supresión",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Esta acción revocará inmediatamente sus credenciales, códigos QR dinámicos y sesiones en todos los dispositivos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Se generará un código oficial de expediente conforme al marco legal de la Ley N° 29733 (plazo de resolución: 10 días hábiles).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = deleteReason,
                        onValueChange = { deleteReason = it },
                        label = { Text("Motivo de la solicitud (opcional)") },
                        placeholder = { Text("Ej: No resido en la zona / No utilizo el servicio") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = vipOutlinedTextFieldColors()
                    )

                    if (deletionError != null) {
                        Text(
                            text = deletionError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = DangerRed
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isDeletingAccount = true
                            deletionError = null
                            val reason = deleteReason.trim().ifBlank { "Solicitud voluntaria del titular" }
                            val res = memberRepository.requestAccountDeletion(reason)
                            isDeletingAccount = false
                            if (res.isSuccess) {
                                showDeleteDialog = false
                                deletionResult = res.getOrNull()
                            } else {
                                deletionError = res.exceptionOrNull()?.message ?: "Error al procesar la solicitud en el servidor."
                            }
                        }
                    },
                    enabled = !isDeletingAccount,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DangerRed,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    if (isDeletingAccount) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onError)
                        Spacer(modifier = Modifier.padding(start = 6.dp))
                        Text("Procesando...")
                    } else {
                        Text("Confirmar y Revocar")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !isDeletingAccount
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de constancia formal emitida de eliminación de cuenta
    deletionResult?.let { result ->
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = "Constancia de Solicitud de Supresión",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Código de Expediente:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = result.trackingCode,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.accentForeground
                            )
                            Text(
                                text = "Plazo Legal de Atención: ${result.legalTerm}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Text(
                        text = "Su sesión se cerrará de manera definitiva ahora.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        deletionResult = null
                        onAccountDeleted()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Entendido y Salir")
                }
            }
        )
    }
}
