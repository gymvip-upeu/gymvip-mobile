package pe.edu.upeu.gymvip.presentation.screen.inbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import pe.edu.upeu.gymvip.domain.model.fechaPagoSocio
import pe.edu.upeu.gymvip.domain.model.fechaAvisoSocio
import pe.edu.upeu.gymvip.presentation.component.*
import pe.edu.upeu.gymvip.presentation.navigation.AlVolver
import pe.edu.upeu.gymvip.presentation.screen.catalog.MarcoComunidad
import pe.edu.upeu.gymvip.presentation.viewmodel.ComunidadViewModel

@Composable
fun AvisosComunidadScreen(vm: ComunidadViewModel) {
    val s by vm.state.collectAsState()
    var seleccionado by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { vm.cargarAvisos() }
    AlVolver(seleccionado != null) { seleccionado = null }
    val aviso = s.datos.avisos.firstOrNull { it.id == seleccionado }
    MarcoComunidad(if (seleccionado == null) "Avisos" else "Detalle del aviso", "Novedades y comunicaciones para tu cuenta.",
        if (seleccionado != null) {{ seleccionado = null }} else null, s.cargandoAvisos, vm::cargarAvisos) {
        s.errorAvisos?.let { item { pe.edu.upeu.gymvip.presentation.screen.catalog.ErrorCargaComunidad(it, s.sinRedAvisos, s.datos.avisos.isNotEmpty(), vm::cargarAvisos) } }
        if (aviso != null) item { PanelSocio {
            Text(aviso.title, style = MaterialTheme.typography.headlineMedium)
            Text(fechaPagoSocio(aviso.createdAt), color = MaterialTheme.colorScheme.primary)
            Text(aviso.message)
            if (!aviso.isRead) TextButton({ vm.leer(aviso.id) }, enabled = !s.guardando) { Text("Marcar como leído") }
        } } else {
            if (s.cargandoAvisos && s.datos.avisos.isEmpty()) item { Esqueleto() }
            if (!s.cargandoAvisos && s.errorAvisos == null && s.datos.avisos.isEmpty()) item { EstadoVacio("No tienes avisos por ahora.") }
            s.datos.avisos.forEach { a -> item(key = a.id) { PanelSocio(Modifier
                .semantics { stateDescription = if (a.isRead) "Leído" else "Sin leer" }
                .clickable { seleccionado = a.id; vm.leer(a.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!a.isRead) Box(Modifier.size(7.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(fechaAvisoSocio(a.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(a.title, style = MaterialTheme.typography.titleMedium)
                        Text(a.message, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } } }
        }
        item { Text("“Leído” solo refleja la apertura en esta app; no equivale a entrega o lectura por WhatsApp o correo.",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
