package com.gymvip.mobile.presentation.screen.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.domain.repository.LegalConsentRepository
import com.gymvip.mobile.presentation.theme.GoldPrimary
import kotlinx.coroutines.launch

@Composable
fun TermsAndPrivacyScreen(
    repository: LegalConsentRepository,
    accountId: String,
    onBackOrAccepted: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var termsAccepted by remember { mutableStateOf(false) }
    var privacyAcknowledged by remember { mutableStateOf(false) }
    var whatsappConsent by remember { mutableStateOf(false) }

    var isSubmitting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    LaunchedEffect(accountId) {
        val res = repository.fetchConsent(accountId)
        res.onSuccess {
            termsAccepted = it.termsAccepted
            privacyAcknowledged = it.privacyAcknowledged
            whatsappConsent = it.whatsappConsent
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CONDICIONES LEGALES",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Términos y Privacidad",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            TextButton(onClick = onBackOrAccepted) {
                Text(text = "Cerrar", color = GoldPrimary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tarjeta de Términos
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "1. Términos y Condiciones de Uso del Portal Móvil",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "GYM VIP ofrece esta aplicación como canal oficial de consulta para socios activos e inscritos. " +
                            "El código QR generado en la aplicación constituye un identificador dinámico personal e intransferible " +
                            "para verificación en el mostrador de recepción y no garantiza acceso físico por sí mismo. " +
                            "Todo pago, renovación y cancelación de contratos se procesa exclusivamente en forma presencial en las instalaciones autorizadas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = termsAccepted,
                        onCheckedChange = { termsAccepted = it },
                        colors = CheckboxDefaults.colors(checkedColor = GoldPrimary)
                    )
                    Text(
                        text = "He leído y acepto los Términos y Condiciones del servicio (Obligatorio)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tarjeta de Privacidad
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "2. Política de Privacidad y Tratamiento de Datos",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Sus datos personales (nombre, documento, historial de pagos y registros de asistencia) se almacenan en servidores " +
                            "seguros conforme a la Ley de Protección de Datos Personales. La aplicación emplea cifrado Keystore/Keychain para credenciales y " +
                            "nunca almacena contraseñas en texto plano. Los datos locales en caché se sincronizan exclusivamente con su cuenta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = privacyAcknowledged,
                        onCheckedChange = { privacyAcknowledged = it },
                        colors = CheckboxDefaults.colors(checkedColor = GoldPrimary)
                    )
                    Text(
                        text = "Constancia de lectura del Aviso de Privacidad (Obligatorio)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tarjeta de Consentimiento Opcional de Comunicaciones (WhatsApp)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "3. Comunicaciones y Recordatorios de Vencimiento (Opcional)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Autorizo al gimnasio a enviarme notificaciones de vencimiento y avisos operativos mediante WhatsApp oficial. " +
                            "Este consentimiento es opcional y puede ser revocado en cualquier momento desde esta sección sin afectar su consulta de membresía.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = whatsappConsent,
                        onCheckedChange = { whatsappConsent = it },
                        colors = CheckboxDefaults.colors(checkedColor = GoldPrimary)
                    )
                    Text(
                        text = "Acepto recibir recordatorios de vencimiento por WhatsApp",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (statusMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = statusMessage ?: "",
                color = if (isError) MaterialTheme.colorScheme.error else GoldPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (!termsAccepted || !privacyAcknowledged) {
                    statusMessage = "Debe marcar la aceptación de Términos y el Aviso de Privacidad para continuar."
                    isError = true
                    return@Button
                }
                coroutineScope.launch {
                    isSubmitting = true
                    statusMessage = null
                    val res = repository.submitConsent(accountId, termsAccepted, privacyAcknowledged, whatsappConsent)
                    isSubmitting = false
                    res.fold(
                        onSuccess = {
                            statusMessage = "¡Consentimientos registrados exitosamente en el servidor!"
                            isError = false
                            onBackOrAccepted()
                        },
                        onFailure = {
                            statusMessage = "Guardado localmente. Se sincronizará al restablecer conexión."
                            isError = false
                            onBackOrAccepted()
                        }
                    )
                }
            },
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
            } else {
                Text(text = "Guardar y Continuar", color = Color.Black, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

