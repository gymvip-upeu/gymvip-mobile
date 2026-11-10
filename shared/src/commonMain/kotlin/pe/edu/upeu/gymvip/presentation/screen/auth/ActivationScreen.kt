package pe.edu.upeu.gymvip.presentation.screen.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import pe.edu.upeu.gymvip.presentation.component.*
import pe.edu.upeu.gymvip.presentation.viewmodel.AuthViewModel

@Composable
fun ActivationScreen(viewModel: AuthViewModel, onBackToLogin: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    if (!state.mustChangePassword) {
        LoginScreen(viewModel, onNavigateToActivation = {}, onBack = onBackToLogin, activacion = true)
        return
    }
    var temporal by remember { mutableStateOf("") }
    var nueva by remember { mutableStateOf("") }
    var confirmacion by remember { mutableStateOf("") }
    var acepta by remember { mutableStateOf(false) }
    var terminos by remember { mutableStateOf(false) }
    val criterios = listOf(nueva.length >= 12, nueva.any { it in 'A'..'Z' },
        nueva.any { it in 'a'..'z' }, nueva.any { it in '0'..'9' })
    MarcoAcceso({ viewModel.logout(); onBackToLogin() }) {
        EncabezadoPantalla("Crea tu contraseña",
            "Elige una contraseña personal para completar tu primer ingreso.", "ACTIVA TU ACCESO · PASO 2 DE 2")
        Spacer(Modifier.height(24.dp))
        if (state.needsTemporaryPassword) {
            CampoTexto(temporal, { temporal = it }, "Contraseña temporal", esPassword = true, habilitado = !state.isLoading)
            Spacer(Modifier.height(12.dp))
        }
        CampoTexto(nueva, { nueva = it; viewModel.clearMessages() }, "Nueva contraseña",
            esPassword = true, habilitado = !state.isLoading)
        Spacer(Modifier.height(12.dp))
        Text("De 12 a 72 caracteres, con mayúsculas, minúsculas y un número.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(progress = { criterios.count { it } / 4f }, modifier = Modifier.fillMaxWidth())
        Text(if (criterios.all { it }) "Cumple los requisitos de formato" else "Completa los requisitos de tu contraseña",
            style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 8.dp))
        CampoTexto(confirmacion, { confirmacion = it; viewModel.clearMessages() }, "Confirma tu contraseña",
            esPassword = true, habilitado = !state.isLoading)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Top) {
            Checkbox(acepta, { acepta = it; viewModel.clearMessages() }, enabled = !state.isLoading)
            Text("Acepto los términos y condiciones, la política de privacidad y el envío de avisos por WhatsApp y la app.",
                Modifier.weight(1f).padding(top = 12.dp), style = MaterialTheme.typography.bodyMedium)
        }
        TextButton({ terminos = true }) { Text("Leer términos y privacidad") }
        state.errorMessage?.let { AvisoAcceso(it); Spacer(Modifier.height(12.dp)) }
        BotonPrimario("Activar mi acceso",
            { viewModel.activateAccount(temporal, nueva, confirmacion, acepta) },
            cargando = state.isLoading, habilitado = nueva.isNotBlank() && confirmacion.isNotBlank() && acepta &&
                (!state.needsTemporaryPassword || temporal.isNotBlank()))
    }
    if (terminos) TerminosActivacion { terminos = false }
}
