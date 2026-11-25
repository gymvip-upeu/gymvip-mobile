package com.gymvip.mobile.presentation.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.presentation.theme.vipOutlinedTextFieldColors
import com.gymvip.mobile.presentation.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
internal fun ConfirmEmailScreen(viewModel: AuthViewModel, registeredEmail: String, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var sent by rememberSaveable(registeredEmail) { mutableStateOf(false) }
    var remaining by rememberSaveable(registeredEmail) { mutableStateOf(0) }
    LaunchedEffect(viewModel) { viewModel.clearMessages() }
    LaunchedEffect(remaining) { if (remaining > 0) { delay(1000); remaining-- } }
    SettingsPage(title = "Restablecer contraseña", onBack = onBack) {
        Text("Enviaremos un enlace al correo registrado en tu cuenta. No necesitas recordar tu contraseña actual.",
            style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(value = registeredEmail, onValueChange = {}, readOnly = true,
            label = { Text("Correo registrado") }, leadingIcon = { Icon(Icons.Outlined.Email, null) },
            singleLine = true, colors = vipOutlinedTextFieldColors(), modifier = Modifier.fillMaxWidth())
        Text("Este proceso no cambia tu dirección de correo. El enlace vence en 15 minutos y solo puede usarse una vez.",
            style = MaterialTheme.typography.bodyMedium)
        if (sent) Text("Enlace enviado. Revisa tu bandeja de entrada y Spam.", color = MaterialTheme.colorScheme.primary)
        state.errorMessage?.let { Text("No se pudo enviar el enlace. Reintenta o consulta a Recepción.", color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            viewModel.requestOwnPasswordReset { result ->
                if (result.isSuccess) { sent = true; remaining = 60 }
            }
        }, enabled = !state.isLoading && remaining == 0 && registeredEmail.isNotBlank(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(when {
                state.isLoading -> "Enviando…"
                remaining > 0 -> "Reenviar en ${remaining}s"
                state.errorMessage != null -> "Reintentar envío"
                sent -> "Reenviar enlace"
                else -> "Enviar enlace de restablecimiento"
            })
        }
        Text("Al guardar la nueva contraseña se cerrarán todas tus sesiones, incluida la de este dispositivo. Luego deberás iniciar sesión nuevamente.",
            style = MaterialTheme.typography.bodySmall)
    }
}
