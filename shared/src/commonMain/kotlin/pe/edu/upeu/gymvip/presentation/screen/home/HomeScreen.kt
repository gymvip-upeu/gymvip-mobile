package pe.edu.upeu.gymvip.presentation.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import io.ktor.http.encodeURLParameter
import pe.edu.upeu.gymvip.AppConfig
import pe.edu.upeu.gymvip.domain.model.*
import pe.edu.upeu.gymvip.presentation.component.*
import pe.edu.upeu.gymvip.presentation.navigation.MainTab
import pe.edu.upeu.gymvip.presentation.viewmodel.SocioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: SocioViewModel, onNavigateTab: (MainTab) -> Unit,
    onOpenPayments: () -> Unit, avisosSinLeer: Int, mostrarAvisos: Boolean = true,
    promociones: List<Oferta> = emptyList(), onVerPromo: (Oferta) -> Unit = {},
    comercial: pe.edu.upeu.gymvip.presentation.viewmodel.ComunidadViewModel? = null) {
    val state by viewModel.state.collectAsState()
    val data = state.datos
    val uri = LocalUriHandler.current
    PullToRefreshBox(isRefreshing = state.cargando, onRefresh = viewModel::refresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        EncabezadoPantalla(data?.perfil?.firstName?.let { "Hola, $it" } ?: "Hola",
                            etiqueta = saludoSocio(state.ahora))
                    }
                    IconButton({ onNavigateTab(MainTab.INBOX) }) {
                        BadgedBox(badge = { if (avisosSinLeer > 0) Badge(containerColor = MaterialTheme.colorScheme.primary) }) {
                            Icon(Icons.Outlined.Notifications, "Ver avisos, $avisosSinLeer sin leer")
                        }
                    }
                }
            }
            if (data != null) item { BannerSocio(state, viewModel::refresh) }
            if (state.error != null) item { ErrorSocio(state, viewModel::refresh) }
            if (data == null && state.cargando) item { Esqueleto() }
            if (data != null) {
                item { MembershipStatusCard(data.membresia) }
                item {
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccesoRapido(Icons.Outlined.CreditCard, "Mis pagos", if (data.pagos.size == 1) "1 registrado" else "${data.pagos.size} registrados", onOpenPayments, Modifier.weight(1f))
                        AccesoRapido(Icons.Outlined.Notifications, "Avisos",
                            if (avisosSinLeer == 0) "Al día" else "$avisosSinLeer sin leer",
                            { onNavigateTab(MainTab.INBOX) }, Modifier.weight(1f))
                        AccesoRapido(Icons.Outlined.GridView, "Catálogo", "Ver novedades",
                            { onNavigateTab(MainTab.CATALOG) }, Modifier.weight(1f))
                    }
                }
                if (promociones.isNotEmpty()) {
                    item { Text("Para ti", style = MaterialTheme.typography.titleLarge) }
                    promociones.forEach { oferta -> item(key = oferta.id) {
                        pe.edu.upeu.gymvip.presentation.screen.catalog.OfertaCard(oferta, comercial) { onVerPromo(oferta) }
                    } }
                }
            }
        }
    }
    if (mostrarAvisos && state.mostrarAviso && data?.membresia != null) {
        val membresia = data.membresia
        ModalBottomSheet(onDismissRequest = viewModel::cerrarAviso) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(if (membresia.status == MembershipStatus.VENCIDA) "Tu membresía venció" else "Tu membresía vence pronto",
                    style = MaterialTheme.typography.headlineMedium)
                Text("Último día válido: " + fechaCortaSocio(membresia.endDate) + ". Renueva presencialmente en recepción.")
                Text("Puedes seguir consultando tu membresía, pagos, catálogo y avisos.")
                BotonPrimario("Entendido", viewModel::cerrarAviso)
                if (AppConfig.WHATSAPP_GYM.isNotBlank()) BotonSecundario("Escribir por WhatsApp", {
                    uri.openUri("https://wa.me/" + AppConfig.WHATSAPP_GYM.filter(Char::isDigit) + "?text=" +
                        "Hola, quiero información para renovar mi membresía.".encodeURLParameter())
                })
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun AccesoRapido(icono: ImageVector, titulo: String, subtitulo: String, click: () -> Unit, modifier: Modifier) {
    Surface(modifier.fillMaxHeight().heightIn(min = 128.dp).clickable(onClick = click), shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icono, null, tint = MaterialTheme.colorScheme.primary)
            Text(titulo, style = MaterialTheme.typography.titleSmall)
            Text(subtitulo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
