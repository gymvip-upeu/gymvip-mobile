package com.gymvip.mobile.presentation.screen.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.presentation.viewmodel.AuthViewModel
import com.gymvip.mobile.presentation.theme.vipOutlinedTextFieldColors

@Composable
fun FirstPasswordScreen(viewModel: AuthViewModel) {
    val state by viewModel.uiState.collectAsState()
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(Modifier.height(32.dp))
            Icon(Icons.Outlined.Lock, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
            Text("VIP CENTER FIT", style = MaterialTheme.typography.labelLarge)
            Text("Crea tu nueva contraseña", style = MaterialTheme.typography.headlineLarge)
            Text("Este es tu primer acceso. Crea una contraseña propia para activar tu cuenta y consultar tu información.", style = MaterialTheme.typography.bodyLarge)
            state.accessEmail?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            FirstPasswordField("Nueva contraseña", password, state.isLoading) { password = it }
            FirstPasswordField("Confirmar nueva contraseña", confirmation, state.isLoading) { confirmation = it }
            Text("Usa al menos 12 caracteres, mayúsculas, minúsculas y números. Debe ser distinta de la temporal.", style = MaterialTheme.typography.bodySmall)
            Text("La contraseña temporal vence 24 horas después de su emisión. Si venció, solicita otra invitación a Recepción.", style = MaterialTheme.typography.bodySmall)
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = { viewModel.createFirstPassword(password, confirmation) },
                enabled = !state.isLoading && password.isNotBlank() && confirmation.isNotBlank(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(if (state.isLoading) "Guardando…" else "Crear mi contraseña")
            }
            TextButton(onClick = viewModel::logout, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesión") }
        }
    }
}

@Composable
private fun FirstPasswordField(label: String, value: String, loading: Boolean, onChange: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        enabled = !loading, modifier = Modifier.fillMaxWidth(), colors = vipOutlinedTextFieldColors(),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = { IconButton(onClick = { visible = !visible }) {
            Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (visible) "Ocultar $label" else "Mostrar $label")
        } })
}
