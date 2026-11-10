package pe.edu.upeu.gymvip.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import pe.edu.upeu.gymvip.domain.model.*
import pe.edu.upeu.gymvip.presentation.viewmodel.SocioUiState
import androidx.compose.ui.unit.dp

@Composable
fun PanelSocio(modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = contenido)
    }
}

@Composable
fun BannerSocio(state: SocioUiState, actualizar: () -> Unit) {
    val data = state.datos ?: return
    val anterior = state.error != null
    val fondo = if (anterior) Color(0xFF33260F) else Color(0xFF173221)
    val texto = if (anterior) Color(0xFFF2A835) else Color(0xFF86EFAC)
    Surface(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = !state.cargando, onClick = actualizar),
        shape = MaterialTheme.shapes.medium, color = fondo, contentColor = texto) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Refresh, "Actualizar membresía y pagos", Modifier.size(18.dp))
            Text((when {
                state.cargando -> "Actualizando datos"
                state.sinConexion -> "Sin conexión · Datos guardados"
                anterior -> "Datos anteriores · No se pudo actualizar"
                else -> "Membresía y pagos actualizados"
            }) + " · " + tiempoSincronizacion(data.actualizadoEn, state.ahora), Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall)
            Icon(Icons.Outlined.ChevronRight, null, Modifier.size(16.dp))
        }
    }
}

@Composable
fun ErrorSocio(state: SocioUiState, actualizar: () -> Unit) {
    state.error?.let { EstadoError(it, actualizar) }
}
