package pe.edu.upeu.gymvip.presentation.screen.membership

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.edu.upeu.gymvip.domain.model.*
import pe.edu.upeu.gymvip.presentation.component.*
import pe.edu.upeu.gymvip.presentation.viewmodel.SocioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembershipScreen(viewModel: SocioViewModel, onOpenPayments: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val data = state.datos
    var condicionesAbiertas by rememberSaveable { mutableStateOf(false) }
    PullToRefreshBox(isRefreshing = state.cargando, onRefresh = viewModel::refresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { EncabezadoPantalla("Mi membresía", "Tu estado y vigencia registrados por recepción.") }
            if (state.error != null) item { ErrorSocio(state, viewModel::refresh) }
            if (data != null) item { BannerSocio(state, viewModel::refresh) }
            if (data == null && state.cargando) item { Esqueleto() }
            if (data != null) {
                item { MembershipStatusCard(data.membresia) }
                data.membresia?.let { membresia ->
                    item { PanelSocio {
                        FilaDetalle("Inicio del período", fechaCortaSocio(membresia.startDate))
                        HorizontalDivider()
                        FilaDetalle("Último día válido", fechaCortaSocio(membresia.endDate))
                        HorizontalDivider()
                        FilaDetalle("Duración del plan", membresia.durationDays?.let { "$it días exactos" } ?: "No informada")
                        HorizontalDivider()
                        FilaDetalle("Condición", "Pago presencial")
                        if (!membresia.benefits.isNullOrBlank() || !membresia.conditions.isNullOrBlank()) {
                            TextButton({ condicionesAbiertas = !condicionesAbiertas }) {
                                Text(if (condicionesAbiertas) "Ocultar condiciones del plan" else "Condiciones del plan")
                            }
                            if (condicionesAbiertas) {
                                membresia.benefits?.takeIf { it.isNotBlank() }?.let { Text("Beneficios: $it") }
                                membresia.conditions?.takeIf { it.isNotBlank() }?.let { Text("Condiciones: $it") }
                            }
                        }
                    } }
                }
                item { BotonSecundario("Historial de pagos · Importes y medios registrados", onOpenPayments) }
                if (data.renovaciones.size > 1) {
                    item { Text("Historial de periodos", style = MaterialTheme.typography.titleLarge) }
                    data.renovaciones.forEach { periodo -> item(key = periodo.id) { PanelSocio {
                        Text(periodo.plan, style = MaterialTheme.typography.titleMedium)
                        FilaDetalle("Inicio", fechaCortaSocio(periodo.inicio))
                        FilaDetalle("Último día válido", fechaCortaSocio(periodo.ultimoDia))
                        periodo.importe?.let { FilaDetalle("Precio", it.amountInSoles) }
                        Text(when { periodo.cancelado -> "Cancelado"; periodo.pagado -> "Pago registrado"; else -> "Sin pago confirmado" },
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } } }
                }
            }
        }
    }
}
