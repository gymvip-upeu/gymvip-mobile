package pe.edu.upeu.gymvip.presentation.screen.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.edu.upeu.gymvip.AppConfig
import pe.edu.upeu.gymvip.domain.model.*
import pe.edu.upeu.gymvip.presentation.component.*
import pe.edu.upeu.gymvip.presentation.screen.catalog.MarcoComunidad

@Composable
fun ProfileScreen(perfil: MemberProfile?, cargando: Boolean, error: String?, actualizar: () -> Unit,
    preferencias: () -> Unit, ayuda: () -> Unit, salir: () -> Unit) {
    var confirmarSalida by remember { mutableStateOf(false) }
    var codigo by remember { mutableStateOf(false) }
    MarcoComunidad("Mi perfil", "Tu cuenta GYM VIP.", cargando = cargando, refrescar = actualizar) {
        error?.let { item { EstadoError(it, actualizar) } }
        if (perfil == null && cargando) item { Esqueleto() }
        perfil?.let { p ->
            item { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(Modifier.size(64.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary) {
                    Box(contentAlignment = Alignment.Center) { Text((p.firstName.take(1) + p.lastName.take(1)).uppercase()) }
                }
                Column {
                    Text("SOCIO GYM VIP", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                    Text(p.fullName, style = MaterialTheme.typography.headlineMedium)
                    p.memberCode?.takeIf { it.isNotBlank() }?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            } }
            item { PanelSocio {
                FilaDetalle("Correo autorizado", correoEnmascarado(p.email))
                HorizontalDivider()
                FilaDetalle("Celular autorizado", celularEnmascarado(p.phone))
            } }
            item { Text("Para actualizar tus datos personales o contractuales, visita recepción.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { PanelSocio {
                AccionPerfil(Icons.Outlined.Settings, "Preferencias", "Tema, avisos y privacidad", preferencias)
                HorizontalDivider()
                AccionPerfil(Icons.Outlined.QrCode, "Mi código / QR", "Opcional · Por validar", { codigo = true })
                HorizontalDivider()
                AccionPerfil(Icons.Outlined.HelpOutline, "Ayuda y FAQ", "Respuestas frecuentes", ayuda)
                HorizontalDivider()
                AccionPerfil(Icons.Outlined.Logout, "Cerrar sesión", "Salida segura de este dispositivo", { confirmarSalida = true })
            } }
            item { Text("GYM VIP Mobile" + AppConfig.versionName.takeIf(String::isNotBlank)?.let { " · v$it" }.orEmpty(),
                Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
    if (confirmarSalida) AlertDialog(onDismissRequest = { confirmarSalida = false }, title = { Text("Cerrar sesión") },
        text = { Text("Se cerrará tu sesión y se borrarán los datos de esta cuenta del dispositivo.") },
        confirmButton = { TextButton(salir) { Text("Cerrar sesión") } },
        dismissButton = { TextButton({ confirmarSalida = false }) { Text("Cancelar") } })
    if (codigo) AlertDialog(onDismissRequest = { codigo = false }, title = { Text("Mi código / QR") },
        text = { Text("Disponible próximamente. Tu código de socio: " + (perfil?.memberCode ?: "consulta en recepción")) },
        confirmButton = { TextButton({ codigo = false }) { Text("Entendido") } })
}

@Composable
private fun AccionPerfil(icono: androidx.compose.ui.graphics.vector.ImageVector, titulo: String, subtitulo: String, click: () -> Unit) {
    TextButton(click, Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 12.dp)) {
        Icon(icono, titulo); Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) { Text(titulo, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitulo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Icon(Icons.Outlined.ChevronRight, null)
    }
}
