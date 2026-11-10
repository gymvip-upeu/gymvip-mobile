package pe.edu.upeu.gymvip.presentation.screen.preferences

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import pe.edu.upeu.gymvip.presentation.component.*
import pe.edu.upeu.gymvip.presentation.screen.auth.TerminosActivacion
import pe.edu.upeu.gymvip.presentation.screen.catalog.MarcoComunidad
import pe.edu.upeu.gymvip.presentation.viewmodel.PreferencesViewModel

@Composable
fun PreferencesScreen(viewModel: PreferencesViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var legal by remember { mutableStateOf(false) }
    MarcoComunidad("Preferencias", "Elige cómo quieres usar tu app.", onBack) {
        item { PanelSocio {
            Text("Tema", style = MaterialTheme.typography.titleLarge)
            listOf("DARK" to "Oscuro", "LIGHT" to "Claro", "SYSTEM" to "Sistema").forEach { (value, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(state.currentTheme == value, { viewModel.setTheme(value) })
                    TextButton({ viewModel.setTheme(value) }) { Text(label) }
                }
            }
        } }
        item { PanelSocio {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Avisos de vencimiento en Inicio")
                    Text("Mostrar el aviso emergente al entrar con una membresía próxima a vencer o vencida.",
                        style = MaterialTheme.typography.bodyMedium) }
                Switch(state.expiryPopups, viewModel::setExpiryPopups)
            }
            Text("Esta preferencia cambia solo los avisos dentro de la app.", style = MaterialTheme.typography.labelSmall)
        } }
        item { BotonSecundario("Términos y política de privacidad", { legal = true }) }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
    }
    if (legal) TerminosActivacion { legal = false }
}
