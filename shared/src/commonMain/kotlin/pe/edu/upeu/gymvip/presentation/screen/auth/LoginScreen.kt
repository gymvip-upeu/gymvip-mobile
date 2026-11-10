package pe.edu.upeu.gymvip.presentation.screen.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.ktor.http.encodeURLParameter
import pe.edu.upeu.gymvip.AppConfig
import pe.edu.upeu.gymvip.presentation.component.*
import pe.edu.upeu.gymvip.presentation.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: AuthViewModel, onNavigateToActivation: () -> Unit, onBack: () -> Unit,
    activacion: Boolean = false) {
    val state by viewModel.uiState.collectAsState()
    var email by rememberSaveable { mutableStateOf("") }
    // Las contraseñas no se restauran desde SavedState.
    var password by remember { mutableStateOf("") }
    var recordar by rememberSaveable { mutableStateOf(false) }
    var ayuda by remember { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    val focus = LocalFocusManager.current
    fun ingresar() {
        if (!state.isLoading && email.isNotBlank() && password.isNotBlank()) {
            focus.clearFocus()
            viewModel.login(email, password, recordar)
        }
    }
    LaunchedEffect(state.rememberedIdentifier) {
        if (email.isBlank() && state.rememberedIdentifier != null) {
            email = state.rememberedIdentifier.orEmpty(); recordar = true
        }
    }
    MarcoAcceso(onBack) {
        EncabezadoPantalla(if (activacion) "Activa tu acceso" else "Inicia sesión",
            "Usa tu correo de acceso y tu contraseña.",
            if (activacion) "PRIMER INGRESO · PASO 1 DE 2" else "BIENVENIDO DE NUEVO")
        Spacer(Modifier.height(28.dp))
        CampoTexto(email, { email = it; viewModel.clearMessages() }, "Correo electrónico",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }), habilitado = !state.isLoading)
        Spacer(Modifier.height(12.dp))
        CampoTexto(password, { password = it; viewModel.clearMessages() },
            if (activacion) "Contraseña temporal" else "Contraseña", esPassword = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { ingresar() }), habilitado = !state.isLoading)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(recordar, { recordar = it }, enabled = !state.isLoading)
            Text("Recordar mi correo en este dispositivo", style = MaterialTheme.typography.bodyMedium)
        }
        state.errorMessage?.let { AvisoAcceso(it); Spacer(Modifier.height(12.dp)) }
        BotonPrimario(if (activacion) "Continuar" else "Iniciar sesión",
            { ingresar() }, cargando = state.isLoading,
            habilitado = email.isNotBlank() && password.isNotBlank())
        Spacer(Modifier.height(12.dp))
        TextButton({ ayuda = true }, Modifier.fillMaxWidth()) { Text("¿Necesitas ayuda para ingresar?") }
        if (!activacion) TextButton({ viewModel.clearMessages(); onNavigateToActivation() }, Modifier.fillMaxWidth()) {
            Text("Activar mi acceso")
        }
    }
    if (ayuda) ModalBottomSheet(onDismissRequest = { ayuda = false }) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Te ayudamos a ingresar", style = MaterialTheme.typography.headlineSmall)
            Text("Recepción habilita tu acceso y te entrega el correo y la contraseña temporal. " +
                "Si olvidaste tu contraseña o tu cuenta no está habilitada, solicita ayuda en recepción.")
            if (AppConfig.WHATSAPP_GYM.isNotBlank()) BotonPrimario("Contactar por WhatsApp", {
                uri.openUri("https://wa.me/" + AppConfig.WHATSAPP_GYM.filter(Char::isDigit) +
                    "?text=" + "Hola, necesito ayuda para ingresar a GYM VIP.".encodeURLParameter())
            })
            TextButton({ ayuda = false }) { Text("Entendido") }
            Spacer(Modifier.height(12.dp))
        }
    }
}
